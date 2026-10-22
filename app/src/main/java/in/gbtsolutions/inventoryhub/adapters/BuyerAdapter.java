package in.gbtsolutions.inventoryhub.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.models.Buyer;

public class BuyerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_ITEM = 0;
    public static final int VIEW_TYPE_FOOTER = 1;

    private final List<Buyer> buyerList = new ArrayList<>();
    private OnBuyerClickListener clickListener;
    private OnBuyerEditListener editListener;
    private OnBuyerStatusToggleListener statusToggleListener;

    private boolean isFooterAdded = false;
    private boolean isRetryActive = false;
    private String retryMessage = null;
    private Runnable retryAction = null;

    public BuyerAdapter() {
    }

    public void setOnBuyerClickListener(OnBuyerClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnBuyerEditListener(OnBuyerEditListener listener) {
        this.editListener = listener;
    }

    public void setOnBuyerStatusToggleListener(OnBuyerStatusToggleListener listener) {
        this.statusToggleListener = listener;
    }

    public List<Buyer> getBuyers() {
        return new ArrayList<>(buyerList);
    }

    public void clearBuyers() {
        int count = getItemCount();
        buyerList.clear();
        isFooterAdded = false;
        isRetryActive = false;
        if (count > 0) {
            notifyItemRangeRemoved(0, count);
        }
    }

    public void addBuyers(@NonNull List<Buyer> moreBuyers) {
        if (moreBuyers.isEmpty()) return;
        List<Buyer> toAdd = new ArrayList<>();
        java.util.HashSet<Integer> existingIds = new java.util.HashSet<>();
        for (Buyer b : buyerList) {
            existingIds.add(b.buyerId);
        }
        for (Buyer b : moreBuyers) {
            if (b.buyerId <= 0 || !existingIds.contains(b.buyerId)) {
                toAdd.add(b);
                if (b.buyerId > 0) {
                    existingIds.add(b.buyerId);
                }
            }
        }
        if (toAdd.isEmpty()) return;
        int insertPos = buyerList.size();
        buyerList.addAll(toAdd);
        notifyItemRangeInserted(insertPos, toAdd.size());
    }

    public void showLoadingFooter() {
        if (!isFooterAdded) {
            isFooterAdded = true;
            isRetryActive = false;
            notifyItemInserted(buyerList.size());
        } else if (isRetryActive) {
            isRetryActive = false;
            notifyItemChanged(buyerList.size());
        }
    }

    public void showRetryFooter(String message, Runnable retry) {
        this.retryMessage = message;
        this.retryAction = retry;
        this.isRetryActive = true;
        if (!isFooterAdded) {
            isFooterAdded = true;
            notifyItemInserted(buyerList.size());
        } else {
            notifyItemChanged(buyerList.size());
        }
    }

    public void removeFooter() {
        if (isFooterAdded) {
            isFooterAdded = false;
            isRetryActive = false;
            retryMessage = null;
            retryAction = null;
            notifyItemRemoved(buyerList.size());
        }
    }

    public boolean isFooterAdded() {
        return isFooterAdded;
    }

    public void setBuyers(@NonNull List<Buyer> newBuyers) {
        isFooterAdded = false;
        isRetryActive = false;
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return buyerList.size();
            }

            @Override
            public int getNewListSize() {
                return newBuyers.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return buyerList.get(oldItemPosition).buyerId == newBuyers.get(newItemPosition).buyerId;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Buyer oldBuyer = buyerList.get(oldItemPosition);
                Buyer newBuyer = newBuyers.get(newItemPosition);
                return TextUtils.equals(oldBuyer.buyerName, newBuyer.buyerName) && TextUtils.equals(oldBuyer.contactPerson, newBuyer.contactPerson) && TextUtils.equals(oldBuyer.phone, newBuyer.phone) && TextUtils.equals(oldBuyer.email, newBuyer.email) && TextUtils.equals(oldBuyer.city, newBuyer.city) && TextUtils.equals(oldBuyer.gst, newBuyer.gst) && oldBuyer.isActive == newBuyer.isActive;
            }
        });

        buyerList.clear();
        buyerList.addAll(newBuyers);
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemViewType(int position) {
        if (isFooterAdded && position == buyerList.size()) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_buyer, parent, false);
        return new BuyerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof BuyerViewHolder) {
            Buyer buyer = buyerList.get(position);
            ((BuyerViewHolder) holder).bind(buyer);
        } else if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).bind(isRetryActive, retryMessage, retryAction);
        }
    }

    @Override
    public int getItemCount() {
        return isFooterAdded ? buyerList.size() + 1 : buyerList.size();
    }

    public interface OnBuyerClickListener {
        void onBuyerClick(@NonNull Buyer buyer);
    }

    public interface OnBuyerEditListener {
        void onBuyerEdit(@NonNull Buyer buyer);
    }

    public interface OnBuyerStatusToggleListener {
        void onBuyerStatusToggle(@NonNull Buyer buyer);
    }

    public class BuyerViewHolder extends RecyclerView.ViewHolder {
        private final View cardRoot;
        private final TextView textName;
        private final TextView textStatus;
        private final TextView textContact;
        private final TextView textSubtext;
        private final FrameLayout btnToggleStatus;
        private final ImageView iconToggleStatus;
        private final FrameLayout btnEdit;

        public BuyerViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_buyer_root);
            textName = itemView.findViewById(R.id.text_buyer_name);
            textStatus = itemView.findViewById(R.id.text_buyer_status);
            textContact = itemView.findViewById(R.id.text_buyer_contact);
            textSubtext = itemView.findViewById(R.id.text_buyer_subtext);
            btnToggleStatus = itemView.findViewById(R.id.btn_toggle_status_buyer);
            iconToggleStatus = itemView.findViewById(R.id.icon_toggle_status);
            btnEdit = itemView.findViewById(R.id.btn_edit_buyer);
        }

        public void bind(@NonNull Buyer buyer) {
            String name = buyer.buyerName != null ? buyer.buyerName.trim() : "";
            textName.setText(name.isEmpty() ? "Unnamed Buyer" : name);

            // Active / Deactivated status styling
            if (buyer.isActive) {
                textStatus.setText("Active");
                textStatus.setBackgroundResource(R.drawable.bg_status_chip);
                textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.accent_teal));
                iconToggleStatus.setImageResource(R.drawable.ic_power);
                iconToggleStatus.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.accent_teal));
                cardRoot.setAlpha(1.0f);
            } else {
                textStatus.setText("Deactivated");
                textStatus.setBackgroundResource(R.drawable.bg_field_normal);
                textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.fg_dim));
                iconToggleStatus.setImageResource(R.drawable.ic_power);
                iconToggleStatus.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.fg_dim));
                cardRoot.setAlpha(0.78f);
            }

            // Contact line: Person + Phone
            StringBuilder contactBuilder = new StringBuilder();
            if (!TextUtils.isEmpty(buyer.contactPerson)) {
                contactBuilder.append(buyer.contactPerson.trim());
            }
            if (!TextUtils.isEmpty(buyer.phone)) {
                if (contactBuilder.length() > 0) {
                    contactBuilder.append(" • ");
                }
                contactBuilder.append(buyer.phone.trim());
            }
            if (contactBuilder.length() > 0) {
                textContact.setVisibility(View.VISIBLE);
                textContact.setText(contactBuilder.toString());
            } else {
                textContact.setVisibility(View.GONE);
            }

            // Subtext line: City / State + GST
            StringBuilder subtextBuilder = new StringBuilder();
            if (!TextUtils.isEmpty(buyer.city)) {
                subtextBuilder.append(buyer.city.trim());
                if (!TextUtils.isEmpty(buyer.stateCode)) {
                    subtextBuilder.append(", ").append(buyer.stateCode.trim());
                }
            } else if (!TextUtils.isEmpty(buyer.stateCode)) {
                subtextBuilder.append(buyer.stateCode.trim());
            }

            if (!TextUtils.isEmpty(buyer.gst)) {
                if (subtextBuilder.length() > 0) {
                    subtextBuilder.append(" • GST: ");
                } else {
                    subtextBuilder.append("GST: ");
                }
                subtextBuilder.append(buyer.gst.trim());
            }

            if (subtextBuilder.length() > 0) {
                textSubtext.setVisibility(View.VISIBLE);
                textSubtext.setText(subtextBuilder.toString());
            } else if (!TextUtils.isEmpty(buyer.email)) {
                textSubtext.setVisibility(View.VISIBLE);
                textSubtext.setText(buyer.email.trim());
            } else {
                textSubtext.setVisibility(View.GONE);
            }

            // Clicks
            cardRoot.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onBuyerClick(buyer);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onBuyerEdit(buyer);
                }
            });

            btnToggleStatus.setOnClickListener(v -> {
                if (statusToggleListener != null) {
                    statusToggleListener.onBuyerStatusToggle(buyer);
                }
            });
        }
    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder {
        private final View containerLoading;
        private final View containerRetry;
        private final TextView textLoadingMore;
        private final TextView textRetryMessage;
        private final TextView btnRetryLoad;

        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
            containerLoading = itemView.findViewById(R.id.container_loading);
            containerRetry = itemView.findViewById(R.id.container_retry);
            textLoadingMore = itemView.findViewById(R.id.text_loading_more);
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
                if (containerLoading != null) {
                    containerLoading.setVisibility(View.VISIBLE);
                    if (textLoadingMore != null) {
                        textLoadingMore.setText("Loading more buyers...");
                    }
                }
            }
        }
    }
}
