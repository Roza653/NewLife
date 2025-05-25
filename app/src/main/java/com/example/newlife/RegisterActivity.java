package com.example.newlife;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private ProgressBar progressBar;
    private TextView passwordToggle, confirmPasswordToggle;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);
        passwordToggle = findViewById(R.id.passwordToggle);
        confirmPasswordToggle = findViewById(R.id.confirmPasswordToggle);

        passwordToggle.setOnClickListener(v -> togglePasswordVisibility(passwordEditText, passwordToggle, true));
        confirmPasswordToggle.setOnClickListener(v -> togglePasswordVisibility(confirmPasswordEditText, confirmPasswordToggle, false));

        registerButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            if (!validateInput(name, email, password, confirmPassword)) return;

            registerButton.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            registerUser(name, email, password);
        });
    }

    private void togglePasswordVisibility(EditText editText, TextView toggleView, boolean isPassword) {
        boolean visible = isPassword ? isPasswordVisible : isConfirmPasswordVisible;
        if (visible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleView.setText("👁");
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleView.setText("🙈");
        }
        editText.setSelection(editText.getText().length());
        if (isPassword) isPasswordVisible = !isPasswordVisible;
        else isConfirmPasswordVisible = !isConfirmPasswordVisible;
    }

    private boolean validateInput(String name, String email, String password, String confirmPassword) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Введите корректный email", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Введите пароль", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Пароль должен содержать не менее 6 символов", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void registerUser(String name, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserNameToDatabase(user.getUid(), name);
                            getSharedPreferences("habits_prefs", MODE_PRIVATE).edit().putString("user_email", email).apply();
                            sendEmailVerification(user);
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        registerButton.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void saveUserNameToDatabase(String uid, String name) {
        mDatabase.child("users").child(uid).child("name").setValue(name);
        getSharedPreferences("habits_prefs", MODE_PRIVATE).edit().putString("user_name", name).apply();
    }

    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Регистрация успешна! Подтвердите email для входа.", Toast.LENGTH_LONG).show();
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Ошибка отправки письма: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        registerButton.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }
}
