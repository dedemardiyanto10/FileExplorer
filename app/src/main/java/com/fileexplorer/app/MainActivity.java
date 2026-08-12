package com.fileexplorer.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fileexplorer.app.databinding.ActivityMainBinding;
import com.google.android.material.color.DynamicColors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private File currentDir;
    private FileAdapter adapter;
    private Menu menuToolbar;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private File currentSelectedFile;

    private com.google.android.material.floatingactionbutton.FloatingActionButton fabNewFolder;

    private int currentSortType = 1;
    private boolean isAscending = true;
    private boolean isSearching = false;
    private boolean isSelectionMode = false;
    private String searchQuery = "";
    private String currentFilter = "all";

    private List<File> fileList = new ArrayList<>();
    private List<File> filteredList = new ArrayList<>();

    private final SearchHelper searchHelper = new SearchHelper();

    private final java.util.concurrent.ExecutorService fileExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    private final android.os.Handler searchHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;

    private final Map<String, Integer> folderScrollPositionMap = new java.util.HashMap<>();

    private static final int MAX_CACHE_SIZE = 20;
    private final LinkedHashMap<String, List<FileModel>> folderCacheMap =
            new LinkedHashMap<String, List<FileModel>>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<FileModel>> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            };

    private static final int REQUEST_SETTINGS = 1001;
    private int lastThemeMode = -1;
    private boolean lastAmoledMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SettingsHelper.applyThemeOnStartup(this);

        lastThemeMode = SettingsHelper.getThemeMode(this);
        lastAmoledMode = SettingsHelper.isAmoledMode(this);

        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int viewModeSaved = prefs.getInt("view_mode_type", 0);

        DynamicColors.applyToActivityIfAvailable(this);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SettingsHelper.applyAmoledMode(
                this,
                binding.getRoot(),
                binding.appBarLayout,
                binding.topAppBar,
                binding.recyclerView);

        setupStatusBar();

        setSupportActionBar(binding.topAppBar);

        CategoryHelper.setupCategory(
                this,
                binding.rvCategory,
                category -> {
                    if (isSelectionMode && adapter != null) {
                        adapter.clearSelection();
                        isSelectionMode = false;
                        updateToolbarState();
                    }

                    currentFilter = category;
                    searchQuery = "";

                    CategoryHelper.updateCategorySelection(category);

                    loadAllFilesByFilter();
                });

        boolean showCategory = SettingsHelper.isShowCategory(this);
        CategoryHelper.setCategoryVisibility(binding.llCategoryContainer, showCategory);

        boolean showBreadcrumb = SettingsHelper.isShowBreadcrumb(this);

        if (binding.llBreadcrumb != null) {
            binding.llBreadcrumb.setVisibility(showBreadcrumb ? View.VISIBLE : View.GONE);
        }

        updateContainerBackgrounds();

        setupBackPressedCallback();

        if (PermissionHelper.checkPermission(this)) {
            initFileManager();
        } else {
            PermissionHelper.requestPermission(this);
        }

        fabNewFolder = binding.getRoot().findViewById(R.id.fabNewFolder);
        fabNewFolder.setOnClickListener(v -> showCreateFolderDialog());

        setupSortButton();

        setupViewModeToggle(prefs, viewModeSaved);

        updateBreadcrumbVisibility(SettingsHelper.isShowBreadcrumb(this));
    }

    @Override
    protected void onResume() {
        super.onResume();

        SettingsHelper.applyAmoledMode(
                this,
                binding.getRoot(),
                binding.appBarLayout,
                binding.topAppBar,
                binding.recyclerView);

        applyRecyclerViewAnimator();
        updateBreadcrumbVisibility(SettingsHelper.isShowBreadcrumb(this));

        boolean showCategory = SettingsHelper.isShowCategory(this);
        CategoryHelper.setCategoryVisibility(binding.llCategoryContainer, showCategory);
        updateContainerBackgrounds();
    }

    public void updateCategoryVisibility(boolean show) {
        if (binding.llCategoryContainer != null) {
            CategoryHelper.toggleCategory(binding.llCategoryContainer, show, null);
            updateContainerBackgrounds();
        }
    }

    private void updateBreadcrumbVisibility(boolean show) {
        if (binding == null || binding.llBreadcrumb == null) return;
        BreadcrumbHelper.animateVisibility(binding.llBreadcrumb, show);
        updateContainerBackgrounds();
    }

    private void updateContainerBackgrounds() {
        boolean showBreadcrumb = SettingsHelper.isShowBreadcrumb(this);
        boolean showCategory = SettingsHelper.isShowCategory(this);

        if (binding.llBreadcrumb != null) {
            binding.llBreadcrumb.setBackgroundResource(R.drawable.bg_top_container);
        }

        if (binding.llCategoryContainer != null) {
            if (showBreadcrumb) {
                binding.llCategoryContainer.setBackgroundResource(R.drawable.bg_noround);
            } else {
                binding.llCategoryContainer.setBackgroundResource(R.drawable.bg_top_container);
            }
        }

        if (binding.llSortContainer != null) {
            if (showBreadcrumb || showCategory) {
                binding.llSortContainer.setBackgroundResource(R.drawable.bg_noround);
            } else {
                binding.llSortContainer.setBackgroundResource(R.drawable.bg_top_container);
            }
        }
    }

    private void updateCategoryUI(String category) {
        CategoryHelper.updateCategorySelection(category);
    }

    private void loadAllFilesByFilter() {
        if (isSelectionMode && adapter != null) {
            adapter.clearSelection();
            isSelectionMode = false;
            updateToolbarState();
        }

        CategoryHelper.updateCategorySelection(currentFilter);

        fileList.clear();
        searchQuery = "";

        if (currentFilter.equals("all")) {
            if (fabNewFolder != null) fabNewFolder.show();
            binding.topAppBar.setSubtitle(null);
            loadFiles(Environment.getExternalStorageDirectory());
            return;
        }

        if (fabNewFolder != null) fabNewFolder.hide();

        if (currentFilter.equals("audio")
                || currentFilter.equals("video")
                || currentFilter.equals("images")) {
            fileExecutor.execute(
                    () -> {
                        List<File> mediaFiles = loadMediaFiles(currentFilter);

                        runOnUiThread(
                                () -> {
                                    fileList.clear();
                                    fileList.addAll(mediaFiles);
                                    currentDir = Environment.getExternalStorageDirectory();

                                    String title = getFilterTitle(currentFilter);
                                    binding.topAppBar.setTitle(title);

                                    binding.topAppBar.setSubtitle(mediaFiles.size() + " file");

                                    refreshData();

                                    if (binding.recyclerView != null) {
                                        binding.recyclerView.scrollToPosition(0);
                                    }
                                });
                    });
            return;
        }

        if (currentFilter.equals("apk")
                || currentFilter.equals("document")
                || currentFilter.equals("archive")) {
            fileExecutor.execute(
                    () -> {
                        List<File> result = new ArrayList<>();
                        Set<String> pathSet = new HashSet<>();

                        File[] dirs = {
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS),
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOCUMENTS),
                            new File(Environment.getExternalStorageDirectory(), "Download"),
                            new File(Environment.getExternalStorageDirectory(), "Documents"),
                            Environment.getExternalStorageDirectory()
                        };

                        String searchType;
                        if (currentFilter.equals("apk")) searchType = "apk";
                        else if (currentFilter.equals("archive")) searchType = "archive";
                        else searchType = "doc";

                        for (File dir : dirs) {
                            if (dir != null && dir.exists()) {
                                searchRecursive(dir, result, pathSet, 4, searchType);
                            }
                        }

                        final List<File> finalResult = result;
                        runOnUiThread(
                                () -> {
                                    fileList.clear();
                                    fileList.addAll(finalResult);
                                    currentDir = Environment.getExternalStorageDirectory();

                                    String title = getFilterTitle(currentFilter);
                                    binding.topAppBar.setTitle(title);

                                    binding.topAppBar.setSubtitle(finalResult.size() + " file");

                                    refreshData();

                                    if (binding.recyclerView != null) {
                                        binding.recyclerView.scrollToPosition(0);
                                    }
                                });
                    });
            return;
        }

        if (currentFilter.equals("download")) {
            fileExecutor.execute(
                    () -> {
                        List<File> result = new ArrayList<>();
                        Set<String> pathSet = new HashSet<>();

                        File[] downloadDirs = {
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS),
                            new File(Environment.getExternalStorageDirectory(), "Download")
                        };

                        for (File dir : downloadDirs) {
                            if (dir != null && dir.exists() && dir.isDirectory()) {
                                File[] files = dir.listFiles();
                                if (files != null) {
                                    for (File f : files) {
                                        if (f != null
                                                && f.exists()
                                                && !f.isHidden()
                                                && !f.isDirectory()) {
                                            if (pathSet.add(f.getAbsolutePath())) {
                                                result.add(f);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        final List<File> finalResult = result;
                        runOnUiThread(
                                () -> {
                                    fileList.clear();
                                    fileList.addAll(finalResult);
                                    currentDir = Environment.getExternalStorageDirectory();

                                    String title = getFilterTitle(currentFilter);
                                    binding.topAppBar.setTitle(title);
                                    binding.topAppBar.setSubtitle(finalResult.size() + " file");

                                    refreshData();

                                    if (binding.recyclerView != null) {
                                        binding.recyclerView.scrollToPosition(0);
                                    }
                                });
                    });
            return;
        }
    }

    private List<File> loadMediaFiles(String filter) {
        List<File> result = new ArrayList<>();
        Uri uri = null;
        String[] proj = {
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        };

        switch (filter) {
            case "audio":
                uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                break;
            case "video":
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                break;
            case "images":
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                break;
            default:
                return result;
        }

        Set<String> pathSet = new HashSet<>();
        if (uri != null) {
            try (Cursor c = getContentResolver().query(uri, proj, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    do {
                        String path = c.getString(0);
                        if (path != null && pathSet.add(path)) {
                            File f = new File(path);
                            if (f.exists()) result.add(f);
                        }
                    } while (c.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private void searchRecursive(
            File dir, List<File> result, Set<String> pathSet, int maxDepth, String type) {
        if (dir == null || !dir.exists() || !dir.isDirectory() || maxDepth <= 0) return;
        if (result.size() >= 300) return;

        String path = dir.getAbsolutePath();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (path.contains("/Android/data/") || path.contains("/Android/obb/")) {
                return;
            }
        }

        File[] files = dir.listFiles();
        if (files == null) return;

        int maxFiles = Math.min(files.length, 100);
        for (int i = 0; i < maxFiles; i++) {
            File f = files[i];
            if (f == null || f.isHidden()) continue;
            if (result.size() >= 300) break;

            if (f.isDirectory()) {
                String name = f.getName();
                if (!name.startsWith(".")
                        && !name.equals("Android")
                        && !name.equals("data")
                        && !name.equals("obb")
                        && !name.equals("cache")
                        && !name.equals("system")
                        && !name.equals("vendor")
                        && !name.equals("dev")
                        && !name.equals("proc")
                        && !name.equals("sys")) {
                    searchRecursive(f, result, pathSet, maxDepth - 1, type);
                }
            } else {
                String name = f.getName().toLowerCase();
                if (type.equals("apk") && name.endsWith(".apk")) {
                    if (pathSet.add(f.getAbsolutePath())) result.add(f);
                } else if (type.equals("doc") && isDocumentFile(name)) {
                    if (pathSet.add(f.getAbsolutePath())) result.add(f);
                } else if (type.equals("archive") && isArchiveFile(name)) {
                    if (pathSet.add(f.getAbsolutePath())) result.add(f);
                }
            }
        }
    }

    private boolean isDocumentFile(String name) {
        return name.endsWith(".pdf")
                || name.endsWith(".doc")
                || name.endsWith(".docx")
                || name.endsWith(".xls")
                || name.endsWith(".xlsx")
                || name.endsWith(".ppt")
                || name.endsWith(".pptx")
                || name.endsWith(".txt")
                || name.endsWith(".odt");
    }

    private boolean isArchiveFile(String name) {
        return name.endsWith(".zip")
                || name.endsWith(".rar")
                || name.endsWith(".7z")
                || name.endsWith(".tar")
                || name.endsWith(".gz")
                || name.endsWith(".bz2");
    }

    private void refreshData() {
        if (!fileList.isEmpty()) {
            java.util.Collections.sort(
                    fileList,
                    (f1, f2) -> {
                        if (f1.isDirectory() && !f2.isDirectory()) return -1;
                        if (!f1.isDirectory() && f2.isDirectory()) return 1;

                        int result = 0;
                        if (currentSortType == 1) {
                            result = f1.getName().compareToIgnoreCase(f2.getName());
                        } else if (currentSortType == 2) {
                            result = Long.compare(f2.lastModified(), f1.lastModified());
                        } else if (currentSortType == 3) {
                            result = Long.compare(f2.length(), f1.length());
                        }
                        return isAscending ? result : -result;
                    });
        }

        List<FileModel> modelList = new ArrayList<>();
        for (File f : fileList) {
            modelList.add(new FileModel(f));
        }

        filteredList.clear();
        filteredList.addAll(fileList);
        adapter.updateData(modelList);
        updateEmptyView();
    }

    private void updateEmptyView() {
        boolean isEmpty = filteredList.isEmpty();

        if (isEmpty) {
            binding.recyclerView.setVisibility(View.GONE);
            binding.emptyLayout.setVisibility(View.VISIBLE);
        } else {
            binding.emptyLayout.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private String getFilterTitle(String f) {
        switch (f) {
            case "all":
                return "Semua";
            case "audio":
                return "Audio";
            case "video":
                return "Video";
            case "images":
                return "Gambar";
            case "apk":
                return "APK";
            case "document":
                return "Dokumen";
            case "archive":
                return "Arsip";
            case "download":
                return "Unduhan";
            default:
                return "File Explorer";
        }
    }

    // ==================== SETUP STATUS BAR ====================

    private void setupStatusBar() {
        boolean isNight =
                (getResources().getConfiguration().uiMode
                                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        int statusBarColor;
        if (isNight && SettingsHelper.isAmoledMode(this)) {
            statusBarColor = android.graphics.Color.BLACK;
        } else {
            TypedValue typedValue = new TypedValue();
            getTheme()
                    .resolveAttribute(
                            com.google.android.material.R.attr.colorSurfaceContainerLow,
                            typedValue,
                            true);
            statusBarColor = typedValue.data;
        }

        getWindow().setStatusBarColor(statusBarColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean isDarkMode =
                    (getResources().getConfiguration().uiMode
                                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                            == android.content.res.Configuration.UI_MODE_NIGHT_YES;

            int appearanceMask =
                    isDarkMode
                            ? 0
                            : android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;

            getWindow()
                    .getInsetsController()
                    .setSystemBarsAppearance(
                            appearanceMask,
                            android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }
    }

    // ==================== BACK PRESSED ====================

    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                if (isSelectionMode) {
                                    clearSelectionMode();
                                    return;
                                }

                                if (isSearching) {
                                    clearSearchMode();
                                    return;
                                }

                                if (!currentFilter.equals("all")) {
                                    currentFilter = "all";
                                    searchQuery = "";
                                    binding.topAppBar.setTitle("File Explorer");
                                    binding.topAppBar.setSubtitle(null);

                                    CategoryHelper.updateCategorySelection("all");

                                    if (isSelectionMode && adapter != null) {
                                        adapter.clearSelection();
                                        isSelectionMode = false;
                                        updateToolbarState();
                                    }

                                    if (fabNewFolder != null) {
                                        fabNewFolder.show();
                                    }

                                    loadFiles(Environment.getExternalStorageDirectory());
                                    return;
                                }

                                if (isRootDirectory()) {
                                    DialogHelper.showExitConfirmationDialog(
                                            MainActivity.this, () -> finish());
                                    return;
                                }

                                if (currentDir != null) {
                                    File parentDir = currentDir.getParentFile();
                                    if (parentDir != null
                                            && parentDir.exists()
                                            && isWithinExternalStorage(parentDir)) {
                                        saveCurrentScrollPosition();
                                        loadFilesWithPosition(parentDir, true);
                                        return;
                                    }
                                }

                                finish();
                            }
                        });
    }

    private boolean isWithinExternalStorage(File dir) {
        File rootDir = Environment.getExternalStorageDirectory();
        if (rootDir == null || dir == null) return false;
        String rootPath = rootDir.getAbsolutePath();
        String currentPath = dir.getAbsolutePath();
        return currentPath.startsWith(rootPath);
    }

    private boolean isRootDirectory() {
        File rootDir = Environment.getExternalStorageDirectory();
        if (currentDir == null || rootDir == null) return true;
        return currentDir.getAbsolutePath().equals(rootDir.getAbsolutePath());
    }

    // ==================== CLEAR MODES ====================

    private void clearSelectionMode() {
        if (adapter != null) {
            adapter.clearSelection();
            isSelectionMode = false;
            updateToolbarState();
        }
    }

    private void clearSearchMode() {
        if (binding != null && menuToolbar != null) {
            MenuItem searchItem = menuToolbar.findItem(R.id.action_search);
            if (searchItem != null) {
                androidx.appcompat.widget.SearchView searchView =
                        (androidx.appcompat.widget.SearchView) searchItem.getActionView();
                if (searchView != null) {
                    searchView.setQuery("", false);
                    searchView.onActionViewCollapsed();
                }
            }
        }
        isSearching = false;
        loadFiles(currentDir);
    }

    // ==================== SORT BUTTON ====================

    private void setupSortButton() {
        if (binding.llSort == null) return;

        binding.llSort.setOnClickListener(
                v -> {
                    SortMenuHelper.showSortMenu(
                            this,
                            v,
                            currentSortType,
                            isAscending,
                            (sortType, ascending) -> {
                                currentSortType = sortType;
                                isAscending = ascending;
                                updateSortButtonUI();
                                loadFiles(currentDir);
                            });
                });
    }

    private void updateSortButtonUI() {
        if (binding.tvSortTitle == null || binding.btnSortIcon == null) return;

        String sortTitle = getString(R.string.sort_name);
        if (currentSortType == 2) {
            sortTitle = getString(R.string.sort_date);
        } else if (currentSortType == 3) {
            sortTitle = getString(R.string.sort_size);
        }

        binding.tvSortTitle.setText(sortTitle);
        binding.btnSortIcon.setIconResource(
                isAscending ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
    }

    // ==================== VIEW MODE ====================

    private void setupViewModeToggle(SharedPreferences prefs, int viewModeSaved) {
        if (binding.toggleButtonViewMode == null) return;

        if (viewModeSaved == 1) {
            binding.toggleButtonViewMode.check(R.id.btnViewGrid);
        } else if (viewModeSaved == 2) {
            binding.toggleButtonViewMode.check(R.id.btnViewGrid3);
        } else {
            binding.toggleButtonViewMode.check(R.id.btnViewList);
        }

        binding.toggleButtonViewMode.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (!isChecked) return;

                    SharedPreferences.Editor editor = prefs.edit();

                    if (checkedId == R.id.btnViewList) {
                        setupLayoutManager(0);
                        editor.putInt("view_mode_type", 0);
                    } else if (checkedId == R.id.btnViewGrid) {
                        setupLayoutManager(1);
                        editor.putInt("view_mode_type", 1);
                    } else if (checkedId == R.id.btnViewGrid3) {
                        setupLayoutManager(2);
                        editor.putInt("view_mode_type", 2);
                    }
                    editor.apply();
                });
    }

    private void setupLayoutManager(int modeType) {
        if (binding == null || binding.recyclerView == null) return;

        if (modeType == 0) {
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
            if (adapter != null) {
                adapter.setGridView(false);
                adapter.setSpanCount(1);
            }
        } else {
            int spanCount = (modeType == 2) ? 3 : 2;
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, spanCount);
            gridLayoutManager.setSpanSizeLookup(
                    new GridLayoutManager.SpanSizeLookup() {
                        @Override
                        public int getSpanSize(int position) {
                            if (adapter != null && adapter.getItemViewType(position) == 2) {
                                return spanCount;
                            }
                            return 1;
                        }
                    });
            binding.recyclerView.setLayoutManager(gridLayoutManager);
            if (adapter != null) {
                adapter.setGridView(true);
                adapter.setSpanCount(spanCount);
            }
        }
    }

    // ==================== INIT ====================

    private void initFileManager() {
        currentDir = Environment.getExternalStorageDirectory();
        setupRecyclerView();
        updateSortButtonUI();
        loadFiles(currentDir);
    }

    // ==================== RECYCLER VIEW ====================

    private void setupRecyclerView() {
        List<FileModel> initialList = new ArrayList<>();
        adapter =
                new FileAdapter(
                        initialList,
                        new FileAdapter.OnItemClickListener() {
                            @Override
                            public void onItemClick(File file) {
                                if (file == null) return;

                                if (file.isDirectory()) {
                                    saveCurrentScrollPosition();
                                    loadFiles(file);
                                } else {
                                    handleFileOpen(file);
                                }
                            }

                            @Override
                            public void onSelectionChanged() {
                                updateSelectionState();
                                updateToolbarState();
                            }

                            @Override
                            public void onMoreClick(File file, View view) {
                                currentSelectedFile = file;
                                showCustomMoreMenu(view);
                            }
                        });

        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int viewModeSaved = prefs.getInt("view_mode_type", 0);
        setupLayoutManager(viewModeSaved);

        applyRecyclerViewAnimator();

        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setItemViewCacheSize(20);
        binding.recyclerView.addOnScrollListener(createScrollListener());
        binding.recyclerView.addItemDecoration(createItemDecoration());
        binding.recyclerView.setAdapter(adapter);
    }

    // ==================== SCROLL LISTENER ====================

    private RecyclerView.OnScrollListener createScrollListener() {
        return new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (fabNewFolder != null
                        && !isSearching
                        && !isSelectionMode
                        && currentFilter.equals("all")) {
                    if (dy > 0 && fabNewFolder.isShown()) {
                        fabNewFolder.hide();
                    } else if (dy < 0 && !fabNewFolder.isShown()) {
                        fabNewFolder.show();
                    }
                } else if (fabNewFolder != null && fabNewFolder.isShown()) {
                    fabNewFolder.hide();
                }
            }
        };
    }

    // ==================== ITEM DECORATION ====================

    private RecyclerView.ItemDecoration createItemDecoration() {
        return new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(
                    @NonNull android.graphics.Rect outRect,
                    @NonNull View view,
                    @NonNull RecyclerView parent,
                    @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);

                if (adapter != null && adapter.getItemViewType(position) == 2) {
                    outRect.set(0, 0, 0, 0);
                    return;
                }

                RecyclerView.LayoutManager lm = binding.recyclerView.getLayoutManager();
                if (lm instanceof GridLayoutManager) {
                    int spanCount = ((GridLayoutManager) lm).getSpanCount();
                    int column = position % spanCount;

                    int spacing = 6;
                    int halfSpacing = spacing / 2;

                    if (spanCount == 3) {
                        if (column == 0) {
                            outRect.left = 0;
                            outRect.right = spacing * 2 / 3;
                        } else if (column == 1) {
                            outRect.left = spacing / 3;
                            outRect.right = spacing / 3;
                        } else {
                            outRect.left = spacing * 2 / 3;
                            outRect.right = 0;
                        }
                    } else {
                        if (column == 0) {
                            outRect.left = 0;
                            outRect.right = halfSpacing;
                        } else {
                            outRect.left = halfSpacing;
                            outRect.right = 0;
                        }
                    }

                    outRect.top = 3;
                    outRect.bottom = 3;

                } else {
                    outRect.set(0, 0, 0, 0);
                }
            }
        };
    }

    // ==================== ANIMATOR ====================

    private void applyRecyclerViewAnimator() {
        if (binding == null || binding.recyclerView == null) return;

        if (!SettingsHelper.isRecyclerViewAnimationEnabled(this)) {
            binding.recyclerView.setItemAnimator(null);
            return;
        }

        int animType = SettingsHelper.getRecyclerViewAnimationType(this);
        switch (animType) {
            case 1:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.FadeInDownAnimator());
                break;
            case 2:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.FadeInUpAnimator());
                break;
            case 3:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.FadeInLeftAnimator());
                break;
            case 4:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.FadeInRightAnimator());
                break;
            case 5:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.SlideInDownAnimator());
                break;
            case 6:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.SlideInUpAnimator());
                break;
            case 7:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.SlideInLeftAnimator());
                break;
            case 8:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.SlideInRightAnimator());
                break;
            case 9:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.ScaleInAnimator());
                break;
            case 10:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.ScaleInTopAnimator());
                break;
            case 11:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.ScaleInBottomAnimator());
                break;
            case 12:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.ScaleInLeftAnimator());
                break;
            case 13:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.ScaleInRightAnimator());
                break;
            case 14:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.LandingAnimator());
                break;
            case 15:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.OvershootInLeftAnimator());
                break;
            case 16:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.OvershootInRightAnimator());
                break;
            case 0:
            default:
                binding.recyclerView.setItemAnimator(
                        new jp.wasabeef.recyclerview.animators.FadeInAnimator());
                break;
        }
    }

    // ==================== SELECTION STATE ====================

    private void updateSelectionState() {
        if (adapter == null) {
            isSelectionMode = false;
            return;
        }

        int count = 0;
        List<FileModel> fileList = adapter.getFileList();
        if (fileList != null) {
            for (FileModel model : fileList) {
                if (model.isSelected()) {
                    count++;
                }
            }
        }
        isSelectionMode = count > 0;
    }

    // ==================== TOOLBAR STATE ====================

    private void updateToolbarState() {
        if (adapter == null || menuToolbar == null || binding == null) {
            return;
        }

        int count = 0;
        int totalItems = 0;
        boolean allSelected = false;

        List<FileModel> fileList = adapter.getFileList();
        if (fileList != null) {
            totalItems = fileList.size();
            for (FileModel model : fileList) {
                if (model.isSelected()) {
                    count++;
                }
            }
            if (totalItems > 0 && count == totalItems) {
                allSelected = true;
            }
        }

        updateFabVisibility(count);

        MenuItem searchItem = menuToolbar.findItem(R.id.action_search);
        MenuItem storageItem = menuToolbar.findItem(R.id.action_storage);
        MenuItem settingsItem = menuToolbar.findItem(R.id.action_settings);
        MenuItem selectAllItem = menuToolbar.findItem(R.id.action_select_all);
        MenuItem moreItem = menuToolbar.findItem(R.id.action_more);

        if (count > 0) {
            String title = getString(R.string.items_selected, count);
            binding.topAppBar.setTitle(title);

            if (searchItem != null) searchItem.setVisible(false);
            if (storageItem != null) storageItem.setVisible(false);
            if (settingsItem != null) settingsItem.setVisible(false);
            if (selectAllItem != null) {
                selectAllItem.setVisible(true);
                if (allSelected) {
                    selectAllItem.setTitle(R.string.deselect_all);
                    selectAllItem.setIcon(R.drawable.ic_unselect_all);
                } else {
                    selectAllItem.setTitle(R.string.select_all);
                    selectAllItem.setIcon(R.drawable.ic_select_all);
                }
            }
            if (moreItem != null) moreItem.setVisible(true);
        } else {
            if (currentFilter.equals("all")) {
                binding.topAppBar.setTitle("File Explorer");
            } else {
                binding.topAppBar.setTitle(getFilterTitle(currentFilter));
            }

            if (searchItem != null) {
                searchItem.setVisible(true);
            }
            if (storageItem != null) storageItem.setVisible(true);
            if (settingsItem != null) settingsItem.setVisible(true);
            if (selectAllItem != null) {
                selectAllItem.setVisible(false);
                selectAllItem.setTitle(R.string.select_all);
                selectAllItem.setIcon(R.drawable.ic_select_all);
            }
            if (moreItem != null) moreItem.setVisible(false);
        }
    }

    private void updateFabVisibility(int selectedCount) {
        if (fabNewFolder == null) return;

        if (isSearching || selectedCount > 0 || !currentFilter.equals("all")) {
            fabNewFolder.hide();
        } else {
            fabNewFolder.show();
        }
    }

    // ==================== SCROLL POSITION ====================

    private void saveCurrentScrollPosition() {
        if (binding == null || binding.recyclerView == null || currentDir == null) {
            return;
        }

        RecyclerView.LayoutManager layoutManager = binding.recyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            int firstVisiblePosition =
                    ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
            folderScrollPositionMap.put(currentDir.getAbsolutePath(), firstVisiblePosition);
        }
    }

    // ==================== LOAD FILES ====================

    private void loadFiles(File dir) {
        if (!currentFilter.equals("all")) {
            currentFilter = "all";
            CategoryHelper.updateCategorySelection("all");
            binding.topAppBar.setTitle("File Explorer");
            binding.topAppBar.setSubtitle(null);
        }

        if (dir != null) {
            folderCacheMap.remove(dir.getAbsolutePath());
        }
        loadFilesWithPosition(dir, false);
    }

    private void loadFilesWithPosition(File dir, boolean restorePosition) {
        if (dir == null || !dir.exists()) {
            showEmptyState();
            return;
        }

        currentDir = dir;

        BreadcrumbHelper.setupBreadcrumb(
                this,
                binding.llBreadcrumbContainer,
                dir,
                folder -> {
                    saveCurrentScrollPosition();
                    loadFilesWithPosition(folder, true);
                });
        binding.horizontalScroll.post(() -> binding.horizontalScroll.fullScroll(View.FOCUS_RIGHT));

        if (restorePosition && folderCacheMap.containsKey(dir.getAbsolutePath())) {
            List<FileModel> cachedModels = folderCacheMap.get(dir.getAbsolutePath());
            displayCachedFiles(cachedModels, dir);
            return;
        }

        loadFilesInBackground(dir, restorePosition);
    }

    private void displayCachedFiles(List<FileModel> cachedModels, File dir) {
        if (cachedModels == null || cachedModels.isEmpty()) {
            showEmptyState();
        } else {
            showContentState();
            if (adapter != null) {
                adapter.updateData(cachedModels);
            }
        }

        restoreScrollPosition(dir);
        updateToolbarState();
    }

    private void restoreScrollPosition(File dir) {
        if (folderScrollPositionMap.containsKey(dir.getAbsolutePath())) {
            int savedPosition = folderScrollPositionMap.get(dir.getAbsolutePath());
            binding.recyclerView.post(
                    () -> {
                        RecyclerView.LayoutManager lm = binding.recyclerView.getLayoutManager();
                        if (lm instanceof LinearLayoutManager) {
                            ((LinearLayoutManager) lm).scrollToPositionWithOffset(savedPosition, 0);
                        }
                    });
        }
    }

    private void loadFilesInBackground(File dir, boolean restorePosition) {
        fileExecutor.execute(
                () -> {
                    try {
                        boolean showHidden = SettingsHelper.isShowHiddenFiles(MainActivity.this);

                        File[] files =
                                dir.listFiles(
                                        file -> {
                                            if (file == null) return false;
                                            if (!showHidden && file.getName().startsWith(".")) {
                                                return false;
                                            }
                                            return true;
                                        });

                        List<FileModel> allModels = new ArrayList<>();

                        if (files != null && files.length > 0) {
                            sortFiles(files);

                            for (File file : files) {
                                allModels.add(new FileModel(file));
                            }
                        }

                        folderCacheMap.put(dir.getAbsolutePath(), allModels);

                        runOnUiThread(
                                () -> {
                                    if (!currentDir.equals(dir)) {
                                        return;
                                    }

                                    if (allModels.isEmpty()) {
                                        showEmptyState();
                                    } else {
                                        showContentState();
                                        if (adapter != null) {
                                            adapter.updateData(allModels);
                                        }
                                    }

                                    if (restorePosition) {
                                        restoreScrollPosition(dir);
                                    } else {
                                        binding.recyclerView.scrollToPosition(0);
                                    }

                                    binding.topAppBar.setTitle(R.string.app_name);
                                    updateToolbarState();
                                });
                    } catch (Exception e) {
                        runOnUiThread(
                                () -> {
                                    Toast.makeText(
                                                    MainActivity.this,
                                                    R.string.error_loading_files,
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                });
                    }
                });
    }

    private void sortFiles(File[] files) {
        if (files == null) return;

        java.util.Arrays.sort(
                files,
                (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;

                    int result = 0;
                    if (currentSortType == 1) {
                        result = f1.getName().compareToIgnoreCase(f2.getName());
                    } else if (currentSortType == 2) {
                        result = Long.compare(f2.lastModified(), f1.lastModified());
                    } else if (currentSortType == 3) {
                        result = Long.compare(f2.length(), f1.length());
                    }

                    return isAscending ? result : -result;
                });
    }

    // ==================== EMPTY/CONTENT STATE ====================

    private void showEmptyState() {
        if (binding != null) {
            binding.recyclerView.setVisibility(View.GONE);
            binding.emptyLayout.setVisibility(View.VISIBLE);
        }
    }

    private void showContentState() {
        if (binding != null) {
            binding.emptyLayout.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // ==================== OPEN FILE ====================

    private void handleFileOpen(File file) {
        if (file == null) return;

        String fileName = file.getName().toLowerCase(Locale.getDefault());

        if (fileName.endsWith(".mp3")
                || fileName.endsWith(".wav")
                || fileName.endsWith(".m4a")
                || fileName.endsWith(".flac")
                || fileName.endsWith(".ogg")) {
            DialogHelper.showAudioPlayerDialog(MainActivity.this, file);
        } else {
            OpenFileHelper.openFile(MainActivity.this, file);
        }
    }

    // ==================== OPTIONS MENU ====================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menuToolbar = menu;
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView =
                (androidx.appcompat.widget.SearchView) searchItem.getActionView();

        if (searchView != null) {
            searchView.setOnQueryTextListener(
                    new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            filterFiles(query);
                            return true;
                        }

                        @Override
                        public boolean onQueryTextChange(String newText) {
                            filterFiles(newText);
                            return true;
                        }
                    });

            searchView.setOnCloseListener(
                    () -> {
                        isSearching = false;
                        loadFiles(currentDir);
                        return false;
                    });
        }

        updateToolbarState();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_select_all) {
            handleSelectAll();
            return true;
        } else if (id == R.id.action_more) {
            showCustomMoreMenu(findViewById(R.id.action_more));
            return true;
        } else if (id == R.id.action_storage) {
            Intent intent = new Intent(MainActivity.this, StorageAnalyzerActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_settings) {
            showSettingsMenu();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ==================== SELECT ALL ====================

    private void handleSelectAll() {
        if (adapter == null) return;

        boolean allSelected = true;
        List<FileModel> fileList = adapter.getFileList();
        if (fileList != null) {
            for (FileModel model : fileList) {
                if (!model.isSelected()) {
                    allSelected = false;
                    break;
                }
            }
        }
        adapter.selectAll(!allSelected);
        updateSelectionState();
        updateToolbarState();
    }

    // ==================== SETTINGS ====================

    private void showSettingsMenu() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivityForResult(intent, REQUEST_SETTINGS);
    }

    // ==================== MORE MENU ====================

    private void showCustomMoreMenu(View anchorView) {
        if (anchorView == null || adapter == null) return;

        List<FileModel> allFiles = adapter.getFileList();

        MoreMenuHelper.showCustomMoreMenu(
                this,
                anchorView,
                currentSelectedFile,
                allFiles,
                new MoreMenuHelper.MenuActionListener() {
                    @Override
                    public void onRenameSelected(List<FileModel> selectedFiles, File targetFile) {
                        if (selectedFiles != null && selectedFiles.size() > 1) {
                            showBatchRenameDialog(selectedFiles);
                        } else if (targetFile != null) {
                            showRenameDialogForFile(targetFile);
                        }
                    }

                    @Override
                    public void onShareSelected(List<FileModel> selectedFiles, File targetFile) {
                        MoreMenuHelper.defaultShareAction(
                                MainActivity.this, selectedFiles, targetFile);
                    }

                    @Override
                    public void onDeleteSelected(List<FileModel> selectedFiles, File targetFile) {
                        if (selectedFiles != null && !selectedFiles.isEmpty()) {
                            if (selectedFiles.size() > 1) {
                                DialogHelper.showBatchDeleteDialog(
                                        MainActivity.this,
                                        selectedFiles,
                                        () -> {
                                            runOnUiThread(
                                                    () -> {
                                                        loadFiles(currentDir);
                                                        if (adapter != null) {
                                                            adapter.clearSelection();
                                                            isSelectionMode = false;
                                                            updateToolbarState();
                                                        }
                                                    });
                                        });
                            } else {
                                showDeleteConfirmationForFile(selectedFiles.get(0).getFile());
                            }
                        } else if (targetFile != null) {
                            showDeleteConfirmationForFile(targetFile);
                        } else if (currentSelectedFile != null) {
                            showDeleteConfirmationForFile(currentSelectedFile);
                        }
                    }

                    @Override
                    public void onPropertiesSelected(File targetFile) {
                        if (targetFile != null) {
                            showPropertiesDialogForFile(targetFile);
                        }
                    }

                    @Override
                    public void onCompressSelected(List<FileModel> selectedFiles) {
                        if (selectedFiles != null && !selectedFiles.isEmpty()) {
                            DialogHelper.showCompressDialog(
                                    MainActivity.this,
                                    selectedFiles,
                                    () -> {
                                        loadFiles(currentDir);
                                        if (adapter != null) {
                                            adapter.clearSelection();
                                            isSelectionMode = false;
                                            updateToolbarState();
                                        }
                                    });
                        } else {
                            Toast.makeText(
                                            MainActivity.this,
                                            "Tidak ada file yang dipilih",
                                            Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }

                    @Override
                    public void onExtractSelected(File zipFile) {
                        if (zipFile != null && zipFile.exists()) {
                            DialogHelper.showExtractDialog(
                                    MainActivity.this,
                                    zipFile,
                                    () -> {
                                        runOnUiThread(
                                                () -> {
                                                    loadFiles(currentDir);
                                                    if (adapter != null) {
                                                        adapter.clearSelection();
                                                        isSelectionMode = false;
                                                        updateToolbarState();
                                                    }
                                                });
                                    });
                        } else {
                            Toast.makeText(
                                            MainActivity.this,
                                            "File arsip tidak ditemukan",
                                            Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }

                    @Override
                    public void onMoveSelected(List<FileModel> selectedFiles, File targetFile) {
                        if (selectedFiles != null && !selectedFiles.isEmpty()) {
                            DialogHelper.showMoveDialog(
                                    MainActivity.this,
                                    selectedFiles,
                                    new DialogHelper.Callback() {
                                        @Override
                                        public void onActionCompleted() {}

                                        @Override
                                        public void onActionCompleted(File targetFolder) {
                                            runOnUiThread(
                                                    () -> {
                                                        if (targetFolder != null
                                                                && targetFolder.exists()) {
                                                            loadFiles(targetFolder);
                                                        } else if (currentDir != null) {
                                                            loadFiles(currentDir);
                                                        }
                                                        if (adapter != null) {
                                                            adapter.clearSelection();
                                                            isSelectionMode = false;
                                                            updateToolbarState();
                                                        }
                                                    });
                                        }
                                    });
                        } else if (targetFile != null) {
                            List<FileModel> singleList = new ArrayList<>();
                            singleList.add(new FileModel(targetFile));
                            DialogHelper.showMoveDialog(
                                    MainActivity.this,
                                    singleList,
                                    new DialogHelper.Callback() {
                                        @Override
                                        public void onActionCompleted() {}

                                        @Override
                                        public void onActionCompleted(File targetFolder) {
                                            runOnUiThread(
                                                    () -> {
                                                        if (targetFolder != null
                                                                && targetFolder.exists()) {
                                                            loadFiles(targetFolder);
                                                        } else if (currentDir != null) {
                                                            loadFiles(currentDir);
                                                        }
                                                        if (adapter != null) {
                                                            adapter.clearSelection();
                                                            isSelectionMode = false;
                                                            updateToolbarState();
                                                        }
                                                    });
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCopySelected(List<FileModel> selectedFiles, File targetFile) {
                        if (selectedFiles != null && !selectedFiles.isEmpty()) {
                            DialogHelper.showCopyDialog(
                                    MainActivity.this,
                                    selectedFiles,
                                    new DialogHelper.Callback() {
                                        @Override
                                        public void onActionCompleted() {}

                                        @Override
                                        public void onActionCompleted(File targetFolder) {
                                            runOnUiThread(
                                                    () -> {
                                                        if (targetFolder != null
                                                                && targetFolder.exists()) {
                                                            loadFiles(targetFolder);
                                                        } else if (currentDir != null) {
                                                            loadFiles(currentDir);
                                                        }
                                                        if (adapter != null) {
                                                            adapter.clearSelection();
                                                            isSelectionMode = false;
                                                            updateToolbarState();
                                                        }
                                                    });
                                        }
                                    });
                        } else if (targetFile != null) {
                            List<FileModel> singleList = new ArrayList<>();
                            singleList.add(new FileModel(targetFile));
                            DialogHelper.showCopyDialog(
                                    MainActivity.this,
                                    singleList,
                                    new DialogHelper.Callback() {
                                        @Override
                                        public void onActionCompleted() {}

                                        @Override
                                        public void onActionCompleted(File targetFolder) {
                                            runOnUiThread(
                                                    () -> {
                                                        if (targetFolder != null
                                                                && targetFolder.exists()) {
                                                            loadFiles(targetFolder);
                                                        } else if (currentDir != null) {
                                                            loadFiles(currentDir);
                                                        }
                                                        if (adapter != null) {
                                                            adapter.clearSelection();
                                                            isSelectionMode = false;
                                                            updateToolbarState();
                                                        }
                                                    });
                                        }
                                    });
                        }
                    }
                });
    }

    // ==================== FILTER FILES ====================

    private void filterFiles(String text) {
        searchHelper.filterFiles(
                text,
                fileExecutor,
                currentDir,
                new SearchHelper.SearchCallback() {
                    @Override
                    public void onSearchStarted() {
                        isSearching = true;
                        if (fabNewFolder != null) {
                            fabNewFolder.hide();
                        }
                    }

                    @Override
                    public void onSearchCompleted(List<FileModel> results) {
                        if (fabNewFolder != null) {
                            fabNewFolder.hide();
                        }

                        if (adapter != null) {
                            adapter.updateData(results);

                            if (results == null || results.isEmpty()) {
                                showEmptyState();
                            } else {
                                showContentState();
                            }

                            adapter.clearSelection();
                            isSelectionMode = false;
                            updateToolbarState();
                        }
                    }

                    @Override
                    public void onSearchCleared() {
                        isSearching = false;
                        loadFiles(currentDir);
                    }
                });
    }

    // ==================== DIALOG HELPERS ====================

    private void showCreateFolderDialog() {
        DialogHelper.showCreateFolderDialog(this, currentDir, () -> loadFiles(currentDir));
    }

    private void showRenameDialogForFile(File targetFile) {
        DialogHelper.showRenameDialogForFile(this, targetFile, () -> loadFiles(currentDir));
    }

    private void showBatchRenameDialog(List<FileModel> selectedFiles) {
        DialogHelper.showBatchRenameDialog(
                this,
                selectedFiles,
                () -> {
                    loadFiles(currentDir);
                    if (adapter != null) {
                        adapter.clearSelection();
                        isSelectionMode = false;
                        updateToolbarState();
                    }
                });
    }

    private void showDeleteConfirmationForFile(File targetFile) {
        DialogHelper.showDeleteConfirmationForFile(
                this,
                targetFile,
                () -> {
                    runOnUiThread(
                            () -> {
                                loadFiles(currentDir);
                                if (adapter != null) {
                                    adapter.clearSelection();
                                    isSelectionMode = false;
                                    updateToolbarState();
                                }
                            });
                });
    }

    private void showPropertiesDialogForFile(File targetFile) {
        DialogHelper.showPropertiesDialogForFile(this, targetFile);
    }

    public void refreshFileList() {
        runOnUiThread(
                () -> {
                    if (currentDir != null) {
                        loadFiles(currentDir);
                    }
                });
    }

    public File getCurrentDirectory() {
        return currentDir;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SETTINGS) {
            int currentThemeMode = SettingsHelper.getThemeMode(this);
            boolean currentAmoledMode = SettingsHelper.isAmoledMode(this);

            if (currentThemeMode != lastThemeMode || currentAmoledMode != lastAmoledMode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAfterTransition();
                    Intent newIntent = new Intent(this, MainActivity.class);
                    newIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(newIntent);
                } else {
                    finish();
                    overridePendingTransition(0, 0);
                    startActivity(new Intent(this, MainActivity.class));
                    overridePendingTransition(0, 0);
                }
                return;
            }
        }

        if (requestCode == PermissionHelper.STORAGE_PERMISSION_CODE) {
            if (PermissionHelper.checkPermission(this)) {
                initFileManager();
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fileExecutor != null) {
            fileExecutor.shutdown();
        }
        if (searchHandler != null) {
            searchHandler.removeCallbacksAndMessages(null);
        }
        if (folderCacheMap != null) {
            folderCacheMap.clear();
        }
        if (folderScrollPositionMap != null) {
            folderScrollPositionMap.clear();
        }
    }
}
