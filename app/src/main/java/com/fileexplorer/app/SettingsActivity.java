package com.fileexplorer.app;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;

import com.fileexplorer.app.databinding.ActivitySettingsBinding;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);

        int currentMode = SettingsHelper.getThemeMode(this);
        if (currentMode == 0 || currentMode == 2) {
            if (SettingsHelper.isAmoledMode(this)) {
                SettingsHelper.saveAmoledMode(this, false);
            }
        }

        SettingsHelper.applyThemeOnStartup(this);
        super.onCreate(savedInstanceState);

        DynamicColors.applyToActivityIfAvailable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SettingsHelper.applyInstantVisuals(this, binding.getRoot(), binding.topAppBar);
        SettingsHelper.setupStatusBar(this);

        setSupportActionBar(binding.topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.topAppBar.setNavigationOnClickListener(v -> finish());

        setupSettingsValues();
        updateThemeUI(SettingsHelper.getThemeMode(this));
    }

    private void updateThemeUI(int mode) {
        binding.ivCheckLight.setVisibility(mode == 0 ? View.VISIBLE : View.INVISIBLE);
        binding.ivCheckDark.setVisibility(mode == 1 ? View.VISIBLE : View.INVISIBLE);
        binding.ivCheckSystem.setVisibility(mode == 2 ? View.VISIBLE : View.INVISIBLE);
    }

    private void applyThemeChange(int themeMode, boolean amoledMode, int nightMode) {
        SettingsHelper.saveThemeMode(this, themeMode);
        SettingsHelper.saveAmoledMode(this, amoledMode);

        int targetColor = SettingsHelper.getTargetBackgroundColor(this);
        SettingsHelper.applyInstantVisuals(this, binding.getRoot(), binding.topAppBar);
        getWindow().setStatusBarColor(targetColor);

        updateThemeUI(themeMode);

        View dividerAmoled = binding.dividerAmoled;
        View layoutAmoledSwitch = binding.layoutAmoledSwitch;
        MaterialSwitch switchAmoled = binding.switchAmoled;

        switchAmoled.setOnCheckedChangeListener(null);

        if (themeMode == 1) {
            dividerAmoled.setVisibility(View.VISIBLE);
            layoutAmoledSwitch.setVisibility(View.VISIBLE);
            switchAmoled.setChecked(amoledMode);
        } else {
            dividerAmoled.setVisibility(View.GONE);
            layoutAmoledSwitch.setVisibility(View.GONE);
            switchAmoled.setChecked(false);
        }

        switchAmoled.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (buttonView.isPressed()) {
                        applyThemeChange(1, isChecked, AppCompatDelegate.MODE_NIGHT_YES);
                    }
                });

        new Handler()
                .postDelayed(
                        () -> {
                            AppCompatDelegate.setDefaultNightMode(nightMode);
                            recreate();
                        },
                        20);
    }

    private void setupSettingsValues() {
        int currentThemeMode = SettingsHelper.getThemeMode(this);
        boolean isAmoledActive = SettingsHelper.isAmoledMode(this);
        boolean showHiddenActive = SettingsHelper.isShowHiddenFiles(this);
        boolean ignoreNomediaActive = SettingsHelper.isIgnoreNomedia(this);
        boolean recyclerViewAnimActive = SettingsHelper.isRecyclerViewAnimationEnabled(this);
        boolean breadcrumbActive = SettingsHelper.isShowBreadcrumb(this);
        boolean showCategoryActive = SettingsHelper.isShowCategory(this);

        View dividerAmoled = binding.dividerAmoled;
        View layoutAmoledSwitch = binding.layoutAmoledSwitch;
        MaterialSwitch switchAmoled = binding.switchAmoled;

        MaterialSwitch switchShowHidden = binding.switchShowHidden;
        MaterialSwitch switchIgnoreNomedia = binding.switchIgnoreNomedia;
        MaterialSwitch switchRecyclerViewAnimation = binding.switchRecyclerViewAnimation;
        View layoutAnimationType = binding.layoutAnimationType;
        android.widget.AutoCompleteTextView spinnerAnimationType = binding.spinnerAnimationType;

        if (spinnerAnimationType != null) {
            CharSequence[] animationArray =
                    getResources().getStringArray(R.array.animation_types_array);
            ArrayAdapter<CharSequence> adapter =
                    new ArrayAdapter<>(this, R.layout.item_spinner, animationArray);
            spinnerAnimationType.setAdapter(adapter);

            int currentIndex = SettingsHelper.getRecyclerViewAnimationType(this);
            if (currentIndex >= 0 && currentIndex < animationArray.length) {
                spinnerAnimationType.setText(animationArray[currentIndex], false);
            }

            int popupHeight =
                    (int)
                            TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP,
                                    320,
                                    getResources().getDisplayMetrics());
            spinnerAnimationType.setDropDownHeight(popupHeight);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                spinnerAnimationType.setDropDownBackgroundResource(R.drawable.bg_spinner_dropdown);
            }

            spinnerAnimationType.setOnItemClickListener(
                    (parent, v, position, id) -> {
                        prefs.edit().putInt(SettingsHelper.KEY_RV_ANIMATION_TYPE, position).apply();
                    });
        }

        MaterialSwitch switchShowBreadcrumb = binding.switchShowBreadcrumb;
        MaterialSwitch switchShowCategory = binding.switchShowCategory;

        // Set initial states
        if (switchShowHidden != null) switchShowHidden.setChecked(showHiddenActive);
        if (switchIgnoreNomedia != null) switchIgnoreNomedia.setChecked(ignoreNomediaActive);
        if (switchRecyclerViewAnimation != null)
            switchRecyclerViewAnimation.setChecked(recyclerViewAnimActive);
        if (switchShowBreadcrumb != null) switchShowBreadcrumb.setChecked(breadcrumbActive);
        if (switchShowCategory != null) switchShowCategory.setChecked(showCategoryActive);

        // Setup visibilitas AMOLED
        if (currentThemeMode == 1) {
            dividerAmoled.setVisibility(View.VISIBLE);
            layoutAmoledSwitch.setVisibility(View.VISIBLE);
            switchAmoled.setChecked(isAmoledActive);
        } else {
            dividerAmoled.setVisibility(View.GONE);
            layoutAmoledSwitch.setVisibility(View.GONE);
            switchAmoled.setChecked(false);
        }

        // Setup visibilitas animation type
        if (layoutAnimationType != null) {
            layoutAnimationType.setVisibility(recyclerViewAnimActive ? View.VISIBLE : View.GONE);
        }

        // Theme listeners
        binding.optionThemeLight.setOnClickListener(
                v -> {
                    int currentMode = SettingsHelper.getThemeMode(this);
                    boolean currentAmoled = SettingsHelper.isAmoledMode(this);
                    if (currentMode != 0 || currentAmoled) {
                        applyThemeChange(0, false, AppCompatDelegate.MODE_NIGHT_NO);
                    }
                });

        binding.optionThemeDark.setOnClickListener(
                v -> {
                    int currentMode = SettingsHelper.getThemeMode(this);
                    if (currentMode != 1) {
                        boolean currentAmoled = SettingsHelper.isAmoledMode(this);
                        applyThemeChange(1, currentAmoled, AppCompatDelegate.MODE_NIGHT_YES);
                    }
                });

        binding.optionThemeSystem.setOnClickListener(
                v -> {
                    int currentMode = SettingsHelper.getThemeMode(this);
                    boolean currentAmoled = SettingsHelper.isAmoledMode(this);
                    if (currentMode != 2 || currentAmoled) {
                        applyThemeChange(2, false, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    }
                });

        // AMOLED switch listener
        switchAmoled.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (buttonView.isPressed()) {
                        applyThemeChange(1, isChecked, AppCompatDelegate.MODE_NIGHT_YES);
                    }
                });

        // Non-theme switch listeners (no recreate needed)
        if (switchShowHidden != null) {
            switchShowHidden.setOnCheckedChangeListener(
                    (b, isChecked) -> {
                        prefs.edit().putBoolean(SettingsHelper.KEY_SHOW_HIDDEN, isChecked).apply();
                    });
        }

        if (switchIgnoreNomedia != null) {
            switchIgnoreNomedia.setOnCheckedChangeListener(
                    (b, isChecked) -> {
                        prefs.edit()
                                .putBoolean(SettingsHelper.KEY_IGNORE_NOMEDIA, isChecked)
                                .apply();
                    });
        }

        if (switchRecyclerViewAnimation != null) {
            switchRecyclerViewAnimation.setOnCheckedChangeListener(
                    (b, isChecked) -> {
                        prefs.edit().putBoolean(SettingsHelper.KEY_RV_ANIMATION, isChecked).apply();
                        if (layoutAnimationType != null) {
                            layoutAnimationType.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                        }
                    });
        }

        if (switchShowBreadcrumb != null) {
            switchShowBreadcrumb.setOnCheckedChangeListener(
                    (b, isChecked) -> {
                        prefs.edit()
                                .putBoolean(SettingsHelper.KEY_SHOW_BREADCRUMB, isChecked)
                                .apply();
                    });
        }

        if (switchShowCategory != null) {
            switchShowCategory.setOnCheckedChangeListener(
                    (b, isChecked) -> {
                        prefs.edit()
                                .putBoolean(SettingsHelper.KEY_SHOW_CATEGORY, isChecked)
                                .apply();
                    });
        }
    }
}
