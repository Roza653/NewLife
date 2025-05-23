package com.example.newlife;
public class HabitModel {
    private String name;
    private String habit;

    public HabitModel() {}

    public HabitModel(String name, String habit) {
        this.name = name;
        this.habit = habit;
    }

    public String getName() {
        return name;
    }

    public String getHabit() {
        return habit;
    }
}
