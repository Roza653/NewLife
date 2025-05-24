package com.example.newlife;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.widget.Switch;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBarDrawerToggle;

public class ProfileActivity extends AppCompatActivity {
    // Request code for image picker intent
    private static final int PICK_IMAGE_REQUEST = 2002;
    // ImageView for displaying the user's profile photo
    private ImageView ivProfilePhoto;
    // TextView for displaying the user's name
    private TextView tvProfileName;
    // SharedPreferences for storing user data (name, photo)
    private SharedPreferences prefs;
    // Keys for SharedPreferences
    private static final String PREFS = "habits_prefs";
    private static final String USER_NAME_KEY = "user_name";
    private static final String PROFILE_IMAGE_URI = "profile_image_uri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize profile photo and name views
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvProfileName = findViewById(R.id.tvProfileName);
        Button btnChangePhoto = findViewById(R.id.btnChangePhoto);
        Button btnHistory = findViewById(R.id.btnHistory);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnSendFeedback = findViewById(R.id.btnSendFeedback);
        TextView tv = findViewById(R.id.tvProfileTitle);
        tv.setText("Профиль пользователя");

        // Load user name and photo from SharedPreferences
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String userName = prefs.getString(USER_NAME_KEY, "Имя пользователя");
        tvProfileName.setText(userName);

        String imagePath = prefs.getString(PROFILE_IMAGE_URI, null);
        if (imagePath != null) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                ivProfilePhoto.setImageURI(Uri.fromFile(imgFile));
            }
        }

        // Button to change the profile photo
        btnChangePhoto.setOnClickListener(v -> pickProfileImage());
        // Click on profile photo also changes photo
        ivProfilePhoto.setOnClickListener(v -> pickProfileImage());

        // Button to view habit history (заглушка)
        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HabitHistoryActivity.class));
        });

        // Button to logout
        btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // Button to send feedback
        btnSendFeedback.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"newlifeapp12@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Предложение/Жалоба по приложению");
            try {
                startActivity(Intent.createChooser(emailIntent, "Выберите почтовый клиент"));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "Нет установленного почтового клиента", Toast.LENGTH_SHORT).show();
            }
        });

        // Bottom navigation setup for switching between main screens
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_profile) {
                    return true;
                } else if (itemId == R.id.navigation_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                    return true;
                } else if (itemId == R.id.navigation_statistics) {
                    startActivity(new Intent(this, StatisticsActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                    return true;
                } else if (itemId == R.id.navigation_facts) {
                    startActivity(new Intent(this, FactsActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                    return true;
                }
                return false;
            });
            bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
        }

        // DrawerLayout, Toolbar, NavigationView
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Имя и email в header бокового меню
        View headerView = navigationView.getHeaderView(0);
        TextView tvUserName = headerView.findViewById(R.id.tvUserName);
        TextView tvUserEmail = headerView.findViewById(R.id.tvUserEmail);
        tvUserName.setText(userName);
        String userEmail = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";
        tvUserEmail.setText(userEmail != null ? userEmail : "");

        // Email в карточке профиля
        TextView tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileEmail.setText(userEmail != null ? userEmail : "");

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                // Уже находимся в профиле
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HabitHistoryActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            } else if (id == R.id.nav_logout) {
                prefs.edit().clear().apply();
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        tvProfileName.setOnClickListener(v -> {
            // Открыть диалог для изменения имени
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Изменить имя профиля");
            final android.widget.EditText input = new android.widget.EditText(this);
            input.setText(tvProfileName.getText().toString());
            builder.setView(input);
            builder.setPositiveButton("Сохранить", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    tvProfileName.setText(newName);
                    // Сохраняем локально
                    prefs.edit().putString(USER_NAME_KEY, newName).apply();
                    // Сохраняем в Firebase
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
                    userRef.child("name").setValue(newName);
                    // Обновляем имя в nav_header, если есть
                    if (navigationView != null && headerView != null) {
                        tvUserName.setText(newName);
                    }
                    Toast.makeText(this, "Имя профиля обновлено", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Отмена", null);
            builder.show();
        });
    }

    // Handle the result from the image picker
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            try {
                // Копируем изображение во внутреннее хранилище приложения
                String fileName = "profile_photo_" + System.currentTimeMillis() + ".jpg";
                File destFile = new File(getFilesDir(), fileName);
                try (InputStream in = getContentResolver().openInputStream(selectedImageUri);
                     FileOutputStream out = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                // Сохраняем путь к новому файлу
                prefs.edit().putString(PROFILE_IMAGE_URI, destFile.getAbsolutePath()).apply();
                ivProfilePhoto.setImageURI(Uri.fromFile(destFile));
                Toast.makeText(this, "Фото профиля обновлено", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка при сохранении фото", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Animate transition when returning from the profile screen
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void pickProfileImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void loadHabitsFromFirebaseAndShowHistory() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference habitsRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("habits");
        habitsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                StringBuilder sb = new StringBuilder();
                for (DataSnapshot habitSnap : snapshot.getChildren()) {
                    Habit habit = habitSnap.getValue(Habit.class);
                    if (habit != null) {
                        sb.append(habit.getName())
                          .append(" (время: ")
                          .append(habit.getTimeString())
                          .append(")\n");
                    }
                }
                if (sb.length() == 0) sb.append("Нет привычек");
                new android.app.AlertDialog.Builder(ProfileActivity.this)
                    .setTitle("История привычек")
                    .setMessage(sb.toString())
                    .setPositiveButton("OK", null)
                    .show();
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Ошибка загрузки истории", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
