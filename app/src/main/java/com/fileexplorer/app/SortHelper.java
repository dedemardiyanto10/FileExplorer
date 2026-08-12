package com.fileexplorer.app;

import java.io.File;

public class SortHelper {
    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_DATE = 2;
    public static final int SORT_BY_SIZE = 3;

    public static int compareFiles(File f1, File f2, int sortType) {
        switch (sortType) {
            case SORT_BY_DATE:
                return Long.compare(f2.lastModified(), f1.lastModified());
            case SORT_BY_SIZE:
                return Long.compare(f2.length(), f1.length());
            default:
                return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }
}
