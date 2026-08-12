package com.fileexplorer.app;

import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DialogUtils {

    public static AlertDialog createBottomSlideDialog(Context context, ViewCustomizer customizer) {
        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(context, R.style.CustomBottomDialogTheme);

        AlertDialog dialog = builder.create();

        if (customizer != null) {
            customizer.onCustomizeDialog(dialog);
        }

        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.BOTTOM);

            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.dimAmount = 0.5f;
            params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.flags |= WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
                params.setBlurBehindRadius(15);
            }

            window.setAttributes(params);
        }

        return dialog;
    }

    public interface ViewCustomizer {
        void onCustomizeDialog(AlertDialog dialog);
    }
}
