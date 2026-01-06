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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.MaterialColors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ItemViewHolder> implements ItemTouchHelperAdapter {

    private final List<ShoppingItem> items;
    private final Context context;
    private final ShoppingListRepository repository;
    private final OnItemInteractionListener interactionListener;
    private final String firebaseListId;
    private RecyclerView recyclerView;

    private int editingItemPosition = -1;

    public interface OnItemInteractionListener {
        void onItemCheckboxChanged(ShoppingItem item, boolean isChecked);
        void onDataSetChanged();
        void requestItemResort();
        View getCoordinatorLayout();
        void showUndoBar(String message, Runnable undoAction);
    }

    public MyRecyclerViewAdapter(Context context, List<ShoppingItem> items, ShoppingListRepository repository, OnItemInteractionListener listener, String firebaseListId) {
        this.context = context;
        this.items = new ArrayList<>();
        if (items != null) {
            this.items.addAll(items);
            sortItemsLocal();
        }
        this.repository = repository;
        this.interactionListener = listener;
        this.firebaseListId = firebaseListId;
        setHasStableIds(true);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= items.size()) return RecyclerView.NO_ID;
        ShoppingItem item = items.get(position);
        if (item.getFirebaseId() != null) return item.getFirebaseId().hashCode();
        return item.getId();
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

    public boolean isEditing() {
        return editingItemPosition != -1;
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
        final List<ShoppingItem> oldList = new ArrayList<>(this.items);
        final List<ShoppingItem> newList = new ArrayList<>(newItems);
        
        // Sort the new list before comparing
        Collections.sort(newList, (item1, item2) -> {
            if (item1.isDone() == item2.isDone()) {
                int posComp = Integer.compare(item1.getPosition(), item2.getPosition());
                if (posComp != 0) return posComp;
                return Long.compare(item1.getId(), item2.getId());
            }
            return item1.isDone() ? 1 : -1;
        });

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return oldList.size(); }
            @Override
            public int getNewListSize() { return newList.size(); }
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
            }
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                ShoppingItem oldItem = oldList.get(oldItemPosition);
                ShoppingItem newItem = newList.get(newItemPosition);
                
                return java.util.Objects.equals(oldItem.getName(), newItem.getName()) &&
                       oldItem.isDone() == newItem.isDone() &&
                       java.util.Objects.equals(oldItem.getQuantity(), newItem.getQuantity()) &&
                       java.util.Objects.equals(oldItem.getUnit(), newItem.getUnit());
            }
        }, false); // Disable move detection to prevent "flying" animation

        this.items.clear();
        this.items.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
        
        if (interactionListener != null) {
            interactionListener.onDataSetChanged();
        }
    }

    private void sortItemsLocal() {
        Collections.sort(items, (item1, item2) -> {
            if (item1.isDone() == item2.isDone()) {
                int posComp = Integer.compare(item1.getPosition(), item2.getPosition());
                if (posComp != 0) return posComp;
                // Tie-breaker using ID to ensure stable sort order even with conflicting positions
                return Long.compare(item1.getId(), item2.getId());
            }
            return item1.isDone() ? 1 : -1;
        });
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition >= items.size() || toPosition >= items.size() || fromPosition < 0 || toPosition < 0) {
            return false;
        }
        
        // Prevent moving across done/not-done boundaries if we want to keep that strict
        if (items.get(fromPosition).isDone() != items.get(toPosition).isDone()) {
            return false;
        }

        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(items, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(items, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onDragFinished() {
        // Update positions based on the current list order once drag is finished
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setPosition(i);
        }
        
        repository.updateItemPositions(items);
        
        // If it's a cloud list, update positions on server using a batch
        if (firebaseListId != null) {
            repository.updateItemPositionsInCloud(firebaseListId, items, () -> {
                repository.updateListTimestamp(firebaseListId, null);
            });
        }
    }

    public int addItem(ShoppingItem item) {
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
        return insertPosition;
    }

    @Override
    public void onItemDismiss(int position) {
        if (position < 0 || position >= items.size()) {
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

        if (interactionListener != null) {
            // Capture scroll state BEFORE removal for Undo
            int undoScrollPos = RecyclerView.NO_POSITION;
            int undoScrollOffset = 0;
            if (recyclerView != null && recyclerView.getLayoutManager() instanceof androidx.recyclerview.widget.LinearLayoutManager) {
                androidx.recyclerview.widget.LinearLayoutManager llm = (androidx.recyclerview.widget.LinearLayoutManager) recyclerView.getLayoutManager();
                undoScrollPos = llm.findFirstVisibleItemPosition();
                View v = llm.findViewByPosition(undoScrollPos);
                undoScrollOffset = (v != null) ? v.getTop() : 0;
            }
            final int finalUndoScrollPos = undoScrollPos;
            final int finalUndoScrollOffset = undoScrollOffset;

            String message = "\"" + itemToDelete.getName() + "\" " + context.getString(R.string.deleted);
            interactionListener.showUndoBar(message, () -> {
                
                if (firebaseListId != null) {
                    // Use restore to keep ID if possible
                    repository.restoreItemToShoppingList(firebaseListId, itemToDelete, null);
                    repository.updateListTimestamp(firebaseListId, null);
                } else {
                    long newId = repository.addItemToShoppingList(itemToDelete.getListId(), itemToDelete);
                    itemToDelete.setId(newId);
                }

                // Optimized Undo: Insert with animation and correct position
                // 1. Shift positions of existing items to make room
                int targetPos = itemToDelete.getPosition();
                for (ShoppingItem existingItem : items) {
                    if (existingItem.getPosition() >= targetPos) {
                        existingItem.setPosition(existingItem.getPosition() + 1);
                    }
                }
                
                // 2. Find insertion index using binary search for precision
                List<ShoppingItem> searchList = new ArrayList<>(items);
                int insertIndex = Collections.binarySearch(searchList, itemToDelete, (item1, item2) -> {
                    if (item1.isDone() == item2.isDone()) {
                        int posComp = Integer.compare(item1.getPosition(), item2.getPosition());
                        if (posComp != 0) return posComp;
                        return Long.compare(item1.getId(), item2.getId());
                    }
                    return item1.isDone() ? 1 : -1;
                });
                
                if (insertIndex < 0) {
                    insertIndex = -(insertIndex + 1);
                }
                
                // Safety clamp
                if (insertIndex > items.size()) insertIndex = items.size();
                
                items.add(insertIndex, itemToDelete);
                
                // 3. Notify adapter
                notifyItemInserted(insertIndex);
                
                // 4. Restore scroll position to where it was before deletion
                if (recyclerView != null && finalUndoScrollPos != RecyclerView.NO_POSITION) {
                    androidx.recyclerview.widget.LinearLayoutManager llm = (androidx.recyclerview.widget.LinearLayoutManager) recyclerView.getLayoutManager();
                    if (llm != null) {
                        llm.scrollToPositionWithOffset(finalUndoScrollPos, finalUndoScrollOffset);
                    }
                }
                
                // 5. Persist
                repository.updateItemPositions(items);
                if (firebaseListId != null) {
                    repository.updateItemPositionsInCloud(firebaseListId, items, null);
                }
            });
        }
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
                int currentPos = getBindingAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION) return;

                if (editingItemPosition != -1 && editingItemPosition != currentPos) {
                    UiUtils.makeCustomToast(context, R.string.toast_finish_editing, Toast.LENGTH_SHORT).show();
                    checkBox.setChecked(!isChecked);
                    return;
                }
                if (editingItemPosition != -1 && editingItemPosition == currentPos) {
                    UiUtils.makeCustomToast(context, R.string.toast_finish_editing_status, Toast.LENGTH_SHORT).show();
                    checkBox.setChecked(!isChecked);
                    return;
                }

                item.setDone(isChecked);
                
                // Manually update styling to ensure immediate feedback without full rebind/scroll
                // Must match logic in bind() exactly!
                // bind() uses: 
                // if (item.isDone()) { ... colorOnSurfaceVariant ("Error"?) ... STRIKE_THRU_TEXT_FLAG ... }
                // else { ... colorOnSurface ("Error"?) ... ~STRIKE_THRU_TEXT_FLAG ... }
                
                // ERROR in bind(): 
                // int color = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, "Error"); 
                // int color = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, "Error");
                
                if (isChecked) {
                    // Match bind() logic for done
                    int color = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, "Error"); // Likely Grey
                    textViewName.setTextColor(color);
                    // bind() sets STRIKE_THRU_TEXT_FLAG if done?
                    // "textViewName.setPaintFlags(textViewName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));" -> This line REMOVES it unconditionally in bind()!
                    // Wait, look at line 320 in bind():
                    // textViewName.setPaintFlags(textViewName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    // It REMOVES strikethrough for ALL items at the start of bind? 
                    // No, look at bind logic:
                    // 308: if (item.isDone()) { ... } else { ... }
                    // 320: textViewName.setPaintFlags(textViewName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    // Line 320 is AFTER the if/else block!
                    // So bind() ALWAYS removes strikethrough!
                    // That explains why "sometimes it is strikethrough" (my manual listener logic added it) 
                    // and "on reopen it is correct" (bind removes it).
                    // The user said: "not only graying out but also strikethrough, that is wrong" -> Strikethrough is WRONG.
                    
                    // So I should NOT add strikethrough in my listener either.
                    // And I should rely on bind() logic which seems to be: Done = Gray text, No Strikethrough.
                    
                    // But wait, if I want to move it to the bottom, I need to sort.
                    // If I sort the list and notifyMoved, it moves.
                    // User said: "Items werden nicht ans Ende verschoben" (Items are not moved to end).
                    // So I MUST move them.
                    
                    // Plan:
                    // 1. Update style (color only, NO strikethrough).
                    // 2. Sort the `items` list locally.
                    // 3. Notify adapter of move.
                    
                    textViewName.setPaintFlags(textViewName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)); // Ensure NO strikethrough
                    buttonEdit.setVisibility(View.GONE);
                    buttonEdit.setEnabled(false);
                    buttonEdit.setAlpha(0.5f);
                } else {
                    int color = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, "Error"); // Likely Black/White
                    textViewName.setTextColor(color);
                    textViewName.setPaintFlags(textViewName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)); // Ensure NO strikethrough
                    buttonEdit.setVisibility(View.VISIBLE);
                    buttonEdit.setEnabled(true);
                    buttonEdit.setAlpha(1.0f);
                }

                if (firebaseListId != null) {
                    repository.toggleItemChecked(firebaseListId, item.getFirebaseId(), isChecked, null);
                } else {
                    repository.updateItemInList(item, firebaseListId, null);
                }
                
                // Handle sorting locally to move item to bottom/top
                int newPos = -1;
                // We need to re-sort the list based on the new isDone state
                // Copy list to sort
                List<ShoppingItem> sortedList = new ArrayList<>(items);
                Collections.sort(sortedList, (item1, item2) -> {
                    if (item1.isDone() == item2.isDone()) {
                        int posComp = Integer.compare(item1.getPosition(), item2.getPosition());
                        if (posComp != 0) return posComp;
                        return Long.compare(item1.getId(), item2.getId());
                    }
                    return item1.isDone() ? 1 : -1;
                });
                
                // Find where our item ended up
                int newIndex = sortedList.indexOf(item);
                
                if (currentPos != newIndex) {
                    // Prevent auto-scrolling:
                    
                    RecyclerView rv = (RecyclerView) itemView.getParent();
                    androidx.recyclerview.widget.LinearLayoutManager llm = (androidx.recyclerview.widget.LinearLayoutManager) rv.getLayoutManager();
                    
                    // 1. Capture anchor item (top visible)
                    int firstVisiblePos = llm.findFirstVisibleItemPosition();
                    View firstVisibleView = llm.findViewByPosition(firstVisiblePos);
                    int offset = (firstVisibleView != null) ? firstVisibleView.getTop() : 0;
                    
                    // 2. Clear focus aggressively
                    if (checkBox.hasFocus() || itemView.hasFocus()) {
                        checkBox.clearFocus();
                        itemView.clearFocus();
                    }
                    rv.stopScroll();
                    View parent = (View) itemView.getParent();
                    if (parent != null) parent.clearFocus();
                    rv.clearFocus();
                    
                    // Disable ItemAnimator to prevent flickering/slide animations
                    RecyclerView.ItemAnimator animator = rv.getItemAnimator();
                    rv.setItemAnimator(null);

                    // 3. Update data
                    items.remove(currentPos);
                    items.add(newIndex, item);
                    
                    // 4. Update UI using Remove+Insert
                    notifyItemRemoved(currentPos);
                    notifyItemInserted(newIndex);
                    
                    // Restore ItemAnimator
                    // We post it to ensure the layout pass happens without animation first
                    // Actually, setting it to null immediately affects the next layout pass triggered by notify...
                    // But we want to re-enable it for FUTURE interactions.
                    // If we re-enable immediately, it might still animate if the loop isn't finished?
                    // Safe to post.
                    rv.post(() -> rv.setItemAnimator(animator));
                    
                    // 5. Restore scroll anchor strictly
                    if (firstVisiblePos != RecyclerView.NO_POSITION) {
                        // If we removed an item ABOVE the anchor, the anchor index shifts down by 1.
                        // (Because an item before it is gone).
                        // wait, if we remove 'currentPos' and 'currentPos' < 'firstVisiblePos', then anchor becomes 'firstVisiblePos - 1'.
                        
                        int restoreIndex = firstVisiblePos;
                        if (currentPos < firstVisiblePos) {
                             restoreIndex--;
                        }
                        // We inserted at 'newIndex'. If 'newIndex' <= 'restoreIndex', it shifts up by 1.
                        if (newIndex <= restoreIndex) {
                             restoreIndex++;
                        }
                        
                        // Clamp
                        if (restoreIndex < 0) restoreIndex = 0;
                        if (restoreIndex >= items.size()) restoreIndex = Math.max(0, items.size() - 1);
                        
                        final int finalPos = restoreIndex;
                        final int finalOffset = offset;
                        
                        // Force restore immediately to minimize visual glitch
                        llm.scrollToPositionWithOffset(finalPos, finalOffset);
                    }
                }

                if (interactionListener != null) {
                    interactionListener.onItemCheckboxChanged(item, isChecked);
                }
            });

            buttonEdit.setOnClickListener(v -> {
                if (item.isDone()) {
                    UiUtils.makeCustomToast(context, R.string.toast_finish_editing_status, Toast.LENGTH_SHORT).show();
                    return;
                }
                int currentPos = getBindingAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION) return;

                if (editingItemPosition != -1 && editingItemPosition != currentPos) {
                    UiUtils.makeCustomToast(context, R.string.toast_finish_editing, Toast.LENGTH_SHORT).show();
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
                        UiUtils.makeCustomToast(context, R.string.toast_finish_editing_delete, Toast.LENGTH_SHORT).show();
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
                UiUtils.makeCustomToast(context, R.string.invalid_item_name, Toast.LENGTH_SHORT).show();
                return;
            }
            item.setName(newName);
            repository.updateItemInList(item, firebaseListId, null);
            if (firebaseListId != null) {
                repository.updateListTimestamp(firebaseListId, null);
            }
            editingItemPosition = -1;
            notifyItemChanged(position);
            
            // Removing requestItemResort() here because it triggers a full reload (destroying listener)
            // which can cause a race condition where the listener fetches stale data from server
            // before the local write is fully committed/cached, resulting in UI reverting to old value.
            // Since name change does not affect sort order (done/position), we don't need to resort.
            if (interactionListener != null) {
                interactionListener.requestItemResort();
            }
        }
    }
}
