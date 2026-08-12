package com.fileexplorer.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {

    private List<String> fileList = new ArrayList<>();

    public FileListAdapter(List<String> fileList) {
        this.fileList = fileList != null ? fileList : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_archive_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (fileList == null || position >= fileList.size()) {
            return;
        }

        String fileName = fileList.get(position);
        holder.tvFileName.setText(fileName);
        holder.ivFileIcon.setImageResource(getFileIconForAdapter(fileName));
    }

    @Override
    public int getItemCount() {
        return fileList != null ? fileList.size() : 0;
    }

    public void updateData(List<String> newList) {
        this.fileList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public int getTotalItems() {
        return fileList != null ? fileList.size() : 0;
    }

    private int getFileIconForAdapter(String fileName) {
        if (fileName == null || fileName.isEmpty()) return R.drawable.ic_file;

        if (fileName.endsWith("/") || fileName.endsWith("\\")) {
            return R.drawable.ic_folder;
        }

        String ext = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            ext = fileName.substring(dotIndex + 1).toLowerCase();
        }

        if (ext.isEmpty()) return R.drawable.ic_file;

        switch (ext) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "webp":
            case "svg":
            case "ico":
            case "heic":
                return R.drawable.ic_image;
            case "mp4":
            case "mkv":
            case "avi":
            case "mov":
            case "wmv":
            case "flv":
            case "3gp":
            case "m4v":
            case "webm":
            case "mpeg":
                return R.drawable.ic_film;
            case "mp3":
            case "wav":
            case "flac":
            case "aac":
            case "ogg":
            case "m4a":
            case "wma":
            case "opus":
            case "amr":
                return R.drawable.ic_audio;
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
            case "bz2":
            case "xz":
            case "tgz":
            case "zst":
                return R.drawable.ic_archive;
            case "apk":
                return R.drawable.ic_apk;
            case "pdf":
                return R.drawable.ic_pdf;
            case "exe":
            case "msi":
            case "sh":
            case "bin":
                return R.drawable.ic_exe;
            case "doc":
            case "odt":
            case "docx":
            case "xls":
            case "xlsx":
            case "ppt":
            case "pptx":
            case "odp":
            case "ods":
                return R.drawable.ic_doc;
            case "txt":
            case "log":
            case "md":
            case "rtf":
            case "csv":
                return R.drawable.ic_txt;
            case "java":
                return R.drawable.ic_java;
            case "kt":
            case "kts":
                return R.drawable.ic_kotlin;
            case "py":
            case "pyw":
                return R.drawable.ic_python;
            case "php":
                return R.drawable.ic_php;
            case "html":
            case "htm":
                return R.drawable.ic_html;
            case "xml":
                return R.drawable.ic_xml;
            case "json":
                return R.drawable.ic_json;
            case "gradle":
                return R.drawable.ic_gradle;
            case "key":
            case "pem":
            case "jks":
            case "crt":
            case "cer":
                return R.drawable.ic_key;
            default:
                return R.drawable.ic_file;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFileIcon;
        TextView tvFileName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
        }
    }
}
