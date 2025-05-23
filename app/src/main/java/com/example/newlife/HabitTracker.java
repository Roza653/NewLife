package com.example.newlife;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class HabitTracker {
    private static final String PREFS_NAME = "HabitPrefs";
    private static final String KEY_LAST_UPDATED = "last_updated";

    private final SharedPreferences sharedPreferences;
    private final SimpleDateFormat dateFormat;

    public HabitTracker(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    /**
     * Saves habit completion status for a specific date
     * @param habitId Unique identifier for the habit
     * @param completed Whether the habit was completed
     */
    public void saveHabitCompletion(String habitId, boolean completed) {
        saveHabitCompletion(dateFormat.format(Calendar.getInstance().getTime()), habitId, completed);
    }

    /**
     * Saves habit completion status for a specific date
     * @param date Date in yyyy-MM-dd format
     * @param habitId Unique identifier for the habit
     * @param completed Whether the habit was completed
     */
    public void saveHabitCompletion(String date, String habitId, boolean completed) {
        Set<String> dailyHabits = new HashSet<>(
                sharedPreferences.getStringSet(date, new HashSet<>())
        );

        if (completed) {
            dailyHabits.add(habitId);
        } else {
            dailyHabits.remove(habitId);
        }

        sharedPreferences.edit()
                .putStringSet(date, dailyHabits)
                .putString(KEY_LAST_UPDATED, date)
                .apply();
    }

    /**
     * Gets the count of completed habits for a specific date
     * @param date Date in yyyy-MM-dd format
     * @return Number of completed habits
     */
    public int getCompletedHabitsCount(String date) {
        return sharedPreferences.getStringSet(date, new HashSet<>()).size();
    }

    /**
     * Gets the count of completed habits for today
     * @return Number of completed habits today
     */
    public int getTodayCompletedHabitsCount() {
        return getCompletedHabitsCount(dateFormat.format(Calendar.getInstance().getTime()));
    }

    /**
     * Gets weekly habit completion data (last 7 days including today)
     * @return Map with dates as keys and completion counts as values
     */
    public Map<String, Integer> getWeeklyData() {
        Map<String, Integer> weeklyData = new LinkedHashMap<>();
        Calendar calendar = Calendar.getInstance();

        // Get data for last 7 days (including today)
        for (int i = 6; i >= 0; i--) {
            calendar.add(Calendar.DAY_OF_YEAR, i == 6 ? 0 : -1);
            String date = dateFormat.format(calendar.getTime());
            weeklyData.put(date, getCompletedHabitsCount(date));
        }

        return weeklyData;
    }

    /**
     * Gets the last date when habits were updated
     * @return Date in yyyy-MM-dd format or empty string if never updated
     */
    public String getLastUpdatedDate() {
        return sharedPreferences.getString(KEY_LAST_UPDATED, "");
    }

    /**
     * Gets all dates with recorded habit data
     * @return List of dates in yyyy-MM-dd format
     */
    public List<String> getAllRecordedDates() {
        List<String> dates = new ArrayList<>(sharedPreferences.getAll().keySet());
        dates.remove(KEY_LAST_UPDATED); // Remove the metadata key
        Collections.sort(dates);
        return dates;
    }

    /**
     * Clears all habit data
     */
    public void clearAllData() {
        sharedPreferences.edit().clear().apply();
    }
}