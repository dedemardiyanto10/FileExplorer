package com.fileexplorer.app;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BreadcrumbHelper {

    public interface OnBreadcrumbClickListener {
        void onFolderClick(File folder);
    }

    public static void setupBreadcrumb(
            Context context,
            LinearLayout container,
            File currentDir,
            OnBreadcrumbClickListener listener) {
        if (container == null || currentDir == null) return;

        container.removeAllViews();

        File rootDir = android.os.Environment.getExternalStorageDirectory();
        List<File> folders = new ArrayList<>();
        File temp = currentDir;

        while (temp != null && temp.getAbsolutePath().startsWith(rootDir.getAbsolutePath())) {
            folders.add(0, temp);
            if (temp.equals(rootDir)) break;
            temp = temp.getParentFile();
        }

        if (folders.isEmpty()) {
            folders.add(rootDir);
        }

        int totalFolders = folders.size();

        for (int i = 0; i < totalFolders; i++) {
            File folder = folders.get(i);
            String displayName = (i == 0) ? "Penyimpanan Internal" : folder.getName();

            TextView tvItem = new TextView(context);
            tvItem.setText(displayName);
            tvItem.setTextAppearance(
                    context, com.google.android.material.R.attr.textAppearanceBodyMedium);
            tvItem.setGravity(Gravity.CENTER_VERTICAL);
            tvItem.setPadding(8, 4, 8, 4);
            tvItem.setClickable(true);
            tvItem.setFocusable(true);

            TypedValue outValue = new TypedValue();
            context.getTheme()
                    .resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            tvItem.setBackgroundResource(outValue.resourceId);

            if (i == totalFolders - 1) {
                TypedValue primaryColorAttr = new TypedValue();
                context.getTheme()
                        .resolveAttribute(
                                com.google.android.material.R.attr.colorPrimaryVariant,
                                primaryColorAttr,
                                true);
                tvItem.setTextColor(primaryColorAttr.data);
                tvItem.setTypeface(null, Typeface.BOLD);
            } else {
                TypedValue textColorAttr = new TypedValue();
                context.getTheme()
                        .resolveAttribute(
                                com.google.android.material.R.attr.colorOnSurfaceVariant,
                                textColorAttr,
                                true);
                tvItem.setTextColor(textColorAttr.data);
                tvItem.setTypeface(null, Typeface.NORMAL);

                tvItem.setOnClickListener(
                        v -> {
                            if (listener != null) {
                                listener.onFolderClick(folder);
                            }
                        });
            }

            container.addView(tvItem);

            if (i < totalFolders - 1) {
                ImageView ivSeparator = new ImageView(context);
                ivSeparator.setImageResource(R.drawable.ic_arrow_right);

                int sizeInPx =
                        (int)
                                TypedValue.applyDimension(
                                        TypedValue.COMPLEX_UNIT_DIP,
                                        16,
                                        context.getResources().getDisplayMetrics());
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(sizeInPx, sizeInPx);
                params.gravity = Gravity.CENTER_VERTICAL;
                params.setMargins(4, 0, 4, 0);
                ivSeparator.setLayoutParams(params);

                TypedValue separatorColorAttr = new TypedValue();
                context.getTheme()
                        .resolveAttribute(
                                com.google.android.material.R.attr.colorOnSurfaceVariant,
                                separatorColorAttr,
                                true);
                ivSeparator.setColorFilter(separatorColorAttr.data);
                ivSeparator.setAlpha(0.6f);

                container.addView(ivSeparator);
            }
        }
    }

    public static void animateVisibility(View view, boolean show) {
        if (view == null) return;

        boolean isCurrentlyVisible = view.getVisibility() == View.VISIBLE;
        if (isCurrentlyVisible == show) return;

        view.animate().cancel();

        if (show) {
            view.setVisibility(View.VISIBLE);
            view.setAlpha(0f);

            view.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            ((View) view.getParent()).getWidth(), View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.UNSPECIFIED);
            int targetHeight = view.getMeasuredHeight();

            android.view.ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = 0;
            view.setLayoutParams(params);

            android.animation.ValueAnimator animator =
                    android.animation.ValueAnimator.ofInt(0, targetHeight);
            animator.setDuration(200);
            animator.addUpdateListener(
                    animation -> {
                        params.height = (int) animation.getAnimatedValue();
                        view.setLayoutParams(params);
                    });
            animator.addListener(
                    new android.animation.AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(android.animation.Animator animation) {
                            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                            view.setLayoutParams(params);
                        }
                    });
            animator.start();
            view.animate().alpha(1f).setDuration(200).start();

        } else {
            int initialHeight = view.getHeight();
            android.view.ViewGroup.LayoutParams params = view.getLayoutParams();

            android.animation.ValueAnimator animator =
                    android.animation.ValueAnimator.ofInt(initialHeight, 0);
            animator.setDuration(200);
            animator.addUpdateListener(
                    animation -> {
                        params.height = (int) animation.getAnimatedValue();
                        view.setLayoutParams(params);
                    });
            animator.addListener(
                    new android.animation.AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(android.animation.Animator animation) {
                            view.setVisibility(View.GONE);
                            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                            view.setLayoutParams(params);
                        }
                    });
            animator.start();
            view.animate().alpha(0f).setDuration(200).start();
        }
    }
}
