package com.fileexplorer.app;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class SearchHelper {

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private boolean isSearching = false;

    public interface SearchCallback {
        void onSearchStarted();

        void onSearchCompleted(List<FileModel> results);

        void onSearchCleared();
    }

    public void filterFiles(
            String text, ExecutorService executor, File currentDir, SearchCallback callback) {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        if (callback != null) {
            callback.onSearchStarted();
        }
        isSearching = true;

        searchRunnable =
                () -> {
                    if (text == null || text.trim().isEmpty()) {
                        isSearching = false;
                        if (callback != null) {
                            callback.onSearchCleared();
                        }
                        return;
                    }

                    String searchQuery = text.toLowerCase(Locale.getDefault()).trim();

                    executor.execute(
                            () -> {
                                List<FileModel> filteredList = new ArrayList<>();
                                Set<String> pathSet = new HashSet<>();

                                if (currentDir != null && currentDir.exists()) {
                                    searchInDirectory(
                                            currentDir,
                                            searchQuery,
                                            filteredList,
                                            pathSet,
                                            0,
                                            3,
                                            200);
                                }

                                if (filteredList.size() < 50) {
                                    File rootDir = Environment.getExternalStorageDirectory();
                                    if (rootDir != null && rootDir.exists()) {
                                        searchInDirectory(
                                                rootDir,
                                                searchQuery,
                                                filteredList,
                                                pathSet,
                                                0,
                                                5,
                                                200);
                                    }
                                }

                                if (!filteredList.isEmpty()) {
                                    Collections.sort(
                                            filteredList,
                                            (m1, m2) -> {
                                                String name1 =
                                                        m1.getFile()
                                                                .getName()
                                                                .toLowerCase(Locale.getDefault());
                                                String name2 =
                                                        m2.getFile()
                                                                .getName()
                                                                .toLowerCase(Locale.getDefault());

                                                boolean startsWith1 = name1.startsWith(searchQuery);
                                                boolean startsWith2 = name2.startsWith(searchQuery);

                                                if (startsWith1 && !startsWith2) return -1;
                                                if (!startsWith1 && startsWith2) return 1;

                                                if (m1.getFile().isDirectory()
                                                        && !m2.getFile().isDirectory()) return -1;
                                                if (!m1.getFile().isDirectory()
                                                        && m2.getFile().isDirectory()) return 1;

                                                return name1.compareTo(name2);
                                            });
                                }

                                isSearching = false;
                                searchHandler.post(
                                        () -> {
                                            if (callback != null) {
                                                callback.onSearchCompleted(filteredList);
                                            }
                                        });
                            });
                };

        searchHandler.postDelayed(searchRunnable, 200);
    }

    private void searchInDirectory(
            File dir,
            String query,
            List<FileModel> result,
            Set<String> pathSet,
            int depth,
            int maxDepth,
            int maxResults) {
        if (result.size() >= maxResults) return;
        if (depth >= maxDepth) return;
        if (dir == null || !dir.exists()) return;

        if (!dir.canRead()) return;

        String path = dir.getAbsolutePath();
        if (path.contains("/Android/data/")
                || path.contains("/Android/obb/")
                || path.contains("/system/")
                || path.contains("/proc/")
                || path.contains("/sys/")) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) return;

        int maxFilesPerFolder = 200;
        int fileCount = 0;

        for (File file : files) {
            if (fileCount >= maxFilesPerFolder) break;
            if (result.size() >= maxResults) return;

            String fileName = file.getName().toLowerCase(Locale.getDefault());

            if (fileName.startsWith(".")) continue;

            fileCount++;

            if (file.isDirectory()) {
                if (fileName.contains(query)) {
                    String absPath = file.getAbsolutePath();
                    if (pathSet.add(absPath)) {
                        result.add(new FileModel(file));
                        if (result.size() >= maxResults) return;
                    }
                }
                searchInDirectory(file, query, result, pathSet, depth + 1, maxDepth, maxResults);
            } else {
                if (fileName.contains(query)) {
                    String absPath = file.getAbsolutePath();
                    if (pathSet.add(absPath)) {
                        result.add(new FileModel(file));
                    }
                }
            }
        }
    }

    public void cancelSearch() {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        isSearching = false;
    }
}
