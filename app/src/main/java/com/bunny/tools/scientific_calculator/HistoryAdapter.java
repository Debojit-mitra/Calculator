package com.bunny.tools.scientific_calculator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<String> history;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String calculation);
    }

    public HistoryAdapter(List<String> history, OnItemClickListener listener) {
        this.history = history != null ? new ArrayList<>(history) : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(history.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return history.size();
    }

    public void updateHistory(List<String> newHistory) {
        if (newHistory == null) {
            newHistory = new ArrayList<>();
        }

        // Compute the difference between old and new lists
        int oldSize = this.history.size();
        int newSize = newHistory.size();

        this.history = new ArrayList<>(newHistory);

        if (newSize > oldSize) {
            notifyItemRangeInserted(oldSize, newSize - oldSize);
        } else if (newSize < oldSize) {
            notifyItemRangeRemoved(newSize, oldSize - newSize);
        }

        int minSize = Math.min(oldSize, newSize);
        notifyItemRangeChanged(0, minSize);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView calculationTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            calculationTextView = itemView.findViewById(R.id.calculationTextView);
        }

        void bind(final String calculation, final OnItemClickListener listener) {
            calculationTextView.setText(calculation);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(calculation);
                }
            });
        }
    }
    public void addItem(String item) {
        history.add(0, item);
        notifyItemInserted(0);
    }

    public void removeLastItem() {
        if (!history.isEmpty()) {
            int lastIndex = history.size() - 1;
            history.remove(lastIndex);
            notifyItemRemoved(lastIndex);
        }
    }

}