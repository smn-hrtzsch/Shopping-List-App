package com.example.einkaufsliste;

import android.content.Context;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder>
        implements ItemTouchHelperAdapter {

    private List<ShoppingItem> mData;
    private List<ShoppingItem> recentlyDeletedItems;
    private LayoutInflater mInflater;
    private ShoppingListRepository repository;
    private Context context;
    private RecyclerView recyclerView;
    private long listId;

    public MyRecyclerViewAdapter(Context context, List<ShoppingItem> data, ShoppingListRepository repository, RecyclerView recyclerView, long listId) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.repository = repository;
        this.context = context;
        this.recyclerView = recyclerView;
        this.recentlyDeletedItems = new ArrayList<>();
        this.listId = listId;
    }

    @NonNull
    @Override
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
                resortItemsByCompletion();
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            ShoppingItem itemToRemove = mData.get(pos);
            recentlyDeletedItems.add(itemToRemove);
            repository.deleteShoppingItem(itemToRemove.getId());
            mData.remove(pos);
            notifyItemRemoved(pos);
            showUndoSnackbar(itemToRemove, pos);
        });

        // Neuer Listener für den Edit-Button:
        holder.editButton.setOnClickListener(v -> {
            showEditItemDialog(item, holder.getAdapterPosition());
        });
    }

    private void updateTextViewAppearance(TextView textView, boolean isCompleted) {
        if (isCompleted) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }

    private void showUndoSnackbar(ShoppingItem item, int position) {
        Snackbar snackbar = Snackbar.make(recyclerView, "'" + item.getName() + "' " + context.getString(R.string.deleted), Snackbar.LENGTH_LONG);
        snackbar.setAction(context.getString(R.string.undo), v -> undoDelete(item, position));
        snackbar.show();
    }

    private void undoDelete(ShoppingItem item, int position) {
        mData.add(position, item);
        recentlyDeletedItems.remove(item);
        repository.addShoppingItem(listId, item.getName());
        notifyItemInserted(position);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    // Drag & Drop innerhalb der gleichen Gruppe (completed/uncompleted) zulassen
    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (mData.get(fromPosition).isCompleted() != mData.get(toPosition).isCompleted()) {
            return false;
        }
        if (fromPosition < mData.size() && toPosition < mData.size()) {
            Collections.swap(mData, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
            for (int i = 0; i < mData.size(); i++) {
                mData.get(i).setSortOrder(i);
            }
            repository.updateShoppingItemsOrder(listId, mData);
            return true;
        }
        return false;
    }

    /**
     * Gruppiert die Items so, dass alle uncompleted Items oben und alle completed Items unten stehen.
     */
    public void resortItemsByCompletion() {
        List<ShoppingItem> unchecked = new ArrayList<>();
        List<ShoppingItem> checked = new ArrayList<>();
        for (ShoppingItem item : mData) {
            if (item.isCompleted()) {
                checked.add(item);
            } else {
                unchecked.add(item);
            }
        }
        unchecked.addAll(checked);
        mData.clear();
        mData.addAll(unchecked);
        for (int i = 0; i < mData.size(); i++) {
            mData.get(i).setSortOrder(i);
        }
        repository.updateShoppingItemsOrder(listId, mData);
        notifyDataSetChanged();
    }

    private void showEditItemDialog(ShoppingItem item, int position) {
        // Lade das benutzerdefinierte Layout für den Editierdialog
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_edit_item, null);

        // Hole den EditText aus dem Layout und setze den aktuellen Namen
        EditText input = dialogView.findViewById(R.id.editTextDialog);
        input.setText(item.getName());

        // Erzeuge einen Dialog, der ausschließlich dein Layout verwendet
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setView(dialogView);
        // Verwende NICHT builder.setTitle(), builder.setPositiveButton(), etc., damit keine Standarddialogelemente hinzugefügt werden.
        android.app.AlertDialog dialog = builder.create();

        // Hole die Buttons aus deinem Layout
        Button positive = dialogView.findViewById(R.id.positiveButton);
        Button negative = dialogView.findViewById(R.id.negativeButton);

        // Setze den Listener für den OK-Button
        positive.setOnClickListener(v -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(item.getName())) {
                item.setName(newName);
                repository.updateShoppingItemName(item.getId(), newName);
                notifyItemChanged(position);
            }
            dialog.dismiss();
        });

        // Setze den Listener für den Abbrechen-Button
        negative.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // Berechne die Breite: Bildschirmbreite minus 2 * 10dp (für links und rechts)
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int margin = dpToPx(10);  // 10dp in Pixel umrechnen
        int dialogWidth = screenWidth - (2 * margin);

        // Setze die Fensterbreite des Dialogs
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView myTextView;
        CheckBox myCheckBox;
        ImageView deleteButton;
        ImageView editButton; // Neuer Edit-Button

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.itemName);
            myCheckBox = itemView.findViewById(R.id.itemCheckbox);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            editButton = itemView.findViewById(R.id.editButton);
        }
    }
}
