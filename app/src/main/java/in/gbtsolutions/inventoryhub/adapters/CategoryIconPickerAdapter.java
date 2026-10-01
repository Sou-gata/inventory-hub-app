package in.gbtsolutions.inventoryhub.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.helpers.CategoryIconHelper;

public class CategoryIconPickerAdapter extends RecyclerView.Adapter<CategoryIconPickerAdapter.IconViewHolder> {

    public interface OnIconSelectedListener {
        void onIconSelected(@NonNull CategoryIconHelper.IconItem iconItem);
    }

    private final List<CategoryIconHelper.IconItem> iconList = new ArrayList<>();
    private String selectedKey = CategoryIconHelper.DEFAULT_ICON_KEY;
    private OnIconSelectedListener listener;

    public CategoryIconPickerAdapter(@Nullable String initialKey, @Nullable OnIconSelectedListener listener) {
        this.selectedKey = CategoryIconHelper.sanitizeIconKey(initialKey);
        this.listener = listener;
        this.iconList.addAll(CategoryIconHelper.getAllIcons());
    }

    public void setIcons(@NonNull List<CategoryIconHelper.IconItem> icons) {
        iconList.clear();
        iconList.addAll(icons);
        notifyDataSetChanged();
    }

    public void setSelectedKey(@Nullable String key) {
        this.selectedKey = CategoryIconHelper.sanitizeIconKey(key);
        notifyDataSetChanged();
    }

    @NonNull
    public String getSelectedKey() {
        return selectedKey;
    }

    public void setOnIconSelectedListener(@Nullable OnIconSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_icon_picker, parent, false);
        return new IconViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
        CategoryIconHelper.IconItem item = iconList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return iconList.size();
    }

    public class IconViewHolder extends RecyclerView.ViewHolder {
        private final View containerCard;
        private final ImageView imgIcon;
        private final ImageView badgeSelected;
        private final TextView textLabel;

        public IconViewHolder(@NonNull View itemView) {
            super(itemView);
            containerCard = itemView.findViewById(R.id.container_icon_card);
            imgIcon = itemView.findViewById(R.id.img_icon);
            badgeSelected = itemView.findViewById(R.id.badge_selected);
            textLabel = itemView.findViewById(R.id.text_icon_label);
        }

        public void bind(@NonNull CategoryIconHelper.IconItem item) {
            Context context = itemView.getContext();
            boolean isSelected = TextUtils.equals(item.key, selectedKey);

            imgIcon.setImageResource(item.resId);
            textLabel.setText(item.label);

            if (isSelected) {
                containerCard.setBackgroundResource(R.drawable.bg_icon_picker_item_selected);
                badgeSelected.setVisibility(View.VISIBLE);
                imgIcon.setColorFilter(ContextCompat.getColor(context, R.color.material_blue));
                textLabel.setTextColor(ContextCompat.getColor(context, R.color.material_blue));
            } else {
                containerCard.setBackgroundResource(R.drawable.bg_icon_picker_item);
                badgeSelected.setVisibility(View.GONE);
                imgIcon.setColorFilter(ContextCompat.getColor(context, R.color.fg));
                textLabel.setTextColor(ContextCompat.getColor(context, R.color.fg_muted));
            }

            containerCard.setOnClickListener(v -> {
                selectedKey = item.key;
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onIconSelected(item);
                }
            });
        }
    }
}
