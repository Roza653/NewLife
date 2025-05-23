package com.example.newlife;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class HabitOptionsDialogFragment extends DialogFragment {
    public interface HabitOptionsListener {
        void onEditHabit(Habit habit, int position);
        void onDeleteHabit(Habit habit, int position);
        void onShowStatistics(Habit habit, int position);
    }

    private static final String ARG_HABIT = "habit";
    private static final String ARG_POSITION = "position";
    private Habit habit;
    private int position;

    public static HabitOptionsDialogFragment newInstance(Habit habit, int position) {
        HabitOptionsDialogFragment fragment = new HabitOptionsDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_HABIT, habit);
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            habit = getArguments().getParcelable(ARG_HABIT);
            position = getArguments().getInt(ARG_POSITION);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(habit != null ? habit.getName() : "Привычка")
                .setItems(new CharSequence[]{"Изменить", "Удалить", "Статистика"}, (dialog, which) -> {
                    HabitOptionsListener listener = (HabitOptionsListener) getActivity();
                    if (listener != null && habit != null) {
                        if (which == 0) {
                            listener.onEditHabit(habit, position);
                        } else if (which == 1) {
                            listener.onDeleteHabit(habit, position);
                        } else if (which == 2) {
                            listener.onShowStatistics(habit, position);
                        }
                    }
                })
                .setNegativeButton("Отмена", null);
        return builder.create();
    }
} 