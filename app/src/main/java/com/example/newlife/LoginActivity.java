package com.example.newlife;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.FirebaseNetworkException;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        try {
            // Инициализация Firebase Auth
            mAuth = FirebaseAuth.getInstance();

            // Инициализация UI элементов
            emailEditText = findViewById(R.id.emailEditText);
            passwordEditText = findViewById(R.id.passwordEditText);
            Button loginButton = findViewById(R.id.loginButton);
            TextView registerTextView = findViewById(R.id.registerTextView);

            // --- Подставляем сохранённые email и пароль ---
            SharedPreferences prefs = getSharedPreferences("habits_prefs", MODE_PRIVATE);
            String savedEmail = prefs.getString("saved_email", "");
            String savedPassword = prefs.getString("saved_password", "");
            if (!savedEmail.isEmpty()) emailEditText.setText(savedEmail);
            if (!savedPassword.isEmpty()) passwordEditText.setText(savedPassword);
            // --- конец блока ---

            // Проверка инициализации UI
            if (emailEditText == null || passwordEditText == null ||
                    loginButton == null || registerTextView == null) {
                throw new IllegalStateException("Не найдены view элементы");
            }

            loginButton.setOnClickListener(v -> attemptLogin());

            registerTextView.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            });

        } catch (Exception e) {
            Log.e(TAG, "Ошибка инициализации", e);
            Toast.makeText(this, "Ошибка инициализации приложения", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void attemptLogin() {
        try {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Пожалуйста, введите email и пароль", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(email, password);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при попытке входа", e);
            Toast.makeText(this, "Неизвестная ошибка", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    try {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user == null) {
                                Toast.makeText(this, "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (!user.isEmailVerified()) {
                                Toast.makeText(this, "Подтвердите email перед входом", Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                                return;
                            }

                            // --- Сохраняем email и пароль ---
                            SharedPreferences prefs = getSharedPreferences("habits_prefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("saved_email", email);
                            editor.putString("saved_password", password);
                            editor.apply();
                            // --- конец блока ---

                            // Загрузка профиля из Firebase Database
                            loadUserProfileAndProceed(user);
                        } else {
                            // Обработка ошибок Firebase
                            String errorMsg = "Ошибка входа";
                            if (task.getException() != null) {
                                if (task.getException() instanceof FirebaseNetworkException) {
                                    errorMsg = "Ошибка сети. Проверьте подключение к интернету и попробуйте снова.";
                                } else {
                                    errorMsg += ": " + task.getException().getMessage();
                                }
                                Log.e(TAG, "Ошибка входа", task.getException());
                            }
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Необработанная ошибка в колбэке", e);
                        Toast.makeText(this, "Критическая ошибка", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserProfileAndProceed(FirebaseUser user) {
        String uid = user.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String photoUrl = snapshot.child("photoUrl").getValue(String.class);
                SharedPreferences prefs = getSharedPreferences("habits_prefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                if (name != null) editor.putString("user_name", name);
                if (photoUrl != null) editor.putString("profile_image_url", photoUrl);

                // --- Загрузка привычек из Firebase ---
                DataSnapshot habitsSnapshot = snapshot.child("habits");
                java.util.List<com.example.newlife.Habit> habitList = new java.util.ArrayList<>();
                for (DataSnapshot habitSnap : habitsSnapshot.getChildren()) {
                    com.example.newlife.Habit habit = habitSnap.getValue(com.example.newlife.Habit.class);
                    if (habit != null) habitList.add(habit);
                }
                // Сохраняем привычки в SharedPreferences (JSON)
                String habitsJson = new com.google.gson.Gson().toJson(habitList);
                editor.putString("habits", habitsJson);
                editor.apply();
                // --- конец загрузки привычек ---

                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                finish();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ошибка загрузки профиля
                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                finish();
            }
        });
    }
}
