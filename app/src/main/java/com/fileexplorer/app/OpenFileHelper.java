package com.fileexplorer.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.Locale;

public class OpenFileHelper {

    // ===== CEK APAKAH FILE ADALAH ARCHIVE =====
    public static boolean isArchiveFile(File file) {
        if (file == null || file.isDirectory()) return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(".zip")
                || name.endsWith(".rar")
                || name.endsWith(".7z")
                || name.endsWith(".tar")
                || name.endsWith(".gz")
                || name.endsWith(".tar.gz")
                || name.endsWith(".tgz")
                || name.endsWith(".bz2")
                || name.endsWith(".xz")
                || name.endsWith(".zst");
    }

    public static void openFile(Context context, File file) {
        try {
            // ===== CEK APAKAH FILE ARCHIVE =====
            if (isArchiveFile(file)) {
                // Tampilkan dialog ekstrak
                DialogHelper.showExtractDialog(
                        context,
                        file,
                        () -> {
                            Toast.makeText(context, "Ekstrak selesai", Toast.LENGTH_SHORT).show();

                            // ===== REFRESH DATA SETELAH EKSTRAK =====
                            if (context instanceof MainActivity) {
                                ((MainActivity) context).refreshFileList();
                            }
                        });
                return;
            }

            Uri uri =
                    FileProvider.getUriForFile(
                            context, context.getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            String fileName = file.getName().toLowerCase(Locale.getDefault());
            if (fileName.endsWith(".apk")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!context.getPackageManager().canRequestPackageInstalls()) {
                        Toast.makeText(
                                        context,
                                        "Izinkan instalasi aplikasi dari sumber ini",
                                        Toast.LENGTH_LONG)
                                .show();
                        Intent permissionIntent =
                                new Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:" + context.getPackageName()));
                        if (context instanceof Activity) {
                            context.startActivity(permissionIntent);
                        } else {
                            permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(permissionIntent);
                        }
                        return;
                    }
                }
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            } else {
                String mimeType = context.getContentResolver().getType(uri);
                if (mimeType == null) {
                    mimeType = "*/*";
                }
                intent.setDataAndType(uri, mimeType);
            }

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Toast.makeText(
                                context,
                                "Tidak ada aplikasi untuk membuka file ini",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Gagal membuka file: " + e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
