package com.example.newlife;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;
import java.util.Collections;
public class Habit implements Parcelable {
    private String category;
    private String time;
    private String description;
    private boolean isCompleted;
    private long id;
    private String name;
    private int hour; // Removed static
    private int minute; // Removed static
    private List<Boolean> days;
    private List<Boolean> completionHistory;
    private long startDate;
    private List<Long> completionDates = new ArrayList<>();
    private int completionCount; // Field to store the completion count
    // Constructor
    public Habit(String name, int hour, int minute, List<Boolean> days) {
        this.id = System.currentTimeMillis(); // Default ID
        this.name = name;
        this.hour = hour;
        this.minute = minute;
        this.days = days != null ? new ArrayList<>(days) : new ArrayList<>();
        this.isCompleted = false;
        this.completionHistory = new ArrayList<>();
        this.startDate = System.currentTimeMillis();
        this.completionCount = 0; // Initialize completion count
    }

    // Parcelable Constructor
    protected Habit(Parcel in) {
        id = in.readLong();
        name = in.readString();
        hour = in.readInt();
        minute = in.readInt();
        isCompleted = in.readByte() != 0;
        days = new ArrayList<>();
        in.readList(days, Boolean.class.getClassLoader());
        completionHistory = new ArrayList<>();
        in.readList(completionHistory, Boolean.class.getClassLoader());
        startDate = in.readLong();
        completionDates = new ArrayList<>();
        in.readList(completionDates, Long.class.getClassLoader());
        completionCount = in.readInt(); // Read completion count from Parcel
    }

    public Habit() {

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeInt(hour);
        dest.writeInt(minute);
        dest.writeByte((byte) (isCompleted ? 1 : 0));
        dest.writeList(days);
        dest.writeList(completionHistory);
        dest.writeLong(startDate);
        dest.writeList(completionDates);
        dest.writeInt(completionCount); // Write completion count to Parcel
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Habit> CREATOR = new Creator<Habit>() {
        @Override
        public Habit createFromParcel(Parcel in) {
            return new Habit(in);
        }

        @Override
        public Habit[] newArray(int size) {
            return new Habit[size];
        }
    };

    // Getters and Setters
    public int getId() {
        return (int) id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getTime() {
        return time;
    }

    // Setter for the time of the habit
    public void setTime(String time) {
        this.time = time;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public List<Boolean> getDays() {
        return new ArrayList<>(days); // Return defensive copy
    }

    public void setDays(List<Boolean> days) {
        this.days = days != null ? new ArrayList<>(days) : new ArrayList<>();
    }
    public String getCategory() {
        return category != null ? category : "General";
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    public List<Boolean> getCompletionHistory() {
        return new ArrayList<>(completionHistory); // Return defensive copy
    }

    public void setCompletionHistory(List<Boolean> completionHistory) {
        this.completionHistory = completionHistory != null ? new ArrayList<>(completionHistory) : new ArrayList<>();
    }

    public long getStartDate() {
        return startDate;
    }

    public List<Long> getCompletionDates() {
        return completionDates != null ? new ArrayList<>(completionDates) : new ArrayList<>();
    }

    public void setCompletionDates(List<Long> completionDates) {
        this.completionDates = completionDates != null ? new ArrayList<>(completionDates) : new ArrayList<>();
    }

    public int getCompletionCount() {
        return completionCount;
    }

    public void setCompletionCount(int completionCount) {
        this.completionCount = completionCount;
    }

    // Utility Methods
    public String getTimeString() {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    public String getDaysString() {
        if (days == null || days.isEmpty()) {
            return "";
        }

        String[] dayNames = DateFormatSymbols.getInstance().getWeekdays();
        List<String> selectedDays = new ArrayList<>();

        for (int i = 0; i < Math.min(days.size(), 7); i++) {
            if (days.get(i) && i + 1 < dayNames.length) {
                selectedDays.add(dayNames[i + 1]);
            }
        }
        return String.join(", ", selectedDays);
    }

    public int getCompletionRate() {
        if (completionHistory == null || completionHistory.isEmpty()) {
            return 0;
        }

        long completedDays = completionHistory.stream().filter(Boolean::booleanValue).count();
        return (int) ((completedDays * 100) / completionHistory.size());
    }

    public int getCurrentStreak() {
        if (completionDates == null || completionDates.isEmpty()) {
            return 0;
        }

        List<Long> sortedDates = new ArrayList<>(completionDates);
        Collections.sort(sortedDates);

        int streak = 0;
        long expectedDate = getStartOfDay(System.currentTimeMillis());
        long oneDayInMillis = 24 * 60 * 60 * 1000;

        for (int i = sortedDates.size() - 1; i >= 0; i--) {
            long date = getStartOfDay(sortedDates.get(i));
            if (date == expectedDate) {
                streak++;
                expectedDate -= oneDayInMillis;
            } else if (date < expectedDate) {
                break;
            }
        }
        return streak;
    }

    public int getMaxStreak() {
        if (completionDates == null || completionDates.isEmpty()) {
            return 0;
        }

        List<Long> sortedDates = new ArrayList<>(completionDates);
        Collections.sort(sortedDates);

        int maxStreak = 0;
        int currentStreak = 1;
        long oneDayInMillis = 24 * 60 * 60 * 1000;

        for (int i = 1; i < sortedDates.size(); i++) {
            long prevDate = getStartOfDay(sortedDates.get(i - 1));
            long currDate = getStartOfDay(sortedDates.get(i));
            if (currDate - prevDate == oneDayInMillis) {
                currentStreak++;
            } else if (currDate != prevDate) {
                maxStreak = Math.max(maxStreak, currentStreak);
                currentStreak = 1;
            }
        }
        maxStreak = Math.max(maxStreak, currentStreak);
        return maxStreak;
    }

    private long getStartOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public void markAsCompletedToday() {
        if (completionDates == null) completionDates = new ArrayList<>();
        if (completionHistory == null) completionHistory = new ArrayList<>();
        long today = getStartOfDay(System.currentTimeMillis());
        if (!completionDates.contains(today)) {
            completionDates.add(today);
            completionHistory.add(true);
            isCompleted = true;
        }
    }

    public void markAsNotCompletedToday() {
        if (completionDates == null) completionDates = new ArrayList<>();
        if (completionHistory == null) completionHistory = new ArrayList<>();
        long today = getStartOfDay(System.currentTimeMillis());
        int index = completionDates.indexOf(today);
        if (index != -1) {
            completionDates.remove(index);
            completionHistory.remove(index);
        }
        isCompleted = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Habit habit = (Habit) o;
        return id == habit.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}