package com.fileexplorer.app;

public class CategoryItem {
    private String name;
    private int iconRes;
    private String key;
    private boolean isSelected;

    public CategoryItem(String name, int iconRes, String key) {
        this.name = name;
        this.iconRes = iconRes;
        this.key = key;
        this.isSelected = false;
    }

    public String getName() {
        return name;
    }

    public int getIconRes() {
        return iconRes;
    }

    public String getKey() {
        return key;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
