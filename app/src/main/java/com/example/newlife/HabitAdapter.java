package com.example.newlife;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    private List<Habit> habits; // List of habits
    private OnHabitClickListener listener; // Listener for habit interactions
    private boolean showCheckbox; // Flag to control checkbox visibility

    // Interface for handling habit click and checkbox interactions
    public interface OnHabitClickListener {
        void onHabitClick(int position);
        void onHabitChecked(int position, boolean isChecked);
    }

    // Constructor to initialize adapter
    public HabitAdapter(List<Habit> habits, OnHabitClickListener listener, boolean showCheckbox, boolean b) {
        this.habits = habits;
        this.listener = listener;
        this.showCheckbox = showCheckbox;
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habits.get(position);
        holder.habitNameTextView.setText(habit.getName());
        holder.habitTimeTextView.setText(habit.getTimeString());

        // Управляем видимостью чекбокса по showCheckbox
        holder.habitCheckBox.setVisibility(showCheckbox ? View.VISIBLE : View.GONE);

        holder.habitCheckBox.setOnCheckedChangeListener(null);
        holder.habitCheckBox.setChecked(habit.isCompleted());
        holder.habitCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            habit.setCompleted(isChecked);
            if (isChecked) {
                habit.markAsCompletedToday();
            } else {
                habit.markAsNotCompletedToday();
            }
            if (listener != null) {
                int realPosition = holder.getAdapterPosition();
                if (realPosition != RecyclerView.NO_POSITION) {
                    listener.onHabitChecked(realPosition, isChecked);
                }
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHabitClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return habits.size();
    }

    // Update the list of habits and notify the adapter
    public void updateHabits(List<Habit> newHabits) {
        this.habits = newHabits;
        notifyDataSetChanged();
    }

    // ViewHolder class to represent each habit item
    static class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView habitNameTextView;
        TextView habitTimeTextView;
        CheckBox habitCheckBox;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            habitNameTextView = itemView.findViewById(R.id.habitNameTextView);
            habitTimeTextView = itemView.findViewById(R.id.habitTimeTextView);
            habitCheckBox = itemView.findViewById(R.id.habitCheckBox);
        }
    }
}
