package com.fileexplorer.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ThumbnailHelper {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int TARGET_SIZE = 256;

    public static void loadThumbnail(Context context, String filePath, ImageView imageView) {
        if (filePath == null || filePath.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_file);
            return;
        }

        String lowerPath = filePath.toLowerCase();

        // APK - load icon pake PackageManager
        if (isApkFile(lowerPath)) {
            loadApkIcon(context, filePath, imageView);
            return;
        }

        // AUDIO - load cover art
        if (isAudioFile(lowerPath)) {
            loadAudioArtwork(context, filePath, imageView);
            return;
        }

        // IMAGE / VIDEO - pake Glide
        if (isImageFile(lowerPath) || isVideoFile(lowerPath)) {
            loadWithGlide(context, filePath, imageView, lowerPath);
            return;
        }

        // FILE LAIN
        imageView.setImageResource(R.drawable.ic_file);
    }

    private static void loadApkIcon(Context context, String filePath, ImageView imageView) {
        // Set default dulu
        imageView.setImageResource(R.drawable.ic_apk);

        executorService.execute(
                () -> {
                    try {
                        PackageManager pm = context.getPackageManager();
                        PackageInfo packageInfo = pm.getPackageArchiveInfo(filePath, 0);
                        if (packageInfo != null) {
                            packageInfo.applicationInfo.sourceDir = filePath;
                            packageInfo.applicationInfo.publicSourceDir = filePath;
                            Drawable icon = packageInfo.applicationInfo.loadIcon(pm);
                            if (icon != null) {
                                Bitmap bitmap = drawableToBitmap(icon);
                                if (bitmap != null) {
                                    mainHandler.post(
                                            () -> {
                                                imageView.setImageBitmap(bitmap);
                                            });
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                });
    }

    private static void loadAudioArtwork(Context context, String filePath, ImageView imageView) {
        // Set default dulu
        imageView.setImageResource(R.drawable.ic_audio);

        executorService.execute(
                () -> {
                    MediaMetadataRetriever retriever = null;
                    try {
                        retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(filePath);
                        byte[] art = retriever.getEmbeddedPicture();
                        if (art != null) {
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inSampleSize = 2;
                            Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length, opts);
                            if (bitmap != null) {
                                mainHandler.post(
                                        () -> {
                                            imageView.setImageBitmap(bitmap);
                                        });
                            }
                        }
                    } catch (Exception ignored) {
                    } finally {
                        if (retriever != null) {
                            try {
                                retriever.release();
                            } catch (Exception e) {
                                // Ignore
                            }
                        }
                    }
                });
    }

    private static void loadWithGlide(
            Context context, String filePath, ImageView imageView, String lowerPath) {
        // Set default dulu
        if (isVideoFile(lowerPath)) {
            imageView.setImageResource(R.drawable.ic_play_circle);
        } else {
            imageView.setImageDrawable(null);
        }

        RequestOptions options =
                new RequestOptions()
                        .override(TARGET_SIZE, TARGET_SIZE)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .skipMemoryCache(false)
                        .error(getDefaultIconRes(lowerPath));

        Glide.with(context).load(new File(filePath)).apply(options).into(imageView);
    }

    private static void setDefaultIcon(ImageView imageView, String lowerPath) {
        if (isVideoFile(lowerPath)) {
            imageView.setImageResource(R.drawable.ic_play_circle);
        } else if (isApkFile(lowerPath)) {
            imageView.setImageResource(R.drawable.ic_apk);
        } else if (isAudioFile(lowerPath)) {
            imageView.setImageResource(R.drawable.ic_audio);
        } else if (isImageFile(lowerPath)) {
            imageView.setImageDrawable(null);
        } else {
            imageView.setImageResource(R.drawable.ic_file);
        }
    }

    private static int getDefaultIconRes(String lowerPath) {
        if (isVideoFile(lowerPath)) return R.drawable.ic_play_circle;
        if (isApkFile(lowerPath)) return R.drawable.ic_apk;
        if (isAudioFile(lowerPath)) return R.drawable.ic_audio;
        if (isImageFile(lowerPath)) return R.drawable.ic_image;
        return R.drawable.ic_file;
    }

    private static boolean isVideoFile(String path) {
        return path.endsWith(".mp4")
                || path.endsWith(".mkv")
                || path.endsWith(".avi")
                || path.endsWith(".3gp")
                || path.endsWith(".mov")
                || path.endsWith(".webm");
    }

    private static boolean isApkFile(String path) {
        return path.endsWith(".apk");
    }

    private static boolean isAudioFile(String path) {
        return path.endsWith(".mp3")
                || path.endsWith(".m4a")
                || path.endsWith(".flac")
                || path.endsWith(".wav")
                || path.endsWith(".ogg");
    }

    private static boolean isImageFile(String path) {
        return path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".png")
                || path.endsWith(".webp")
                || path.endsWith(".bmp")
                || path.endsWith(".gif");
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
            return ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        if (width <= 0) width = TARGET_SIZE;
        if (height <= 0) height = TARGET_SIZE;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    // Preload thumbnail untuk daftar file
    public static void preloadThumbnails(Context context, java.util.List<FileModel> files) {
        for (int i = 0; i < Math.min(files.size(), 20); i++) {
            FileModel model = files.get(i);
            if (model != null && model.getFile() != null) {
                String path = model.getFile().getAbsolutePath();
                String lower = path.toLowerCase();
                if (isImageFile(lower) || isVideoFile(lower)) {
                    Glide.with(context)
                            .load(new File(path))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .preload(TARGET_SIZE, TARGET_SIZE);
                }
            }
        }
    }

    public static void clearCache(Context context) {
        Glide.get(context).clearMemory();
        new Thread(
                        () -> {
                            try {
                                Glide.get(context).clearDiskCache();
                            } catch (Exception e) {
                                // Ignore
                            }
                        })
                .start();
    }
}
