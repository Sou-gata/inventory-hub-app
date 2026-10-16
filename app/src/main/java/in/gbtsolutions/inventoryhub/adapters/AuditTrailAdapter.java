package in.gbtsolutions.inventoryhub.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.models.AuditTrail;

public class AuditTrailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_ITEM = 0;
    public static final int VIEW_TYPE_FOOTER = 1;

    private final List<AuditTrail> auditList = new ArrayList<>();
    private OnAuditTrailClickListener clickListener;

    private boolean isFooterAdded = false;
    private boolean isRetryActive = false;
    private String retryMessage = null;
    private Runnable retryAction = null;

    public AuditTrailAdapter() {
    }

    public void setOnAuditTrailClickListener(OnAuditTrailClickListener listener) {
        this.clickListener = listener;
    }

    public void clearAuditTrails() {
        int count = getItemCount();
        auditList.clear();
        isFooterAdded = false;
        isRetryActive = false;
        retryMessage = null;
        retryAction = null;
        if (count > 0) {
            notifyDataSetChanged();
        }
    }

    public void addAuditTrails(@NonNull List<AuditTrail> newBatch) {
        if (newBatch.isEmpty()) return;
        int insertPos = auditList.size();
        auditList.addAll(newBatch);
        notifyItemRangeInserted(insertPos, newBatch.size());
    }

    public void showLoadingFooter() {
        if (!isFooterAdded) {
            isFooterAdded = true;
            isRetryActive = false;
            notifyItemInserted(auditList.size());
        } else if (isRetryActive) {
            isRetryActive = false;
            notifyItemChanged(auditList.size());
        }
    }

    public void showRetryFooter(String message, Runnable retry) {
        this.retryMessage = message;
        this.retryAction = retry;
        this.isRetryActive = true;
        if (!isFooterAdded) {
            isFooterAdded = true;
            notifyItemInserted(auditList.size());
        } else {
            notifyItemChanged(auditList.size());
        }
    }

    public void removeFooter() {
        if (isFooterAdded) {
            isFooterAdded = false;
            isRetryActive = false;
            retryMessage = null;
            retryAction = null;
            notifyItemRemoved(auditList.size());
        }
    }

    public boolean isFooterAdded() {
        return isFooterAdded;
    }

    public List<AuditTrail> getAuditTrails() {
        return new ArrayList<>(auditList);
    }

    public void setAuditTrails(@NonNull List<AuditTrail> newAuditTrails) {
        isFooterAdded = false;
        isRetryActive = false;
        retryMessage = null;
        retryAction = null;

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return auditList.size();
            }

            @Override
            public int getNewListSize() {
                return newAuditTrails.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return auditList.get(oldItemPosition).id == newAuditTrails.get(newItemPosition).id;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                AuditTrail oldItem = auditList.get(oldItemPosition);
                AuditTrail newItem = newAuditTrails.get(newItemPosition);
                return TextUtils.equals(oldItem.actionType, newItem.actionType)
                        && TextUtils.equals(oldItem.module, newItem.module)
                        && TextUtils.equals(oldItem.recordId, newItem.recordId)
                        && TextUtils.equals(oldItem.details, newItem.details)
                        && TextUtils.equals(oldItem.performedBy, newItem.performedBy)
                        && oldItem.timestamp == newItem.timestamp;
            }
        });

        auditList.clear();
        auditList.addAll(newAuditTrails);
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemViewType(int position) {
        if (isFooterAdded && position == auditList.size()) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audit_trail, parent, false);
        return new AuditTrailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AuditTrailViewHolder) {
            ((AuditTrailViewHolder) holder).bind(auditList.get(position));
        } else if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).bind(isRetryActive, retryMessage, retryAction);
        }
    }

    @Override
    public int getItemCount() {
        return isFooterAdded ? auditList.size() + 1 : auditList.size();
    }

    public interface OnAuditTrailClickListener {
        void onAuditTrailClick(@NonNull AuditTrail auditTrail);
    }

    public class AuditTrailViewHolder extends RecyclerView.ViewHolder {

        private final View cardView;
        private final LinearLayout layoutActionBadge;
        private final ImageView iconActionType;
        private final TextView textActionType;
        private final TextView textModuleName;
        private final TextView textTimestamp;
        private final TextView textRecordId;
        private final TextView textAuditDetails;
        private final TextView textPerformedBy;

        public AuditTrailViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_audit_item);
            layoutActionBadge = itemView.findViewById(R.id.layout_action_badge);
            iconActionType = itemView.findViewById(R.id.icon_action_type);
            textActionType = itemView.findViewById(R.id.text_action_type);
            textModuleName = itemView.findViewById(R.id.text_module_name);
            textTimestamp = itemView.findViewById(R.id.text_timestamp);
            textRecordId = itemView.findViewById(R.id.text_record_id);
            textAuditDetails = itemView.findViewById(R.id.text_audit_details);
            textPerformedBy = itemView.findViewById(R.id.text_performed_by);
        }

        public void bind(@NonNull AuditTrail item) {
            Context context = itemView.getContext();

            // Action type badge formatting
            String action = item.actionType != null ? item.actionType.toUpperCase() : "EVENT";
            textActionType.setText(action);

            if (AuditTrail.ACTION_ADD.equalsIgnoreCase(action)) {
                layoutActionBadge.setBackgroundResource(R.drawable.bg_badge_add);
                int greenColor = ContextCompat.getColor(context, R.color.status_green);
                textActionType.setTextColor(greenColor);
                iconActionType.setImageResource(R.drawable.ic_add);
                iconActionType.setColorFilter(greenColor);
            } else if (AuditTrail.ACTION_EDIT.equalsIgnoreCase(action)) {
                layoutActionBadge.setBackgroundResource(R.drawable.bg_badge_edit);
                int blueColor = ContextCompat.getColor(context, R.color.status_blue);
                textActionType.setTextColor(blueColor);
                iconActionType.setImageResource(R.drawable.ic_edit);
                iconActionType.setColorFilter(blueColor);
            } else if (AuditTrail.ACTION_DELETE.equalsIgnoreCase(action)) {
                layoutActionBadge.setBackgroundResource(R.drawable.bg_badge_delete);
                int redColor = ContextCompat.getColor(context, R.color.status_red);
                textActionType.setTextColor(redColor);
                iconActionType.setImageResource(R.drawable.ic_delete_outline);
                iconActionType.setColorFilter(redColor);
            } else {
                layoutActionBadge.setBackgroundResource(R.drawable.bg_badge_edit);
                int mutedColor = ContextCompat.getColor(context, R.color.fg_muted);
                textActionType.setTextColor(mutedColor);
                iconActionType.setImageResource(R.drawable.ic_nav_info);
                iconActionType.setColorFilter(mutedColor);
            }

            // Module
            textModuleName.setText(item.module != null && !item.module.isEmpty() ? item.module : "General");

            // Timestamp
            textTimestamp.setText(item.getFormattedDate());

            // Record ID
            if (item.recordId != null && !item.recordId.trim().isEmpty()) {
                textRecordId.setText(item.recordId);
                textRecordId.setVisibility(View.VISIBLE);
            } else {
                textRecordId.setVisibility(View.GONE);
            }

            // Details
            textAuditDetails.setText(item.details != null ? item.details : "");

            // Performed By (Logged-in user)
            String user = item.performedBy != null && !item.performedBy.trim().isEmpty() ? item.performedBy : "System";
            textPerformedBy.setText(user);

            // Click listener
            cardView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onAuditTrailClick(item);
                }
            });
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
                            if (retryAction != null) retryAction.run();
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
