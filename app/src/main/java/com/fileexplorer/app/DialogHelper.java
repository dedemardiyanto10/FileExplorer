package com.fileexplorer.app;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fileexplorer.app.databinding.DialogAudioPlayerBinding;
import com.fileexplorer.app.databinding.DialogBatchRenameBinding;
import com.fileexplorer.app.databinding.DialogCompressBinding;
import com.fileexplorer.app.databinding.DialogCreateFolderBinding;
import com.fileexplorer.app.databinding.DialogDeleteConfirmationBinding;
import com.fileexplorer.app.databinding.DialogExitConfirmationBinding;
import com.fileexplorer.app.databinding.DialogMoveBinding;
import com.fileexplorer.app.databinding.DialogPropertiesBinding;
import com.fileexplorer.app.databinding.DialogRenameBinding;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DialogHelper {

    public interface Callback {
        void onActionCompleted();

        default void onActionCompleted(File targetFolder) {
            onActionCompleted();
        }
    }

    public static void showCreateFolderDialog(Context context, File currentDir, Callback callback) {
        if (currentDir == null) return;

        DialogCreateFolderBinding binding =
                DialogCreateFolderBinding.inflate(android.view.LayoutInflater.from(context));

        binding.tvHeader.setText("Buat Folder Baru");

        AlertDialog dialog =
                DialogUtils.createBottomSlideDialog(
                        context,
                        d -> {
                            d.setView(binding.getRoot());
                            d.setButton(
                                    AlertDialog.BUTTON_POSITIVE,
                                    "Buat",
                                    (dialogInt, which) -> {
                                        String folderName =
                                                binding.inputFolderName.getText().toString().trim();

                                        if (folderName.isEmpty()) {
                                            Toast.makeText(
                                                            context,
                                                            "Nama folder tidak boleh kosong",
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                            return;
                                        }

                                        File newFolder = new File(currentDir, folderName);

                                        if (newFolder.exists()) {
                                            Toast.makeText(
                                                            context,
                                                            "Folder dengan nama tersebut sudah ada",
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                            return;
                                        }

                                        if (newFolder.mkdir()) {
                                            Toast.makeText(
                                                            context,
                                                            "Berhasil membuat folder",
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                            callback.onActionCompleted();
                                        } else {
                                            Toast.makeText(
                                                            context,
                                                            "Gagal membuat folder",
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                        }
                                    });
                            d.setButton(
                                    AlertDialog.BUTTON_NEGATIVE,
                                    "Batal",
                                    (dialogInt, which) -> {
                                        dialogInt.dismiss();
                                    });
                        });

        dialog.show();

        binding.inputFolderName.postDelayed(
                () -> {
                    binding.inputFolderName.requestFocus();
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager)
                                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(
                                binding.inputFolderName,
                                android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                    }
                },
                100);
    }

    public static void showRenameDialogForFile(
            Context context, File targetFile, Callback callback) {
        if (targetFile == null) return;

        DialogRenameBinding binding =
                DialogRenameBinding.inflate(android.view.LayoutInflater.from(context));

        binding.tvHeader.setText("Ubah Nama File");

        String currentName = targetFile.getName();
        binding.inputNewName.setText(currentName);
        binding.inputNewName.selectAll();

        DialogUtils.createBottomSlideDialog(
                        context,
                        dialog -> {
                            dialog.setView(binding.getRoot());
                            dialog.setButton(
                                    AlertDialog.BUTTON_POSITIVE,
                                    "Simpan",
                                    (dialogInt, which) -> {
                                        String newName =
                                                binding.inputNewName.getText().toString().trim();

                                        if (newName.isEmpty()) {
                                            Toast.makeText(
                                                            context,
                                                            "Nama baru tidak boleh kosong",
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                            return;
                                        }

                                        File newFile = new File(targetFile.getParent(), newName);

                                        if (targetFile.renameTo(newFile)) {
                                            Toast.makeText(
                                                            context,
                                                            "Berhasil mengubah nama file",
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                            callback.onActionCompleted();
                                        } else {
                                            Toast.makeText(
                                                            context,
                                                            "Gagal mengubah nama file",
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                        }
                                    });
                            dialog.setButton(
                                    AlertDialog.BUTTON_NEGATIVE,
                                    "Batal",
                                    (dialogInt, which) -> {
                                        dialog.dismiss();
                                    });
                        })
                .show();
    }

    public static void showBatchRenameDialog(
            Context context, List<FileModel> selectedFiles, Callback callback) {
        if (selectedFiles == null || selectedFiles.isEmpty()) return;

        DialogBatchRenameBinding binding =
                DialogBatchRenameBinding.inflate(android.view.LayoutInflater.from(context));

        binding.tvHeader.setText("Ubah nama " + selectedFiles.size() + " File");

        binding.radioGroupBatchMode.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (checkedId == R.id.radioCustomPattern) {
                        binding.layoutCustomPattern.setVisibility(View.VISIBLE);
                    } else {
                        binding.layoutCustomPattern.setVisibility(View.GONE);
                    }
                });

        DialogUtils.createBottomSlideDialog(
                        context,
                        dialog -> {
                            dialog.setView(binding.getRoot());
                            dialog.setButton(
                                    AlertDialog.BUTTON_POSITIVE,
                                    "Simpan",
                                    (dialogInt, which) -> {
                                        int checkedId =
                                                binding.radioGroupBatchMode
                                                        .getCheckedRadioButtonId();
                                        boolean success = true;

                                        if (checkedId == R.id.radioCustomPattern) {
                                            String baseName =
                                                    binding.inputNewName
                                                            .getText()
                                                            .toString()
                                                            .trim();
                                            String startNumStr =
                                                    binding.inputStartNumber
                                                            .getText()
                                                            .toString()
                                                            .trim();
                                            String extension =
                                                    binding.inputNewExtension
                                                            .getText()
                                                            .toString()
                                                            .trim();

                                            if (baseName.isEmpty()) {
                                                Toast.makeText(
                                                                context,
                                                                "Nama baru tidak boleh kosong",
                                                                Toast.LENGTH_SHORT)
                                                        .show();
                                                return;
                                            }

                                            int digitLength =
                                                    startNumStr.isEmpty()
                                                            ? 1
                                                            : startNumStr.length();
                                            String numberFormat = "%0" + digitLength + "d";
                                            int currentNum =
                                                    startNumStr.isEmpty()
                                                            ? 1
                                                            : Integer.parseInt(startNumStr);

                                            if (!extension.isEmpty()
                                                    && !extension.startsWith(".")) {
                                                extension = "." + extension;
                                            }

                                            for (int i = 0; i < selectedFiles.size(); i++) {
                                                File file = selectedFiles.get(i).getFile();
                                                String formattedNum =
                                                        String.format(
                                                                Locale.getDefault(),
                                                                numberFormat,
                                                                currentNum);
                                                String finalName =
                                                        baseName + "" + formattedNum + extension;
                                                File newFile =
                                                        new File(file.getParent(), finalName);

                                                if (!file.renameTo(newFile)) {
                                                    success = false;
                                                }
                                                currentNum++;
                                            }

                                        } else if (checkedId == R.id.radioChangeExtensionOnly) {
                                            String extension =
                                                    binding.inputNewExtension
                                                            .getText()
                                                            .toString()
                                                            .trim();
                                            if (extension.isEmpty()) {
                                                Toast.makeText(
                                                                context,
                                                                "Ekstensi baru tidak boleh kosong",
                                                                Toast.LENGTH_SHORT)
                                                        .show();
                                                return;
                                            }

                                            if (!extension.startsWith(".")) {
                                                extension = "." + extension;
                                            }

                                            for (FileModel model : selectedFiles) {
                                                File file = model.getFile();
                                                String fileName = file.getName();
                                                int dotIndex = fileName.lastIndexOf('.');
                                                String nameWithoutExt =
                                                        (dotIndex > 0)
                                                                ? fileName.substring(0, dotIndex)
                                                                : fileName;
                                                String finalName = nameWithoutExt + extension;

                                                File newFile =
                                                        new File(file.getParent(), finalName);
                                                if (!file.renameTo(newFile)) {
                                                    success = false;
                                                }
                                            }
                                        }

                                        if (success) {
                                            Toast.makeText(
                                                            context,
                                                            "Berhasil mengubah nama file",
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                        } else {
                                            Toast.makeText(
                                                            context,
                                                            "Beberapa file gagal diubah namanya",
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                        }
                                        callback.onActionCompleted();
                                    });
                            dialog.setButton(
                                    AlertDialog.BUTTON_NEGATIVE,
                                    "Batal",
                                    (dialogInt, which) -> {
                                        dialog.dismiss();
                                    });
                        })
                .show();
    }

    public static void showDeleteConfirmationForFile(
            Context context, File targetFile, Callback callback) {
        if (targetFile == null) return;

        DialogDeleteConfirmationBinding binding =
                DialogDeleteConfirmationBinding.inflate(android.view.LayoutInflater.from(context));

        binding.tvHeader.setText("Hapus File");
        binding.tvDeleteMessage.setText(
                "Apakah Anda yakin ingin menghapus \"" + targetFile.getName() + "\"?");

        final AlertDialog[] dialogRef = new AlertDialog[1];

        dialogRef[0] =
                DialogUtils.createBottomSlideDialog(
                        context,
                        d -> {
                            d.setView(binding.getRoot());
                            d.setButton(
                                    AlertDialog.BUTTON_POSITIVE, "Hapus", (dialogInt, which) -> {});
                            d.setButton(
                                    AlertDialog.BUTTON_NEGATIVE,
                                    "Batal",
                                    (dialogInt, which) -> {
                                        d.dismiss();
                                    });
                        });

        dialogRef[0].show();

        dialogRef[0]
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        v -> {
                            binding.layoutProgress.setVisibility(View.VISIBLE);
                            binding.tvDeleteSub.setText("Menghapus...");
                            binding.tvDeleteMessage.setText(targetFile.getName());

                            dialogRef[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                            dialogRef[0].getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

                            new Thread(
                                            () -> {
                                                int totalItems = countTotalItems(targetFile);
                                                if (totalItems == 0) totalItems = 1;

                                                final int[] deletedCount = {0};
                                                boolean success =
                                                        deleteRecursiveWithProgress(
                                                                targetFile,
                                                                totalItems,
                                                                deletedCount,
                                                                binding,
                                                                (Activity) context);

                                                ((Activity) context)
                                                        .runOnUiThread(
                                                                () -> {
                                                                    if (success) {
                                                                        Toast.makeText(
                                                                                        context,
                                                                                        "Berhasil menghapus file",
                                                                                        Toast
                                                                                                .LENGTH_SHORT)
                                                                                .show();
                                                                        if (callback != null)
                                                                            callback
                                                                                    .onActionCompleted();
                                                                    } else {
                                                                        Toast.makeText(
                                                                                        context,
                                                                                        "Gagal menghapus beberapa file",
                                                                                        Toast
                                                                                                .LENGTH_SHORT)
                                                                                .show();
                                                                    }
                                                                    dialogRef[0].dismiss();
                                                                });
                                            })
                                    .start();
                        });
    }

    public static void showBatchDeleteDialog(
            Context context, List<FileModel> selectedFiles, Callback callback) {
        if (selectedFiles == null || selectedFiles.isEmpty()) return;

        DialogDeleteConfirmationBinding binding =
                DialogDeleteConfirmationBinding.inflate(android.view.LayoutInflater.from(context));

        binding.tvHeader.setText("Hapus " + selectedFiles.size() + " Item");
        binding.tvDeleteMessage.setText(
                "Apakah Anda yakin ingin menghapus "
                        + selectedFiles.size()
                        + " item yang dipilih?");

        final AlertDialog[] dialogRef = new AlertDialog[1];

        dialogRef[0] =
                DialogUtils.createBottomSlideDialog(
                        context,
                        d -> {
                            d.setView(binding.getRoot());
                            d.setButton(
                                    AlertDialog.BUTTON_POSITIVE, "Hapus", (dialogInt, which) -> {});
                            d.setButton(
                                    AlertDialog.BUTTON_NEGATIVE,
                                    "Batal",
                                    (dialogInt, which) -> {
                                        d.dismiss();
                                    });
                        });

        dialogRef[0].show();

        dialogRef[0]
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        v -> {
                            binding.layoutProgress.setVisibility(View.VISIBLE);
                            binding.tvDeleteSub.setText("Menghapus...");
                            binding.tvDeleteMessage.setText("Memproses penghapusan...");

                            dialogRef[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                            dialogRef[0].getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

                            new Thread(
                                            () -> {
                                                int totalItems = 0;
                                                for (FileModel model : selectedFiles) {
                                                    totalItems += countTotalItems(model.getFile());
                                                }
                                                if (totalItems == 0) totalItems = 1;

                                                final int[] deletedCount = {0};
                                                boolean allSuccess = true;

                                                for (FileModel model : selectedFiles) {
                                                    if (!deleteRecursiveWithProgress(
                                                            model.getFile(),
                                                            totalItems,
                                                            deletedCount,
                                                            binding,
                                                            (Activity) context)) {
                                                        allSuccess = false;
                                                    }
                                                }

                                                final boolean finalAllSuccess = allSuccess;
                                                ((Activity) context)
                                                        .runOnUiThread(
                                                                () -> {
                                                                    if (finalAllSuccess) {
                                                                        Toast.makeText(
                                                                                        context,
                                                                                        "Berhasil menghapus file terpilih",
                                                                                        Toast
                                                                                                .LENGTH_SHORT)
                                                                                .show();
                                                                    } else {
                                                                        Toast.makeText(
                                                                                        context,
                                                                                        "Beberapa file gagal dihapus",
                                                                                        Toast
                                                                                                .LENGTH_SHORT)
                                                                                .show();
                                                                    }

                                                                    if (callback != null) {
                                                                        callback
                                                                                .onActionCompleted();
                                                                    }

                                                                    dialogRef[0].dismiss();
                                                                });
                                            })
                                    .start();
                        });
    }

    private static int countTotalItems(String path) {
        return countTotalItems(new File(path));
    }

    private static int countTotalItems(File file) {
        if (file == null || !file.exists()) return 0;
        if (!file.isDirectory()) return 1;
        int count = 1;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                count += countTotalItems(child);
            }
        }
        return count;
    }

    private static boolean deleteRecursiveWithProgress(
            File fileOrDirectory,
            int totalItems,
            int[] deletedCount,
            DialogDeleteConfirmationBinding binding,
            Activity activity) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursiveWithProgress(child, totalItems, deletedCount, binding, activity);
                }
            }
        }

        boolean deleted = fileOrDirectory.delete();
        deletedCount[0]++;

        final int progress = (int) Math.min(100, (deletedCount[0] * 100) / totalItems);
        final String currentItemName = fileOrDirectory.getName();

        activity.runOnUiThread(
                () -> {
                    binding.progressDelete.setProgress(progress);
                    binding.tvProgressPercent.setText(progress + "%");
                    binding.tvDeleteMessage.setText(currentItemName);
                });

        return deleted;
    }

    public static void showPropertiesDialogForFile(Context context, File targetFile) {
        if (targetFile == null) return;

        DialogPropertiesBinding binding =
                DialogPropertiesBinding.inflate(android.view.LayoutInflater.from(context));

        binding.tvHeader.setText("Informasi File");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String lastModified = sdf.format(new Date(targetFile.lastModified()));

        binding.tvPropName.setText(targetFile.getName());
        binding.tvPropPath.setText(targetFile.getAbsolutePath());
        binding.tvPropSize.setText(
                android.text.format.Formatter.formatFileSize(context, targetFile.length()));
        binding.tvPropDate.setText(lastModified);

        StringBuilder perm = new StringBuilder();
        perm.append(targetFile.canRead() ? "R " : "- ");
        perm.append(targetFile.canWrite() ? "W " : "- ");
        perm.append(targetFile.canExecute() ? "X" : "-");
        binding.tvPropPermissions.setText(perm.toString());

        if (targetFile.isDirectory()) {
            binding.tvPropType.setText("Folder");
            if (binding.layoutChecksum != null) {
                binding.layoutChecksum.setVisibility(View.GONE);
            }
        } else {
            String fileName = targetFile.getName();
            String ext = "";
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                ext = fileName.substring(dotIndex + 1).toLowerCase();
            }
            binding.tvPropType.setText(getDetailedFileType(ext));

            if (binding.layoutChecksum != null) {
                binding.layoutChecksum.setVisibility(View.VISIBLE);
                binding.tvPropMd5.setText("Menghitung...");
                binding.tvPropSha1.setText("Menghitung...");
                binding.tvPropSha256.setText("Menghitung...");

                java.util.concurrent.Executors.newSingleThreadExecutor()
                        .execute(
                                () -> {
                                    String md5 = "-";
                                    String sha1 = "-";
                                    String sha256 = "-";
                                    try {
                                        java.security.MessageDigest md5Digest =
                                                java.security.MessageDigest.getInstance("MD5");
                                        java.security.MessageDigest sha1Digest =
                                                java.security.MessageDigest.getInstance("SHA-1");
                                        java.security.MessageDigest sha256Digest =
                                                java.security.MessageDigest.getInstance("SHA-256");

                                        try (java.io.InputStream fis =
                                                new java.io.FileInputStream(targetFile)) {
                                            byte[] buffer = new byte[8192];
                                            int read;
                                            while ((read = fis.read(buffer)) != -1) {
                                                md5Digest.update(buffer, 0, read);
                                                sha1Digest.update(buffer, 0, read);
                                                sha256Digest.update(buffer, 0, read);
                                            }
                                        }

                                        md5 = bytesToHex(md5Digest.digest());
                                        sha1 = bytesToHex(sha1Digest.digest());
                                        sha256 = bytesToHex(sha256Digest.digest());
                                    } catch (Exception e) {
                                        md5 = "Gagal menghitung";
                                        sha1 = "Gagal menghitung";
                                        sha256 = "Gagal menghitung";
                                    }

                                    final String finalMd5 = md5;
                                    final String finalSha1 = sha1;
                                    final String finalSha256 = sha256;

                                    new android.os.Handler(android.os.Looper.getMainLooper())
                                            .post(
                                                    () -> {
                                                        binding.tvPropMd5.setText(finalMd5);
                                                        binding.tvPropSha1.setText(finalSha1);
                                                        binding.tvPropSha256.setText(finalSha256);
                                                    });
                                });
            }
        }

        String lowerPath = targetFile.getAbsolutePath().toLowerCase();
        boolean isMedia =
                lowerPath.endsWith(".mp4")
                        || lowerPath.endsWith(".mkv")
                        || lowerPath.endsWith(".avi")
                        || lowerPath.endsWith(".mov")
                        || lowerPath.endsWith(".3gp")
                        || lowerPath.endsWith(".mp3")
                        || lowerPath.endsWith(".m4a")
                        || lowerPath.endsWith(".flac")
                        || lowerPath.endsWith(".wav");

        if (!targetFile.isDirectory()
                && isMedia
                && binding.getRoot().findViewById(R.id.layoutMediaMeta) != null) {
            View layoutMedia = binding.getRoot().findViewById(R.id.layoutMediaMeta);
            layoutMedia.setVisibility(View.VISIBLE);

            android.media.MediaMetadataRetriever retriever =
                    new android.media.MediaMetadataRetriever();
            try {
                retriever.setDataSource(targetFile.getAbsolutePath());

                String durationStr =
                        retriever.extractMetadata(
                                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (durationStr != null) {
                    long durationMs = Long.parseLong(durationStr);
                    long seconds = (durationMs / 1000) % 60;
                    long minutes = (durationMs / (1000 * 60)) % 60;
                    long hours = durationMs / (1000 * 60 * 60);

                    String formattedDuration =
                            hours > 0
                                    ? String.format(
                                            Locale.getDefault(),
                                            "%d:%02d:%02d",
                                            hours,
                                            minutes,
                                            seconds)
                                    : String.format(
                                            Locale.getDefault(), "%d:%02d", minutes, seconds);

                    binding.tvPropDuration.setText(formattedDuration);
                }

                String width =
                        retriever.extractMetadata(
                                android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                String height =
                        retriever.extractMetadata(
                                android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                if (width != null && height != null) {
                    binding.tvPropResolution.setText(width + " x " + height + " px");
                } else {
                    binding.tvPropResolution.setText("Audio / Tidak ada video");
                }
            } catch (Exception ignored) {
            } finally {
                try {
                    retriever.release();
                } catch (Exception ignored) {
                }
            }
        } else {
            View layoutMedia = binding.getRoot().findViewById(R.id.layoutMediaMeta);
            if (layoutMedia != null) {
                layoutMedia.setVisibility(View.GONE);
            }
        }

        DialogUtils.createBottomSlideDialog(
                        context,
                        dialog -> {
                            dialog.setView(binding.getRoot());
                            dialog.setButton(
                                    AlertDialog.BUTTON_POSITIVE,
                                    "Tutup",
                                    (dialogInt, which) -> {
                                        dialog.dismiss();
                                    });
                        })
                .show();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format(Locale.getDefault(), "%02x", b));
        }
        return sb.toString();
    }

    public static void showAudioPlayerDialog(Context context, File audioFile) {
        if (audioFile == null) return;

        DialogAudioPlayerBinding binding =
                DialogAudioPlayerBinding.inflate(android.view.LayoutInflater.from(context));

        binding.tvAudioTitle.setText(audioFile.getName());

        AlertDialog dialog =
                DialogUtils.createBottomSlideDialog(
                        context,
                        d -> {
                            d.setView(binding.getRoot());
                        });

        ThumbnailHelper.loadThumbnail(context, audioFile.getAbsolutePath(), binding.ivAudioCover);

        androidx.media3.exoplayer.ExoPlayer exoPlayer =
                new androidx.media3.exoplayer.ExoPlayer.Builder(context).build();

        android.net.Uri audioUri =
                androidx.core.content.FileProvider.getUriForFile(
                        context, context.getPackageName() + ".fileprovider", audioFile);

        androidx.media3.common.MediaItem mediaItem =
                androidx.media3.common.MediaItem.fromUri(audioUri);
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();

        binding.ivPlayPause.setIcon(context.getDrawable(R.drawable.ic_pause));

        android.os.Handler progressHandler =
                new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable progressRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        if (exoPlayer.isPlaying()) {
                            long currentPosition = exoPlayer.getCurrentPosition();
                            long duration = exoPlayer.getDuration();

                            binding.audioSeekBar.setMax((int) duration);
                            binding.audioSeekBar.setProgress((int) currentPosition);

                            binding.tvCurrentTime.setText(formatMilliseconds(currentPosition));
                            binding.tvTotalDuration.setText(formatMilliseconds(duration));
                        }
                        progressHandler.postDelayed(this, 500);
                    }
                };
        progressHandler.post(progressRunnable);

        binding.ivPlayPause.setOnClickListener(
                v -> {
                    if (exoPlayer.isPlaying()) {
                        exoPlayer.pause();
                        binding.ivPlayPause.setIcon(context.getDrawable(R.drawable.ic_play));
                    } else {
                        exoPlayer.play();
                        binding.ivPlayPause.setIcon(context.getDrawable(R.drawable.ic_pause));
                    }
                });

        binding.ivPrev.setOnClickListener(
                v -> {
                    long currentPos = exoPlayer.getCurrentPosition();
                    long targetPos = Math.max(0, currentPos - 10000);
                    exoPlayer.seekTo(targetPos);
                });

        binding.ivNext.setOnClickListener(
                v -> {
                    long currentPos = exoPlayer.getCurrentPosition();
                    long duration = exoPlayer.getDuration();
                    long targetPos = Math.min(duration, currentPos + 10000);
                    exoPlayer.seekTo(targetPos);
                });

        binding.audioSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            exoPlayer.seekTo(progress);
                            binding.tvCurrentTime.setText(formatMilliseconds(progress));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

        exoPlayer.addListener(
                new androidx.media3.common.Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                            binding.ivPlayPause.setIcon(context.getDrawable(R.drawable.ic_play));
                            exoPlayer.seekTo(0);
                            exoPlayer.pause();
                        }
                    }
                });

        dialog.setOnDismissListener(
                dialogInterface -> {
                    progressHandler.removeCallbacks(progressRunnable);
                    exoPlayer.stop();
                    exoPlayer.release();
                });

        dialog.show();
    }

    public static void showExitConfirmationDialog(Context context, Runnable onExitConfirmed) {
        DialogExitConfirmationBinding binding =
                DialogExitConfirmationBinding.inflate(android.view.LayoutInflater.from(context));

        binding.tvHeader.setText("Keluar Aplikasi");
        binding.tvMessage.setText("Apakah Anda yakin ingin keluar dari aplikasi?");

        DialogUtils.createBottomSlideDialog(
                        context,
                        dialog -> {
                            dialog.setView(binding.getRoot());
                            dialog.setButton(
                                    AlertDialog.BUTTON_POSITIVE,
                                    "Keluar",
                                    (dialogInt, which) -> {
                                        if (onExitConfirmed != null) {
                                            onExitConfirmed.run();
                                        }
                                    });
                            dialog.setButton(
                                    AlertDialog.BUTTON_NEGATIVE,
                                    "Batal",
                                    (dialogInt, which) -> {
                                        dialog.dismiss();
                                    });
                        })
                .show();
    }

    private static String formatMilliseconds(long milliseconds) {
        long seconds = (milliseconds / 1000) % 60;
        long minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private static String getDetailedFileType(String ext) {
        if (ext.isEmpty()) return "File Tanpa Ekstensi";
        switch (ext) {
            case "pdf":
                return "Dokumen PDF";
            case "doc":
            case "docx":
                return "Dokumen Microsoft Word";
            case "xls":
            case "xlsx":
                return "Lembar Sebar Excel";
            case "ppt":
            case "pptx":
                return "Presentasi PowerPoint";
            case "txt":
                return "Dokumen Teks (TXT)";
            case "md":
                return "Markdown Document";
            case "rtf":
                return "Rich Text Format";
            case "zip":
                return "Arsip ZIP";
            case "rar":
                return "Arsip RAR";
            case "7z":
                return "Arsip 7-Zip";
            case "tar":
            case "gz":
                return "Arsip TAR/GZ";
            case "jpg":
            case "jpeg":
                return "Gambar JPEG";
            case "png":
                return "Gambar PNG";
            case "webp":
                return "Gambar WebP";
            case "gif":
                return "Gambar GIF";
            case "bmp":
                return "Gambar Bitmap";
            case "svg":
                return "Gambar Vektor SVG";
            case "mp4":
                return "Video MP4";
            case "mkv":
                return "Video Matroska (MKV)";
            case "avi":
                return "Video AVI";
            case "mov":
                return "Video QuickTime (MOV)";
            case "3gp":
                return "Video 3GP";
            case "mp3":
                return "Audio MP3";
            case "m4a":
                return "Audio M4A";
            case "flac":
                return "Audio FLAC (Lossless)";
            case "wav":
                return "Audio WAV";
            case "ogg":
                return "Audio OGG";
            case "apk":
                return "Aplikasi Android (APK)";
            case "java":
                return "File Sumber Java";
            case "kt":
            case "kts":
                return "File Sumber Kotlin";
            case "html":
            case "htm":
                return "Dokumen HTML";
            case "xml":
                return "File Konfigurasi XML";
            case "json":
                return "File Data JSON";
            case "gradle":
                return "File Build Gradle";
            case "py":
                return "Skrip Python";
            case "php":
                return "Skrip PHP";
            case "js":
                return "Skrip JavaScript";
            case "css":
                return "Cascading Style Sheet (CSS)";
            case "pem":
            case "crt":
            case "key":
                return "File Kunci/Sertifikat Keamanan";
            default:
                return "File ." + ext.toUpperCase(Locale.getDefault());
        }
    }

    public static void showExtractDialog(Context context, File zipFile, Callback callback) {
        if (zipFile == null || !zipFile.exists()) {
            Toast.makeText(context, "File tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }

        showExtractDialogWithLayout(context, zipFile, callback);
    }

    private static void showExtractDialogWithLayout(
            Context context, File zipFile, Callback callback) {
        DialogCompressBinding binding =
                DialogCompressBinding.inflate(android.view.LayoutInflater.from(context));

        binding.tvDialogTitle.setText("Menghitung...");
        binding.tvDialogSubtitle.setText(zipFile.getName());
        binding.tilZipName.setVisibility(View.GONE);

        binding.rvFileList.setVisibility(View.VISIBLE);
        binding.rvFileList.setLayoutManager(new LinearLayoutManager(context));

        FileListAdapter adapter = new FileListAdapter(new ArrayList<>());
        binding.rvFileList.setAdapter(adapter);

        binding.layoutProgress.setVisibility(View.VISIBLE);
        binding.tvProgressStatus.setText("Membaca isi arsip...");

        final AlertDialog[] dialogRef = new AlertDialog[1];

        dialogRef[0] =
                DialogUtils.createBottomSlideDialog(
                        context,
                        d -> {
                            d.setView(binding.getRoot());
                            d.setButton(
                                    AlertDialog.BUTTON_POSITIVE,
                                    "Ekstrak",
                                    (dialogInt, which) -> {
                                        // Aksi klik tombol Ekstrak bawaan dialog
                                    });
                            d.setButton(
                                    AlertDialog.BUTTON_NEGATIVE,
                                    "Batal",
                                    (dialogInt, which) -> {
                                        d.dismiss();
                                    });
                        });

        dialogRef[0].show();

        dialogRef[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        new Thread(
                        () -> {
                            int[] realTotalHolder = new int[1];
                            List<String> previewList = getZipFileList(zipFile, realTotalHolder);
                            int totalRealItems = realTotalHolder[0];

                            new android.os.Handler(android.os.Looper.getMainLooper())
                                    .post(
                                            () -> {
                                                binding.layoutProgress.setVisibility(View.GONE);
                                                adapter.updateData(previewList);

                                                if (binding.rvFileList != null) {
                                                    int maxHeightPx =
                                                            (int)
                                                                    android.util.TypedValue
                                                                            .applyDimension(
                                                                                    android.util
                                                                                            .TypedValue
                                                                                            .COMPLEX_UNIT_DIP,
                                                                                    240,
                                                                                    context.getResources()
                                                                                            .getDisplayMetrics());

                                                    binding.rvFileList.getLayoutParams().height =
                                                            maxHeightPx;
                                                    binding.rvFileList.requestLayout();
                                                }

                                                binding.tvDialogTitle.setText(
                                                        "Ekstrak \"" + zipFile.getName() + "\"?");
                                                binding.tvDialogSubtitle.setText(
                                                        totalRealItems
                                                                + " file akan diekstrak dan disusun berdasarkan kategori.");

                                                dialogRef[0]
                                                        .getButton(AlertDialog.BUTTON_POSITIVE)
                                                        .setEnabled(true);
                                            });
                        })
                .start();

        dialogRef[0]
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        v -> {
                            binding.layoutProgress.setVisibility(View.VISIBLE);
                            binding.tvProgressStatus.setText(
                                    "Mengekstrak " + zipFile.getName() + "...");
                            dialogRef[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                            dialogRef[0].getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

                            new Thread(
                                            () -> {
                                                boolean success = false;
                                                String resultMessage = "";

                                                try {
                                                    File parentDir = zipFile.getParentFile();
                                                    String zipName = zipFile.getName();
                                                    int dotIndex = zipName.lastIndexOf('.');
                                                    String folderName =
                                                            (dotIndex > 0)
                                                                    ? zipName.substring(0, dotIndex)
                                                                    : zipName;
                                                    File extractDir =
                                                            new File(parentDir, folderName);

                                                    if (!extractDir.exists()) {
                                                        extractDir.mkdirs();
                                                    }

                                                    success =
                                                            extractZipWithProgress(
                                                                    zipFile,
                                                                    extractDir,
                                                                    binding,
                                                                    (Activity) context);

                                                    if (success) {
                                                        resultMessage =
                                                                "Berhasil mengekstrak "
                                                                        + zipFile.getName();
                                                    } else {
                                                        resultMessage = "Gagal mengekstrak file";
                                                    }

                                                } catch (Exception e) {
                                                    resultMessage = "Error: " + e.getMessage();
                                                    success = false;
                                                }

                                                final String finalMessage = resultMessage;
                                                final boolean finalSuccess = success;

                                                new android.os.Handler(
                                                                android.os.Looper.getMainLooper())
                                                        .post(
                                                                () -> {
                                                                    Toast.makeText(
                                                                                    context,
                                                                                    finalMessage,
                                                                                    Toast
                                                                                            .LENGTH_SHORT)
                                                                            .show();
                                                                    if (finalSuccess
                                                                            && callback != null) {
                                                                        callback
                                                                                .onActionCompleted();
                                                                    }
                                                                    dialogRef[0].dismiss();
                                                                });
                                            })
                                    .start();
                        });
    }

    private static boolean extractZipWithProgress(
            File zipFile, File destDir, DialogCompressBinding binding, Activity activity) {
        long totalBytes = zipFile.length();
        if (totalBytes == 0) totalBytes = 1;
        long bytesProcessed = 0;
        long startTime = System.currentTimeMillis();

        try (java.util.zip.ZipInputStream zis =
                new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {

            byte[] buffer = new byte[8192];
            java.util.zip.ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                File entryFile = new File(destDir, entry.getName());

                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    if (!entryFile.getParentFile().exists()) {
                        entryFile.getParentFile().mkdirs();
                    }

                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(entryFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                            bytesProcessed += len;

                            final int progress =
                                    (int) Math.min(100, (bytesProcessed * 100) / totalBytes);
                            long elapsedTime = System.currentTimeMillis() - startTime;
                            String timeLeftStr = "Menghitung...";

                            if (progress > 2 && elapsedTime > 500) {
                                long estimatedTotalTime = (elapsedTime * 100) / progress;
                                long remainingTimeMs =
                                        Math.max(0, estimatedTotalTime - elapsedTime);
                                long secondsLeft = (remainingTimeMs / 1000) % 60;
                                long minutesLeft = remainingTimeMs / (1000 * 60);
                                timeLeftStr =
                                        minutesLeft > 0
                                                ? minutesLeft + "m " + secondsLeft + "s lagi"
                                                : secondsLeft + "s lagi";
                            }

                            final String finalTimeLeft = timeLeftStr;
                            activity.runOnUiThread(
                                    () -> {
                                        binding.progressCompress.setProgress(progress);
                                        binding.tvProgressPercent.setText(
                                                progress + "% (" + finalTimeLeft + ")");
                                    });
                        }
                    }
                }
                zis.closeEntry();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static List<String> getZipFileList(File zipFile, int[] outTotalCount) {
        List<String> files = new ArrayList<>();
        int totalFileCount = 0;
        int maxDisplay = 50;

        try (java.util.zip.ZipInputStream zis =
                new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {

            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String name = entry.getName();
                    if (totalFileCount < maxDisplay) {
                        files.add(name);
                    }
                    totalFileCount++;
                }
                zis.closeEntry();
            }

            if (outTotalCount != null && outTotalCount.length > 0) {
                outTotalCount[0] = totalFileCount;
            }

            if (totalFileCount > maxDisplay) {
                files.add("+ " + (totalFileCount - maxDisplay) + " file lainnya");
            }
        } catch (Exception e) {
            if (outTotalCount != null && outTotalCount.length > 0) {
                outTotalCount[0] = 0;
            }
            return new ArrayList<>();
        }
        return files;
    }

    public static void showCompressDialog(
            Context context, List<FileModel> selectedFiles, Callback callback) {
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            Toast.makeText(context, "Tidak ada file yang dipilih", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hasFolder = false;
        for (FileModel model : selectedFiles) {
            if (model.getFile().isDirectory()) {
                hasFolder = true;
                break;
            }
        }

        showCompressDialogWithFormat(context, selectedFiles, callback, hasFolder);
    }

    private static void showCompressDialogWithFormat(
            Context context, List<FileModel> selectedFiles, Callback callback, boolean forceZip) {

        DialogCompressBinding binding =
                DialogCompressBinding.inflate(android.view.LayoutInflater.from(context));

        binding.tvFormatInfo.setVisibility(View.GONE);

        String defaultName = "compressed";
        if (selectedFiles.size() == 1) {
            String fileName = selectedFiles.get(0).getFile().getName();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                defaultName = fileName.substring(0, dotIndex);
            } else {
                defaultName = fileName;
            }
        } else {
            defaultName = "archive";
        }
        binding.etZipName.setText(defaultName);
        binding.etZipName.selectAll();

        final AlertDialog[] dialogRef = new AlertDialog[1];

        dialogRef[0] =
                DialogUtils.createBottomSlideDialog(
                        context,
                        d -> {
                            d.setView(binding.getRoot());
                            d.setButton(
                                    AlertDialog.BUTTON_POSITIVE,
                                    "Kompres",
                                    (dialogInt, which) -> {
                                        // Aksi klik tombol Kompres ditangani di bawah via
                                        // setOnClickListener
                                    });
                            d.setButton(
                                    AlertDialog.BUTTON_NEGATIVE,
                                    "Batal",
                                    (dialogInt, which) -> {
                                        dialogInt.dismiss();
                                    });
                        });

        dialogRef[0].show();

        dialogRef[0]
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        v -> {
                            final String zipName = binding.etZipName.getText().toString().trim();
                            if (zipName.isEmpty()) {
                                Toast.makeText(
                                                context,
                                                "Nama file tidak boleh kosong",
                                                Toast.LENGTH_SHORT)
                                        .show();
                                return;
                            }

                            final String format = "zip";

                            binding.layoutProgress.setVisibility(View.VISIBLE);
                            binding.etZipName.setEnabled(false);
                            dialogRef[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                            dialogRef[0].getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

                            final String finalFormat = format;
                            final String finalZipName = zipName;

                            new Thread(
                                            () -> {
                                                boolean success = false;
                                                String resultMessage = "";

                                                try {
                                                    File parentDir =
                                                            selectedFiles
                                                                    .get(0)
                                                                    .getFile()
                                                                    .getParentFile();
                                                    String fullName =
                                                            finalZipName + "." + finalFormat;
                                                    File zipFile = new File(parentDir, fullName);

                                                    if (zipFile.exists()) {
                                                        int counter = 1;
                                                        while (zipFile.exists()) {
                                                            fullName =
                                                                    finalZipName
                                                                            + "_"
                                                                            + counter
                                                                            + "."
                                                                            + finalFormat;
                                                            zipFile = new File(parentDir, fullName);
                                                            counter++;
                                                        }
                                                    }

                                                    success =
                                                            compressToZipWithProgress(
                                                                    selectedFiles,
                                                                    zipFile,
                                                                    binding,
                                                                    (Activity) context);

                                                    if (success) {
                                                        resultMessage =
                                                                "Berhasil mengompresi "
                                                                        + selectedFiles.size()
                                                                        + " file";
                                                    } else {
                                                        resultMessage = "Gagal mengompresi file";
                                                    }

                                                } catch (Exception e) {
                                                    resultMessage = "Error: " + e.getMessage();
                                                    success = false;
                                                }

                                                final String finalMessage = resultMessage;
                                                final boolean finalSuccess = success;

                                                ((Activity) context)
                                                        .runOnUiThread(
                                                                () -> {
                                                                    Toast.makeText(
                                                                                    context,
                                                                                    finalMessage,
                                                                                    Toast
                                                                                            .LENGTH_SHORT)
                                                                            .show();
                                                                    if (finalSuccess
                                                                            && callback != null) {
                                                                        callback
                                                                                .onActionCompleted();
                                                                    }
                                                                    dialogRef[0].dismiss();
                                                                });
                                            })
                                    .start();
                        });

        binding.etZipName.postDelayed(
                () -> {
                    binding.etZipName.requestFocus();
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager)
                                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(
                                binding.etZipName,
                                android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                    }
                },
                100);
    }

    private static boolean compressToZipWithProgress(
            List<FileModel> files, File zipFile, DialogCompressBinding binding, Activity activity) {
        try {
            long totalBytes = 0;
            for (FileModel model : files) {
                totalBytes += getFolderSize(model.getFile());
            }
            if (totalBytes == 0) totalBytes = 1;

            final long finalTotalBytes = totalBytes;
            final long startTime = System.currentTimeMillis();

            try (ZipOutputStream zos =
                    new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {

                byte[] buffer = new byte[8192];
                long bytesProcessed = 0;

                for (FileModel model : files) {
                    File file = model.getFile();
                    if (file.isDirectory()) {
                        bytesProcessed =
                                addDirectoryToZipWithProgress(
                                        zos,
                                        file,
                                        file.getName() + "/",
                                        buffer,
                                        finalTotalBytes,
                                        bytesProcessed,
                                        startTime,
                                        binding,
                                        activity);
                    } else {
                        bytesProcessed =
                                addFileToZipWithProgress(
                                        zos,
                                        file,
                                        file.getName(),
                                        buffer,
                                        finalTotalBytes,
                                        bytesProcessed,
                                        startTime,
                                        binding,
                                        activity);
                    }
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static long addFileToZipWithProgress(
            ZipOutputStream zos,
            File file,
            String entryName,
            byte[] buffer,
            long totalBytes,
            long bytesProcessed,
            long startTime,
            DialogCompressBinding binding,
            Activity activity)
            throws java.io.IOException {

        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);

        try (FileInputStream fis = new FileInputStream(file)) {
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
                bytesProcessed += len;

                final int progress = (int) Math.min(100, (bytesProcessed * 100) / totalBytes);
                long elapsedTime = System.currentTimeMillis() - startTime;
                String timeLeftStr = "Menghitung...";

                if (progress > 2 && elapsedTime > 500) {
                    long estimatedTotalTime = (elapsedTime * 100) / progress;
                    long remainingTimeMs = Math.max(0, estimatedTotalTime - elapsedTime);
                    long secondsLeft = (remainingTimeMs / 1000) % 60;
                    long minutesLeft = remainingTimeMs / (1000 * 60);
                    timeLeftStr =
                            minutesLeft > 0
                                    ? minutesLeft + "m " + secondsLeft + "s lagi"
                                    : secondsLeft + "s lagi";
                }

                final String finalTimeLeft = timeLeftStr;
                activity.runOnUiThread(
                        () -> {
                            binding.progressCompress.setProgress(progress);
                            binding.tvProgressPercent.setText(
                                    progress + "% (" + finalTimeLeft + ")");
                        });
            }
        }
        zos.closeEntry();
        return bytesProcessed;
    }

    private static long addDirectoryToZipWithProgress(
            ZipOutputStream zos,
            File dir,
            String entryName,
            byte[] buffer,
            long totalBytes,
            long bytesProcessed,
            long startTime,
            DialogCompressBinding binding,
            Activity activity)
            throws java.io.IOException {

        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);
        zos.closeEntry();

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    bytesProcessed =
                            addDirectoryToZipWithProgress(
                                    zos,
                                    file,
                                    entryName + file.getName() + "/",
                                    buffer,
                                    totalBytes,
                                    bytesProcessed,
                                    startTime,
                                    binding,
                                    activity);
                } else {
                    bytesProcessed =
                            addFileToZipWithProgress(
                                    zos,
                                    file,
                                    entryName + file.getName(),
                                    buffer,
                                    totalBytes,
                                    bytesProcessed,
                                    startTime,
                                    binding,
                                    activity);
                }
            }
        }
        return bytesProcessed;
    }

    private static long getFolderSize(File f) {
        if (f == null || !f.exists()) return 0;
        if (!f.isDirectory()) return f.length();
        long size = 0;
        File[] arr = f.listFiles();
        if (arr != null) {
            for (File sub : arr) {
                size += getFolderSize(sub);
            }
        }
        return size;
    }

    // ==================== MOVE DIALOG ====================

    public static void showMoveDialog(
            Context context, List<FileModel> filesToMove, Callback callback) {
        if (filesToMove == null || filesToMove.isEmpty()) {
            Toast.makeText(context, "Tidak ada file yang dipilih", Toast.LENGTH_SHORT).show();
            return;
        }

        File parentDir = filesToMove.get(0).getFile().getParentFile();
        if (parentDir == null) {
            parentDir = Environment.getExternalStorageDirectory();
        }

        showMoveDialogWithFolder(context, filesToMove, parentDir, callback);
    }

    private static void showMoveDialogWithFolder(
            Context context, List<FileModel> filesToMove, File currentFolder, Callback callback) {

        DialogMoveBinding binding =
                DialogMoveBinding.inflate(android.view.LayoutInflater.from(context));

        binding.tvTitle.setText("Pindahkan " + filesToMove.size() + " File");
        binding.tvSubtitle.setText("Pilih folder tujuan");
        binding.tvCurrentPath.setText(currentFolder.getAbsolutePath());

        final File[] currentFolderRef = {currentFolder};
        final File[] selectedFolderRef = {null};

        FolderMoveAdapter adapter = new FolderMoveAdapter(null);

        adapter.setListener(
                new FolderMoveAdapter.OnFolderClickListener() {
                    @Override
                    public void onFolderClick(File folder) {
                        File root = Environment.getExternalStorageDirectory();

                        boolean isParent = false;
                        if (currentFolderRef[0].getParentFile() != null) {
                            isParent =
                                    folder.getAbsolutePath()
                                            .equals(
                                                    currentFolderRef[0]
                                                            .getParentFile()
                                                            .getAbsolutePath());
                        }

                        if (isParent) {
                            File parent = currentFolderRef[0].getParentFile();
                            if (parent != null && parent.exists()) {
                                selectedFolderRef[0] = parent;
                                adapter.setSelectedFolder(parent);
                                currentFolderRef[0] = parent;
                                binding.tvCurrentPath.setText(
                                        currentFolderRef[0].getAbsolutePath());
                                loadMoveFolders(adapter, currentFolderRef[0]);
                            }
                        } else if (folder.isDirectory()) {
                            selectedFolderRef[0] = folder;
                            adapter.setSelectedFolder(folder);
                            binding.tvCurrentPath.setText(folder.getAbsolutePath());

                            currentFolderRef[0] = folder;
                            loadMoveFolders(adapter, currentFolderRef[0]);
                        }
                    }
                });

        binding.rvFolders.setLayoutManager(new LinearLayoutManager(context));
        binding.rvFolders.setAdapter(adapter);
        loadMoveFolders(adapter, currentFolder);

        AlertDialog dialog =
                DialogUtils.createBottomSlideDialog(
                        context,
                        d -> {
                            d.setView(binding.getRoot());
                            d.setButton(
                                    AlertDialog.BUTTON_POSITIVE,
                                    "Pindahkan",
                                    (dialogInt, which) -> {
                                        File targetFolder = selectedFolderRef[0];
                                        if (targetFolder == null) {
                                            Toast.makeText(
                                                            context,
                                                            "Error! File sudah berada di folder ini",
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                            return;
                                        }

                                        final File finalTargetFolder = targetFolder;

                                        ((AlertDialog) dialogInt)
                                                .getButton(AlertDialog.BUTTON_POSITIVE)
                                                .setEnabled(false);
                                        ((AlertDialog) dialogInt)
                                                .getButton(AlertDialog.BUTTON_POSITIVE)
                                                .setText("Memindahkan...");

                                        new Thread(
                                                        () -> {
                                                            boolean allSuccess = true;
                                                            int successCount = 0;
                                                            int failCount = 0;

                                                            for (FileModel model : filesToMove) {
                                                                File source = model.getFile();
                                                                File dest =
                                                                        new File(
                                                                                targetFolder,
                                                                                source.getName());

                                                                if (dest.exists()) {
                                                                    String name = source.getName();
                                                                    String ext = "";
                                                                    String baseName = name;
                                                                    int dotIndex =
                                                                            name.lastIndexOf('.');
                                                                    if (dotIndex > 0) {
                                                                        baseName =
                                                                                name.substring(
                                                                                        0,
                                                                                        dotIndex);
                                                                        ext =
                                                                                name.substring(
                                                                                        dotIndex);
                                                                    }
                                                                    int counter = 1;
                                                                    while (dest.exists()) {
                                                                        String newName =
                                                                                baseName + "_"
                                                                                        + counter
                                                                                        + ext;
                                                                        dest =
                                                                                new File(
                                                                                        targetFolder,
                                                                                        newName);
                                                                        counter++;
                                                                    }
                                                                }

                                                                if (source.renameTo(dest)) {
                                                                    successCount++;
                                                                } else {
                                                                    failCount++;
                                                                    allSuccess = false;
                                                                }
                                                            }

                                                            final int finalSuccess = successCount;
                                                            final int finalFail = failCount;
                                                            final boolean finalAllSuccess =
                                                                    allSuccess;

                                                            new android.os.Handler(
                                                                            android.os.Looper
                                                                                    .getMainLooper())
                                                                    .post(
                                                                            () -> {
                                                                                if (finalAllSuccess) {
                                                                                    Toast.makeText(
                                                                                                    context,
                                                                                                    "Berhasil memindahkan "
                                                                                                            + finalSuccess
                                                                                                            + " file ke "
                                                                                                            + finalTargetFolder
                                                                                                                    .getName(),
                                                                                                    Toast
                                                                                                            .LENGTH_SHORT)
                                                                                            .show();
                                                                                } else {
                                                                                    Toast.makeText(
                                                                                                    context,
                                                                                                    "Berhasil: "
                                                                                                            + finalSuccess
                                                                                                            + ", Gagal: "
                                                                                                            + finalFail,
                                                                                                    Toast
                                                                                                            .LENGTH_SHORT)
                                                                                            .show();
                                                                                }

                                                                                if (callback
                                                                                        != null) {
                                                                                    callback
                                                                                            .onActionCompleted(
                                                                                                    finalTargetFolder);
                                                                                }
                                                                                dialogInt.dismiss();
                                                                            });
                                                        })
                                                .start();
                                    });
                            d.setButton(
                                    AlertDialog.BUTTON_NEGATIVE,
                                    "Batal",
                                    (dialogInt, which) -> {
                                        dialogInt.dismiss();
                                    });
                        });

        dialog.show();
    }

    private static void loadMoveFolders(FolderMoveAdapter adapter, File directory) {
        List<File> folderList = new ArrayList<>();

        File root = Environment.getExternalStorageDirectory();
        boolean isRoot = root != null && directory.getAbsolutePath().equals(root.getAbsolutePath());

        if (!isRoot && directory.getParentFile() != null) {
            File parent = directory.getParentFile();
            if (parent != null && !parent.getName().isEmpty()) {
                folderList.add(parent);
                adapter.setHasParent(true, parent.getName());
            } else {
                adapter.setHasParent(false, "");
            }
        } else {
            adapter.setHasParent(false, "");
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isHidden()) continue;
                if (file.getName().startsWith(".")) continue;
                if (file.isDirectory()) {
                    folderList.add(file);
                }
            }
        }

        adapter.updateData(folderList);
    }

    // ==================== COPY DIALOG ====================

    public static void showCopyDialog(
            Context context, List<FileModel> filesToCopy, Callback callback) {
        if (filesToCopy == null || filesToCopy.isEmpty()) {
            Toast.makeText(context, "Tidak ada file yang dipilih", Toast.LENGTH_SHORT).show();
            return;
        }

        File parentDir = filesToCopy.get(0).getFile().getParentFile();
        if (parentDir == null) {
            parentDir = Environment.getExternalStorageDirectory();
        }

        showCopyDialogWithFolder(context, filesToCopy, parentDir, callback);
    }

    private static void showCopyDialogWithFolder(
            Context context, List<FileModel> filesToCopy, File currentFolder, Callback callback) {

        DialogMoveBinding binding =
                DialogMoveBinding.inflate(android.view.LayoutInflater.from(context));

        binding.tvTitle.setText("Salin " + filesToCopy.size() + " File");
        binding.tvSubtitle.setText("Pilih folder tujuan");
        binding.tvCurrentPath.setText(currentFolder.getAbsolutePath());

        final File[] currentFolderRef = {currentFolder};
        final File[] selectedFolderRef = {null};

        FolderMoveAdapter adapter = new FolderMoveAdapter(null);

        adapter.setListener(
                new FolderMoveAdapter.OnFolderClickListener() {
                    @Override
                    public void onFolderClick(File folder) {
                        File root = Environment.getExternalStorageDirectory();

                        boolean isParent = false;
                        if (currentFolderRef[0].getParentFile() != null) {
                            isParent =
                                    folder.getAbsolutePath()
                                            .equals(
                                                    currentFolderRef[0]
                                                            .getParentFile()
                                                            .getAbsolutePath());
                        }

                        if (isParent) {
                            File parent = currentFolderRef[0].getParentFile();
                            if (parent != null && parent.exists()) {
                                selectedFolderRef[0] = parent;
                                adapter.setSelectedFolder(parent);
                                currentFolderRef[0] = parent;
                                binding.tvCurrentPath.setText(
                                        currentFolderRef[0].getAbsolutePath());
                                loadMoveFolders(adapter, currentFolderRef[0]);
                            }
                        } else if (folder.isDirectory()) {
                            selectedFolderRef[0] = folder;
                            adapter.setSelectedFolder(folder);
                            binding.tvCurrentPath.setText(folder.getAbsolutePath());

                            currentFolderRef[0] = folder;
                            loadMoveFolders(adapter, currentFolderRef[0]);
                        }
                    }
                });

        binding.rvFolders.setLayoutManager(new LinearLayoutManager(context));
        binding.rvFolders.setAdapter(adapter);
        loadMoveFolders(adapter, currentFolder);

        AlertDialog dialog =
                DialogUtils.createBottomSlideDialog(
                        context,
                        d -> {
                            d.setView(binding.getRoot());
                            d.setButton(
                                    AlertDialog.BUTTON_POSITIVE,
                                    "Salin",
                                    (dialogInt, which) -> {
                                        File targetFolder = selectedFolderRef[0];
                                        if (targetFolder == null) {
                                            Toast.makeText(
                                                            context,
                                                            "Error! File sudah berada di folder ini",
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                            return;
                                        }

                                        final File finalTargetFolder = targetFolder;

                                        ((AlertDialog) dialogInt)
                                                .getButton(AlertDialog.BUTTON_POSITIVE)
                                                .setEnabled(false);
                                        ((AlertDialog) dialogInt)
                                                .getButton(AlertDialog.BUTTON_POSITIVE)
                                                .setText("Menyalin...");

                                        new Thread(
                                                        () -> {
                                                            boolean allSuccess = true;
                                                            int successCount = 0;
                                                            int failCount = 0;

                                                            for (FileModel model : filesToCopy) {
                                                                File source = model.getFile();
                                                                File dest =
                                                                        new File(
                                                                                targetFolder,
                                                                                source.getName());

                                                                if (dest.exists()) {
                                                                    String name = source.getName();
                                                                    String ext = "";
                                                                    String baseName = name;
                                                                    int dotIndex =
                                                                            name.lastIndexOf('.');
                                                                    if (dotIndex > 0) {
                                                                        baseName =
                                                                                name.substring(
                                                                                        0,
                                                                                        dotIndex);
                                                                        ext =
                                                                                name.substring(
                                                                                        dotIndex);
                                                                    }
                                                                    int counter = 1;
                                                                    while (dest.exists()) {
                                                                        String newName =
                                                                                baseName + "_"
                                                                                        + counter
                                                                                        + ext;
                                                                        dest =
                                                                                new File(
                                                                                        targetFolder,
                                                                                        newName);
                                                                        counter++;
                                                                    }
                                                                }

                                                                if (copyFile(source, dest)) {
                                                                    successCount++;
                                                                } else {
                                                                    failCount++;
                                                                    allSuccess = false;
                                                                }
                                                            }

                                                            final int finalSuccess = successCount;
                                                            final int finalFail = failCount;
                                                            final boolean finalAllSuccess =
                                                                    allSuccess;

                                                            new android.os.Handler(
                                                                            android.os.Looper
                                                                                    .getMainLooper())
                                                                    .post(
                                                                            () -> {
                                                                                if (finalAllSuccess) {
                                                                                    Toast.makeText(
                                                                                                    context,
                                                                                                    "Berhasil menyalin "
                                                                                                            + finalSuccess
                                                                                                            + " file ke "
                                                                                                            + finalTargetFolder
                                                                                                                    .getName(),
                                                                                                    Toast
                                                                                                            .LENGTH_SHORT)
                                                                                            .show();
                                                                                } else {
                                                                                    Toast.makeText(
                                                                                                    context,
                                                                                                    "Berhasil: "
                                                                                                            + finalSuccess
                                                                                                            + ", Gagal: "
                                                                                                            + finalFail,
                                                                                                    Toast
                                                                                                            .LENGTH_SHORT)
                                                                                            .show();
                                                                                }

                                                                                if (callback
                                                                                        != null) {
                                                                                    callback
                                                                                            .onActionCompleted(
                                                                                                    finalTargetFolder);
                                                                                }
                                                                                dialogInt.dismiss();
                                                                            });
                                                        })
                                                .start();
                                    });
                            d.setButton(
                                    AlertDialog.BUTTON_NEGATIVE,
                                    "Batal",
                                    (dialogInt, which) -> {
                                        dialogInt.dismiss();
                                    });
                        });

        dialog.show();
    }

    private static boolean copyFile(File source, File dest) {
        try {
            if (dest.getParentFile() != null && !dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }

            try (FileInputStream fis = new FileInputStream(source);
                    FileOutputStream fos = new FileOutputStream(dest)) {

                byte[] buffer = new byte[8192];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
