package com.example.newlife;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import com.google.gson.Gson;
import android.widget.ImageButton;
import android.net.Uri;
import android.widget.ImageView;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.navigation.NavigationView;
import android.view.View;

public class FactsActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView factsRecyclerView;
    private HabitAdapter habitAdapter;
    private List<Habit> famousHabits = new ArrayList<>();
    private List<Habit> filteredHabits = new ArrayList<>();
    private DatabaseReference databaseReference;
    private AutoCompleteTextView searchAutoComplete;
    private ArrayAdapter<String> autoCompleteAdapter;
    private List<String> allHabitNames = new ArrayList<>();

    private HabitDatabaseHelper dbHelper;

    private static final int PICK_IMAGE_REQUEST = 2001;
    private ImageView ivProfileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facts);

        initializeViews();
        setupNavigation();
        setupRecyclerView();
        setupSearch();
        loadFactsFromFirebase();

        // --- Профиль в Navigation Drawer (если есть) ---
        NavigationView navigationView = findViewById(R.id.navigation_view);
        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            ivProfileImage = headerView.findViewById(R.id.ivProfileImage);
            // Загрузка сохранённого аватара
            String imageUri = getSharedPreferences("habits_prefs", MODE_PRIVATE).getString("profile_image_uri", null);
            if (imageUri != null) {
                ivProfileImage.setImageURI(Uri.parse(imageUri));
            }
            ivProfileImage.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
                } else {
                    pickProfileImage();
                }
            });
        }
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        factsRecyclerView = findViewById(R.id.factsRecyclerView);
        searchAutoComplete = findViewById(R.id.searchAutoComplete);
        dbHelper = new HabitDatabaseHelper(this); // Инициализация базы данных
    }

    private void setupRecyclerView() {
        habitAdapter = new HabitAdapter(filteredHabits, new HabitAdapter.OnHabitClickListener() {
            @Override
            public void onHabitClick(int position) {
                Habit selectedHabit = filteredHabits.get(position);
                String fullName = selectedHabit.getName();
                String habitOnly = fullName;
                int colonIndex = fullName.indexOf(":");
                if (colonIndex != -1 && colonIndex + 1 < fullName.length()) {
                    habitOnly = fullName.substring(colonIndex + 1).trim();
                }
                Intent intent = new Intent(FactsActivity.this, CreateHabitActivity.class);
                intent.putExtra("habit_name", habitOnly);
                startActivityForResult(intent, 1);
            }

            @Override
            public void onHabitChecked(int position, boolean isChecked) {
                // Не используется здесь
            }
        }, false, true); // showCheckbox=false, чекбоксы не отображаются

        factsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        factsRecyclerView.setAdapter(habitAdapter);
    }

    private void setupSearch() {
        searchAutoComplete = findViewById(R.id.searchAutoComplete);
        ImageButton btnSearch = findViewById(R.id.btnSearch);
        autoCompleteAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                allHabitNames
        );
        searchAutoComplete.setAdapter(autoCompleteAdapter);
        searchAutoComplete.setThreshold(1);

        btnSearch.setOnClickListener(v -> {
            String query = searchAutoComplete.getText().toString().trim();
            filterHabits(query);
        });

        searchAutoComplete.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String query = searchAutoComplete.getText().toString().trim();
                filterHabits(query);
                return true;
            }
            return false;
        });

        searchAutoComplete.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Не фильтруем автоматически, только по кнопке или Enter
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            searchAutoComplete.setText(selected);
            filterHabits(selected);
        });
    }

    private void filterHabits(String query) {
        filteredHabits.clear();

        if (query.isEmpty()) {
            filteredHabits.addAll(famousHabits);
        } else {
            String[] words = query.toLowerCase().split("\\s+");
            for (Habit habit : famousHabits) {
                String name = habit.getName().toLowerCase();
                boolean allMatch = true;
                for (String word : words) {
                    if (!name.contains(word)) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    filteredHabits.add(habit);
                }
            }
        }

        habitAdapter.notifyDataSetChanged();

        if (filteredHabits.isEmpty()) {
            Toast.makeText(this, "Совпадений не найдено", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_facts) {
                return true;
            } else if (itemId == R.id.navigation_statistics) {
                startActivity(new Intent(this, StatisticsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.navigation_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.navigation_facts);
    }

    private void loadFactsFromFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference("famous_habits/famous_habits");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                famousHabits.clear();
                allHabitNames.clear();

                for (DataSnapshot habitSnapshot : snapshot.getChildren()) {
                    String name = habitSnapshot.child("name").getValue(String.class);
                    String habit = habitSnapshot.child("habit").getValue(String.class);

                    if (name != null && habit != null) {
                        String fullText = name + ": " + habit;
                        Habit newHabit = new Habit(
                                fullText,
                                10,
                                0,
                                new ArrayList<>(Collections.nCopies(7, false))
                        );
                        famousHabits.add(newHabit);
                        allHabitNames.add(fullText);
                    }
                }

                autoCompleteAdapter.notifyDataSetChanged();
                filteredHabits.addAll(famousHabits);
                habitAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FactsActivity.this, "Ошибка загрузки: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickProfileImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            if (ivProfileImage != null) {
                ivProfileImage.setImageURI(selectedImageUri);
            }
            getSharedPreferences("habits_prefs", MODE_PRIVATE).edit().putString("profile_image_uri", selectedImageUri.toString()).apply();
        }
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String habitJson = data.getStringExtra("new_habit");
            if (habitJson != null) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.putExtra("new_habit", habitJson);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
            finish();
        }
    }
}