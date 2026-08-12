package com.fileexplorer.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.PopupWindow;

public class SortMenuHelper {

    public interface SortActionListener {
        void onSortChanged(int sortType, boolean isAscending);
    }

    public static void showSortMenu(
            Activity activity,
            View anchorView,
            int currentSortType,
            boolean isAscending,
            SortActionListener listener) {
        if (anchorView == null || activity == null) return;

        View sheetView = activity.getLayoutInflater().inflate(R.layout.dialog_sort_menu, null);

        int widthInPx =
                (int)
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                240,
                                activity.getResources().getDisplayMetrics());

        PopupWindow popupWindow =
                new PopupWindow(
                        sheetView,
                        widthInPx,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        true);

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(8.0f);

        View ivCheckName = sheetView.findViewById(R.id.ivCheckName);
        View ivCheckDate = sheetView.findViewById(R.id.ivCheckDate);
        View ivCheckSize = sheetView.findViewById(R.id.ivCheckSize);
        View ivCheckAsc = sheetView.findViewById(R.id.ivCheckAsc);
        View ivCheckDesc = sheetView.findViewById(R.id.ivCheckDesc);

        ivCheckName.setVisibility(currentSortType == 1 ? View.VISIBLE : View.INVISIBLE);
        ivCheckDate.setVisibility(currentSortType == 2 ? View.VISIBLE : View.INVISIBLE);
        ivCheckSize.setVisibility(currentSortType == 3 ? View.VISIBLE : View.INVISIBLE);

        ivCheckAsc.setVisibility(isAscending ? View.VISIBLE : View.INVISIBLE);
        ivCheckDesc.setVisibility(!isAscending ? View.VISIBLE : View.INVISIBLE);

        sheetView
                .findViewById(R.id.optionSortName)
                .setOnClickListener(
                        v -> {
                            popupWindow.dismiss();
                            if (listener != null) {
                                listener.onSortChanged(1, isAscending);
                            }
                        });

        sheetView
                .findViewById(R.id.optionSortDate)
                .setOnClickListener(
                        v -> {
                            popupWindow.dismiss();
                            if (listener != null) {
                                listener.onSortChanged(2, isAscending);
                            }
                        });

        sheetView
                .findViewById(R.id.optionSortSize)
                .setOnClickListener(
                        v -> {
                            popupWindow.dismiss();
                            if (listener != null) {
                                listener.onSortChanged(3, isAscending);
                            }
                        });

        sheetView
                .findViewById(R.id.optionOrderAsc)
                .setOnClickListener(
                        v -> {
                            popupWindow.dismiss();
                            if (listener != null) {
                                listener.onSortChanged(currentSortType, true);
                            }
                        });

        sheetView
                .findViewById(R.id.optionOrderDesc)
                .setOnClickListener(
                        v -> {
                            popupWindow.dismiss();
                            if (listener != null) {
                                listener.onSortChanged(currentSortType, false);
                            }
                        });

        popupWindow.showAsDropDown(anchorView, 0, 8);
    }
}
