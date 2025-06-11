// MyRecyclerViewAdapter.java
package com.example.einkaufsliste;

import com.example.einkaufsliste.R;

import android.content.Context;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.util.Log;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemTouchHelperAdapter {

    private List<ShoppingItem> items;
    private final Context context;
    private final ShoppingListRepository repository;
    private final long currentListId;
    private final OnItemInteractionListener interactionListener;

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_ADD_ITEM_INLINE = 1;

    private int editingItemPosition = -1;
    private ShoppingItem recentlyDeletedItem;
    private int recentlyDeletedItemPosition = -1;

    public interface OnItemInteractionListener {
        void onItemCheckboxChanged(ShoppingItem item, boolean isChecked);
        void onDataSetChanged();
        void requestItemResort();
    }

    public MyRecyclerViewAdapter(Context context, List<ShoppingItem> items, ShoppingListRepository repository, long currentListId, OnItemInteractionListener listener) {
        this.context = context;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.repository = repository;
        this.currentListId = currentListId;
        this.interactionListener = listener;
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

    @Override
    public int getItemViewType(int position) {
        if (position == items.size()) {
            return VIEW_TYPE_ADD_ITEM_INLINE;
        }
        return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_ITEM) {
            View view = inflater.inflate(R.layout.recyclerview_row, parent, false);
            return new ItemViewHolder(view);
        } else { // VIEW_TYPE_ADD_ITEM_INLINE
            View view = inflater.inflate(R.layout.recyclerview_add_item_inline_row, parent, false);
            return new AddItemInlineViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_ITEM) {
            ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
            if (position < items.size()) {
                ShoppingItem currentItem = items.get(position);
                itemViewHolder.bind(currentItem, position == editingItemPosition);
            }
        } else if (holder.getItemViewType() == VIEW_TYPE_ADD_ITEM_INLINE) {
            AddItemInlineViewHolder addItemViewHolder = (AddItemInlineViewHolder) holder;
            addItemViewHolder.bind();
        }
    }

    @Override
    public int getItemCount() {
        return items.size() + 1;
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
        repository.updateItemPositions(new ArrayList<>(items));

        if (interactionListener != null) {
            interactionListener.requestItemResort();
        }
        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        if (position < 0 || position >= items.size()) {
            notifyItemChanged(position);
            return;
        }

        recentlyDeletedItem = items.get(position);
        recentlyDeletedItemPosition = position;

        items.remove(position);
        notifyItemRemoved(position);

        View rootView = ((EinkaufslisteActivity) context).findViewById(R.id.einkaufsliste_activity_root);
        if (rootView == null) {
            rootView = ((EinkaufslisteActivity) context).findViewById(android.R.id.content);
        }

        Snackbar snackbar = Snackbar.make(rootView,
                        "\"" + recentlyDeletedItem.getName() + "\" " + context.getString(R.string.deleted), Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, view -> {
                    if (recentlyDeletedItem != null && recentlyDeletedItemPosition != -1) {
                        items.add(recentlyDeletedItemPosition, recentlyDeletedItem);
                        notifyItemInserted(recentlyDeletedItemPosition);
                        recentlyDeletedItem = null;
                        recentlyDeletedItemPosition = -1;
                        if (interactionListener != null) {
                            interactionListener.requestItemResort();
                        }
                    }
                })
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        if (event != DISMISS_EVENT_ACTION) {
                            if (recentlyDeletedItem != null) {
                                repository.deleteItemFromList(recentlyDeletedItem.getId());
                                if (interactionListener != null) {
                                    interactionListener.requestItemResort();
                                }
                                recentlyDeletedItem = null;
                                recentlyDeletedItemPosition = -1;
                            }
                        }
                    }
                });
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
                TypedValue typedValue = new TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
                textViewName.setTextColor(typedValue.data);
                textViewName.setPaintFlags(textViewName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                buttonEdit.setEnabled(false);
                buttonEdit.setAlpha(0.5f);
            } else {
                textViewName.setPaintFlags(textViewName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                buttonEdit.setEnabled(true);
                buttonEdit.setAlpha(1.0f);
            }

            if (isInEditMode && !item.isDone()) {
                layoutDisplay.setVisibility(View.GONE);
                layoutEdit.setVisibility(View.VISIBLE);
                buttonEdit.setVisibility(View.GONE);
                buttonSave.setVisibility(View.VISIBLE);
                editTextName.setText(item.getName());
                editTextName.requestFocus();
                editTextName.setSelection(editTextName.getText().length());
            } else {
                layoutDisplay.setVisibility(View.VISIBLE);
                layoutEdit.setVisibility(View.GONE);
                buttonEdit.setVisibility(item.isDone() ? View.GONE : View.VISIBLE);
                buttonSave.setVisibility(View.GONE);
            }

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(item.isDone());
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (editingItemPosition != -1 && editingItemPosition != getAdapterPosition()) {
                    Toast.makeText(context, "Bitte zuerst die andere Bearbeitung abschließen.", Toast.LENGTH_SHORT).show();
                    checkBox.setChecked(!isChecked);
                    return;
                }
                if (editingItemPosition != -1 && editingItemPosition == getAdapterPosition()) {
                    Toast.makeText(context, "Bitte Bearbeitung abschließen, um Status zu ändern.", Toast.LENGTH_SHORT).show();
                    checkBox.setChecked(!isChecked);
                    return;
                }

                item.setDone(isChecked);
                repository.updateItemInList(item);
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
                int currentPos = getAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION || currentPos >= items.size()) return;

                if (editingItemPosition != -1 && editingItemPosition != currentPos) {
                    Toast.makeText(context, "Bitte zuerst die andere Bearbeitung abschließen.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int previousEditingPosition = editingItemPosition;
                editingItemPosition = currentPos;

                if (previousEditingPosition != -1 && previousEditingPosition < items.size() && previousEditingPosition != editingItemPosition) {
                    notifyItemChanged(previousEditingPosition);
                }
                notifyItemChanged(editingItemPosition);
            });

            buttonSave.setOnClickListener(v -> saveItemChanges(item, getAdapterPosition()));
            editTextName.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveItemChanges(item, getAdapterPosition());
                    return true;
                }
                return false;
            });

            buttonDelete.setOnClickListener(v -> {
                int currentPos = getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION && currentPos < items.size()) {
                    if (editingItemPosition == currentPos) {
                        Toast.makeText(context, "Bitte Bearbeitung abschließen vor dem Löschen.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    onItemDismiss(currentPos);
                }
            });
        }

        private void saveItemChanges(ShoppingItem item, int position) {
            if (position == RecyclerView.NO_POSITION || position >= items.size()) return;
            String newName = editTextName.getText().toString().trim();

            if (newName.isEmpty()) {
                Toast.makeText(context, R.string.invalid_item_name, Toast.LENGTH_SHORT).show();
                return;
            }
            item.setName(newName);
            repository.updateItemInList(item);
            editingItemPosition = -1;
            notifyItemChanged(position);
            if (interactionListener != null) {
                interactionListener.requestItemResort();
            }
        }
    }

    public class AddItemInlineViewHolder extends RecyclerView.ViewHolder {
        EditText editTextNewItemNameInline;
        ImageButton buttonAddNewItemInline;

        public AddItemInlineViewHolder(@NonNull View itemView) {
            super(itemView);
            editTextNewItemNameInline = itemView.findViewById(R.id.edit_text_new_item_name_inline_add);
            buttonAddNewItemInline = itemView.findViewById(R.id.button_add_new_item_inline_add);
        }

        public void bind() {
            buttonAddNewItemInline.setVisibility(View.GONE);

            editTextNewItemNameInline.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    buttonAddNewItemInline.setVisibility(s.toString().trim().isEmpty() ? View.GONE : View.VISIBLE);
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            View.OnClickListener addItemAction = v -> {
                String name = editTextNewItemNameInline.getText().toString().trim();
                if (!name.isEmpty()) {
                    int nextPosition = 0;
                    for (ShoppingItem existingItem : items) {
                        if (!existingItem.isDone()) {
                            nextPosition = Math.max(nextPosition, existingItem.getPosition() + 1);
                        }
                    }

                    ShoppingItem newItem = new ShoppingItem(name, "1", "", false, currentListId, "", nextPosition);
                    long newId = repository.addItemToShoppingList(currentListId, newItem);
                    if (newId != -1) {
                        newItem.setId(newId);
                        if (interactionListener != null) {
                            interactionListener.requestItemResort();
                        }
                        editTextNewItemNameInline.setText("");
                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(editTextNewItemNameInline.getWindowToken(), 0);
                        }
                    } else {
                        Toast.makeText(context, R.string.error_adding_item, Toast.LENGTH_SHORT).show();
                    }
                }
            };

            buttonAddNewItemInline.setOnClickListener(addItemAction);
            editTextNewItemNameInline.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    if (!editTextNewItemNameInline.getText().toString().trim().isEmpty()){
                        addItemAction.onClick(v);
                        return true;
                    }
                }
                return false;
            });
        }
    }
}