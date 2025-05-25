package com.example.newlife;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TimePicker;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Type;
import android.widget.CheckBox;
import androidx.appcompat.widget.Toolbar;
import android.widget.CompoundButton;

public class CreateHabitActivity extends AppCompatActivity {

    private static final String HABITS_PREFS = "HabitsPreferences";
    private static final String HABITS_KEY = "saved_habits";
    
    private AutoCompleteTextView nameAutoCompleteTextView;
    private TimePicker timePicker;
    private ArrayAdapter<String> adapter;
    private CheckBox[] dayCheckBoxes;
    private int editPosition = -1;
    private Habit editingHabit = null;
    private CheckBox allDaysCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_habit);

        initializeViews();
        setupAutoComplete();
        setupSaveButton();

        // Если пришёл habit для редактирования — заполняем поля
        editingHabit = getIntent().getParcelableExtra("habit");
        editPosition = getIntent().getIntExtra("habit_position", -1);
        if (editingHabit != null) {
            fillFieldsForEdit(editingHabit);
        } else {
            // Если пришло имя привычки для автозаполнения (например, из FactsActivity)
            String habitName = getIntent().getStringExtra("habit_name");
            if (habitName != null && !habitName.isEmpty()) {
                nameAutoCompleteTextView.setText(habitName);
                nameAutoCompleteTextView.setSelection(habitName.length());
            }
        }

        Toolbar toolbar = findViewById(R.id.toolbarCreateHabit);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupAllDaysCheckBox();
    }

    private void initializeViews() {
        nameAutoCompleteTextView = findViewById(R.id.nameAutoCompleteTextView);
        timePicker = findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true); // Use 24-hour format
        timePicker.setHour(10); // Set default hour to 10
        timePicker.setMinute(0); // Set default minute to 0

        allDaysCheckBox = findViewById(R.id.allDaysCheckBox);

        dayCheckBoxes = new CheckBox[]{
            findViewById(R.id.mondayCheckBox),
            findViewById(R.id.tuesdayCheckBox),
            findViewById(R.id.wednesdayCheckBox),
            findViewById(R.id.thursdayCheckBox),
            findViewById(R.id.fridayCheckBox),
            findViewById(R.id.saturdayCheckBox),
            findViewById(R.id.sundayCheckBox)
        };
    }

    private void setupAutoComplete() {
        List<String> suggestions = HabitSuggestions.getAllHabitNames(this);
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

        // Собираем дни недели из чекбоксов
        List<Boolean> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            days.add(dayCheckBoxes[i].isChecked());
        }
        // Новая проверка: хотя бы один день должен быть выбран
        boolean atLeastOneDay = false;
        for (boolean checked : days) {
            if (checked) {
                atLeastOneDay = true;
                break;
            }
        }
        if (!atLeastOneDay) {
            // Можно использовать Toast или ошибку на чекбоксах
            android.widget.Toast.makeText(this, "Пожалуйста, выберите хотя бы один день недели!", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        Habit habit;
        if (editingHabit != null) {
            // Обновляем существующий объект
            editingHabit.setName(name);
            editingHabit.setHour(hour);
            editingHabit.setMinute(minute);
            editingHabit.setDays(days);
            habit = editingHabit;
        } else {
            // Создаём новый объект
            habit = new Habit(name, hour, minute, days);
        }

        // Save habit to SharedPreferences
        saveHabitToPreferences(habit);

        // Save the new habit name for future suggestions
        HabitSuggestions.saveHabitName(this, name);

        // Update adapter with new suggestion
        adapter.add(name);
        adapter.notifyDataSetChanged();

        // Return the result as JSON string with key 'new_habit' and position
        Gson gson = new Gson();
        Intent resultIntent = new Intent();
        resultIntent.putExtra("new_habit", gson.toJson(habit));
        resultIntent.putExtra("habit_position", editPosition);
        setResult(Activity.RESULT_OK, resultIntent);
        scheduleNotification(habit);
        finish();
    }

    private void saveHabitToPreferences(Habit habit) {
        SharedPreferences prefs = getSharedPreferences(HABITS_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Get existing habits
        List<Habit> habits = getHabitsFromPreferences();
        if (editPosition >= 0 && editPosition < habits.size()) {
            habits.set(editPosition, habit); // обновляем существующую привычку
        } else {
            habits.add(habit); // добавляем новую
        }
        
        // Convert habits list to JSON
        Gson gson = new Gson();
        String habitsJson = gson.toJson(habits);
        
        // Save updated habits list
        editor.putString(HABITS_KEY, habitsJson);
        editor.apply();
    }

    private List<Habit> getHabitsFromPreferences() {
        SharedPreferences prefs = getSharedPreferences(HABITS_PREFS, Context.MODE_PRIVATE);
        String habitsJson = prefs.getString(HABITS_KEY, "");
        
        if (habitsJson.isEmpty()) {
            return new ArrayList<>();
        }
        
        Gson gson = new Gson();
        Type type = new TypeToken<List<Habit>>(){}.getType();
        return gson.fromJson(habitsJson, type);
    }

    public static class HabitSuggestions {
        private static final String PREF_NAME = "HabitSuggestionsPrefs";
        private static final String KEY_SUGGESTIONS = "saved_habit_names";

        private static final List<String> DEFAULT_SUGGESTIONS = Arrays.asList(
                "Morning run",
                "Reading books",
                "Morning exercises",
                "Meditation",
                "Drinking water",
                "Waking up early",
                "Learning a new language",
                "Daily planning",
                "Walk in fresh air",
                "Giving up sweets",
                "Daily journaling",
                "Morning yoga",
                "Listening to podcasts",
                "Learning programming",
                "Mindfulness practice",
                "House cleaning",
                "Weekly planning",
                "Learning financial literacy",
                "Gratitude practice",
                "Learning new recipes",
                "Watching documentaries",
                "Breathing exercises",
                "Studying history",
                "Budget planning",
                "Stretching practice",
                "Studying art",
                "Watching TED talks",
                "Time management practice",
                "Studying psychology",
                "Writing practice",
                "Studying philosophy",
                "Watching educational videos",
                "Visualization practice",
                "Studying music",
                "Art therapy practice",
                "Studying astronomy",
                "Watching inspirational movies",
                "Affirmation practice",
                "Studying biology",
                "Massage practice",
                "Studying geography",
                "Watching motivational videos",
                "Relaxation practice",
                "Studying chemistry",
                "Gardening practice",
                "Studying physics",
                "Watching science programs",
                "Cooking practice",
                "Studying literature",
                "Photography practice",
                "Бег по утрам",
                "Чтение книги",
                "Зарядка",
                "Медитация",
                "Пить воду",
                "Ранний подъем",
                "Изучение нового языка",
                "Планирование дня",
                "Прогулка на свежем воздухе",
                "Отказ от сладкого",
                "Ежедневный дневник",
                "Утренняя йога",
                "Прослушивание подкастов",
                "Изучение программирования",
                "Практика осознанности",
                "Уборка дома",
                "Планирование недели",
                "Изучение финансовой грамотности",
                "Практика благодарности",
                "Изучение нового рецепта",
                "Просмотр документальных фильмов",
                "Практика дыхательных упражнений",
                "Изучение истории",
                "Планирование бюджета",
                "Практика растяжки",
                "Изучение искусства",
                "Просмотр TED-лекций",
                "Практика тайм-менеджмента",
                "Изучение психологии",
                "Практика письма",
                "Изучение философии",
                "Просмотр образовательных видео",
                "Практика визуализации",
                "Изучение музыки",
                "Практика арт-терапии",
                "Изучение астрономии",
                "Просмотр вдохновляющих фильмов",
                "Практика аффирмаций",
                "Изучение биологии",
                "Практика массажа",
                "Изучение географии",
                "Просмотр мотивационных роликов","Практика релаксации","Изучение химии","Практика садоводства",
                "Изучение физики","Просмотр научных передач","Практика кулинарии","Изучение литературы","Практика фотографии"
        );

        public static List<String> getDefaultHabitNames() {
            return new ArrayList<>(DEFAULT_SUGGESTIONS);
        }

        public static List<String> getSavedHabitNames(Context context) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Set<String> savedNames = prefs.getStringSet(KEY_SUGGESTIONS, new HashSet<>());
            return new ArrayList<>(savedNames);
        }

        public static void saveHabitName(Context context, String name) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Set<String> savedNames = new HashSet<>(getSavedHabitNames(context));
            savedNames.add(name);
            prefs.edit().putStringSet(KEY_SUGGESTIONS, savedNames).apply();
        }

        public static List<String> getAllHabitNames(Context context) {
            List<String> allNames = new ArrayList<>(getDefaultHabitNames());
            allNames.addAll(getSavedHabitNames(context));
            return allNames;
        }
    }

    private void scheduleNotification(Habit habit) {
        List<Boolean> days = habit.getDays();
        if (days == null || days.size() != 7) return;

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
            if (days.get(dayOfWeek)) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, habit.getHour());
                calendar.set(Calendar.MINUTE, habit.getMinute());
                calendar.set(Calendar.SECOND, 0);

                int today = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0 - воскресенье
                int daysUntilAlarm = (dayOfWeek - today + 7) % 7;
                if (daysUntilAlarm == 0 && calendar.before(Calendar.getInstance())) {
                    daysUntilAlarm = 7;
                }
                calendar.add(Calendar.DAY_OF_YEAR, daysUntilAlarm);

                Intent intent = new Intent(this, NotificationReceiver.class);
                intent.putExtra("title", "Привычка: " + habit.getName());
                intent.putExtra("message", "Пора выполнить привычку!");
                intent.putExtra("habit_id", habit.getId());
                intent.putExtra("day_of_week", dayOfWeek);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        habit.getName().hashCode() + dayOfWeek,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                if (alarmManager != null) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                }
            }
        }
    }

    private void fillFieldsForEdit(Habit habit) {
        nameAutoCompleteTextView.setText(habit.getName());
        timePicker.setHour(habit.getHour());
        timePicker.setMinute(habit.getMinute());
        List<Boolean> days = habit.getDays();
        if (days != null && dayCheckBoxes != null) {
            for (int i = 0; i < 7 && i < days.size(); i++) {
                dayCheckBoxes[i].setChecked(days.get(i));
            }
            allDaysCheckBox.setChecked(areAllDaysChecked());
        }
    }

    private void setupAllDaysCheckBox() {
        // При нажатии на 'Все дни' отмечаем/снимаем все чекбоксы дней недели
        allDaysCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (CheckBox cb : dayCheckBoxes) {
                cb.setChecked(isChecked);
            }
        });
        // Если пользователь вручную снимает любой из дней — снимаем 'Все дни'
        for (CheckBox cb : dayCheckBoxes) {
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked && allDaysCheckBox.isChecked()) {
                    allDaysCheckBox.setOnCheckedChangeListener(null);
                    allDaysCheckBox.setChecked(false);
                    allDaysCheckBox.setOnCheckedChangeListener((buttonView2, isChecked2) -> {
                        for (CheckBox cb2 : dayCheckBoxes) {
                            cb2.setChecked(isChecked2);
                        }
                    });
                } else if (areAllDaysChecked() && !allDaysCheckBox.isChecked()) {
                    allDaysCheckBox.setOnCheckedChangeListener(null);
                    allDaysCheckBox.setChecked(true);
                    allDaysCheckBox.setOnCheckedChangeListener((buttonView2, isChecked2) -> {
                        for (CheckBox cb2 : dayCheckBoxes) {
                            cb2.setChecked(isChecked2);
                        }
                    });
                }
            });
        }
    }

    private boolean areAllDaysChecked() {
        for (CheckBox cb : dayCheckBoxes) {
            if (!cb.isChecked()) return false;
        }
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}