package com.fileexplorer.app;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fileexplorer.app.databinding.ItemCategoryBinding;
import com.google.android.material.color.MaterialColors;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<CategoryItem> categories;
    private OnCategoryClickListener listener;
    private int selectedPosition = 0;

    public interface OnCategoryClickListener {
        void onCategoryClick(String key, int position);
    }

    public CategoryAdapter(List<CategoryItem> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
        if (!categories.isEmpty()) {
            categories.get(0).setSelected(true);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding binding =
                ItemCategoryBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryItem item = categories.get(position);

        holder.binding.ivCategoryIcon.setImageResource(item.getIconRes());
        holder.binding.tvCategoryName.setText(item.getName());

        if (item.isSelected()) {
            int primaryColor =
                    MaterialColors.getColor(
                            holder.binding.getRoot().getContext(),
                            com.google.android.material.R.attr.colorPrimaryVariant,
                            android.R.color.black);

            holder.binding.ivCategoryIcon.setColorFilter(primaryColor);
            holder.binding.tvCategoryName.setTextColor(primaryColor);
            holder.binding.layoutCategory.setBackgroundResource(R.drawable.bg_category_selected);
        } else {
            int onSurfaceVariant =
                    MaterialColors.getColor(
                            holder.binding.getRoot().getContext(),
                            com.google.android.material.R.attr.colorOnSurfaceVariant,
                            android.R.color.darker_gray);

            holder.binding.ivCategoryIcon.setColorFilter(onSurfaceVariant);
            holder.binding.tvCategoryName.setTextColor(onSurfaceVariant);
            holder.binding.layoutCategory.setBackgroundResource(0);
        }

        holder.itemView.setOnClickListener(
                v -> {
                    if (listener != null) {
                        for (CategoryItem cat : categories) {
                            cat.setSelected(false);
                        }
                        item.setSelected(true);
                        selectedPosition = position;
                        notifyDataSetChanged();
                        listener.onCategoryClick(item.getKey(), position);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void setSelectedPosition(String key) {
        for (int i = 0; i < categories.size(); i++) {
            categories.get(i).setSelected(categories.get(i).getKey().equals(key));
            if (categories.get(i).isSelected()) {
                selectedPosition = i;
            }
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemCategoryBinding binding;

        ViewHolder(@NonNull ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
