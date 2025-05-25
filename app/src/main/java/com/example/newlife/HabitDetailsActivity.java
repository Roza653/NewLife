package com.example.newlife;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.newlife.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class HabitDetailsActivity extends AppCompatActivity {

    private TextView habitNameTextView;
    private TextView habitDescriptionTextView;
    private Button statisticsButton;
    private Button editHabitButton;
    private Button deleteHabitButton;
    private Habit habit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_details);

        habitNameTextView = findViewById(R.id.habitName);
        habitDescriptionTextView = findViewById(R.id.habitDescription);
        statisticsButton = findViewById(R.id.statisticsButton);
        editHabitButton = findViewById(R.id.editHabitButton);
        deleteHabitButton = findViewById(R.id.deleteHabitButton);

        // Получаем данные из Intent
        String habitJson = getIntent().getStringExtra("habit");
        if (habitJson != null) {
            habit = new Gson().fromJson(habitJson, Habit.class);
            habitNameTextView.setText(habit.getName());
            habitDescriptionTextView.setText(habit.getDescription());
        }

        // Обработчики кнопок
        statisticsButton.setOnClickListener(v -> openStatistics());
        editHabitButton.setOnClickListener(v -> editHabit());
        deleteHabitButton.setOnClickListener(v -> deleteHabit());
    }

    // Открыть экран статистики привычки
    private void openStatistics() {
        // Для примера, просто показываем Toast
        Toast.makeText(this, "Statistics for " + habit.getName(), Toast.LENGTH_SHORT).show();
        // Можете заменить это на переход к экрану статистики
    }

    // Редактировать привычку
    private void editHabit() {
        // ВАЖНО: если появится возможность редактировать дни недели, добавьте проверку как в CreateHabitActivity:
        // List<Boolean> days = ... // собрать из чекбоксов
        // boolean atLeastOneDay = false;
        // for (boolean checked : days) { if (checked) { atLeastOneDay = true; break; } }
        // if (!atLeastOneDay) { Toast.makeText(this, "Пожалуйста, выберите хотя бы один день недели!", Toast.LENGTH_SHORT).show(); return; }
        // Здесь откроется экран для редактирования привычки
        Intent intent = new Intent(this, EditHabitActivity.class);
        String habitJson = new Gson().toJson(habit);
        intent.putExtra("habit", habitJson);
        startActivity(intent);
    }

    // Удалить привычку
    private void deleteHabit() {
        // Удаление привычки (например, удаление из базы данных и SharedPreferences)
        // Сначала удалим привычку из базы данных
        deleteHabitFromDatabase(habit);
        // Затем удалим привычку из SharedPreferences
        removeHabitFromPreferences(habit);

        // Переход обратно на главную активность
        Toast.makeText(this, "Habit deleted", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void deleteHabitFromDatabase(Habit habit) {
        // Логика удаления привычки из базы данных
        HabitDatabaseHelper dbHelper = new HabitDatabaseHelper(this);
        dbHelper.deleteHabit(habit);
    }

    private void removeHabitFromPreferences(Habit habit) {
        SharedPreferences sharedPreferences = getSharedPreferences("habits_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = sharedPreferences.getString("habits", "");
        Type type = new TypeToken<List<Habit>>(){}.getType();
        List<Habit> habits = new Gson().fromJson(json, type);
        if (habits != null) {
            habits.remove(habit);
            editor.putString("habits", new Gson().toJson(habits));
            editor.apply();
        }
    }
}
