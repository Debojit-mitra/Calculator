package com.bunny.tools.scientific_calculator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HISTORY_ITEM = 0;
    private static final int TYPE_CLEAR_BUTTON = 1;

    private List<String> history;
    private final OnItemClickListener listener;
    private final OnClearClickListener clearListener;

    public interface OnItemClickListener {
        void onItemClick(String calculation, List<String> fullHistory, int position);
    }

    public interface OnClearClickListener {
        void onClearClick();
    }

    public HistoryAdapter(List<String> history, OnItemClickListener listener, OnClearClickListener clearListener) {
        this.history = history != null ? new ArrayList<>(history) : new ArrayList<>();
        this.listener = listener;
        this.clearListener = clearListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_CLEAR_BUTTON) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.clear_history_button, parent, false);
            return new ClearButtonViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item, parent, false);
            return new HistoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HistoryViewHolder) {
            ((HistoryViewHolder) holder).bind(history.get(position), position, listener, new ArrayList<>(history));
        } else if (holder instanceof ClearButtonViewHolder) {
            ((ClearButtonViewHolder) holder).bind(clearListener);
        }
    }

    @Override
    public int getItemCount() {
        return history.size() + 1; // +1 for the clear button
    }

    @Override
    public int getItemViewType(int position) {
        return position == history.size() ? TYPE_CLEAR_BUTTON : TYPE_HISTORY_ITEM;
    }

    public void updateHistory(List<String> newHistory) {
        if (newHistory == null) {
            newHistory = new ArrayList<>();
        }

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
        notifyItemChanged(this.history.size()); // Notify change for the clear button position
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView calculationTextView;

        public HistoryViewHolder(View itemView) {
            super(itemView);
            calculationTextView = itemView.findViewById(R.id.calculationTextView);
        }

        void bind(final String calculation, final int position, final OnItemClickListener listener, final List<String> fullHistory) {
            calculationTextView.setText(calculation);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(calculation, fullHistory, position);
                }
            });
        }
    }

    public static class ClearButtonViewHolder extends RecyclerView.ViewHolder {
        Button clearButton;

        public ClearButtonViewHolder(View itemView) {
            super(itemView);
            clearButton = itemView.findViewById(R.id.clearHistoryButton);
        }

        void bind(final OnClearClickListener listener) {
            clearButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClearClick();
                }
            });
        }
    }

    public void addItem(String item) {
        history.add(0, item);
        notifyItemInserted(0);
        notifyItemChanged(history.size()); // Notify change for the clear button position
    }

    public void removeLastItem() {
        if (!history.isEmpty()) {
            int lastIndex = history.size() - 1;
            history.remove(lastIndex);
            notifyItemRemoved(lastIndex);
            notifyItemChanged(history.size()); // Notify change for the clear button position
        }
    }

    public void clearHistory() {
        int size = history.size();
        history.clear();
        notifyItemRangeRemoved(0, size);
        notifyItemChanged(0); // Notify change for the clear button position
    }
}