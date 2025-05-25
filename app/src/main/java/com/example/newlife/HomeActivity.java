package com.example.newlife;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatDelegate;
import android.widget.ImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import java.io.File;


public class HomeActivity extends AppCompatActivity implements HabitAdapter.OnHabitClickListener, HabitOptionsDialogFragment.HabitOptionsListener {

    private static final String TAG = "HomeActivity";
    private static final String HABITS_PREFS = "habits_prefs";
    private static final String HABITS_KEY = "habits";
    private static final String USER_NAME_KEY = "user_name";
    private static final int PICK_IMAGE_REQUEST = 1001;

    private RecyclerView habitsRecyclerView;
    private HabitAdapter habitAdapter;
    private List<Habit> habits;
    private BottomNavigationView bottomNavigationView;
    private SharedPreferences sharedPreferences;
    private HabitDatabaseHelper dbHelper;
    private Map<Long, Integer> habitCompletionCounts;
    private ImageView ivProfileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // DrawerLayout, Toolbar, NavigationView
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Имя пользователя и аватар в header
        View headerView = navigationView.getHeaderView(0);
        TextView tvUserName = headerView.findViewById(R.id.tvUserName);
        TextView tvUserEmail = headerView.findViewById(R.id.tvUserEmail);
        ivProfileImage = headerView.findViewById(R.id.ivProfileImage);
        String userName = getSharedPreferences(HABITS_PREFS, MODE_PRIVATE).getString(USER_NAME_KEY, "Пользователь");
        tvUserName.setText(userName);
        String userEmail = getSharedPreferences(HABITS_PREFS, MODE_PRIVATE).getString("user_email", "");
        tvUserEmail.setText(userEmail);

        // Загрузка сохранённого аватара
        String imageUri = getSharedPreferences(HABITS_PREFS, MODE_PRIVATE).getString("profile_image_uri", null);
        if (imageUri != null) {
            File imgFile = new File(imageUri);
            if (imgFile.exists()) {
                Glide.with(this)
                    .load(imgFile)
                    .circleCrop()
                    .into(ivProfileImage);
            } else {
                ivProfileImage.setImageResource(R.drawable.ic_person);
            }
        } else {
            ivProfileImage.setImageResource(R.drawable.ic_person);
        }

        ivProfileImage.setOnClickListener(v -> {
            // Запрос разрешения на чтение, если нужно (Android 6-12)
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
            } else {
                pickProfileImage();
            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HabitHistoryActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            } else if (id == R.id.nav_logout) {
                SharedPreferences prefs = getSharedPreferences(HABITS_PREFS, MODE_PRIVATE);
                prefs.edit().clear().apply();
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // Initialize components
        initializeSharedPreferences();
        initializeDatabaseHelper();
        habits = new ArrayList<>();
        setupRecyclerView();
        loadHabitsFromFirebase();
        setupFloatingActionButton();
        setupBottomNavigationView();
        handleNewHabitFromIntent(getIntent());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNewHabitFromIntent(intent);
    }

    private void handleNewHabitFromIntent(Intent intent) {
        if (intent != null && intent.hasExtra("new_habit")) {
            String habitJson = intent.getStringExtra("new_habit");
            if (habitJson != null) {
                Habit newHabit = new Gson().fromJson(habitJson, Habit.class);
                habits.add(newHabit);
                habitAdapter.notifyItemInserted(habits.size() - 1);
                saveHabits();
                dbHelper.addHabit(newHabit);
                Toast.makeText(this, "Привычка добавлена из Facts", Toast.LENGTH_SHORT).show();
                intent.removeExtra("new_habit");
            }
        }
    }

    private void initializeSharedPreferences() {
        try {
            sharedPreferences = getSharedPreferences(HABITS_PREFS, Context.MODE_PRIVATE);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SharedPreferences", e);
            Toast.makeText(this, "Error initializing app settings", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeDatabaseHelper() {
        try {
            dbHelper = new HabitDatabaseHelper(this);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing database", e);
            Toast.makeText(this, "Error initializing database", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupRecyclerView() {
        habitsRecyclerView = findViewById(R.id.habitsRecyclerView);
        if (habitsRecyclerView == null) {
            Log.e(TAG, "RecyclerView not found in layout");
            Toast.makeText(this, "App layout error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        habitsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        habitAdapter = new HabitAdapter(habits, this, true, true);
        habitsRecyclerView.setAdapter(habitAdapter);
        // Анимация появления списка
        habitsRecyclerView.animate().alpha(1f).setDuration(700).start();
    }

    private void setupFloatingActionButton() {
        FloatingActionButton addHabitButton = findViewById(R.id.addHabitButton);
        if (addHabitButton != null) {
            addHabitButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(HomeActivity.this, CreateHabitActivity.class);
                    startActivityForResult(intent, 1);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting CreateHabitActivity", e);
                }
            });
        }
    }

    private void setupBottomNavigationView() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                if (item == null) return false;
                int itemId = item.getItemId();
                animateNavigationItems(itemId);
                if (itemId == R.id.navigation_home) {
                    return true;
                } else if (itemId == R.id.navigation_statistics) {
                    Intent intent = new Intent(this, StatisticsActivity.class);
                    intent.putParcelableArrayListExtra("habits", new ArrayList<>(habits));
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                    return true;
                } else if (itemId == R.id.navigation_facts) {
                    Intent intent = new Intent(this, FactsActivity.class);
                    startActivityForResult(intent, 2);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                } else if (itemId == R.id.navigation_profile) {
                    Intent intent = new Intent(this, ProfileActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    return true;
                }
                return false;
            });
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
    }

    private void animateNavigationItems(int selectedItemId) {
        Menu menu = bottomNavigationView.getMenu();
        if (menu == null) return;

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            View view = bottomNavigationView.findViewById(item.getItemId());
            if (view != null) {
                int animationId = (item.getItemId() == selectedItemId) ? R.anim.nav_item_selected : R.anim.nav_item_deselected;
                Animation animation = AnimationUtils.loadAnimation(this, animationId);
                view.startAnimation(animation);
            }
        }
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    /**
     * Загрузка привычек из Firebase
     */
    private void loadHabitsFromFirebase() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference habitsRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("habits");
        habitsRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                habits.clear();
                for (com.google.firebase.database.DataSnapshot habitSnap : snapshot.getChildren()) {
                    Habit habit = habitSnap.getValue(Habit.class);
                    if (habit != null) {
                        habits.add(habit);
                    }
                }
                habitAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveHabits() {
        if (sharedPreferences == null || habits == null) return;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = new Gson().toJson(habits);
        editor.putString(HABITS_KEY, json);
        editor.apply();
    }

    @Override
    public void onHabitClick(int position) {
        Habit habit = habits.get(position);
        HabitOptionsDialogFragment dialog = HabitOptionsDialogFragment.newInstance(habit, position);
        dialog.show(getSupportFragmentManager(), "HabitOptionsDialog");
    }

    @Override
    public void onEditHabit(Habit habit, int position) {
        Intent intent = new Intent(this, CreateHabitActivity.class);
        intent.putExtra("habit", habit);
        intent.putExtra("habit_position", position);
        startActivityForResult(intent, 1);
    }

    @Override
    public void onDeleteHabit(Habit habit, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Удалить привычку?")
            .setMessage("Вы действительно хотите удалить привычку '" + habit.getName() + "'?")
            .setPositiveButton("Удалить", (dialog, which) -> {
                habits.remove(position);
                saveHabits();
                habitAdapter.notifyItemRemoved(position);
                Toast.makeText(this, "Привычка удалена", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    @Override
    public void onHabitChecked(int position, boolean isChecked) {
        Habit habit = habits.get(position);
        habit.setCompleted(isChecked);
        if (isChecked) {
            habit.markAsCompletedToday();
        } else {
            habit.markAsNotCompletedToday();
        }
        saveHabits(); // Сохраняем изменения привычек
        habitAdapter.notifyItemChanged(position);
        saveHabitToFirebase(habit); // Сохраняем изменения привычки в Firebase
    }

    @Override
    public void onShowStatistics(Habit habit, int position) {
        Intent intent = new Intent(this, HabitStatisticsActivity.class);
        intent.putExtra("habit", new Gson().toJson(habit));
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String habitJson = data.getStringExtra("new_habit");
            int position = data.getIntExtra("habit_position", -1);
            if (habitJson != null) {
                Habit newHabit = new Gson().fromJson(habitJson, Habit.class);

                if (position >= 0 && position < habits.size()) {
                    habits.set(position, newHabit);
                    habitAdapter.notifyItemChanged(position);
                    Toast.makeText(this, "Habit updated", Toast.LENGTH_SHORT).show();
                } else {
                    habits.add(newHabit);
                    habitAdapter.notifyItemInserted(habits.size() - 1);
                    Toast.makeText(this, "Habit added", Toast.LENGTH_SHORT).show();
                }

                saveHabits();
                addHabitToDatabase(newHabit);
                saveHabitToFirebase(newHabit);
            }
        }
        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            String habitJson = data.getStringExtra("new_habit");
            if (habitJson != null) {
                Habit newHabit = new Gson().fromJson(habitJson, Habit.class);

                // Добавляем в память
                habits.add(newHabit);
                habitAdapter.notifyItemInserted(habits.size() - 1);

                // Сохраняем в SharedPreferences
                saveHabits();

                // Добавляем в БД
                dbHelper.addHabit(newHabit);

                saveHabitToFirebase(newHabit);

                Toast.makeText(this, "Привычка добавлена из Facts", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            ivProfileImage.setImageURI(selectedImageUri);
            getSharedPreferences(HABITS_PREFS, MODE_PRIVATE).edit().putString("profile_image_uri", selectedImageUri.toString()).apply();
        }
    }

    private void addHabitToDatabase(Habit habit) {
        try {
            dbHelper.addHabit(habit); // Добавляем привычку в базу данных
        } catch (Exception e) {
            Log.e(TAG, "Error adding habit to database", e);
        }
    }

    private void pickProfileImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void saveHabitToFirebase(Habit habit) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference habitsRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("habits");
        habitsRef.child(String.valueOf(habit.getId())).setValue(habit);
    }

}
