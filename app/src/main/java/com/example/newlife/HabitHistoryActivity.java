package com.example.newlife;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import androidx.appcompat.widget.Toolbar;

public class HabitHistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private HabitHistoryAdapter adapter;
    private List<Habit> habitList = new ArrayList<>();
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_history);

        recyclerView = findViewById(R.id.recyclerViewHabitHistory);
        progressBar = findViewById(R.id.progressBarHabitHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HabitHistoryAdapter(habitList);
        recyclerView.setAdapter(adapter);

        adapter.setOnHabitClickListener((habit, position) -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Удалить привычку?")
                .setMessage("Вы действительно хотите удалить привычку '" + habit.getName() + "'?")
                .setPositiveButton("Удалить", (dialog, which) -> deleteHabit(habit, position))
                .setNegativeButton("Отмена", null)
                .show();
        });

        loadHabitsFromFirebase();

        Toolbar toolbar = findViewById(R.id.toolbarHabitHistory);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("История привычек");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadHabitsFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference habitsRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("habits");
        habitsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                habitList.clear();
                for (DataSnapshot habitSnap : snapshot.getChildren()) {
                    Habit habit = habitSnap.getValue(Habit.class);
                    if (habit != null) {
                        habitList.add(habit);
                    }
                }
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HabitHistoryActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void deleteHabit(Habit habit, int position) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference habitsRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("habits");
        habitsRef.child(String.valueOf(habit.getId())).removeValue((error, ref) -> {
            if (error == null) {
                habitList.remove(position);
                adapter.notifyItemRemoved(position);
                Toast.makeText(this, "Привычка удалена", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
            }
        });
    }
} 