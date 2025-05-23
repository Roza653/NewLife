package com.example.newlife;

public class Person {
    private String name;
    private String habit;

    public Person(String name, String habit) {
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