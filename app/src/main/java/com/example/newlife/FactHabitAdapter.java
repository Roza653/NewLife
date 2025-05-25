package com.example.newlife;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FactHabitAdapter extends RecyclerView.Adapter<FactHabitAdapter.FactHabitViewHolder> {
    private List<FactHabit> factHabits;
    public interface OnFactHabitClickListener {
        void onFactHabitClick(FactHabit factHabit);
    }

    private OnFactHabitClickListener clickListener;

    public FactHabitAdapter(List<FactHabit> factHabits, OnFactHabitClickListener clickListener) {
        this.factHabits = factHabits;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public FactHabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fact_habit, parent, false);
        return new FactHabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FactHabitViewHolder holder, int position) {
        FactHabit factHabit = factHabits.get(position);
        holder.tvCelebrityName.setText(factHabit.celebrityName);
        holder.tvOriginalHabit.setText(factHabit.originalHabit);
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onFactHabitClick(factHabit);
            }
        });
    }

    @Override
    public int getItemCount() {
        return factHabits.size();
    }

    static class FactHabitViewHolder extends RecyclerView.ViewHolder {
        TextView tvCelebrityName, tvOriginalHabit;
        public FactHabitViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCelebrityName = itemView.findViewById(R.id.tvCelebrityName);
            tvOriginalHabit = itemView.findViewById(R.id.tvOriginalHabit);
        }
    }
} 