package com.fileexplorer.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CategoryHelper {

    private static CategoryAdapter categoryAdapter;
    private static String currentCategory = "all";

    // ==================== SETUP CATEGORY ====================

    public static void setupCategory(
            Context context, RecyclerView rvCategory, OnCategorySelectedListener listener) {

        List<CategoryItem> categories = new ArrayList<>();
        categories.add(new CategoryItem("Semua", R.drawable.ic_listview, "all"));
        categories.add(new CategoryItem("Gambar", R.drawable.ic_image, "images"));
        categories.add(new CategoryItem("Video", R.drawable.ic_film, "video"));
        categories.add(new CategoryItem("Audio", R.drawable.ic_audio, "audio"));
        categories.add(new CategoryItem("Dokumen", R.drawable.ic_doc, "document"));
        categories.add(new CategoryItem("Arsip", R.drawable.ic_compress, "archive"));
        categories.add(new CategoryItem("Aplikasi", R.drawable.ic_apk, "apk"));
        categories.add(new CategoryItem("Download", R.drawable.ic_download, "download"));

        categoryAdapter =
                new CategoryAdapter(
                        categories,
                        (key, position) -> {
                            currentCategory = key;
                            if (listener != null) {
                                listener.onCategorySelected(key);
                            }
                        });

        rvCategory.setLayoutManager(new GridLayoutManager(context, 4));
        rvCategory.setAdapter(categoryAdapter);

        categoryAdapter.setSelectedPosition("all");
        currentCategory = "all";
    }

    // ==================== UPDATE CATEGORY UI ====================

    public static void updateCategorySelection(String category) {
        if (categoryAdapter != null) {
            categoryAdapter.setSelectedPosition(category);
            currentCategory = category;
        }
    }

    public static String getCurrentCategory() {
        return currentCategory;
    }

    // ==================== HIDE / SHOW CATEGORY (SAMA KAYAK BREADCRUMB) ====================

    public static void animateVisibility(View view, boolean show) {
        if (view == null) return;
        if (view.getParent() == null) {
            view.setVisibility(show ? View.VISIBLE : View.GONE);
            return;
        }

        view.animate().cancel();

        if (show) {
            view.setVisibility(View.VISIBLE);
            view.setAlpha(0f);

            view.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            ((View) view.getParent()).getWidth(), View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.UNSPECIFIED);
            int targetHeight = view.getMeasuredHeight();

            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = 0;
            view.setLayoutParams(params);

            ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
            animator.setDuration(250);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(
                    animation -> {
                        params.height = (int) animation.getAnimatedValue();
                        view.setLayoutParams(params);
                    });
            animator.addListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            view.setLayoutParams(params);
                        }
                    });
            animator.start();
            view.animate().alpha(1f).setDuration(200).start();

        } else {
            int initialHeight = view.getHeight();
            ViewGroup.LayoutParams params = view.getLayoutParams();

            ValueAnimator animator = ValueAnimator.ofInt(initialHeight, 0);
            animator.setDuration(200);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(
                    animation -> {
                        params.height = (int) animation.getAnimatedValue();
                        view.setLayoutParams(params);
                    });
            animator.addListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            view.setVisibility(View.GONE);
                            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            view.setLayoutParams(params);
                        }
                    });
            animator.start();
            view.animate().alpha(0f).setDuration(200).start();
        }
    }

    // ==================== HIDE / SHOW CATEGORY (SIMPLE VERSION) ====================

    public static void setCategoryVisibility(View categoryContainer, boolean show) {
        if (categoryContainer == null) return;

        categoryContainer.animate().cancel();

        if (show) {
            categoryContainer.setVisibility(View.VISIBLE);
            categoryContainer.setAlpha(1f);
            ViewGroup.LayoutParams params = categoryContainer.getLayoutParams();
            if (params != null) {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                categoryContainer.setLayoutParams(params);
            }
        } else {
            categoryContainer.setVisibility(View.GONE);
            categoryContainer.setAlpha(0f);
        }
    }

    // ===== TOGGLE =====
    public static void toggleCategory(View categoryContainer, boolean show, Runnable onComplete) {
        animateVisibility(categoryContainer, show);
        if (onComplete != null) {
            categoryContainer.postDelayed(onComplete, 300);
        }
    }

    // ==================== INTERFACE ====================

    public interface OnCategorySelectedListener {
        void onCategorySelected(String category);
    }

    // ==================== GETTER ====================

    public static CategoryAdapter getAdapter() {
        return categoryAdapter;
    }

    public static void setAdapter(CategoryAdapter adapter) {
        categoryAdapter = adapter;
    }
}
