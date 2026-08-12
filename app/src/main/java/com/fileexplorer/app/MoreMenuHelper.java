package com.fileexplorer.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MoreMenuHelper {

    public interface MenuActionListener {
        void onRenameSelected(List<FileModel> selectedFiles, File targetFile);

        void onShareSelected(List<FileModel> selectedFiles, File targetFile);

        void onDeleteSelected(List<FileModel> selectedFiles, File targetFile);

        void onPropertiesSelected(File targetFile);

        void onCompressSelected(List<FileModel> selectedFiles);

        void onExtractSelected(File zipFile);

        void onMoveSelected(List<FileModel> selectedFiles, File targetFile);

        void onCopySelected(List<FileModel> selectedFiles, File targetFile);
    }

    // ===== METHOD CEK ARCHIVE FILE =====
    private static boolean isArchiveFile(File file) {
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

    public static void showCustomMoreMenu(
            Activity activity,
            View anchorView,
            File currentSelectedFile,
            List<FileModel> allFiles,
            MenuActionListener listener) {
        if (anchorView == null || activity == null) return;

        View view = activity.getLayoutInflater().inflate(R.layout.dialog_more_menu, null);

        TextView tvMenuFileName = view.findViewById(R.id.tvMenuFileName);
        View menuRename = view.findViewById(R.id.menuRename);
        View menuShare = view.findViewById(R.id.menuShare);
        View menuCompress = view.findViewById(R.id.menuCompress);
        View menuDelete = view.findViewById(R.id.menuDelete);
        View menuProperties = view.findViewById(R.id.menuProperties);
        View menuMove = view.findViewById(R.id.menuMove);

        ImageView ivCompressIcon = view.findViewById(R.id.ivCompressIcon);
        TextView tvCompressTitle = view.findViewById(R.id.tvCompressTitle);
        TextView tvCompressSub = view.findViewById(R.id.tvCompressSub);

        // ===== TAMBAHKAN VIEW UNTUK MOVE =====
        TextView tvMoveTitle = view.findViewById(R.id.tvMoveTitle);
        TextView tvMoveSub = view.findViewById(R.id.tvMoveSub);

        // ===== TAMBAHKAN VIEW UNTUK COPY =====
        // ===== DI BAGIAN INISIALISASI VIEW =====
        View menuCopy = view.findViewById(R.id.menuCopy);
        TextView tvCopyTitle = view.findViewById(R.id.tvCopyTitle);
        TextView tvCopySub = view.findViewById(R.id.tvCopySub);

        // ===== COPY CLICK LISTENER =====

        // ===== AMBIL FILE DARI MORE MENU (currentSelectedFile) =====
        List<FileModel> selectedFiles = new ArrayList<>();
        boolean hasSelection = false;

        if (allFiles != null) {
            for (FileModel model : allFiles) {
                if (model.isSelected()) {
                    selectedFiles.add(model);
                    hasSelection = true;
                }
            }
        }

        // ===== JIKA TIDAK ADA SELECTION, GUNAKAN FILE DARI MORE MENU =====
        if (!hasSelection && currentSelectedFile != null) {
            FileModel singleModel = new FileModel(currentSelectedFile);
            selectedFiles.add(singleModel);
        }

        boolean hasFolder = false;
        boolean hasArchive = false;
        boolean allArchives = false;

        for (FileModel model : selectedFiles) {
            if (model.getFile().isDirectory()) {
                hasFolder = true;
            }
            if (isArchiveFile(model.getFile())) {
                hasArchive = true;
            }
        }

        // CEK APAKAH SEMUA FILE ARCHIVE
        if (selectedFiles.size() > 1) {
            allArchives = true;
            for (FileModel model : selectedFiles) {
                if (!isArchiveFile(model.getFile())) {
                    allArchives = false;
                    break;
                }
            }
        }

        final File targetFile =
                (selectedFiles.size() <= 1 && currentSelectedFile != null)
                        ? currentSelectedFile
                        : (selectedFiles.size() == 1 ? selectedFiles.get(0).getFile() : null);

        // ===== CEK APAKAH SINGLE ARCHIVE FILE =====
        boolean isSingleArchive = false;
        if (selectedFiles.size() == 1 && targetFile != null && !targetFile.isDirectory()) {
            isSingleArchive = isArchiveFile(targetFile);
        }

        // ===== SET JUDUL =====
        if (selectedFiles.size() > 1) {
            tvMenuFileName.setText(selectedFiles.size() + " item dipilih");
            menuProperties.setVisibility(View.GONE);

            if (hasFolder) {
                menuRename.setVisibility(View.GONE);
            } else {
                menuRename.setVisibility(View.VISIBLE);
            }
        } else {
            if (targetFile != null) {
                tvMenuFileName.setText(targetFile.getName());
            }
            menuRename.setVisibility(View.VISIBLE);
            menuProperties.setVisibility(View.VISIBLE);
        }

        // ===== SET MOVE TITLE & SUBTITLE =====
        if (selectedFiles.size() > 1) {
            tvMoveTitle.setText("Pindahkan (" + selectedFiles.size() + " item)");
            tvMoveSub.setText("Pindahkan " + selectedFiles.size() + " file ke folder lain");
        } else {
            if (targetFile != null) {
                tvMoveTitle.setText("Pindahkan");
                tvMoveSub.setText("Pindahkan " + targetFile.getName() + " ke folder lain");
            } else {
                tvMoveTitle.setText("Pindahkan");
                tvMoveSub.setText("Pindahkan file ke folder lain");
            }
        }

        // ===== SET COPY TITLE & SUBTITLE =====
        if (selectedFiles.size() > 1) {
            tvCopyTitle.setText("Salin (" + selectedFiles.size() + " item)");
            tvCopySub.setText("Salin " + selectedFiles.size() + " file ke folder lain");
        } else {
            if (targetFile != null) {
                tvCopyTitle.setText("Salin");
                tvCopySub.setText("Salin " + targetFile.getName() + " ke folder lain");
            } else {
                tvCopyTitle.setText("Salin");
                tvCopySub.setText("Salin file ke folder lain");
            }
        }

        if (isSingleArchive) {
            ivCompressIcon.setImageResource(R.drawable.ic_unarchive);
            tvCompressTitle.setText("Ekstrak");
            tvCompressSub.setText("Ekstrak " + targetFile.getName());
            menuCompress.setVisibility(View.VISIBLE);
        } else if (selectedFiles.size() == 1 && targetFile != null && targetFile.isDirectory()) {
            ivCompressIcon.setImageResource(R.drawable.ic_compress);
            tvCompressTitle.setText("Kompres Folder");
            tvCompressSub.setText("Jadikan " + targetFile.getName() + ".zip");
            menuCompress.setVisibility(View.VISIBLE);
        } else if (selectedFiles.size() > 1 && allArchives) {
            menuCompress.setVisibility(View.GONE);
        } else if (selectedFiles.size() == 1 && targetFile != null && !targetFile.isDirectory()) {
            ivCompressIcon.setImageResource(R.drawable.ic_compress);
            tvCompressTitle.setText("Kompres");
            tvCompressSub.setText("Jadikan " + targetFile.getName() + ".zip");
            menuCompress.setVisibility(View.VISIBLE);
        } else if (selectedFiles.size() > 1 && hasFolder) {
            // ===== TAMPILKAN COMPRESS UNTUK FOLDER + FILE =====
            ivCompressIcon.setImageResource(R.drawable.ic_compress);
            tvCompressTitle.setText("Kompres (" + selectedFiles.size() + " item)");
            tvCompressSub.setText("Jadikan satu file zip");
            menuCompress.setVisibility(View.VISIBLE);
        } else if (selectedFiles.size() > 1 && hasArchive && !allArchives) {
            ivCompressIcon.setImageResource(R.drawable.ic_compress);
            tvCompressTitle.setText("Kompres (" + selectedFiles.size() + " item)");
            tvCompressSub.setText("Jadikan satu file zip");
            menuCompress.setVisibility(View.VISIBLE);
        } else if (selectedFiles.size() > 1 && !hasFolder && !hasArchive) {
            ivCompressIcon.setImageResource(R.drawable.ic_compress);
            tvCompressTitle.setText("Kompres (" + selectedFiles.size() + " item)");
            tvCompressSub.setText("Jadikan satu file zip");
            menuCompress.setVisibility(View.VISIBLE);
        } else {
            ivCompressIcon.setImageResource(R.drawable.ic_compress);
            tvCompressTitle.setText("Kompres");
            tvCompressSub.setText("Jadikan file zip");
            menuCompress.setVisibility(View.VISIBLE);
        }

        // ===== BUAT FINAL VARIABLE UNTUK LAMBDA =====
        final boolean isSingleArchiveFinal = isSingleArchive;
        final File targetFileFinal = targetFile;
        final List<FileModel> selectedFilesFinal = selectedFiles;

        int widthInPx =
                (int)
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                240,
                                activity.getResources().getDisplayMetrics());

        PopupWindow popupWindow =
                new PopupWindow(
                        view, widthInPx, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(8.0f);

        menuRename.setOnClickListener(
                v -> {
                    popupWindow.dismiss();
                    if (listener != null) {
                        listener.onRenameSelected(selectedFilesFinal, targetFileFinal);
                    }
                });

        menuShare.setOnClickListener(
                v -> {
                    popupWindow.dismiss();
                    if (listener != null) {
                        listener.onShareSelected(selectedFilesFinal, targetFileFinal);
                    } else {
                        defaultShareAction(activity, selectedFilesFinal, targetFileFinal);
                    }
                });

        menuMove.setOnClickListener(
                v -> {
                    popupWindow.dismiss();
                    if (listener != null) {
                        listener.onMoveSelected(selectedFilesFinal, targetFileFinal);
                    }
                });

        menuCopy.setOnClickListener(
                v -> {
                    popupWindow.dismiss();
                    if (listener != null) {
                        listener.onCopySelected(selectedFilesFinal, targetFileFinal);
                    }
                });

        menuCompress.setOnClickListener(
                v -> {
                    popupWindow.dismiss();
                    if (listener != null) {
                        if (isSingleArchiveFinal) {
                            listener.onExtractSelected(targetFileFinal);
                        } else {
                            listener.onCompressSelected(selectedFilesFinal);
                        }
                    }
                });

        menuDelete.setOnClickListener(
                v -> {
                    popupWindow.dismiss();
                    if (listener != null) {
                        listener.onDeleteSelected(selectedFilesFinal, targetFileFinal);
                    }
                });

        menuProperties.setOnClickListener(
                v -> {
                    popupWindow.dismiss();
                    if (listener != null) {
                        listener.onPropertiesSelected(targetFileFinal);
                    }
                });

        view.measure(
                View.MeasureSpec.makeMeasureSpec(widthInPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED);
        int popupHeight = view.getMeasuredHeight();

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);

        int x = location[0] + anchorView.getWidth() - widthInPx;

        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int marginSafety = 48;

        if (x < marginSafety) {
            x = marginSafety;
        } else if (x + widthInPx > screenWidth - marginSafety) {
            x = screenWidth - widthInPx - marginSafety;
        }

        int y = location[1];
        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;

        if (y > screenHeight / 2) {
            y = y - popupHeight - 10;
        } else {
            y = y + anchorView.getHeight() + 10;
        }

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);
    }

    public static void defaultShareAction(
            Activity activity, List<FileModel> selectedFiles, File targetFile) {
        if (selectedFiles == null) {
            selectedFiles = new ArrayList<>();
        }

        if (selectedFiles.size() > 1) {
            ArrayList<Uri> uriList = new ArrayList<>();
            for (FileModel model : selectedFiles) {
                if (model != null && model.getFile() != null && !model.getFile().isDirectory()) {
                    try {
                        Uri uri =
                                FileProvider.getUriForFile(
                                        activity,
                                        activity.getPackageName() + ".fileprovider",
                                        model.getFile());
                        uriList.add(uri);
                    } catch (Exception e) {
                        // Skip file yang error
                    }
                }
            }

            if (!uriList.isEmpty()) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                shareIntent.setType("*/*");
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivity(Intent.createChooser(shareIntent, "Bagikan file via"));
            } else {
                Toast.makeText(activity, "Tidak ada file valid yang dipilih", Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (targetFile != null && !targetFile.isDirectory()) {
            try {
                Uri uri =
                        FileProvider.getUriForFile(
                                activity, activity.getPackageName() + ".fileprovider", targetFile);

                String mimeType = activity.getContentResolver().getType(uri);
                if (mimeType == null) mimeType = "*/*";

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType(mimeType);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivity(Intent.createChooser(shareIntent, "Bagikan file via"));
            } catch (Exception e) {
                Toast.makeText(activity, "Gagal membagikan file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(activity, "Folder tidak dapat dibagikan", Toast.LENGTH_SHORT).show();
        }
    }
}
