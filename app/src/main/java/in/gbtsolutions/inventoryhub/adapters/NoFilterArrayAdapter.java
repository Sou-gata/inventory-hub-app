package in.gbtsolutions.inventoryhub.adapters;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

public class NoFilterArrayAdapter<T> extends ArrayAdapter<T> {

    private final List<T> items;

    private final Filter noFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            results.values = items;
            results.count = items.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return resultValue == null ? "" : resultValue.toString();
        }
    };

    public NoFilterArrayAdapter(@NonNull Context context, int resource, @NonNull List<T> items) {
        super(context, resource, items);
        this.items = items;
    }

    public NoFilterArrayAdapter(@NonNull Context context, int resource, @NonNull T[] items) {
        this(context, resource, Arrays.asList(items));
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Nullable
    @Override
    public T getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return noFilter;
    }
}
