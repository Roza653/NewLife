package com.example.newlife;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HabitDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "habits.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_HABITS = "habits";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_HOUR = "hour";
    private static final String COLUMN_MINUTE = "minute";
    private static final String COLUMN_START_DATE = "start_date";
    private static final String COLUMN_COMPLETED = "completed"; // for habit completion
    private static final String TABLE_HABIT_COMPLETIONS = "habit_completions";
    private static final String COLUMN_HABIT_ID = "habit_id";
    private static final String COLUMN_IS_COMPLETED = "is_completed";
    private static final String COLUMN_COMPLETION_DATE = "completion_date";

    public HabitDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_HABIT_COMPLETIONS + " (" +
                COLUMN_HABIT_ID + " INTEGER, " +
                COLUMN_IS_COMPLETED + " INTEGER, " +
                COLUMN_COMPLETION_DATE + " TEXT, " +
                "PRIMARY KEY (" + COLUMN_HABIT_ID + ", " + COLUMN_COMPLETION_DATE + "))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_HABITS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_HOUR + " INTEGER, " +
                COLUMN_MINUTE + " INTEGER, " +
                COLUMN_START_DATE + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_HABIT_COMPLETIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_HABITS);
            onCreate(db);
        }
    }

    // Insert a new habit
    public long insertHabit(String name, int hour, int minute, String startDate) {
        SQLiteDatabase db = getWritableDatabase();
        long habitId = -1;
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME, name);
            values.put(COLUMN_HOUR, hour);
            values.put(COLUMN_MINUTE, minute);
            values.put(COLUMN_START_DATE, startDate);
            habitId = db.insert(TABLE_HABITS, null, values);
        } catch (Exception e) {
            Log.e("HabitDatabaseHelper", "Error inserting habit", e);
        } finally {
            db.close();
        }
        return habitId;
    }
    public boolean habitExists(String name) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    TABLE_HABITS,
                    new String[]{COLUMN_NAME},
                    COLUMN_NAME + " = ?",
                    new String[]{name},
                    null, null, null
            );
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    // Update an existing habit
    public boolean updateHabit(long habitId, String name, int hour, int minute) {
        SQLiteDatabase db = getWritableDatabase();
        boolean success = false;
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME, name);
            values.put(COLUMN_HOUR, hour);
            values.put(COLUMN_MINUTE, minute);

            int rowsAffected = db.update(TABLE_HABITS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(habitId)});
            success = rowsAffected > 0;
        } catch (Exception e) {
            Log.e("HabitDatabaseHelper", "Error updating habit with ID: " + habitId, e);
        } finally {
            db.close();
        }
        return success;
    }

    // Delete a habit
    public boolean deleteHabit(long habitId) {
        SQLiteDatabase db = getWritableDatabase();
        boolean success = false;
        try {
            int rowsDeleted = db.delete(TABLE_HABITS, COLUMN_ID + " = ?", new String[]{String.valueOf(habitId)});
            success = rowsDeleted > 0;
            db.delete(TABLE_HABIT_COMPLETIONS, COLUMN_HABIT_ID + " = ?", new String[]{String.valueOf(habitId)});
        } catch (Exception e) {
            Log.e("HabitDatabaseHelper", "Error deleting habit with ID: " + habitId, e);
        } finally {
            db.close();
        }
        return success;
    }

    // Save habit completion status
    public void saveHabitCompletionStatus(long habitId, boolean isCompleted, String date) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_HABIT_ID, habitId);
            values.put(COLUMN_IS_COMPLETED, isCompleted ? 1 : 0);
            values.put(COLUMN_COMPLETION_DATE, date);

            db.insertWithOnConflict(TABLE_HABIT_COMPLETIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Log.e("HabitDatabaseHelper", "Error saving habit completion status", e);
        } finally {
            db.close();
        }
    }

    // Get habit status for a specific date
    public boolean getHabitStatus(long habitId, String date) {
        SQLiteDatabase db = getReadableDatabase();
        boolean completed = false;

        Cursor cursor = db.query(
                TABLE_HABIT_COMPLETIONS,
                new String[]{COLUMN_IS_COMPLETED},
                COLUMN_HABIT_ID + " = ? AND " + COLUMN_COMPLETION_DATE + " = ?",
                new String[]{String.valueOf(habitId), date},
                null, null, null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                completed = cursor.getInt(0) == 1;
            }
            cursor.close();
        }

        return completed;
    }

    // Get completion count for a habit
    public int getHabitCompletionCount(long habitId) {
        SQLiteDatabase db = getReadableDatabase();
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HABIT_COMPLETIONS + " WHERE " + COLUMN_HABIT_ID + " = ?", new String[]{String.valueOf(habitId)});
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e("HabitDatabaseHelper", "Error counting completions for habit: " + habitId, e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return count;
    }

    // Reset all completion counts (clear completions)
    public void resetAllCompletionCounts() {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.delete(TABLE_HABIT_COMPLETIONS, null, null);
        } catch (Exception e) {
            Log.e("HabitDatabaseHelper", "Error resetting all completion counts", e);
        } finally {
            db.close();
        }
    }

    // Get the streak count for a habit
    public int getHabitStreak(long habitId) {
        SQLiteDatabase db = getReadableDatabase();
        int streak = 0;
        String currentDate = getCurrentDate();

        while (true) {
            Cursor cursor = db.query(
                    TABLE_HABIT_COMPLETIONS,
                    new String[]{COLUMN_IS_COMPLETED},
                    COLUMN_HABIT_ID + " = ? AND " + COLUMN_COMPLETION_DATE + " = ?",
                    new String[]{String.valueOf(habitId), currentDate},
                    null, null, null
            );

            boolean completed = false;
            if (cursor != null && cursor.moveToFirst()) {
                completed = cursor.getInt(0) == 1;
                cursor.close();
            }

            if (!completed) break;

            streak++;
            currentDate = getPreviousDate(currentDate);
        }

        return streak;
    }

    private String getPreviousDate(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(date));
            cal.add(Calendar.DAY_OF_YEAR, -1);
            return sdf.format(cal.getTime());
        } catch (ParseException e) {
            Log.e("HabitDatabaseHelper", "Error parsing date: " + date, e);
            return null; // Handle error more gracefully
        }
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
    // Update the completion status of a habit for a specific date
    public boolean updateHabitCompletionStatus(long habitId, boolean isCompleted, String date) {
        SQLiteDatabase db = getWritableDatabase();
        boolean success = false;

        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_IS_COMPLETED, isCompleted ? 1 : 0);
            values.put(COLUMN_COMPLETION_DATE, date);

            // Update the completion status for the specific habit and date
            int rowsAffected = db.update(
                    TABLE_HABIT_COMPLETIONS,
                    values,
                    COLUMN_HABIT_ID + " = ? AND " + COLUMN_COMPLETION_DATE + " = ?",
                    new String[]{String.valueOf(habitId), date}
            );

            success = rowsAffected > 0; // If any rows were affected, it was a successful update
        } catch (Exception e) {
            Log.e("HabitDatabaseHelper", "Error updating completion status for habit ID: " + habitId, e);
        } finally {
            db.close();
        }

        return success;
    }
    public void updateHabitStatus(long habitId, boolean isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("is_completed", isCompleted ? 1 : 0); // Сохраняем статус как 1 (true) или 0 (false)

        db.update("habits", values, "id = ?", new String[]{String.valueOf(habitId)});
        db.close();
    }

    public List<Habit> getAllHabits() {
        List<Habit> habitList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query("habits", null, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Habit habit = new Habit();
                habit.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                habit.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                habit.setHour(cursor.getInt(cursor.getColumnIndexOrThrow("hour")));
                habit.setMinute(cursor.getInt(cursor.getColumnIndexOrThrow("minute")));
                habitList.add(habit);
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();

        return habitList;
    }
    // Add multiple habits at once
    public boolean addHabits(List<Habit> habits) {
        SQLiteDatabase db = getWritableDatabase();
        boolean success = true;
        db.beginTransaction();

        try {
            for (Habit habit : habits) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME, habit.getName());
                values.put(COLUMN_HOUR, habit.getHour());
                values.put(COLUMN_MINUTE, habit.getMinute());
                values.put(COLUMN_START_DATE, habit.getStartDate());

                // Insert habit into the database
                long habitId = db.insert(TABLE_HABITS, null, values);

                // If the insert fails for any habit, mark success as false
                if (habitId == -1) {
                    success = false;
                    break;
                }
            }

            if (success) {
                db.setTransactionSuccessful(); // Commit the transaction if all inserts were successful
            }
        } catch (Exception e) {
            Log.e("HabitDatabaseHelper", "Error adding habits", e);
            success = false;
        } finally {
            db.endTransaction();
            db.close();
        }

        return success;
    }

    public void addHabit(Habit habit) {
    }
    public void deleteHabit(Habit habit) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete the habit from the database based on the habit's ID
        db.delete(TABLE_HABITS, COLUMN_ID + " = ?", new String[]{String.valueOf(habit.getId())});
        db.close();
    }
}
