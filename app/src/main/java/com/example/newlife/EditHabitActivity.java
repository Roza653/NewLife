package com.example.newlife;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TimePicker;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class EditHabitActivity extends AppCompatActivity {

    private AutoCompleteTextView nameAutoCompleteTextView;
    private TimePicker timePicker;
    private ArrayAdapter<String> adapter;
    private Habit habit;
    private int habitPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_habit); // Use the same layout as create habit

        initializeViews();
        setupAutoComplete();
        setupSaveButton();

        // Get the habit and position from the intent
        habit = getIntent().getParcelableExtra("habit");
        habitPosition = getIntent().getIntExtra("position", -1);

        // Set existing habit details
        if (habit != null) {
            nameAutoCompleteTextView.setText(habit.getName());
            timePicker.setHour(habit.getHour());
            timePicker.setMinute(habit.getMinute());
        }
    }

    private void initializeViews() {
        nameAutoCompleteTextView = findViewById(R.id.nameAutoCompleteTextView);
        timePicker = findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true); // Use 24-hour format
    }

    private void setupAutoComplete() {
        List<String> suggestions = CreateHabitActivity.HabitSuggestions.getAllHabitNames(this);
        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                suggestions
        );
        nameAutoCompleteTextView.setAdapter(adapter);
        nameAutoCompleteTextView.setThreshold(1); // Show suggestions after 1 character
    }

    private void setupSaveButton() {
        Button saveButton = findViewById(R.id.saveHabitButton);
        saveButton.setOnClickListener(v -> saveHabit());
    }

    private void saveHabit() {
        String name = nameAutoCompleteTextView.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(name)) {
            nameAutoCompleteTextView.setError("Please enter a habit name");
            return;
        }

        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        // Update habit details
        habit.setName(name);
        habit.setHour(hour);
        habit.setMinute(minute);

        // Save the updated habit name for future suggestions
        CreateHabitActivity.HabitSuggestions.saveHabitName(this, name);

        // Update adapter with new suggestion
        adapter.add(name);
        adapter.notifyDataSetChanged();

        // Return the result
        Intent resultIntent = new Intent();
        resultIntent.putExtra("habit", habit);
        resultIntent.putExtra("position", habitPosition);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}