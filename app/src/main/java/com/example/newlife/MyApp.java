package com.example.newlife;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean darkTheme = prefs.getBoolean("dark_theme", false);
        AppCompatDelegate.setDefaultNightMode(
            darkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
} 