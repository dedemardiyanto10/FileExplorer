package com.fileexplorer.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fileexplorer.app.databinding.ItemFolderMoveBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderMoveAdapter extends RecyclerView.Adapter<FolderMoveAdapter.ViewHolder> {

    private List<File> folderList = new ArrayList<>();
    private File selectedFolder = null;
    private OnFolderClickListener listener;
    private boolean hasParent = false;
    private String parentName = "";

    public interface OnFolderClickListener {
        void onFolderClick(File folder);
    }

    public FolderMoveAdapter(OnFolderClickListener listener) {
        this.listener = listener;
    }

    public void setListener(OnFolderClickListener listener) {
        this.listener = listener;
    }

    public void setHasParent(boolean hasParent, String parentName) {
        this.hasParent = hasParent;
        this.parentName = parentName != null ? parentName : "";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFolderMoveBinding binding =
                ItemFolderMoveBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File folder = folderList.get(position);

        // ===== CEK APAKAH INI PARENT FOLDER =====
        boolean isParent = (position == 0 && hasParent);

        if (isParent) {
            // ===== PARENT FOLDER =====
            String displayName = parentName.isEmpty() ? "Root" : parentName;
            holder.binding.tvFolderName.setText(displayName);
            holder.binding.ivFolderIcon.setImageResource(R.drawable.ic_arrow_back);
        } else {
            // ===== FOLDER BIASA =====
            holder.binding.tvFolderName.setText(folder.getName());
            holder.binding.ivFolderIcon.setImageResource(R.drawable.ic_folder);
        }

        holder.itemView.setOnClickListener(
                v -> {
                    if (listener != null) {
                        listener.onFolderClick(folder);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    public void updateData(List<File> folders) {
        this.folderList = folders != null ? folders : new ArrayList<>();
        // ===== JANGAN RESET hasParent DI SINI =====
        notifyDataSetChanged();
    }

    // ===== TAMBAHKAN METHOD UNTUK RESET =====
    public void resetParent() {
        this.hasParent = false;
        this.parentName = "";
    }

    public File getSelectedFolder() {
        return selectedFolder;
    }

    public void setSelectedFolder(File folder) {
        this.selectedFolder = folder;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemFolderMoveBinding binding;

        ViewHolder(@NonNull ItemFolderMoveBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
