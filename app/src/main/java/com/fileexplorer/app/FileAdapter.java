package com.fileexplorer.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fileexplorer.app.databinding.ItemFileGrid3Binding;
import com.fileexplorer.app.databinding.ItemFileListBinding;
import com.fileexplorer.app.databinding.ItemFileGridBinding;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_LIST_ITEM = 0;
    private static final int TYPE_GRID_ITEM = 1;
    private static final int TYPE_GRID3_ITEM = 3;
    private static final int TYPE_FOOTER = 2;
    private int spanCount = 2;

    private List<FileModel> fileList = new ArrayList<>();
    private OnItemClickListener listener;
    private boolean isGridView = false;
    private boolean showFooter = true;

    // Reference ke RecyclerView untuk update span
    private RecyclerView recyclerView;

    public interface OnItemClickListener {
        void onItemClick(File file);

        void onSelectionChanged();

        void onMoreClick(File file, View view);
    }

    public FileAdapter(List<FileModel> newFileList, OnItemClickListener listener) {
        this.fileList = new ArrayList<>(newFileList);
        this.listener = listener;
    }

    public void setGridView(boolean gridView) {
        if (this.isGridView != gridView) {
            this.isGridView = gridView;
            // Update span size lookup setiap kali ganti mode
            updateSpanSizeLookup();
            notifyDataSetChanged();
        }
    }

    public void setSpanCount(int spanCount) {
        if (this.spanCount != spanCount) {
            this.spanCount = spanCount;
            // Update span size lookup setiap kali ganti span
            updateSpanSizeLookup();
            notifyDataSetChanged();
        }
    }

    private void updateSpanSizeLookup() {
        if (recyclerView == null) return;

        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager instanceof GridLayoutManager) {
            final GridLayoutManager gridLayoutManager = (GridLayoutManager) manager;
            gridLayoutManager.setSpanSizeLookup(
                    new GridLayoutManager.SpanSizeLookup() {
                        @Override
                        public int getSpanSize(int position) {
                            // FOOTER mengambil FULL SPAN
                            if (getItemViewType(position) == TYPE_FOOTER) {
                                return gridLayoutManager.getSpanCount();
                            }
                            return 1;
                        }
                    });
        }
    }

    public List<FileModel> getFileList() {
        return fileList;
    }

    public void updateData(List<FileModel> newFileList) {
        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new FileDiffCallback(this.fileList, newFileList));
        this.fileList.clear();
        this.fileList.addAll(newFileList);
        diffResult.dispatchUpdatesTo(this);
    }

    public File getFirstSelectedFile() {
        for (FileModel model : fileList) {
            if (model.isSelected()) {
                return model.getFile();
            }
        }
        return null;
    }

    public boolean isFooterPosition(int position) {
        return position == fileList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == fileList.size()) {
            return TYPE_FOOTER;
        }

        if (!isGridView) {
            return TYPE_LIST_ITEM;
        } else {
            return (spanCount == 3) ? TYPE_GRID3_ITEM : TYPE_GRID_ITEM;
        }
    }

    @Override
    public int getItemCount() {
        return fileList.size() + 1;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        updateSpanSizeLookup();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_FOOTER) {
            View view = inflater.inflate(R.layout.item_footer, parent, false);
            return new FooterViewHolder(view);
        } else if (viewType == TYPE_GRID3_ITEM) {
            ItemFileGrid3Binding binding = ItemFileGrid3Binding.inflate(inflater, parent, false);
            return new FileGrid3ViewHolder(binding);
        } else if (viewType == TYPE_GRID_ITEM) {
            ItemFileGridBinding binding = ItemFileGridBinding.inflate(inflater, parent, false);
            return new FileGridViewHolder(binding);
        } else {
            ItemFileListBinding binding = ItemFileListBinding.inflate(inflater, parent, false);
            return new FileViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == TYPE_FOOTER) {
            return;
        }

        if (viewType == TYPE_LIST_ITEM
                || viewType == TYPE_GRID_ITEM
                || viewType == TYPE_GRID3_ITEM) {
            FileModel model = fileList.get(position);
            File file = model.getFile();
            Context context = holder.itemView.getContext();

            android.widget.TextView tvName;
            android.widget.TextView tvMeta;
            com.google.android.material.imageview.ShapeableImageView ivIcon;
            android.widget.ImageView ivPlayOverlay;
            com.google.android.material.button.MaterialButton btnCheckbox;
            com.google.android.material.button.MaterialButton btnMore;
            MaterialCardView cardView;

            if (holder instanceof FileViewHolder) {
                FileViewHolder listHolder = (FileViewHolder) holder;
                tvName = listHolder.binding.tvName;
                tvMeta = listHolder.binding.tvMeta;
                ivIcon = listHolder.binding.ivIcon;
                ivPlayOverlay = listHolder.binding.ivPlayOverlay;
                btnCheckbox = listHolder.binding.btnCheckbox;
                btnMore = listHolder.binding.btnMore;
                cardView = listHolder.binding.getRoot();

            } else if (holder instanceof FileGrid3ViewHolder) {
                FileGrid3ViewHolder grid3Holder = (FileGrid3ViewHolder) holder;
                tvName = grid3Holder.binding.tvName;
                tvMeta = grid3Holder.binding.tvMeta;
                ivIcon = grid3Holder.binding.ivIcon;
                ivPlayOverlay = grid3Holder.binding.ivPlayOverlay;
                btnCheckbox = grid3Holder.binding.btnCheckbox;
                btnMore = grid3Holder.binding.btnMore;
                cardView = grid3Holder.binding.getRoot();

            } else {
                FileGridViewHolder gridHolder = (FileGridViewHolder) holder;
                tvName = gridHolder.binding.tvName;
                tvMeta = gridHolder.binding.tvMeta;
                ivIcon = gridHolder.binding.ivIcon;
                ivPlayOverlay = gridHolder.binding.ivPlayOverlay;
                btnCheckbox = gridHolder.binding.btnCheckbox;
                btnMore = gridHolder.binding.btnMore;
                cardView = gridHolder.binding.getRoot();
            }

            tvName.setText(file.getName());

            if (file.isDirectory()) {
                ivIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
                ivIcon.setImageResource(R.drawable.ic_folder);
                ivPlayOverlay.setVisibility(View.GONE);
                tvMeta.setText("Folder");
            } else {
                androidx.core.widget.ImageViewCompat.setImageTintList(ivIcon, null);
                String filePath = file.getAbsolutePath();
                String lowerPath = filePath.toLowerCase();

                if (isVideoFile(filePath)) {
                    ivPlayOverlay.setVisibility(View.VISIBLE);
                    ivIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                    ThumbnailHelper.loadThumbnail(context, filePath, ivIcon);
                } else if (isImageFile(filePath)) {
                    ivPlayOverlay.setVisibility(View.GONE);
                    ivIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                    ThumbnailHelper.loadThumbnail(context, filePath, ivIcon);
                } else if (lowerPath.endsWith(".apk")
                        || lowerPath.endsWith(".mp3")
                        || lowerPath.endsWith(".m4a")
                        || lowerPath.endsWith(".flac")) {
                    ivPlayOverlay.setVisibility(View.GONE);

                    if (lowerPath.endsWith(".apk")) {
                        ivIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
                        ivIcon.setImageResource(R.drawable.ic_apk);
                    } else {
                        ivIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                        ivIcon.setImageResource(R.drawable.ic_audio);
                    }

                    ThumbnailHelper.loadThumbnail(context, filePath, ivIcon);
                } else {
                    ivPlayOverlay.setVisibility(View.GONE);
                    ivIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);

                    String fileName = file.getName();
                    String ext = "";
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                        ext = fileName.substring(dotIndex + 1);
                    }

                    int fileIconRes = getFileIcon(ext);
                    ivIcon.setImageResource(fileIconRes);
                }

                tvMeta.setText(
                        android.text.format.Formatter.formatFileSize(context, file.length()));
            }

            boolean anySelected = isAnySelected();
            if (anySelected) {
                btnCheckbox.setVisibility(View.VISIBLE);
                if (model.isSelected()) {
                    btnCheckbox.setIconResource(R.drawable.ic_checked);
                } else {
                    btnCheckbox.setIconResource(R.drawable.ic_unchecked);
                }
                btnMore.setVisibility(View.GONE);
            } else {
                btnCheckbox.setVisibility(View.GONE);
                btnMore.setVisibility(View.VISIBLE);
            }

            if (model.isSelected()) {
                int selectedColor =
                        MaterialColors.getColor(
                                context,
                                com.google.android.material.R.attr.colorSurfaceContainerHighest,
                                android.graphics.Color.LTGRAY);
                cardView.setCardBackgroundColor(selectedColor);
            } else {
                int normalColor =
                        MaterialColors.getColor(
                                context,
                                com.google.android.material.R.attr.colorSurfaceContainerLowest,
                                android.graphics.Color.TRANSPARENT);
                cardView.setCardBackgroundColor(normalColor);
            }

            holder.itemView.setOnClickListener(
                    v -> {
                        if (isAnySelected()) {
                            toggleSelection(position);
                        } else {
                            listener.onItemClick(file);
                        }
                    });

            holder.itemView.setOnLongClickListener(
                    v -> {
                        toggleSelection(position);
                        return true;
                    });

            btnCheckbox.setOnClickListener(v -> toggleSelection(position));

            btnMore.setOnClickListener(
                    v -> {
                        if (listener != null) {
                            listener.onMoreClick(file, v);
                        }
                    });
        }
    }

    private boolean isImageFile(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp")
                || lower.endsWith(".bmp");
    }

    private boolean isVideoFile(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".mp4")
                || lower.endsWith(".mkv")
                || lower.endsWith(".avi")
                || lower.endsWith(".mov")
                || lower.endsWith(".3gp");
    }

    private static int getFileIcon(String ext) {
        if (ext == null || ext.isEmpty()) return R.drawable.ic_file;

        String lowerExt = ext.toLowerCase();
        switch (lowerExt) {
            case "pdf":
                return R.drawable.ic_pdf;
            case "exe":
                return R.drawable.ic_exe;
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
                return R.drawable.ic_archive;
            case "doc":
            case "odt":
                return R.drawable.ic_doc;
            case "docx":
                return R.drawable.ic_docx;
            case "txt":
            case "log":
            case "md":
                return R.drawable.ic_txt;
            case "java":
                return R.drawable.ic_java;
            case "html":
            case "htm":
                return R.drawable.ic_html;
            case "kt":
            case "kts":
                return R.drawable.ic_kotlin;
            case "php":
                return R.drawable.ic_php;
            case "key":
            case "pem":
            case "jks":
            case "crt":
                return R.drawable.ic_key;
            case "xml":
                return R.drawable.ic_xml;
            case "gradle":
                return R.drawable.ic_gradle;
            case "py":
            case "pyw":
                return R.drawable.ic_python;
            case "json":
                return R.drawable.ic_json;
            default:
                return R.drawable.ic_file;
        }
    }

    private void toggleSelection(int position) {
        FileModel model = fileList.get(position);
        boolean wasAnySelected = isAnySelected();
        model.setSelected(!model.isSelected());
        boolean isNowAnySelected = isAnySelected();

        if (wasAnySelected != isNowAnySelected) {
            notifyItemRangeChanged(0, fileList.size());
        } else {
            notifyItemChanged(position);
        }

        listener.onSelectionChanged();
    }

    public void selectAll(boolean select) {
        for (FileModel model : fileList) {
            model.setSelected(select);
        }
        notifyItemRangeChanged(0, fileList.size());
        listener.onSelectionChanged();
    }

    public boolean isAnySelected() {
        for (FileModel model : fileList) {
            if (model.isSelected()) return true;
        }
        return false;
    }

    public void clearSelection() {
        for (FileModel model : fileList) {
            model.setSelected(false);
        }
        notifyItemRangeChanged(0, fileList.size());
        listener.onSelectionChanged();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        ItemFileListBinding binding;

        public FileViewHolder(ItemFileListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class FileGridViewHolder extends RecyclerView.ViewHolder {
        ItemFileGridBinding binding;

        public FileGridViewHolder(ItemFileGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class FileGrid3ViewHolder extends RecyclerView.ViewHolder {
        ItemFileGrid3Binding binding;

        public FileGrid3ViewHolder(ItemFileGrid3Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private static class FileDiffCallback extends DiffUtil.Callback {
        private final List<FileModel> oldList;
        private final List<FileModel> newList;

        public FileDiffCallback(List<FileModel> oldList, List<FileModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition)
                    .getFile()
                    .getAbsolutePath()
                    .equals(newList.get(newItemPosition).getFile().getAbsolutePath());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            FileModel oldModel = oldList.get(oldItemPosition);
            FileModel newModel = newList.get(newItemPosition);
            return oldModel.isSelected() == newModel.isSelected()
                    && oldModel.getFile().getName().equals(newModel.getFile().getName());
        }
    }
}
