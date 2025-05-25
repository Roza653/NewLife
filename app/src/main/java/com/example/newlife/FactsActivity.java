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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private SwipeRefreshLayout swipeRefreshLayout;

    private List<Habit> allFirebaseHabits = new ArrayList<>();

    // --- ДОБАВЛЯЮ новые поля для FactHabit ---
    private FactHabitAdapter factHabitAdapter;
    private List<FactHabit> allFactHabits = new ArrayList<>();
    private List<FactHabit> filteredFactHabits = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facts);

        initializeViews();
        setupNavigation();
        setupFactHabitRecyclerView();
        setupSearch();
        loadFactsFromFirebase();

        // --- SwipeRefreshLayout (pull-to-refresh) ---
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadFactsFromFirebase();
            });
        }

        // --- Кнопка обновления списка ---
        ImageButton btnRefresh = findViewById(R.id.btnRefresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> loadFactsFromFirebase());
        }

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

    private void setupFactHabitRecyclerView() {
        factsRecyclerView = findViewById(R.id.factsRecyclerView);
        factHabitAdapter = new FactHabitAdapter(filteredFactHabits, factHabit -> {
            // Открываем CreateHabitActivity и передаём habit_for_list как имя привычки
            Intent intent = new Intent(FactsActivity.this, CreateHabitActivity.class);
            intent.putExtra("habit_name", factHabit.habitForList);
            startActivityForResult(intent, 1);
        });
        factsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        factsRecyclerView.setAdapter(factHabitAdapter);
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
            filterFactHabits(query);
        });

        searchAutoComplete.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String query = searchAutoComplete.getText().toString().trim();
                filterFactHabits(query);
                return true;
            }
            return false;
        });

        searchAutoComplete.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            searchAutoComplete.setText(selected);
            filterFactHabits(selected);
        });
    }

    private void filterFactHabits(String query) {
        filteredFactHabits.clear();
        if (query.isEmpty()) {
            filteredFactHabits.addAll(allFactHabits);
        } else {
            String[] words = query.toLowerCase().split("\\s+");
            for (FactHabit fact : allFactHabits) {
                String name = fact.celebrityName != null ? fact.celebrityName.toLowerCase() : "";
                String orig = fact.originalHabit != null ? fact.originalHabit.toLowerCase() : "";
                String forList = fact.habitForList != null ? fact.habitForList.toLowerCase() : "";
                boolean allMatch = true;
                for (String word : words) {
                    if (!name.contains(word) && !orig.contains(word) && !forList.contains(word)) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) filteredFactHabits.add(fact);
            }
        }
        factHabitAdapter.notifyDataSetChanged();
        if (filteredFactHabits.isEmpty()) {
            Toast.makeText(this, "Совпадений не найдено", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_facts) {
                    return true;
                } else if (itemId == R.id.navigation_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                    return true;
                } else if (itemId == R.id.navigation_statistics) {
                    startActivity(new Intent(this, StatisticsActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                    return true;
                } else if (itemId == R.id.navigation_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();
                    return true;
                }
                return false;
            });
            bottomNavigationView.setSelectedItemId(R.id.navigation_facts);
        }
    }

    private void loadFactsFromFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allFactHabits.clear();
                filteredFactHabits.clear();
                for (DataSnapshot habitSnapshot : snapshot.getChildren()) {
                    String name = habitSnapshot.child("name").getValue(String.class);
                    String originalHabit = habitSnapshot.child("original_habit").getValue(String.class);
                    String habitForList = habitSnapshot.child("habit_for_list").getValue(String.class);
                    if (name != null && originalHabit != null && habitForList != null) {
                        FactHabit factHabit = new FactHabit(name, originalHabit, habitForList);
                        allFactHabits.add(factHabit);
                    }
                }
                // Перемешиваем и берём только 20
                Collections.shuffle(allFactHabits);
                List<FactHabit> twenty = allFactHabits.size() > 20 ? allFactHabits.subList(0, 20) : new ArrayList<>(allFactHabits);
                filteredFactHabits.clear();
                filteredFactHabits.addAll(twenty);
                factHabitAdapter.notifyDataSetChanged();
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(FactsActivity.this, "Список обновлён!", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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

    // --- Обновить autocomplete для поиска ---
    private void updateAutoCompleteNames() {
        allHabitNames.clear();
        for (Habit h : famousHabits) {
            allHabitNames.add(h.getName());
        }
        if (autoCompleteAdapter != null) {
            autoCompleteAdapter.clear();
            autoCompleteAdapter.addAll(allHabitNames);
            autoCompleteAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}