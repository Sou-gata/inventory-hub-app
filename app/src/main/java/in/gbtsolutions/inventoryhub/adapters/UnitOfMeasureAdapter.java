package in.gbtsolutions.inventoryhub.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import in.gbtsolutions.inventoryhub.models.UnitOfMeasure;

public class UnitOfMeasureAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_FOOTER = 1;

    private final List<UnitOfMeasure> uomList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private OnUomClickListener clickListener;
    private OnUomEditListener editListener;
    private OnUomDeleteListener deleteListener;

    private boolean isFooterAdded = false;
    private boolean isRetryActive = false;
    private String retryMessage = null;
    private Runnable retryAction = null;

    public UnitOfMeasureAdapter() {
    }

    public void setOnUomClickListener(OnUomClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnUomEditListener(OnUomEditListener listener) {
        this.editListener = listener;
    }

    public void setOnUomDeleteListener(OnUomDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setUnits(@NonNull List<UnitOfMeasure> newUnits) {
        isFooterAdded = false;
        isRetryActive = false;
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return uomList.size();
            }

            @Override
            public int getNewListSize() {
                return newUnits.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return uomList.get(oldItemPosition).uomId == newUnits.get(newItemPosition).uomId;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                UnitOfMeasure oldU = uomList.get(oldItemPosition);
                UnitOfMeasure newU = newUnits.get(newItemPosition);
                return TextUtils.equals(oldU.name, newU.name) && TextUtils.equals(oldU.description, newU.description) && oldU.isDefault == newU.isDefault && oldU.createdAt == newU.createdAt;
            }
        });

        uomList.clear();
        uomList.addAll(newUnits);
        diffResult.dispatchUpdatesTo(this);
    }

    public void addUnits(@NonNull List<UnitOfMeasure> moreUnits) {
        if (moreUnits.isEmpty()) return;
        int startPos = uomList.size();
        uomList.addAll(moreUnits);
        notifyItemRangeInserted(startPos, moreUnits.size());
    }

    public void clearUnits() {
        int count = getItemCount();
        uomList.clear();
        isFooterAdded = false;
        isRetryActive = false;
        if (count > 0) {
            notifyItemRangeRemoved(0, count);
        }
    }

    public List<UnitOfMeasure> getUnits() {
        return new ArrayList<>(uomList);
    }

    public void showLoadingFooter() {
        if (!isFooterAdded) {
            isFooterAdded = true;
            isRetryActive = false;
            notifyItemInserted(uomList.size());
        } else if (isRetryActive) {
            isRetryActive = false;
            notifyItemChanged(uomList.size());
        }
    }

    public void showRetryFooter(String message, Runnable retry) {
        this.retryMessage = message;
        this.retryAction = retry;
        this.isRetryActive = true;
        if (!isFooterAdded) {
            isFooterAdded = true;
            notifyItemInserted(uomList.size());
        } else {
            notifyItemChanged(uomList.size());
        }
    }

    public void removeFooter() {
        if (isFooterAdded) {
            isFooterAdded = false;
            isRetryActive = false;
            retryMessage = null;
            retryAction = null;
            notifyItemRemoved(uomList.size());
        }
    }

    public boolean isFooterAdded() {
        return isFooterAdded;
    }

    @Override
    public int getItemViewType(int position) {
        if (isFooterAdded && position == uomList.size()) {
            return VIEW_TYPE_FOOTER;
        }
        return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading_footer, parent, false);
            return new FooterViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_unit_of_measure, parent, false);
        return new UomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof UomViewHolder) {
            UnitOfMeasure uom = uomList.get(position);
            ((UomViewHolder) holder).bind(uom);
        } else if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).bind(isRetryActive, retryMessage, retryAction);
        }
    }

    @Override
    public int getItemCount() {
        return isFooterAdded ? uomList.size() + 1 : uomList.size();
    }

    public interface OnUomClickListener {
        void onUomClick(@NonNull UnitOfMeasure uom);
    }

    public interface OnUomEditListener {
        void onUomEdit(@NonNull UnitOfMeasure uom);
    }

    public interface OnUomDeleteListener {
        void onUomDelete(@NonNull UnitOfMeasure uom);
    }

    public class UomViewHolder extends RecyclerView.ViewHolder {
        private final View cardRoot;
        private final TextView textName;
        private final TextView textDescription;
        private final TextView textDate;
        private final View badgeDefault;
        private final View btnLockedInfo;
        private final View btnEdit;
        private final View btnDelete;

        public UomViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_uom_root);
            textName = itemView.findViewById(R.id.text_uom_name);
            textDescription = itemView.findViewById(R.id.text_uom_description);
            textDate = itemView.findViewById(R.id.text_uom_date);
            badgeDefault = itemView.findViewById(R.id.badge_default);
            btnLockedInfo = itemView.findViewById(R.id.btn_locked_info);
            btnEdit = itemView.findViewById(R.id.btn_edit_uom);
            btnDelete = itemView.findViewById(R.id.btn_delete_uom);
        }

        public void bind(@NonNull UnitOfMeasure uom) {
            String name = uom.name != null ? uom.name.trim() : "";
            textName.setText(name);

            // Description
            if (!TextUtils.isEmpty(uom.description)) {
                textDescription.setVisibility(View.VISIBLE);
                textDescription.setText(uom.description.trim());
            } else {
                textDescription.setVisibility(View.GONE);
            }

            // Default vs Custom UI controls
            if (uom.isDefault) {
                badgeDefault.setVisibility(View.VISIBLE);
                btnLockedInfo.setVisibility(View.VISIBLE);
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
                textDate.setVisibility(View.GONE);

                View.OnClickListener lockedClick = v -> {
                    if (clickListener != null) {
                        clickListener.onUomClick(uom);
                    }
                };
                cardRoot.setOnClickListener(lockedClick);
                btnLockedInfo.setOnClickListener(lockedClick);
            } else {
                badgeDefault.setVisibility(View.GONE);
                btnLockedInfo.setVisibility(View.GONE);
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);

                if (uom.createdAt > 0) {
                    textDate.setVisibility(View.VISIBLE);
                    textDate.setText(String.format("Added %s", dateFormat.format(new Date(uom.createdAt))));
                } else {
                    textDate.setVisibility(View.GONE);
                }

                cardRoot.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onUomClick(uom);
                    }
                });

                btnEdit.setOnClickListener(v -> {
                    if (editListener != null) {
                        editListener.onUomEdit(uom);
                    }
                });

                btnDelete.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onUomDelete(uom);
                    }
                });
            }
        }
    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder {
        private final View containerLoading;
        private final View containerRetry;
        private final TextView textRetryMessage;
        private final TextView btnRetryLoad;

        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
            containerLoading = itemView.findViewById(R.id.container_loading);
            containerRetry = itemView.findViewById(R.id.container_retry);
            textRetryMessage = itemView.findViewById(R.id.text_retry_message);
            btnRetryLoad = itemView.findViewById(R.id.btn_retry_load);
        }

        public void bind(boolean isRetry, String message, Runnable retryAction) {
            if (isRetry) {
                if (containerLoading != null) containerLoading.setVisibility(View.GONE);
                if (containerRetry != null) {
                    containerRetry.setVisibility(View.VISIBLE);
                    if (textRetryMessage != null && message != null) {
                        textRetryMessage.setText(message);
                    }
                    if (btnRetryLoad != null) {
                        btnRetryLoad.setOnClickListener(v -> {
                            if (retryAction != null) {
                                retryAction.run();
                            }
                        });
                    }
                }
            } else {
                if (containerRetry != null) containerRetry.setVisibility(View.GONE);
                if (containerLoading != null) containerLoading.setVisibility(View.VISIBLE);
            }
        }
    }
}

