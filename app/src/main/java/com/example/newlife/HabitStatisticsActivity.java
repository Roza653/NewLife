package com.example.newlife;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.gson.Gson;
import com.github.mikephil.charting.animation.Easing;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HabitStatisticsActivity extends AppCompatActivity {
    private LineChart habitLineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_statistics);

        habitLineChart = findViewById(R.id.habitLineChart);
        Button btnBackToHome = findViewById(R.id.btnBackToHome);
        btnBackToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HabitStatisticsActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        String habitJson = getIntent().getStringExtra("habit");
        Habit habit = new Gson().fromJson(habitJson, Habit.class);

        // Устанавливаем значения для новых TextView
        TextView tvHabitName = findViewById(R.id.tvHabitName);
        TextView tvHabitDays = findViewById(R.id.tvHabitDays);
        TextView tvHabitTime = findViewById(R.id.tvHabitTime);
        if (habit != null) {
            tvHabitName.setText(habit.getName() != null ? habit.getName() : "-");
            String daysString = habit.getDaysString();
            tvHabitDays.setText("Дни: " + (daysString != null && !daysString.isEmpty() ? daysString : "не выбраны"));
            String timeString = habit.getTimeString();
            tvHabitTime.setText("Время: " + (timeString != null && !timeString.isEmpty() ? timeString : "не указано"));
        } else {
            tvHabitName.setText("-");
            tvHabitDays.setText("Дни: -");
            tvHabitTime.setText("Время: -");
        }

        setupHabitChart(habit);
    }

    private void setupHabitChart(Habit habit) {
        List<Long> completionDates = habit.getCompletionDates();
        if (completionDates == null || completionDates.isEmpty()) {
            Toast.makeText(this, "Нет данных по привычке", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> labels = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM", Locale.getDefault());
        Collections.sort(completionDates);
        for (int i = 0; i < completionDates.size(); i++) {
            entries.add(new Entry(i, 1)); // 1 - выполнено
            labels.add(sdf.format(new Date(completionDates.get(i))));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Выполнено");
        int lineColor = getResources().getColor(R.color.beige_accent);
        int valueTextColor = getResources().getColor(R.color.primaryTextColor);
        int circleColor = getResources().getColor(R.color.beige_secondary);
        int startColor = getResources().getColor(R.color.beige_card);
        int endColor = getResources().getColor(R.color.beige_secondary);
        dataSet.setColor(lineColor);
        dataSet.setValueTextColor(valueTextColor);
        dataSet.setCircleColor(circleColor);
        dataSet.setLineWidth(4f);
        dataSet.setCircleRadius(7f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(180);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{startColor, endColor});
        android.graphics.drawable.Drawable drawable = gd;
        dataSet.setFillDrawable(drawable);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        habitLineChart.setData(lineData);
        habitLineChart.getDescription().setText(""); // Без подписи
        habitLineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        habitLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        habitLineChart.getXAxis().setGranularity(1f);
        habitLineChart.getXAxis().setDrawGridLines(true);
        habitLineChart.getXAxis().setTextColor(android.graphics.Color.parseColor("#333333"));
        habitLineChart.getAxisLeft().setAxisMinimum(0f);
        habitLineChart.getAxisLeft().setTextColor(android.graphics.Color.parseColor("#333333"));
        habitLineChart.getAxisRight().setEnabled(false);
        habitLineChart.getLegend().setEnabled(false);
        habitLineChart.setExtraOffsets(16f, 16f, 16f, 16f);
        habitLineChart.setBackgroundColor(getResources().getColor(R.color.beige_card));
        habitLineChart.animateX(1200, Easing.EaseInOutQuart);
        habitLineChart.invalidate();

        habitLineChart.getAxisLeft().setDrawGridLines(true);
    }
} 