package com.example.newlife;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HabitSuggestions {
    private static final String PREF_NAME = "HabitSuggestionsPrefs";
    private static final String KEY_SUGGESTIONS = "saved_habit_names";

    // Default suggestions
    private static final List<String> DEFAULT_SUGGESTIONS = Arrays.asList(
            "Morning run",
            "Reading",
            "Meditation",
            "Drink water",
            "Exercise",
            "Learn language",
            "Journaling",
            "Early bedtime"
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
        List<String> allNames = new ArrayList<>(DEFAULT_SUGGESTIONS);
        allNames.addAll(getSavedHabitNames(context));
        return allNames;
    }
}