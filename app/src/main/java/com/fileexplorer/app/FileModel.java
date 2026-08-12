package com.fileexplorer.app;

import java.io.File;

public class FileModel {
    private File file;
    private boolean isSelected;

    public FileModel(File file) {
        this.file = file;
        this.isSelected = false;
    }

    public File getFile() {
        return file;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
