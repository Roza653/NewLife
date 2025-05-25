package com.example.newlife;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.widget.TimePicker;
import java.util.List;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnGuestLogin = findViewById(R.id.btnGuestLogin);

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
        btnGuestLogin.setOnClickListener(v -> {
            String email = "individualproject2025@gmail.com";
            String password = "Samsung2025";
            com.google.firebase.auth.FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Сохраняем email гостя
                        getSharedPreferences("habits_prefs", MODE_PRIVATE).edit().putString("user_email", email).apply();
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    } else {
                        android.widget.Toast.makeText(this, "Ошибка входа как гость", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
        });

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                scheduleAllHabitNotifications();
            }
        } else {
            scheduleAllHabitNotifications();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scheduleAllHabitNotifications();
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleAllHabitNotifications() {
        HabitDatabaseHelper dbHelper = new HabitDatabaseHelper(this);
        List<Habit> habits = dbHelper.getAllHabits();
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        for (Habit habit : habits) {
            Intent intent = new Intent(this, NotificationReceiver.class);
            intent.putExtra("habit_name", habit.getName());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                habit.getId(), // уникальный requestCode для каждой привычки
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            // Отменяем старое уведомление для этой привычки
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, habit.getHour());
                calendar.set(Calendar.MINUTE, habit.getMinute());
                calendar.set(Calendar.SECOND, 0);

                // Если время уже прошло сегодня — ставим на завтра
                if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent
                    );
                }
            }
        }
    }
}