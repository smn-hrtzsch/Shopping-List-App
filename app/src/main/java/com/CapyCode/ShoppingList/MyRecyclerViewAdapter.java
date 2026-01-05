import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.MaterialColors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ItemViewHolder> implements ItemTouchHelperAdapter {

    private final Context context;
    private final ShoppingListRepository repository;
    private final OnItemInteractionListener interactionListener;
    private final String firebaseListId;

    private int editingItemPosition = -1;

    private static final DiffUtil.ItemCallback<ShoppingItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<ShoppingItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull ShoppingItem oldItem, @NonNull ShoppingItem newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull ShoppingItem oldItem, @NonNull ShoppingItem newItem) {
            return java.util.Objects.equals(oldItem.getName(), newItem.getName()) &&
                   oldItem.isDone() == newItem.isDone() &&
                   oldItem.getPosition() == newItem.getPosition() &&
                   java.util.Objects.equals(oldItem.getQuantity(), newItem.getQuantity()) &&
                   java.util.Objects.equals(oldItem.getUnit(), newItem.getUnit());
        }
    };

    private final AsyncListDiffer<ShoppingItem> mDiffer = new AsyncListDiffer<>(this, DIFF_CALLBACK);

    public interface OnItemInteractionListener {
        void onItemCheckboxChanged(ShoppingItem item, boolean isChecked);
        void onDataSetChanged();
        void requestItemResort();
        View getCoordinatorLayout();
        void showUndoBar(String message, Runnable undoAction);
    }

    public MyRecyclerViewAdapter(Context context, List<ShoppingItem> items, ShoppingListRepository repository, OnItemInteractionListener listener, String firebaseListId) {
        this.context = context;
        this.repository = repository;
        this.interactionListener = listener;
        this.firebaseListId = firebaseListId;
        setHasStableIds(true);
        if (items != null) {
            setItems(items);
        }
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= mDiffer.getCurrentList().size()) return RecyclerView.NO_ID;
        ShoppingItem item = mDiffer.getCurrentList().get(position);
        if (item.getFirebaseId() != null) return item.getFirebaseId().hashCode();
        return item.getId();
    }

    public void resetEditingPosition() {
        if (editingItemPosition != -1 && editingItemPosition < mDiffer.getCurrentList().size()) {
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
        ShoppingItem currentItem = mDiffer.getCurrentList().get(position);
        holder.bind(currentItem, position == editingItemPosition);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void setItems(List<ShoppingItem> newItems) {
        // Use a background thread for sorting before submitting to differ
        new Thread(() -> {
            final List<ShoppingItem> sortedList = new ArrayList<>(newItems);
            Collections.sort(sortedList, (item1, item2) -> {
                if (item1.isDone() == item2.isDone()) {
                    int posComp = Integer.compare(item1.getPosition(), item2.getPosition());
                    if (posComp != 0) return posComp;
                    return Long.compare(item1.getId(), item2.getId());
                }
                return item1.isDone() ? 1 : -1;
            });

            // Submit to AsyncListDiffer (it will handle background diffing)
            mDiffer.submitList(sortedList, () -> {
                if (interactionListener != null) {
                    interactionListener.onDataSetChanged();
                }
            });
        }).start();
    }

    private void sortItemsLocal() {
        // This is now redundant as setItems handles sorting, but we keep it for in-memory swaps during drag
        Collections.sort(mDiffer.getCurrentList(), (item1, item2) -> {
            if (item1.isDone() == item2.isDone()) {
                int posComp = Integer.compare(item1.getPosition(), item2.getPosition());
                if (posComp != 0) return posComp;
                return Long.compare(item1.getId(), item2.getId());
            }
            return item1.isDone() ? 1 : -1;
        });
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        List<ShoppingItem> currentList = new ArrayList<>(mDiffer.getCurrentList());
        if (fromPosition >= currentList.size() || toPosition >= currentList.size() || fromPosition < 0 || toPosition < 0) {
            return false;
        }
        
        if (currentList.get(fromPosition).isDone() != currentList.get(toPosition).isDone()) {
            return false;
        }

        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(currentList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(currentList, i, i - 1);
            }
        }
        
        // Use submitList to keep internal list in sync while dragging
        mDiffer.submitList(currentList);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onDragFinished() {
        List<ShoppingItem> currentList = mDiffer.getCurrentList();
        // Update positions based on the current list order once drag is finished
        for (int i = 0; i < currentList.size(); i++) {
            currentList.get(i).setPosition(i);
        }
        
        repository.updateItemPositions(currentList);
        
        if (firebaseListId != null) {
            repository.updateItemPositionsInCloud(firebaseListId, currentList, null);
        }
    }

    public int addItem(ShoppingItem item) {
        List<ShoppingItem> newList = new ArrayList<>(mDiffer.getCurrentList());
        int insertPosition = 0;
        for (int i = 0; i < newList.size(); i++) {
            if (newList.get(i).isDone()) {
                break;
            }
            insertPosition = i + 1;
        }
        newList.add(insertPosition, item);
        mDiffer.submitList(newList, () -> {
            if (interactionListener != null) {
                interactionListener.onDataSetChanged();
            }
        });
        return insertPosition;
    }

    @Override
    public void onItemDismiss(int position) {
        if (position < 0 || position >= mDiffer.getCurrentList().size()) {
            return;
        }

        final ShoppingItem itemToDelete = mDiffer.getCurrentList().get(position);
        final int deletedPosition = position;

        if (firebaseListId != null) {
            repository.deleteItemFromList(firebaseListId, itemToDelete.getFirebaseId());
            repository.updateListTimestamp(firebaseListId, null);
        } else {
            repository.deleteItemFromList(itemToDelete.getId());
        }

        List<ShoppingItem> newListAfterRemoval = new ArrayList<>(mDiffer.getCurrentList());
        newListAfterRemoval.remove(position);
        mDiffer.submitList(newListAfterRemoval);

        if (interactionListener != null) {
            String message = "\"" + itemToDelete.getName() + "\" " + context.getString(R.string.deleted);
            interactionListener.showUndoBar(message, () -> {
                int reInsertPos = Math.min(deletedPosition, mDiffer.getCurrentList().size());
                
                if (firebaseListId != null) {
                    repository.restoreItemToShoppingList(firebaseListId, itemToDelete, null);
                    repository.updateListTimestamp(firebaseListId, null);
                } else {
                    long newId = repository.addItemToShoppingList(itemToDelete.getListId(), itemToDelete);
                    itemToDelete.setId(newId);
                }

                List<ShoppingItem> newListWithRestored = new ArrayList<>(mDiffer.getCurrentList());
                
                // Shift existing items logic
                int targetPos = itemToDelete.getPosition();
                for (ShoppingItem item : newListWithRestored) {
                    if (item.getPosition() >= targetPos) {
                        item.setPosition(item.getPosition() + 1);
                    }
                }

                // Add back to local list
                if (reInsertPos >= 0 && reInsertPos <= newListWithRestored.size()) {
                    newListWithRestored.add(reInsertPos, itemToDelete);
                } else {
                    newListWithRestored.add(itemToDelete);
                }
                
                // Submit update to differ (will handle sorting and notify)
                setItems(newListWithRestored);
            });
        }
    }

    public List<ShoppingItem> getCurrentItems() {
        return new ArrayList<>(mDiffer.getCurrentList());
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
                if (firebaseListId != null) {
                    repository.toggleItemChecked(firebaseListId, item.getFirebaseId(), isChecked, null);
                }
                else {
                    repository.updateItemInList(item, firebaseListId, null);
                }
                if (interactionListener != null) {
                    interactionListener.onItemCheckboxChanged(item, isChecked);
                    interactionListener.requestItemResort();
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
            if (interactionListener != null) {
                interactionListener.requestItemResort();
            }
        }
    }
}
