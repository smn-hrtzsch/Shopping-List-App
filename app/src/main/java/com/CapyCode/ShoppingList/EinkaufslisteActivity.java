package com.CapyCode.ShoppingList;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EinkaufslisteActivity extends AppCompatActivity implements MyRecyclerViewAdapter.OnItemInteractionListener {

    private RecyclerView recyclerView;
    private MyRecyclerViewAdapter adapter;
    private List<ShoppingItem> shoppingItems;
    private ShoppingListRepository shoppingListRepository;
    private ShoppingList currentShoppingList;
    private long currentShoppingListId = -1L;
    private String firebaseListId;
    private TextView toolbarTitleTextView;
    private TextView emptyView;
    private View activityRootView;

    private EditText editTextAddItem;
    private ImageButton buttonAddItem;
    private ImageView toolbarCloudIcon;
    private com.google.firebase.firestore.ListenerRegistration listSnapshotListener;
    private com.google.firebase.firestore.ListenerRegistration itemsSnapshotListener;
    private final android.os.Handler syncIconHandler = new android.os.Handler();
    private FirebaseAuth mAuth;
    private boolean isUnsyncing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_einkaufsliste);

        mAuth = FirebaseAuth.getInstance();
        Toolbar toolbar = findViewById(R.id.toolbar_einkaufsliste);
        setSupportActionBar(toolbar);
        toolbarTitleTextView = findViewById(R.id.toolbar_title_einkaufsliste);
        emptyView = findViewById(R.id.empty_view_items);
        activityRootView = findViewById(R.id.einkaufsliste_activity_root);
        toolbarCloudIcon = findViewById(R.id.toolbar_cloud_icon);

        AppBarLayout appBarLayout = findViewById(R.id.appbar);
        View addItemBarContainer = findViewById(R.id.add_item_bar_container);

        ViewCompat.setOnApplyWindowInsetsListener(activityRootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(systemBars.bottom, ime.bottom);
            appBarLayout.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            addItemBarContainer.setPadding(systemBars.left, 0, systemBars.right, bottomInset);
            return insets;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        shoppingListRepository = new ShoppingListRepository(this);
        currentShoppingListId = getIntent().getLongExtra("LIST_ID", -1L);
        firebaseListId = getIntent().getStringExtra("FIREBASE_LIST_ID");
        currentShoppingList = shoppingListRepository.getShoppingListById(currentShoppingListId);

        String listName = getIntent().getStringExtra("LIST_NAME");
        if (toolbarTitleTextView != null) {
            toolbarTitleTextView.setText(listName);
        }

        if (toolbarCloudIcon != null) {
            toolbarCloudIcon.setVisibility(View.VISIBLE); // Always visible now
            if (firebaseListId != null) {
                toolbarCloudIcon.setImageResource(R.drawable.ic_cloud_synced_24);
            } else {
                toolbarCloudIcon.setImageResource(R.drawable.ic_cloud_unsynced_24);
            }
        }

        if (firebaseListId != null) {
            listSnapshotListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("shopping_lists")
                    .document(firebaseListId)
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) return;
                        if (isUnsyncing) return; // Prevent closing if we are currently unsyncing
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            Boolean sharedOnServer = documentSnapshot.getBoolean("isShared");
                            if (currentShoppingList != null && sharedOnServer != null) {
                                currentShoppingList.setShared(sharedOnServer);
                                invalidateOptionsMenu();
                            }
                        } else if (documentSnapshot != null && !documentSnapshot.exists()) {
                            Toast.makeText(this, R.string.error_list_deleted_by_owner, Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
        }

        shoppingItems = new ArrayList<>();
        recyclerView = findViewById(R.id.item_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyRecyclerViewAdapter(this, shoppingItems, shoppingListRepository, this, firebaseListId);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        setupAddItemBar();
        refreshItemList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.einkaufsliste_menu, menu);
        boolean isShared = currentShoppingList != null && currentShoppingList.isShared();
        menu.findItem(R.id.action_share_list).setVisible(!isShared);
        menu.findItem(R.id.action_view_members).setVisible(isShared);

        MenuItem syncItem = menu.findItem(R.id.action_sync_switch);
        syncItem.setVisible(!isShared && mAuth.getCurrentUser() != null);
        if (syncItem.isVisible()) {
            com.google.android.material.switchmaterial.SwitchMaterial syncSwitch = 
                (com.google.android.material.switchmaterial.SwitchMaterial) syncItem.getActionView();
            if (syncSwitch != null) {
                syncSwitch.setOnCheckedChangeListener(null);
                syncSwitch.setChecked(firebaseListId != null);
                syncSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> toggleCloudSync(isChecked, syncSwitch));
            }
        }
        return true;
    }

    private void toggleCloudSync(boolean enable, com.google.android.material.switchmaterial.SwitchMaterial syncSwitch) {
        if (enable && firebaseListId == null) {
            showCustomDialog(getString(R.string.dialog_enable_sync_title), getString(R.string.dialog_enable_sync_message), getString(R.string.button_enable_sync), () -> {
                updateSyncIcon(R.drawable.ic_cloud_upload_24);
                shoppingListRepository.uploadSingleListToCloud(currentShoppingList, () -> {
                    firebaseListId = currentShoppingList.getFirebaseId();
                    updateSyncIcon(R.drawable.ic_cloud_synced_24);
                    Toast.makeText(this, R.string.toast_sync_enabled, Toast.LENGTH_SHORT).show();
                    refreshItemList();
                });
            }, () -> {
                syncSwitch.setOnCheckedChangeListener(null);
                syncSwitch.setChecked(false);
                syncSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> toggleCloudSync(isChecked, syncSwitch));
            });
        } else if (!enable && firebaseListId != null) {
            showCustomDialog(getString(R.string.dialog_stop_sync_title), getString(R.string.dialog_stop_sync_message), getString(R.string.button_stop_sync), () -> {
                isUnsyncing = true; // Set early here!
                performSafeUnsync();
            }, () -> {
                syncSwitch.setOnCheckedChangeListener(null);
                syncSwitch.setChecked(true);
                syncSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> toggleCloudSync(isChecked, syncSwitch));
            });
        }
    }

    private void performSafeUnsync() {
        if (firebaseListId == null) return;
        // isUnsyncing = true; // No longer needed here as it's set in the dialog callback
        Toast.makeText(this, R.string.syncing_data, Toast.LENGTH_SHORT).show();

        // 1. Fetch items from cloud (One-Time) to ensure we have latest state
        shoppingListRepository.fetchItemsFromCloudOneTime(firebaseListId, cloudItems -> {
            // 2. Remove listeners immediately
            if (listSnapshotListener != null) { listSnapshotListener.remove(); listSnapshotListener = null; }
            if (itemsSnapshotListener != null) { itemsSnapshotListener.remove(); itemsSnapshotListener = null; }

            // 3. Atomically decouple list and replace items locally
            // This prevents "deleteObsoleteCloudLists" from deleting this list because firebaseId becomes NULL
            shoppingListRepository.decoupleListAndReplaceItems(currentShoppingListId, cloudItems);

            String listToDeleteId = firebaseListId;

            // 4. Update RAM objects
            currentShoppingList.setFirebaseId(null);
            currentShoppingList.setShared(false);
            currentShoppingList.setOwnerId(null);
            firebaseListId = null;

            // 5. Update UI immediately
            updateSyncIcon(R.drawable.ic_cloud_24);
            Toast.makeText(this, R.string.toast_sync_disabled, Toast.LENGTH_SHORT).show();
            refreshItemList(); // Reloads items from DB (which are now the decoupled ones)

            // 6. Delete from Cloud
            // Even if MainActivity listener fires now, it won't find this list in "obsolete" check because it has no firebaseId locally.
            shoppingListRepository.deleteSingleListFromCloud(listToDeleteId, null);
        });
    }

    private void updateSyncIcon(int drawableId) {
        if (toolbarCloudIcon == null) return;
        
        toolbarCloudIcon.setVisibility(View.VISIBLE);
        if (firebaseListId != null) {
             // List is synced
             toolbarCloudIcon.setImageResource(drawableId);
             if (drawableId != R.drawable.ic_cloud_synced_24) {
                 syncIconHandler.removeCallbacksAndMessages(null);
                 syncIconHandler.postDelayed(() -> {
                     if (toolbarCloudIcon != null) toolbarCloudIcon.setImageResource(R.drawable.ic_cloud_synced_24);
                 }, 1000);
             }
        } else {
             // List is local
             toolbarCloudIcon.setImageResource(R.drawable.ic_cloud_unsynced_24);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        syncIconHandler.removeCallbacksAndMessages(null);
        if (listSnapshotListener != null) listSnapshotListener.remove();
        if (itemsSnapshotListener != null) itemsSnapshotListener.remove();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_clear_list) { showClearListOptionsDialog(); return true; }
        else if (itemId == R.id.action_share_list) { shareShoppingList(); return true; }
        else if (itemId == R.id.action_view_members) { showMembersDialog(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void showMembersDialog() {
        if (firebaseListId == null) return;
        shoppingListRepository.getMembersWithNames(firebaseListId, new ShoppingListRepository.OnMembersLoadedListener() {
            @Override
            public void onLoaded(List<Map<String, String>> membersWithNames) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(EinkaufslisteActivity.this);
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_members_list, null);
                builder.setView(dialogView);
                androidx.appcompat.app.AlertDialog dialog = builder.create();
                if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                android.widget.LinearLayout container = dialogView.findViewById(R.id.container_members_list);
                View buttonAdd = dialogView.findViewById(R.id.button_add_member);
                View buttonClose = dialogView.findViewById(R.id.button_close_dialog);
                String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                for (Map<String, String> member : membersWithNames) {
                    android.widget.LinearLayout row = new android.widget.LinearLayout(EinkaufslisteActivity.this);
                    row.setOrientation(android.widget.LinearLayout.HORIZONTAL); row.setGravity(android.view.Gravity.CENTER_VERTICAL); row.setPadding(0, 24, 0, 24);
                    ImageView icon = new ImageView(EinkaufslisteActivity.this); icon.setImageResource(R.drawable.ic_account_circle_24);
                    String uid = member.get("uid"); boolean isMe = uid != null && uid.equals(currentUid);
                    int textColor = com.google.android.material.color.MaterialColors.getColor(dialogView, com.google.android.material.R.attr.colorOnSurface);
                    int highlightColor = androidx.core.content.ContextCompat.getColor(EinkaufslisteActivity.this, R.color.dialog_action_text_adaptive);
                    icon.setColorFilter(isMe ? highlightColor : textColor);
                    android.widget.LinearLayout.LayoutParams iconParams = new android.widget.LinearLayout.LayoutParams(64, 64); iconParams.setMargins(0, 0, 32, 0);
                    row.addView(icon, iconParams);
                    TextView text = new TextView(EinkaufslisteActivity.this);
                    String role = member.get("role"); String username = member.get("username");
                    if (isMe) username += getString(R.string.member_is_me);
                    android.text.SpannableString content = new android.text.SpannableString(username + "\n" + role);
                    content.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, username.length(), 0);
                    content.setSpan(new android.text.style.RelativeSizeSpan(0.85f), username.length() + 1, content.length(), 0);
                    content.setSpan(new android.text.style.ForegroundColorSpan(com.google.android.material.color.MaterialColors.getColor(dialogView, com.google.android.material.R.attr.colorOnSurfaceVariant)), username.length() + 1, content.length(), 0);
                    text.setText(content); text.setTextColor(isMe ? highlightColor : textColor); text.setTextSize(16); row.addView(text);
                    container.addView(row);
                }
                buttonAdd.setOnClickListener(v -> { dialog.dismiss(); showInviteUserDialog(); });
                buttonClose.setOnClickListener(v -> dialog.dismiss());
                dialog.show();
            }
            @Override
            public void onError(String error) { Toast.makeText(EinkaufslisteActivity.this, getString(R.string.error_generic_message, error), Toast.LENGTH_SHORT).show(); }
        });
    }

    private void showInviteUserDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_invite_user, null);
        builder.setView(dialogView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        com.google.android.material.textfield.TextInputEditText editText = dialogView.findViewById(R.id.edit_text_invite_username);
        View btnPositive = dialogView.findViewById(R.id.dialog_button_positive);
        View btnClose = dialogView.findViewById(R.id.button_dialog_close);
        btnPositive.setOnClickListener(v -> {
            String username = editText.getText().toString().trim();
            if (!username.isEmpty()) {
                UserRepository userRepository = new UserRepository(this);
                userRepository.findUidByUsername(username, new UserRepository.OnUserSearchListener() {
                    @Override
                    public void onUserFound(String uid) {
                        shoppingListRepository.addMemberToList(firebaseListId, uid, new ShoppingListRepository.OnMemberAddListener() {
                            @Override
                            public void onMemberAdded() { Toast.makeText(EinkaufslisteActivity.this, getString(R.string.user_invited, username), Toast.LENGTH_SHORT).show(); dialog.dismiss(); }
                            @Override
                            public void onMemberAlreadyExists() { Toast.makeText(EinkaufslisteActivity.this, R.string.error_member_already_exists, Toast.LENGTH_LONG).show(); }
                            @Override
                            public void onError(String message) { Toast.makeText(EinkaufslisteActivity.this, getString(R.string.error_generic_message, message), Toast.LENGTH_LONG).show(); }
                        });
                    }
                    @Override
                    public void onError(String message) { Toast.makeText(EinkaufslisteActivity.this, getString(R.string.error_generic_message, message), Toast.LENGTH_LONG).show(); }
                });
            }
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void shareShoppingList() {
        if (currentShoppingList != null && currentShoppingList.isShared()) {
            Toast.makeText(this, R.string.error_cannot_share_shared, Toast.LENGTH_SHORT).show();
            return;
        }
        shoppingListRepository.getItemsForListId(currentShoppingListId, items -> {
            try {
                org.json.JSONObject jsonList = new org.json.JSONObject();
                jsonList.put("app_id", "com.CapyCode.ShoppingList");
                jsonList.put("name", currentShoppingList != null ? currentShoppingList.getName() : "List");
                org.json.JSONArray jsonItems = new org.json.JSONArray();
                for (ShoppingItem item : items) {
                    org.json.JSONObject jsonItem = new org.json.JSONObject();
                    jsonItem.put("name", item.getName()); jsonItem.put("quantity", item.getQuantity()); jsonItem.put("unit", item.getUnit()); jsonItem.put("done", item.isDone()); jsonItem.put("notes", item.getNotes()); jsonItem.put("position", item.getPosition());
                    jsonItems.put(jsonItem);
                }
                jsonList.put("items", jsonItems);
                String jsonString = jsonList.toString(2);
                java.io.File file = new java.io.File(getCacheDir(), "list.json");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) { fos.write(jsonString.getBytes()); }
                android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(this, "com.CapyCode.ShoppingList.provider", file);
                android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("application/json"); shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri); shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(android.content.Intent.createChooser(shareIntent, getString(R.string.share_list)));
            } catch (org.json.JSONException | java.io.IOException e) { e.printStackTrace(); Toast.makeText(this, R.string.error_create_share_data, Toast.LENGTH_SHORT).show(); }
        });
    }

    private void setupAddItemBar() {
        editTextAddItem = findViewById(R.id.edit_text_add_item_bar);
        buttonAddItem = findViewById(R.id.button_add_item_bar);
        editTextAddItem.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = !s.toString().trim().isEmpty();
                buttonAddItem.setEnabled(hasText); buttonAddItem.setAlpha(hasText ? 1.0f : 0.5f);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        View.OnClickListener addItemAction = v -> {
            String name = editTextAddItem.getText().toString().trim();
            if (!name.isEmpty()) {
                int nextPosition = 0;
                if(shoppingItems != null) for (ShoppingItem existingItem : shoppingItems) if (!existingItem.isDone()) nextPosition = Math.max(nextPosition, existingItem.getPosition() + 1);
                ShoppingItem newItem = new ShoppingItem(name, "1", "", false, currentShoppingListId, "", nextPosition);
                if (firebaseListId != null) {
                    updateSyncIcon(R.drawable.ic_cloud_upload_24);
                    shoppingListRepository.addItemToShoppingList(firebaseListId, newItem, () -> updateSyncIcon(R.drawable.ic_cloud_synced_24));
                    shoppingListRepository.updateListTimestamp(firebaseListId, null);
                } else {
                    long newId = shoppingListRepository.addItemToShoppingList(currentShoppingListId, newItem);
                    if (newId != -1) { newItem.setId(newId); adapter.addItem(newItem); recyclerView.scrollToPosition(adapter.getItemCount() - 1); }
                    else Toast.makeText(this, R.string.error_adding_item, Toast.LENGTH_SHORT).show();
                }
                editTextAddItem.setText("");
            }
        };
        buttonAddItem.setOnClickListener(addItemAction);
        editTextAddItem.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { if (buttonAddItem.isEnabled()) addItemAction.onClick(v); return true; }
            return false;
        });
    }

    private void showClearListOptionsDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_clear, null);
        builder.setView(dialogView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        View btnRemoveChecked = dialogView.findViewById(R.id.button_remove_checked);
        View btnRemoveAll = dialogView.findViewById(R.id.button_remove_all);
        View btnClose = dialogView.findViewById(R.id.button_dialog_close);

        btnRemoveChecked.setOnClickListener(v -> {
            if (firebaseListId != null) {
                updateSyncIcon(R.drawable.ic_cloud_upload_24);
                shoppingListRepository.clearCheckedItemsFromList(firebaseListId, () -> shoppingListRepository.updateListTimestamp(firebaseListId, () -> updateSyncIcon(R.drawable.ic_cloud_synced_24)));
            } else shoppingListRepository.clearCheckedItemsFromList(currentShoppingListId);
            refreshItemList(); Toast.makeText(this, R.string.toast_items_cleared_done, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnRemoveAll.setOnClickListener(v -> {
            if (firebaseListId != null) {
                updateSyncIcon(R.drawable.ic_cloud_upload_24);
                shoppingListRepository.clearAllItemsFromList(firebaseListId, () -> shoppingListRepository.updateListTimestamp(firebaseListId, () -> updateSyncIcon(R.drawable.ic_cloud_synced_24)));
            } else shoppingListRepository.clearAllItemsFromList(currentShoppingListId);
            refreshItemList(); Toast.makeText(this, R.string.toast_items_cleared_all, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void refreshItemList() {
        if (shoppingListRepository == null || adapter == null) return;
        adapter.resetEditingPosition();
        if (itemsSnapshotListener != null) { itemsSnapshotListener.remove(); itemsSnapshotListener = null; }
        if (firebaseListId != null) {
            itemsSnapshotListener = shoppingListRepository.getItemsForListId(firebaseListId, loadedItems -> {
                updateSyncIcon(R.drawable.ic_cloud_download_24);
                this.shoppingItems = loadedItems; adapter.setItems(shoppingItems); checkEmptyViewItems(shoppingItems.isEmpty());
            });
        } else {
            shoppingListRepository.getItemsForListId(currentShoppingListId, loadedItems -> {
                this.shoppingItems = loadedItems; adapter.setItems(shoppingItems); checkEmptyViewItems(shoppingItems.isEmpty());
            });
        }
    }

    private void checkEmptyViewItems(boolean isEmpty) { if (emptyView != null) { emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE); recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE); } }

    @Override
    public boolean onSupportNavigateUp() { getOnBackPressedDispatcher().onBackPressed(); return true; }

    @Override
    public void onItemCheckboxChanged(ShoppingItem item, boolean isChecked) {
        if (firebaseListId != null) {
            updateSyncIcon(R.drawable.ic_cloud_upload_24);
            shoppingListRepository.toggleItemChecked(firebaseListId, item.getFirebaseId(), isChecked, () -> updateSyncIcon(R.drawable.ic_cloud_synced_24));
            shoppingListRepository.updateListTimestamp(firebaseListId, null);
        } else shoppingListRepository.toggleItemChecked(item.getId(), isChecked);
        refreshItemList();
    }

    @Override
    public void onDataSetChanged() { if (adapter != null) checkEmptyViewItems(adapter.getCurrentItems().isEmpty()); }

    @Override
    public void requestItemResort() { refreshItemList(); }

    @Override
    public View getCoordinatorLayout() { return activityRootView; }

    @Override
    public View getSnackbarAnchorView() { return findViewById(R.id.add_item_bar_container); }

    private void showCustomDialog(String title, String message, String positiveButtonText, Runnable onPositiveAction, Runnable onNegativeAction) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_standard, null);
        builder.setView(dialogView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        TextView textTitle = dialogView.findViewById(R.id.dialog_title); TextView textMessage = dialogView.findViewById(R.id.dialog_message);
        com.google.android.material.button.MaterialButton btnPositive = dialogView.findViewById(R.id.dialog_button_positive); com.google.android.material.button.MaterialButton btnNegative = dialogView.findViewById(R.id.dialog_button_negative);
        textTitle.setText(title); textMessage.setText(message); btnPositive.setText(positiveButtonText);
        btnPositive.setOnClickListener(v -> { if (onPositiveAction != null) onPositiveAction.run(); dialog.dismiss(); });
        btnNegative.setOnClickListener(v -> { if (onNegativeAction != null) onNegativeAction.run(); dialog.dismiss(); });
        dialog.show();
    }
}