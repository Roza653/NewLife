package com.example.newlife;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HabitHistoryAdapter extends RecyclerView.Adapter<HabitHistoryAdapter.HabitViewHolder> {
    private List<Habit> habitList;

    public interface OnHabitClickListener {
        void onHabitClick(Habit habit, int position);
    }

    private OnHabitClickListener clickListener;

    public void setOnHabitClickListener(OnHabitClickListener listener) {
        this.clickListener = listener;
    }

    public HabitHistoryAdapter(List<Habit> habitList) {
        this.habitList = habitList;
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit_history, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habitList.get(position);
        holder.textViewHabitName.setText(habit.getName());
        holder.textViewHabitDesc.setText(habit.getDescription());
        holder.textViewHabitTime.setText("Время: " + (habit.getTimeString() != null ? habit.getTimeString() : "-"));
        holder.textViewHabitDays.setText("Дни: " + (habit.getDaysString() != null ? habit.getDaysString() : "-"));
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onHabitClick(habit, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView textViewHabitName, textViewHabitDesc, textViewHabitTime, textViewHabitDays;
        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewHabitName = itemView.findViewById(R.id.textViewHabitNameHistory);
            textViewHabitDesc = itemView.findViewById(R.id.textViewHabitDescHistory);
            textViewHabitTime = itemView.findViewById(R.id.textViewHabitTimeHistory);
            textViewHabitDays = itemView.findViewById(R.id.textViewHabitDaysHistory);
        }
    }
} 