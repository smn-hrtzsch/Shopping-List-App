package com.example.einkaufsliste;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class ListRecyclerViewAdapter extends RecyclerView.Adapter<ListRecyclerViewAdapter.ViewHolder>
        implements ItemTouchHelperAdapter {

    private List<ShoppingList> mData;
    private LayoutInflater mInflater;
    private ShoppingListRepository repository;
    private Context context;

    public ListRecyclerViewAdapter(Context context, List<ShoppingList> data, ShoppingListRepository repository) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.repository = repository;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_list_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingList list = mData.get(position);
        holder.myTextView.setText(list.getName());

        holder.deleteButton.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).showDeleteListDialog(list);
            }
        });

        // Neuer Listener für den Edit-Button:
        holder.editButton.setOnClickListener(v -> {
            showEditListDialog(list, holder.getAdapterPosition());
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, Einkaufsliste.class);
            intent.putExtra("list_name", list.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if(fromPosition < mData.size() && toPosition < mData.size()){
            Collections.swap(mData, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
            for (int i = 0; i < mData.size(); i++) {
                mData.get(i).setSortOrder(i);
            }
            repository.updateShoppingListsOrder(mData);
            return true;
        }
        return false;
    }

    private void showEditListDialog(ShoppingList list, int position) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_edit_list, null);
        EditText input = dialogView.findViewById(R.id.editTextDialog);
        input.setText(list.getName());

        // Erstelle den Dialog ohne den Builder, der eigene Buttons hinzufügt:
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        // Entferne den Aufruf von setTitle, da der Titel in deinem Layout enthalten ist.
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        // Finde die Buttons in deinem benutzerdefinierten Layout
        Button positive = dialogView.findViewById(R.id.positiveButton);
        Button negative = dialogView.findViewById(R.id.negativeButton);

        positive.setOnClickListener(v -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(list.getName())) {
                list.setName(newName);
                repository.updateShoppingListName(list.getId(), newName);
                notifyItemChanged(position);
            }
            dialog.dismiss();
        });

        negative.setOnClickListener(v -> dialog.cancel());

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
        ImageView deleteButton;
        ImageView editButton; // Neuer Edit-Button

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.listNameTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            editButton = itemView.findViewById(R.id.editButton);
        }
    }
}
