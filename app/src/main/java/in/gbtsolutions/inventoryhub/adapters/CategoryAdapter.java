package in.gbtsolutions.inventoryhub.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.helpers.CategoryIconHelper;
import in.gbtsolutions.inventoryhub.models.Category;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<Category> categoryList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private OnCategoryClickListener clickListener;
    private OnCategoryEditListener editListener;

    public CategoryAdapter() {
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnCategoryEditListener(OnCategoryEditListener listener) {
        this.editListener = listener;
    }

    public void setCategories(@NonNull List<Category> newCategories) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return categoryList.size();
            }

            @Override
            public int getNewListSize() {
                return newCategories.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return categoryList.get(oldItemPosition).categoryId == newCategories.get(newItemPosition).categoryId;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Category oldCat = categoryList.get(oldItemPosition);
                Category newCat = newCategories.get(newItemPosition);
                return TextUtils.equals(oldCat.categoryName, newCat.categoryName) && TextUtils.equals(oldCat.description, newCat.description) && TextUtils.equals(oldCat.icon, newCat.icon) && oldCat.createdAt == newCat.createdAt;
            }
        });

        categoryList.clear();
        categoryList.addAll(newCategories);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public interface OnCategoryClickListener {
        void onCategoryClick(@NonNull Category category);
    }

    public interface OnCategoryEditListener {
        void onCategoryEdit(@NonNull Category category);
    }

    public class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final View cardRoot;
        private final ImageView imgIcon;
        private final TextView textName;
        private final TextView textDescription;
        private final TextView textDate;
        private final View btnEdit;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_category_root);
            imgIcon = itemView.findViewById(R.id.img_category_icon);
            textName = itemView.findViewById(R.id.text_category_name);
            textDescription = itemView.findViewById(R.id.text_category_description);
            textDate = itemView.findViewById(R.id.text_category_date);
            btnEdit = itemView.findViewById(R.id.btn_edit_category);
        }

        public void bind(@NonNull Category category) {
            String name = category.categoryName != null ? category.categoryName.trim() : "";
            textName.setText(name);

            // Icon
            if (imgIcon != null) {
                imgIcon.setImageResource(CategoryIconHelper.getIconResId(itemView.getContext(), category.icon));
            }

            // Description
            if (!TextUtils.isEmpty(category.description)) {
                textDescription.setVisibility(View.VISIBLE);
                textDescription.setText(category.description.trim());
            } else {
                textDescription.setVisibility(View.GONE);
            }

            // Date
            if (category.createdAt > 0) {
                textDate.setVisibility(View.VISIBLE);
                textDate.setText(String.format("Added %s", dateFormat.format(new Date(category.createdAt))));
            } else {
                textDate.setVisibility(View.GONE);
            }

            // Clicks
            cardRoot.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onCategoryClick(category);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onCategoryEdit(category);
                }
            });
        }
    }
}
