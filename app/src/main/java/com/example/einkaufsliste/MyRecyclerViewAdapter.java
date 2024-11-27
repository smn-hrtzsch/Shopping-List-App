package com.example.einkaufsliste;

import android.content.Context;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {

    private List<ShoppingItem> mData;
    private List<ShoppingItem> recentlyDeletedItems; // Temporäre Liste für gelöschte Artikel
    private LayoutInflater mInflater;
    private ShoppingListRepository repository;
    private Context context;
    private RecyclerView recyclerView;

    public MyRecyclerViewAdapter(Context context, List<ShoppingItem> data, ShoppingListRepository repository, RecyclerView recyclerView) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.repository = repository;
        this.context = context;
        this.recyclerView = recyclerView;
        this.recentlyDeletedItems = new ArrayList<>();
        saveOriginalPositions();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingItem item = mData.get(position);
        holder.myTextView.setText(item.getName());
        holder.myCheckBox.setOnCheckedChangeListener(null);
        holder.myCheckBox.setChecked(item.isCompleted());
        updateTextViewAppearance(holder.myTextView, item.isCompleted());

        holder.myCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (item.isCompleted() != isChecked) {
                item.setCompleted(isChecked);
                repository.updateShoppingItemStatus(item.getId(), isChecked);
                updateTextViewAppearance(holder.myTextView, isChecked);
                new Handler(Looper.getMainLooper()).post(() -> {
                    sortItems();
                    notifyDataSetChanged();
                });
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            ShoppingItem itemToRemove = mData.get(pos);
            recentlyDeletedItems.add(itemToRemove); // Artikel in die temporäre Liste hinzufügen
            repository.deleteShoppingItem(itemToRemove.getId());
            mData.remove(pos);
            notifyItemRemoved(pos);
            showUndoSnackbar(itemToRemove, pos); // Snackbar anzeigen
        });
    }

    private void updateTextViewAppearance(TextView textView, boolean isCompleted) {
        if (isCompleted) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }

    private void saveOriginalPositions() {
        for (int i = 0; i < mData.size(); i++) {
            mData.get(i).setOriginalPosition(i);
        }
    }

    private void sortItems() {
        Collections.sort(mData, new Comparator<ShoppingItem>() {
            @Override
            public int compare(ShoppingItem o1, ShoppingItem o2) {
                if (o1.isCompleted() && !o2.isCompleted()) {
                    return 1;
                } else if (!o1.isCompleted() && o2.isCompleted()) {
                    return -1;
                } else if (!o1.isCompleted() && !o2.isCompleted()) {
                    return Integer.compare(o1.getOriginalPosition(), o2.getOriginalPosition());
                } else {
                    return 0;
                }
            }
        });
    }

    private void showUndoSnackbar(ShoppingItem item, int position) {
        Snackbar snackbar = Snackbar.make(recyclerView, item.getName() + " gelöscht", Snackbar.LENGTH_LONG);
        snackbar.setAction("Rückgängig", v -> undoDelete(item, position));
        snackbar.show();
    }

    private void undoDelete(ShoppingItem item, int position) {
        mData.add(position, item);
        recentlyDeletedItems.remove(item);
        repository.addShoppingItem(item.getId(), item.getName()); // Item zurück zur Datenbank hinzufügen
        notifyItemInserted(position);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView myTextView;
        CheckBox myCheckBox;
        ImageView deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.itemName);
            myCheckBox = itemView.findViewById(R.id.itemCheckbox);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
