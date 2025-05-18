package com.example.einkaufsliste;

import android.app.AlertDialog;
import android.content.Context;
// import android.content.Intent;
import android.util.Log; // Import für Log hinzugefügt
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ListRecyclerViewAdapter extends RecyclerView.Adapter<ListRecyclerViewAdapter.ViewHolder> implements ItemTouchHelperAdapter {

    private List<ShoppingList> localDataSet;
    private ShoppingListManager shoppingListManager;
    private Context context;
    private OnListInteractionListener interactionListener;
    private int editingPosition = -1;


    public interface OnListInteractionListener {
        void onListClicked(ShoppingList list);
        void onListDataSetChanged();
        void requestListDeleteConfirmation(ShoppingList list);
    }


    public ListRecyclerViewAdapter(Context context, List<ShoppingList> dataSet, ShoppingListManager manager, OnListInteractionListener listener) {
        this.context = context;
        localDataSet = dataSet; // ACHTUNG: Direkte Zuweisung. Änderungen an dataSet außen beeinflussen den Adapter. Besser Kopie?
        shoppingListManager = manager;
        this.interactionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.recyclerview_list_row, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {
        // Verwende getAdapterPosition() innerhalb von Listenern, da 'position' veraltet sein kann
        int adapterPosition = viewHolder.getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= localDataSet.size()) {
            Log.w("AdapterBind", "Invalid position in onBindViewHolder: " + adapterPosition);
            return; // Position ist ungültig, nichts binden
        }
        ShoppingList list = localDataSet.get(adapterPosition);
        boolean isEditing = adapterPosition == editingPosition;

        viewHolder.textViewListName.setText(list.getName());

        String itemCountText = String.format(Locale.getDefault(), "%d %s",
                list.getItemCount(),
                list.getItemCount() == 1 ? context.getString(R.string.item_count_suffix_singular) : context.getString(R.string.item_count_suffix_plural));
        viewHolder.textViewListItemCount.setText(itemCountText);


        viewHolder.itemView.setOnClickListener(v -> {
            int currentPos = viewHolder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;
            if (editingPosition != -1 && editingPosition == currentPos){
                Toast.makeText(context, "Bitte Bearbeitung abschließen.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (interactionListener != null) {
                interactionListener.onListClicked(localDataSet.get(currentPos));
            }
        });

        viewHolder.textViewListName.setVisibility(isEditing ? View.GONE : View.VISIBLE);
        viewHolder.editTextListName.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        viewHolder.buttonEditListName.setVisibility(isEditing ? View.GONE : View.VISIBLE);
        viewHolder.buttonSaveListName.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        viewHolder.textViewListItemCount.setVisibility(isEditing ? View.GONE : View.VISIBLE);


        if (isEditing) {
            viewHolder.editTextListName.setText(list.getName());
            viewHolder.editTextListName.requestFocus();
            viewHolder.editTextListName.setSelection(viewHolder.editTextListName.getText().length());
        }


        viewHolder.buttonEditListName.setOnClickListener(v -> {
            int currentPos = viewHolder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                int previousEditingPosition = editingPosition;
                editingPosition = currentPos;
                // Benachrichtige nur die geänderten Items
                if (previousEditingPosition != -1) {
                    notifyItemChanged(previousEditingPosition);
                }
                notifyItemChanged(editingPosition);
            }
        });

        viewHolder.buttonSaveListName.setOnClickListener(v -> saveListName(viewHolder)); // Übergabe von 'list' nicht mehr nötig
        viewHolder.editTextListName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveListName(viewHolder);
                return true;
            }
            return false;
        });


        viewHolder.buttonDeleteList.setOnClickListener(v -> {
            int currentPos = viewHolder.getAdapterPosition(); // Immer getAdapterPosition() in Listenern verwenden
            if (currentPos == RecyclerView.NO_POSITION) return; // Position ungültig

            // Verhindern, dass gelöscht wird, während gerade editiert wird
            if (editingPosition != -1 && editingPosition == currentPos){
                Toast.makeText(context, "Bitte Bearbeitung abschließen.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Die Liste zur aktuellen Position holen
            ShoppingList listToDelete = localDataSet.get(currentPos);

            // Die MainActivity über den Listener benachrichtigen, dass der Custom Dialog gezeigt werden soll
            if (interactionListener != null) {
                interactionListener.requestListDeleteConfirmation(listToDelete);
            }
        });
    }

    private void saveListName(ViewHolder viewHolder) {
        int currentPosition = viewHolder.getAdapterPosition();
        if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= localDataSet.size()) return;

        ShoppingList list = localDataSet.get(currentPosition);
        String newName = viewHolder.editTextListName.getText().toString().trim();

        if (!newName.isEmpty() && !newName.equals(list.getName())) {
            shoppingListManager.updateShoppingListName(list.getId(), newName);
            list.setName(newName); // Update local data set directy
            Toast.makeText(context, "Liste umbenannt in \"" + newName + "\"", Toast.LENGTH_SHORT).show();
            // Keine Notwendigkeit für onListDataSetChanged, da nur ein Item betroffen ist
        }
        editingPosition = -1;
        notifyItemChanged(currentPosition); // Nur dieses Item neu zeichnen
    }


    private void showDeleteListDialog(ShoppingList list, int position) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.confirm_delete_list_title)
                .setMessage(String.format(context.getString(R.string.confirm_delete_list_message), list.getName()))
                .setPositiveButton(R.string.button_delete, (dialog, which) -> {
                    shoppingListManager.deleteShoppingList(list.getId());
                    // Entferne das Element sicher aus der Liste und benachrichtige den Adapter
                    if (position != RecyclerView.NO_POSITION && position < localDataSet.size() && localDataSet.get(position).getId() == list.getId()) {
                        localDataSet.remove(position);
                        notifyItemRemoved(position);
                        // Optional: notifyItemRangeChanged, aber oft reicht das Entfernen für die Animation
                    } else {
                        // Fallback: Finde die Liste nach ID, falls die Position nicht mehr stimmt
                        int foundPos = -1;
                        for (int i = 0; i < localDataSet.size(); i++) {
                            if (localDataSet.get(i).getId() == list.getId()) {
                                foundPos = i;
                                break;
                            }
                        }
                        if (foundPos != -1) {
                            localDataSet.remove(foundPos);
                            notifyItemRemoved(foundPos);
                        } else {
                            // Wenn nicht gefunden, einfach neu laden als sichersten Fallback
                            if (interactionListener != null) {
                                interactionListener.onListDataSetChanged(); // MainActivity lädt neu
                            }
                        }
                    }

                    Toast.makeText(context, "Liste \"" + list.getName() + "\" gelöscht", Toast.LENGTH_SHORT).show();
                    // Benachrichtige die Activity auch nach dem lokalen Entfernen, um z.B. die Empty View zu aktualisieren
                    if (interactionListener != null) {
                        interactionListener.onListDataSetChanged();
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }


    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public void updateLists(List<ShoppingList> newLists) {
        editingPosition = -1;
        localDataSet.clear();
        localDataSet.addAll(newLists);
        notifyDataSetChanged();
        // Kein Listener-Aufruf hier, MainActivity.loadShoppingLists ruft checkEmptyView auf.
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        editingPosition = -1;
        if (fromPosition < 0 || fromPosition >= localDataSet.size() || toPosition < 0 || toPosition >= localDataSet.size()){
            return false; // Ungültige Positionen
        }
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(localDataSet, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(localDataSet, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        // TODO: Persist new order in database for lists (if needed)
        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        // Swipe not implemented for lists
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewListName;
        final EditText editTextListName;
        final ImageButton buttonEditListName;
        final ImageButton buttonSaveListName;
        final ImageButton buttonDeleteList;
        final TextView textViewListItemCount;


        public ViewHolder(View view) {
            super(view);
            textViewListName = view.findViewById(R.id.list_name_text_view);
            editTextListName = view.findViewById(R.id.edit_text_list_name_inline);
            buttonEditListName = view.findViewById(R.id.button_edit_list_name_inline);
            buttonSaveListName = view.findViewById(R.id.button_save_list_name_inline);
            buttonDeleteList = view.findViewById(R.id.button_delete_list_inline);
            textViewListItemCount = view.findViewById(R.id.list_item_count_text_view);
        }
        // Getter sind nicht unbedingt nötig, wenn die Views final sind
    }
} // <-- Diese schließende Klammer hat gefehlt