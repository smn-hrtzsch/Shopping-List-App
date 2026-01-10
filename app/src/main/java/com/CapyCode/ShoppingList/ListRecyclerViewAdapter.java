// ListRecyclerViewAdapter.java
package com.CapyCode.ShoppingList;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ListRecyclerViewAdapter extends RecyclerView.Adapter<ListRecyclerViewAdapter.ViewHolder> implements ItemTouchHelperAdapter {

    private final List<ShoppingList> localDataSet;
    private final ShoppingListManager shoppingListManager;
    private final Context context;
    private final OnListInteractionListener interactionListener;
    private int editingPosition = -1;

    public interface OnListInteractionListener {
        void onListClicked(ShoppingList list);
        void onListDataSetChanged();
        void requestListDeleteConfirmation(ShoppingList list);
        void onJoinClicked(ShoppingList list);
        void onDeclineClicked(ShoppingList list);
        void onLeaveClicked(ShoppingList list);
        void onEditStarted();
        void onEditFinished();
    }

    public ListRecyclerViewAdapter(Context context, List<ShoppingList> dataSet, ShoppingListManager manager, OnListInteractionListener listener) {
        this.context = context;
        this.localDataSet = new ArrayList<>();
        if (dataSet != null) {
            this.localDataSet.addAll(dataSet);
        }
        this.shoppingListManager = manager;
        this.interactionListener = listener;
        setHasStableIds(true);
    }
    
    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= localDataSet.size()) return RecyclerView.NO_ID;
        ShoppingList list = localDataSet.get(position);
        if (list.getFirebaseId() != null) return list.getFirebaseId().hashCode();
        return list.getId();
    }

    public void resetEditingPosition() {
        if (editingPosition != -1) {
            int oldPos = editingPosition;
            editingPosition = -1;
            notifyItemChanged(oldPos);
            if (interactionListener != null) interactionListener.onEditFinished();
        }
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
        int adapterPosition = viewHolder.getBindingAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= localDataSet.size()) {
            return;
        }
        ShoppingList list = localDataSet.get(adapterPosition);
        boolean isEditing = adapterPosition == editingPosition;
        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        boolean isPending = list.isCurrentUserPending(currentUid);
        boolean isOwner = list.isOwner(currentUid);

        viewHolder.textViewListName.setText(list.getName());

        if (isPending) {
            viewHolder.cloudIcon.setVisibility(View.GONE);
        } else {
            viewHolder.cloudIcon.setVisibility(isEditing ? View.GONE : View.VISIBLE);
            if (list.getFirebaseId() != null) {
                // Use explicit isShared flag
                if (list.isShared()) {
                    viewHolder.cloudIcon.setImageResource(R.drawable.ic_members_group);
                } else {
                    viewHolder.cloudIcon.setImageResource(R.drawable.ic_cloud_synced_24); // Assuming this is the synced icon
                }
            } else {
                // Local list
                viewHolder.cloudIcon.setImageResource(R.drawable.ic_cloud_unsynced_24);
            }
        }

        String itemCountText = String.format(Locale.getDefault(), "%d %s",
                list.getItemCount(),
                list.getItemCount() == 1 ? context.getString(R.string.item_count_suffix_singular) : context.getString(R.string.item_count_suffix_plural));
        
        if (isPending) {
            String ownerName = list.getOwnerUsername() != null ? list.getOwnerUsername() : "...";
            viewHolder.textViewListItemCount.setText(context.getString(R.string.invited_by, ownerName));
            viewHolder.buttonJoin.setVisibility(View.VISIBLE);
            viewHolder.buttonDecline.setVisibility(View.VISIBLE);
            viewHolder.buttonEditListName.setVisibility(View.GONE);
            viewHolder.buttonDeleteList.setVisibility(View.GONE);
        } else {
            viewHolder.textViewListItemCount.setText(itemCountText);
            viewHolder.buttonJoin.setVisibility(View.GONE);
            viewHolder.buttonDecline.setVisibility(View.GONE);
            viewHolder.buttonEditListName.setVisibility(isEditing ? View.GONE : View.VISIBLE);
            viewHolder.buttonDeleteList.setVisibility(isEditing ? View.GONE : View.VISIBLE);
            
            // Set correct icon for delete/leave
            if (list.getFirebaseId() != null && !isOwner) {
                viewHolder.buttonDeleteList.setImageResource(R.drawable.ic_logout_24);
                viewHolder.buttonDeleteList.setContentDescription(context.getString(R.string.button_leave));
            } else {
                viewHolder.buttonDeleteList.setImageResource(R.drawable.ic_delete_24);
                viewHolder.buttonDeleteList.setContentDescription(context.getString(R.string.button_delete_list));
            }
        }


        viewHolder.itemView.setOnClickListener(v -> {
            int currentPos = viewHolder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;
            if (isPending) {
                UiUtils.makeCustomToast(context, R.string.toast_pending_invite, Toast.LENGTH_SHORT).show();
                return;
            }
            if (editingPosition != -1 && editingPosition == currentPos){
                UiUtils.makeCustomToast(context, R.string.toast_finish_editing, Toast.LENGTH_SHORT).show();
                return;
            }
            if (interactionListener != null) {
                interactionListener.onListClicked(localDataSet.get(currentPos));
            }
        });

        viewHolder.textViewListName.setVisibility(isEditing ? View.GONE : View.VISIBLE);
        viewHolder.editTextListName.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        viewHolder.buttonSaveListName.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        viewHolder.textViewListItemCount.setVisibility(isEditing ? View.GONE : View.VISIBLE);


        if (isEditing) {
            viewHolder.editTextListName.setText(list.getName());
            viewHolder.editTextListName.requestFocus();
            viewHolder.editTextListName.setSelection(viewHolder.editTextListName.getText().length());
            viewHolder.editTextListName.post(() -> {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(viewHolder.editTextListName, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            });
        }


        viewHolder.buttonEditListName.setOnClickListener(v -> {
            int currentPos = viewHolder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                int previousEditingPosition = editingPosition;
                editingPosition = currentPos;
                if (previousEditingPosition != -1) {
                    notifyItemChanged(previousEditingPosition);
                }
                notifyItemChanged(editingPosition);
                if (interactionListener != null) interactionListener.onEditStarted();
            }
        });

        viewHolder.buttonSaveListName.setOnClickListener(v -> saveListName(viewHolder));
        viewHolder.editTextListName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveListName(viewHolder);
                return true;
            }
            return false;
        });


        viewHolder.buttonDeleteList.setOnClickListener(v -> {
            int currentPos = viewHolder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;
            
            ShoppingList listToHandle = localDataSet.get(currentPos);
            if (listToHandle.getFirebaseId() != null && !isOwner) {
                // Not owner of cloud list -> Leave
                if (interactionListener != null) interactionListener.onLeaveClicked(listToHandle);
            } else {
                // Local list or owner of cloud list -> Delete
                if (interactionListener != null) interactionListener.requestListDeleteConfirmation(listToHandle);
            }
        });

        viewHolder.buttonJoin.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onJoinClicked(list);
        });

        viewHolder.buttonDecline.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onDeclineClicked(list);
        });
    }

    private void saveListName(ViewHolder viewHolder) {
        int currentPosition = viewHolder.getBindingAdapterPosition();
        if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= localDataSet.size()) return;

        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(viewHolder.editTextListName.getWindowToken(), 0);

        ShoppingList list = localDataSet.get(currentPosition);
        String newName = viewHolder.editTextListName.getText().toString().trim();

        if (!newName.isEmpty() && !newName.equals(list.getName())) {
            list.setName(newName); 
            shoppingListManager.updateShoppingList(list);
            UiUtils.makeCustomToast(context, context.getString(R.string.toast_renamed, newName), Toast.LENGTH_SHORT).show();
        }
        editingPosition = -1;
        notifyItemChanged(currentPosition);
        if (interactionListener != null) interactionListener.onEditFinished();
    }


    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public void updateLists(List<ShoppingList> newLists) {
        final List<ShoppingList> oldList = new ArrayList<>(this.localDataSet);
        final List<ShoppingList> newList = new ArrayList<>(newLists);
        
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return oldList.size(); }
            @Override
            public int getNewListSize() { return newList.size(); }
            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return oldList.get(oldPos).equals(newList.get(newPos));
            }
            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                ShoppingList o = oldList.get(oldPos);
                ShoppingList n = newList.get(newPos);
                return java.util.Objects.equals(o.getName(), n.getName()) &&
                       o.getItemCount() == n.getItemCount() &&
                       o.isShared() == n.isShared() &&
                       java.util.Objects.equals(o.getFirebaseId(), n.getFirebaseId()) &&
                       java.util.Objects.equals(o.getOwnerUsername(), n.getOwnerUsername()) &&
                       java.util.Objects.equals(o.getMembers(), n.getMembers()) &&
                       java.util.Objects.equals(o.getPendingMembers(), n.getPendingMembers());
            }
        });

        this.editingPosition = -1;
        this.localDataSet.clear();
        this.localDataSet.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        editingPosition = -1;
        if (fromPosition < 0 || fromPosition >= localDataSet.size() || toPosition < 0 || toPosition >= localDataSet.size()){
            return false;
        }

        ShoppingList movedItem = localDataSet.remove(fromPosition);
        localDataSet.add(toPosition, movedItem);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(int position) {
    }

    @Override
    public void onDragFinished() {
        for (int i = 0; i < localDataSet.size(); i++) {
            localDataSet.get(i).setPosition(i);
        }
        shoppingListManager.updateListPositions(localDataSet); 
        shoppingListManager.saveListOrder(localDataSet);
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewListName;
        final EditText editTextListName;
        final ImageButton buttonEditListName;
        final ImageButton buttonSaveListName;
        final ImageButton buttonDeleteList;
        final TextView textViewListItemCount;
        final ImageView cloudIcon;
        final ImageButton buttonJoin;
        final com.google.android.material.button.MaterialButton buttonDecline;


        public ViewHolder(View view) {
            super(view);
            textViewListName = view.findViewById(R.id.list_name_text_view);
            editTextListName = view.findViewById(R.id.edit_text_list_name_inline);
            buttonEditListName = view.findViewById(R.id.button_edit_list_name_inline);
            buttonSaveListName = view.findViewById(R.id.button_save_list_name_inline);
            buttonDeleteList = view.findViewById(R.id.button_delete_list_inline);
            textViewListItemCount = view.findViewById(R.id.list_item_count_text_view);
            cloudIcon = view.findViewById(R.id.cloud_icon);
            buttonJoin = view.findViewById(R.id.button_join_list);
            buttonDecline = view.findViewById(R.id.button_decline_list);
        }
    }
}