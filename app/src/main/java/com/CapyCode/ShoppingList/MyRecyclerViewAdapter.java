package com.CapyCode.ShoppingList;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ItemViewHolder> implements ItemTouchHelperAdapter {

    private final List<ShoppingItem> items;
    private final Context context;
    private final ShoppingListRepository repository;
    private final OnItemInteractionListener interactionListener;
    private final String firebaseListId;

    private int editingItemPosition = -1;

    public interface OnItemInteractionListener {
        void onItemCheckboxChanged(ShoppingItem item, boolean isChecked);
        void onDataSetChanged();
        void requestItemResort();
        View getCoordinatorLayout();
        View getSnackbarAnchorView();
    }

    public MyRecyclerViewAdapter(Context context, List<ShoppingItem> items, ShoppingListRepository repository, OnItemInteractionListener listener, String firebaseListId) {
        this.context = context;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.repository = repository;
        this.interactionListener = listener;
        this.firebaseListId = firebaseListId;
        sortItemsLocal();
    }

    public void resetEditingPosition() {
        if (editingItemPosition != -1 && editingItemPosition < items.size()) {
            int oldEditingPosition = editingItemPosition;
            editingItemPosition = -1;
            notifyItemChanged(oldEditingPosition);
        } else {
            editingItemPosition = -1;
        }
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_row, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ShoppingItem currentItem = items.get(position);
        holder.bind(currentItem, position == editingItemPosition);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<ShoppingItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        sortItemsLocal();
        notifyDataSetChanged();
        if (interactionListener != null) {
            interactionListener.onDataSetChanged();
        }
    }

    private void sortItemsLocal() {
        Collections.sort(items, (item1, item2) -> {
            if (item1.isDone() == item2.isDone()) {
                return Integer.compare(item1.getPosition(), item2.getPosition());
            }
            return item1.isDone() ? 1 : -1;
        });
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition >= items.size() || toPosition >= items.size() || fromPosition < 0 || toPosition < 0) {
            return false;
        }
        ShoppingItem movedItem = items.remove(fromPosition);
        items.add(toPosition, movedItem);
        notifyItemMoved(fromPosition, toPosition);

        for (int i = 0; i < items.size(); i++) {
            items.get(i).setPosition(i);
        }
        repository.updateItemPositions(items);
        return true;
    }

    public void addItem(ShoppingItem item) {
        int insertPosition = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isDone()) {
                break;
            }
            insertPosition = i + 1;
        }
        items.add(insertPosition, item);
        notifyItemInserted(insertPosition);
        if (interactionListener != null) {
            interactionListener.onDataSetChanged();
        }
    }

    @Override
    public void onItemDismiss(int position) {
        if (position < 0 || position >= items.size()) {
            notifyItemChanged(position);
            return;
        }

        final ShoppingItem itemToDelete = items.get(position);
        final int deletedPosition = position;

        if (firebaseListId != null) {
            repository.deleteItemFromList(firebaseListId, itemToDelete.getFirebaseId());
            repository.updateListTimestamp(firebaseListId, null);
        } else {
            repository.deleteItemFromList(itemToDelete.getId());
        }

        items.remove(position);
        notifyItemRemoved(position);

        View rootView = interactionListener.getCoordinatorLayout();
        if (rootView == null) {
            rootView = ((EinkaufslisteActivity) context).findViewById(android.R.id.content);
        }

        Snackbar snackbar = Snackbar.make(rootView,
                        "\"" + itemToDelete.getName() + "\" " + context.getString(R.string.deleted), Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, view -> {
                    if (firebaseListId != null) {
                        repository.addItemToShoppingList(firebaseListId, itemToDelete, null);
                        repository.updateListTimestamp(firebaseListId, null);
                    } else {
                        long newId = repository.addItemToShoppingList(itemToDelete.getListId(), itemToDelete);
                        itemToDelete.setId(newId);
                    }

                    items.add(deletedPosition, itemToDelete);
                    notifyItemInserted(deletedPosition);

                    if (interactionListener != null) {
                        interactionListener.requestItemResort();
                    }
                });

        View anchor = interactionListener.getSnackbarAnchorView();
        if (anchor != null) {
            snackbar.setAnchorView(anchor);
        }

        snackbar.show();
    }

    public List<ShoppingItem> getCurrentItems() {
        return new ArrayList<>(this.items);
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView textViewName;
        ImageButton buttonEdit, buttonDelete, buttonSave;
        LinearLayout layoutDisplay, layoutEdit;
        EditText editTextName;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.item_checkbox);
            textViewName = itemView.findViewById(R.id.item_name_text_view);
            buttonEdit = itemView.findViewById(R.id.button_edit_item_inline);
            buttonDelete = itemView.findViewById(R.id.button_delete_item_inline);
            buttonSave = itemView.findViewById(R.id.button_save_item_inline);
            layoutDisplay = itemView.findViewById(R.id.item_details_layout_display);
            layoutEdit = itemView.findViewById(R.id.item_details_layout_edit);
            editTextName = itemView.findViewById(R.id.edit_text_item_name_inline);
        }

        public void bind(final ShoppingItem item, boolean isInEditMode) {
            textViewName.setText(item.getName());

            if (item.isDone()) {
                int color = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, "Error");
                textViewName.setTextColor(color);
                buttonEdit.setEnabled(false);
                buttonEdit.setAlpha(0.5f);
            } else {
                int color = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, "Error");
                textViewName.setTextColor(color);
                buttonEdit.setEnabled(true);
                buttonEdit.setAlpha(1.0f);
            }

            textViewName.setPaintFlags(textViewName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

            if (isInEditMode && !item.isDone()) {
                layoutDisplay.setVisibility(View.GONE);
                layoutEdit.setVisibility(View.VISIBLE);
                buttonEdit.setVisibility(View.GONE);
                buttonSave.setVisibility(View.VISIBLE);
                editTextName.setText(item.getName());
                editTextName.requestFocus();
                editTextName.setSelection(editTextName.getText().length());
                editTextName.post(() -> {
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editTextName, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                });
            } else {
                layoutDisplay.setVisibility(View.VISIBLE);
                layoutEdit.setVisibility(View.GONE);
                buttonEdit.setVisibility(item.isDone() ? View.GONE : View.VISIBLE);
                buttonSave.setVisibility(View.GONE);
            }

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(item.isDone());
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (editingItemPosition != -1 && editingItemPosition != getBindingAdapterPosition()) {
                    Toast.makeText(context, "Bitte zuerst die andere Bearbeitung abschließen.", Toast.LENGTH_SHORT).show();
                    checkBox.setChecked(!isChecked);
                    return;
                }
                if (editingItemPosition != -1 && editingItemPosition == getBindingAdapterPosition()) {
                    Toast.makeText(context, "Bitte Bearbeitung abschließen, um Status zu ändern.", Toast.LENGTH_SHORT).show();
                    checkBox.setChecked(!isChecked);
                    return;
                }

                item.setDone(isChecked);
                if (firebaseListId != null) {
                    repository.toggleItemChecked(firebaseListId, item.getFirebaseId(), isChecked, null);
                } else {
                    repository.updateItemInList(item, firebaseListId, null);
                }
                if (interactionListener != null) {
                    interactionListener.onItemCheckboxChanged(item, isChecked);
                    interactionListener.requestItemResort();
                }
            });

            buttonEdit.setOnClickListener(v -> {
                if (item.isDone()) {
                    Toast.makeText(context, "Erledigte Artikel können nicht bearbeitet werden.", Toast.LENGTH_SHORT).show();
                    return;
                }
                int currentPos = getBindingAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION) return;

                if (editingItemPosition != -1 && editingItemPosition != currentPos) {
                    Toast.makeText(context, "Bitte zuerst die andere Bearbeitung abschließen.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int previousEditingPosition = editingItemPosition;
                editingItemPosition = currentPos;

                if (previousEditingPosition != -1) {
                    notifyItemChanged(previousEditingPosition);
                }
                notifyItemChanged(editingItemPosition);
            });

            buttonSave.setOnClickListener(v -> saveItemChanges(getBindingAdapterPosition()));
            editTextName.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveItemChanges(getBindingAdapterPosition());
                    return true;
                }
                return false;
            });

            buttonDelete.setOnClickListener(v -> {
                int currentPos = getBindingAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    if (editingItemPosition == currentPos) {
                        Toast.makeText(context, "Bitte Bearbeitung abschließen vor dem Löschen.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    onItemDismiss(currentPos);
                }
            });
        }

        private void saveItemChanges(int position) {
            if (position == RecyclerView.NO_POSITION || position >= items.size()) return;

            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(editTextName.getWindowToken(), 0);

            ShoppingItem item = items.get(position);
            String newName = editTextName.getText().toString().trim();

            if (newName.isEmpty()) {
                Toast.makeText(context, R.string.invalid_item_name, Toast.LENGTH_SHORT).show();
                return;
            }
            item.setName(newName);
            repository.updateItemInList(item, firebaseListId, null);
            if (firebaseListId != null) {
                repository.updateListTimestamp(firebaseListId, null);
            }
            editingItemPosition = -1;
            notifyItemChanged(position);
            if (interactionListener != null) {
                interactionListener.requestItemResort();
            }
        }
    }
}
