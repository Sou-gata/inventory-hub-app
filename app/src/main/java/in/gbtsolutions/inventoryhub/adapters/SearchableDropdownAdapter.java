package in.gbtsolutions.inventoryhub.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import in.gbtsolutions.inventoryhub.R;

public class SearchableDropdownAdapter<T> extends ArrayAdapter<T> implements Filterable {

    private final List<T> allItems = new ArrayList<>();
    private final List<T> filteredItems = new ArrayList<>();
    private final FilterCriterion<T> criterion;
    private final LayoutInflater inflater;

    public SearchableDropdownAdapter(@NonNull Context context, @NonNull List<T> items, FilterCriterion<T> criterion) {
        super(context, R.layout.item_dropdown_searchable);
        this.criterion = criterion;
        this.inflater = LayoutInflater.from(context);
        updateData(items);
    }

    public void updateData(@Nullable List<T> items) {
        allItems.clear();
        if (items != null) {
            allItems.addAll(items);
        }
        filteredItems.clear();
        filteredItems.addAll(allItems);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return filteredItems.size();
    }

    @Nullable
    @Override
    public T getItem(int position) {
        if (position >= 0 && position < filteredItems.size()) {
            return filteredItems.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_dropdown_searchable, parent, false);
        }

        T item = getItem(position);
        if (item != null) {
            TextView textTitle = view.findViewById(R.id.text_dropdown_title);
            TextView textSubtitle = view.findViewById(R.id.text_dropdown_subtitle);

            String title = criterion.getTitle(item);
            String subtitle = criterion.getSubtitle(item);

            textTitle.setText(title != null ? title : "");
            if (!TextUtils.isEmpty(subtitle)) {
                textSubtitle.setText(subtitle);
                textSubtitle.setVisibility(View.VISIBLE);
            } else {
                textSubtitle.setVisibility(View.GONE);
            }
        }

        return view;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint == null || TextUtils.isEmpty(constraint.toString().trim())) {
                    List<T> copy = new ArrayList<>(allItems);
                    results.values = copy;
                    results.count = copy.size();
                } else {
                    String query = constraint.toString().trim().toLowerCase(Locale.getDefault());
                    List<T> matches = new ArrayList<>();
                    for (T item : allItems) {
                        if (criterion.matches(item, query)) {
                            matches.add(item);
                        }
                    }
                    results.values = matches;
                    results.count = matches.size();
                }
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredItems.clear();
                if (results.values != null) {
                    filteredItems.addAll((List<T>) results.values);
                }
                notifyDataSetChanged();
            }

            @SuppressWarnings("unchecked")
            @Override
            public CharSequence convertResultToString(Object resultValue) {
                if (resultValue != null) {
                    try {
                        return criterion.getTitle((T) resultValue);
                    } catch (ClassCastException ignored) {
                    }
                }
                return super.convertResultToString(resultValue);
            }
        };
    }

    public interface FilterCriterion<T> {
        boolean matches(T item, String query);

        String getTitle(T item);

        String getSubtitle(T item);
    }
}
