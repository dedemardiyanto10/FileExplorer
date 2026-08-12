package com.fileexplorer.app;

import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.TypedValue;
import android.view.WindowInsetsController;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.fileexplorer.app.databinding.ActivityStorageAnalyzerBinding;
import com.fileexplorer.app.databinding.ItemStorageCategoryBinding;
import com.google.android.material.color.DynamicColors;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageAnalyzerActivity extends AppCompatActivity {

    private ActivityStorageAnalyzerBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingsHelper.applyThemeOnStartup(this);
        super.onCreate(savedInstanceState);

        DynamicColors.applyToActivityIfAvailable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityStorageAnalyzerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SettingsHelper.applyAmoledMode(this, binding.getRoot(), null, binding.topAppBar, null);

        setupStatusBar();

        setSupportActionBar(binding.topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.topAppBar.setNavigationOnClickListener(v -> finish());

        loadStorageData();
    }

    private void setupStatusBar() {
        boolean isNight =
                (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;

        int statusBarColor;
        if (isNight && SettingsHelper.isAmoledMode(this)) {
            statusBarColor = Color.BLACK;
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
            int appearanceMask = isNight ? 0 : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
            getWindow()
                    .getInsetsController()
                    .setSystemBarsAppearance(
                            appearanceMask, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }
    }

    private void loadStorageData() {
        binding.tvStorageSummary.setText("Menghitung penyimpanan...");

        executor.execute(
                () -> {
                    File path = Environment.getExternalStorageDirectory();
                    StatFs stat = new StatFs(path.getPath());
                    long totalBytes = stat.getBlockCountLong() * stat.getBlockSizeLong();
                    long freeBytes = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
                    long usedBytes = totalBytes - freeBytes;

                    long imageBytes = getMediaSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    long videoBytes = getMediaSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    long audioBytes = getMediaSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);

                    long docBytes =
                            scanFolderExtensions(
                                    new String[] {
                                        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
                                        ".txt", ".odt"
                                    });
                    long apkBytes = scanFolderExtensions(new String[] {".apk"});
                    long archiveBytes =
                            scanFolderExtensions(
                                    new String[] {".zip", ".rar", ".7z", ".tar", ".gz"});

                    long accountedBytes =
                            imageBytes
                                    + videoBytes
                                    + audioBytes
                                    + docBytes
                                    + apkBytes
                                    + archiveBytes;
                    long otherBytes = Math.max(0, usedBytes - accountedBytes);

                    runOnUiThread(
                            () -> {
                                binding.tvStorageSummary.setText(
                                        Formatter.formatFileSize(this, usedBytes)
                                                + " terpakai dari "
                                                + Formatter.formatFileSize(this, totalBytes));
                                binding.tvUsedStorage.setText(
                                        Formatter.formatFileSize(this, usedBytes));
                                binding.tvFreeStorage.setText(
                                        Formatter.formatFileSize(this, freeBytes));
                                int mainPercent =
                                        totalBytes > 0 ? (int) ((usedBytes * 100) / totalBytes) : 0;
                                binding.progressStorageMain.setProgress(0);
                                binding.progressStorageMain.post(
                                        () -> {
                                            binding.progressStorageMain.setProgress(
                                                    mainPercent, true);
                                        });

                                setupCategoryItem(
                                        binding.layoutImages,
                                        "Gambar",
                                        imageBytes,
                                        usedBytes,
                                        R.drawable.ic_image);
                                setupCategoryItem(
                                        binding.layoutVideos,
                                        "Video",
                                        videoBytes,
                                        usedBytes,
                                        R.drawable.ic_film);
                                setupCategoryItem(
                                        binding.layoutAudio,
                                        "Audio",
                                        audioBytes,
                                        usedBytes,
                                        R.drawable.ic_audio);
                                setupCategoryItem(
                                        binding.layoutDocuments,
                                        "Dokumen",
                                        docBytes,
                                        usedBytes,
                                        R.drawable.ic_doc);
                                setupCategoryItem(
                                        binding.layoutApk,
                                        "Aplikasi (APK)",
                                        apkBytes,
                                        usedBytes,
                                        R.drawable.ic_apk);
                                setupCategoryItem(
                                        binding.layoutArchives,
                                        "Arsip",
                                        archiveBytes,
                                        usedBytes,
                                        R.drawable.ic_compress);

                                setupCategoryItem(
                                        binding.layoutOthers,
                                        "Lainnya",
                                        otherBytes,
                                        usedBytes,
                                        R.drawable.ic_file);
                            });
                });
    }

    private long getMediaSize(Uri uri) {
        long size = 0;
        String[] projection = {MediaStore.MediaColumns.SIZE};
        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                do {
                    if (sizeColumn != -1) {
                        size += cursor.getLong(sizeColumn);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {
        }
        return size;
    }

    private long scanFolderExtensions(String[] extensions) {
        long totalSize = 0;
        File root = Environment.getExternalStorageDirectory();
        if (root == null || !root.exists()) return 0;

        File[] targetFolders = {
            new File(root, "Download"),
            new File(root, "Documents"),
            new File(root, "Download/Telegram"),
            new File(root, "WhatsApp/Media/WhatsApp Documents"),
            new File(root, "WhatsApp/Media/WhatsApp Audio"),
            new File(root, "Bluetooth")
        };

        for (File folder : targetFolders) {
            if (folder.exists() && folder.isDirectory()) {
                totalSize += scanDirectoryRecursive(folder, extensions);
            }
        }

        if (totalSize == 0) {
            File[] rootFiles = root.listFiles();
            if (rootFiles != null) {
                for (File f : rootFiles) {
                    if (f.isDirectory()
                            && !f.getName().startsWith(".")
                            && !f.getName().equals("Android")) {
                        totalSize += scanDirectoryRecursive(f, extensions);
                    } else if (!f.isDirectory()) {
                        String nameLower = f.getName().toLowerCase();
                        for (String ext : extensions) {
                            if (nameLower.endsWith(ext)) {
                                totalSize += f.length();
                                break;
                            }
                        }
                    }
                }
            }
        }

        return totalSize;
    }

    private long scanDirectoryRecursive(File dir, String[] extensions) {
        long size = 0;
        if (dir == null || !dir.exists() || !dir.isDirectory()) return 0;

        String dirName = dir.getName();
        if (dir.isHidden() || dirName.startsWith(".") || dirName.equals("Android")) {
            return 0;
        }

        File[] files = dir.listFiles();
        if (files == null) return 0;

        for (File f : files) {
            if (f.isHidden()) continue;
            if (f.isDirectory()) {
                size += scanDirectoryRecursive(f, extensions);
            } else {
                String nameLower = f.getName().toLowerCase();
                for (String ext : extensions) {
                    if (nameLower.endsWith(ext)) {
                        size += f.length();
                        break;
                    }
                }
            }
        }
        return size;
    }

    private void setupCategoryItem(
            ItemStorageCategoryBinding itemBinding,
            String title,
            long sizeBytes,
            long usedBytes,
            int iconRes) {
        if (itemBinding == null) return;

        itemBinding.tvCategoryName.setText(title);
        itemBinding.tvCategorySize.setText(Formatter.formatFileSize(this, sizeBytes));
        itemBinding.ivCategoryIcon.setImageResource(iconRes);

        int percent = usedBytes > 0 ? (int) ((sizeBytes * 100) / usedBytes) : 0;

        itemBinding.progressCategory.post(
                () -> {
                    itemBinding.progressCategory.setProgress(0);
                    itemBinding.progressCategory.setProgress(percent, true);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
