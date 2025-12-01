package com.example.einkaufsliste;

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

import java.util.ArrayList;
import java.util.List;

public class EinkaufslisteActivity extends AppCompatActivity implements MyRecyclerViewAdapter.OnItemInteractionListener {

    private RecyclerView recyclerView;
    private MyRecyclerViewAdapter adapter;
    private List<ShoppingItem> shoppingItems;
    private ShoppingListRepository shoppingListRepository;
    private long currentShoppingListId = -1L;
    private String firebaseListId;
    private TextView toolbarTitleTextView;
    private TextView emptyView;
    private View activityRootView;

    private EditText editTextAddItem;
    private ImageButton buttonAddItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_einkaufsliste);

        Toolbar toolbar = findViewById(R.id.toolbar_einkaufsliste);
        setSupportActionBar(toolbar);
        toolbarTitleTextView = findViewById(R.id.toolbar_title_einkaufsliste);
        emptyView = findViewById(R.id.empty_view_items);
        activityRootView = findViewById(R.id.einkaufsliste_activity_root);

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

        String listName = getIntent().getStringExtra("LIST_NAME");
        if (toolbarTitleTextView != null) {
            toolbarTitleTextView.setText(listName);
        }

        if (firebaseListId != null) {
            ImageView toolbarCloudIcon = findViewById(R.id.toolbar_cloud_icon);
            if (toolbarCloudIcon != null) {
                toolbarCloudIcon.setVisibility(View.VISIBLE);
            }
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.einkaufsliste_menu, menu);
        if (firebaseListId != null) {
            menu.findItem(R.id.action_share_list).setVisible(false);
            menu.findItem(R.id.action_invite_user).setVisible(true);
        } else {
            menu.findItem(R.id.action_invite_user).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_clear_list) {
            showClearListOptionsDialog();
            return true;
        } else if (itemId == R.id.action_share_list) {
            shareShoppingList();
            return true;
        } else if (itemId == R.id.action_invite_user) {
            showInviteUserDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showInviteUserDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_invite_user, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        com.google.android.material.textfield.TextInputEditText editText = dialogView.findViewById(R.id.edit_text_invite_username);
        View btnPositive = dialogView.findViewById(R.id.dialog_button_positive);
        View btnNegative = dialogView.findViewById(R.id.dialog_button_negative);

        btnPositive.setOnClickListener(v -> {
            String username = editText.getText().toString().trim();
            if (!username.isEmpty()) {
                // 1. Search for UID by username
                UserRepository userRepository = new UserRepository(this);
                userRepository.findUidByUsername(username, new UserRepository.OnUserSearchListener() {
                    @Override
                    public void onUserFound(String uid) {
                        // 2. Add UID to list
                        shoppingListRepository.addMemberToList(firebaseListId, uid, new ShoppingListRepository.OnMemberAddListener() {
                            @Override
                            public void onMemberAdded() {
                                Toast.makeText(EinkaufslisteActivity.this, getString(R.string.user_invited, username), Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }

                            @Override
                            public void onMemberAlreadyExists() {
                                Toast.makeText(EinkaufslisteActivity.this, R.string.error_member_already_exists, Toast.LENGTH_LONG).show();
                                // Don't dismiss dialog so user can correct input if needed
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(EinkaufslisteActivity.this, message, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(EinkaufslisteActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        btnNegative.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void shareShoppingList() {
        if (firebaseListId != null) {
            Toast.makeText(this, "Cannot share a shared list.", Toast.LENGTH_SHORT).show();
            return;
        }
        ShoppingList shoppingList = shoppingListRepository.getShoppingListById(currentShoppingListId);
        if (shoppingList == null) {
            Toast.makeText(this, R.string.list_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        shoppingListRepository.getItemsForListId(currentShoppingListId, items -> {
            try {
                org.json.JSONObject jsonList = new org.json.JSONObject();
                jsonList.put("app_id", "com.example.einkaufsliste");
                jsonList.put("name", shoppingList.getName());

                org.json.JSONArray jsonItems = new org.json.JSONArray();
                for (ShoppingItem item : items) {
                    org.json.JSONObject jsonItem = new org.json.JSONObject();
                    jsonItem.put("name", item.getName());
                    jsonItem.put("quantity", item.getQuantity());
                    jsonItem.put("unit", item.getUnit());
                    jsonItem.put("done", item.isDone());
                    jsonItem.put("notes", item.getNotes());
                    jsonItem.put("position", item.getPosition());
                    jsonItems.put(jsonItem);
                }
                jsonList.put("items", jsonItems);

                String jsonString = jsonList.toString(2);

                java.io.File file = new java.io.File(getCacheDir(), "list.json");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    fos.write(jsonString.getBytes());
                }

                android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(this, "com.example.einkaufsliste.provider", file);

                android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("application/json");
                shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri);
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.shopping_list) + ": " + shoppingList.getName());
                shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(android.content.Intent.createChooser(shareIntent, getString(R.string.share_list)));

            } catch (org.json.JSONException | java.io.IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error creating share data.", Toast.LENGTH_SHORT).show();
            }
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
                buttonAddItem.setEnabled(hasText);
                buttonAddItem.setAlpha(hasText ? 1.0f : 0.5f);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        View.OnClickListener addItemAction = v -> {
            String name = editTextAddItem.getText().toString().trim();
            if (!name.isEmpty()) {
                int nextPosition = 0;
                if(shoppingItems != null){
                    for (ShoppingItem existingItem : shoppingItems) {
                        if (!existingItem.isDone()) {
                            nextPosition = Math.max(nextPosition, existingItem.getPosition() + 1);
                        }
                    }
                }

                ShoppingItem newItem = new ShoppingItem(name, "1", "", false, currentShoppingListId, "", nextPosition);
                if (firebaseListId != null) {
                    shoppingListRepository.addItemToShoppingList(firebaseListId, newItem);
                    shoppingListRepository.updateListTimestamp(firebaseListId);
                } else {
                    long newId = shoppingListRepository.addItemToShoppingList(currentShoppingListId, newItem);
                    if (newId != -1) {
                        newItem.setId(newId);
                        adapter.addItem(newItem);
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    } else {
                        Toast.makeText(this, R.string.error_adding_item, Toast.LENGTH_SHORT).show();
                    }
                }
                editTextAddItem.setText("");
            }
        };

        buttonAddItem.setOnClickListener(addItemAction);
        editTextAddItem.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (buttonAddItem.isEnabled()) {
                    addItemAction.onClick(v);
                }
                return true;
            }
            return false;
        });
    }

    private void showClearListOptionsDialog() {
        new MaterialAlertDialogBuilder(this, R.style.AppMaterialAlertDialogTheme)
                .setTitle(R.string.dialog_clear_list_title)
                .setMessage(R.string.dialog_clear_list_message)
                .setPositiveButton(R.string.dialog_option_remove_checked, (dialog, which) -> {
                    if (firebaseListId != null) {
                        shoppingListRepository.clearCheckedItemsFromList(firebaseListId);
                        shoppingListRepository.updateListTimestamp(firebaseListId);
                    } else {
                        shoppingListRepository.clearCheckedItemsFromList(currentShoppingListId);
                    }
                    refreshItemList();
                    Toast.makeText(this, "Erledigte Artikel entfernt", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.dialog_option_remove_all, (dialog, which) -> {
                    if (firebaseListId != null) {
                        shoppingListRepository.clearAllItemsFromList(firebaseListId);
                        shoppingListRepository.updateListTimestamp(firebaseListId);
                    } else {
                        shoppingListRepository.clearAllItemsFromList(currentShoppingListId);
                    }
                    refreshItemList();
                    Toast.makeText(this, "Alle Artikel entfernt", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.dialog_option_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void refreshItemList() {
        if (shoppingListRepository == null || adapter == null) return;
        adapter.resetEditingPosition();

        if (firebaseListId != null) {
            shoppingListRepository.getItemsForListId(firebaseListId, loadedItems -> {
                this.shoppingItems = loadedItems;
                adapter.setItems(shoppingItems);
                checkEmptyViewItems(shoppingItems.isEmpty());
            });
        } else {
            shoppingListRepository.getItemsForListId(currentShoppingListId, loadedItems -> {
                this.shoppingItems = loadedItems;
                adapter.setItems(shoppingItems);
                checkEmptyViewItems(shoppingItems.isEmpty());
            });
        }
    }

    private void checkEmptyViewItems(boolean isEmpty) {
        if (emptyView != null) {
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    @Override
    public void onItemCheckboxChanged(ShoppingItem item, boolean isChecked) {
        if (firebaseListId != null) {
            shoppingListRepository.toggleItemChecked(firebaseListId, item.getFirebaseId(), isChecked);
            shoppingListRepository.updateListTimestamp(firebaseListId);
        } else {
            shoppingListRepository.toggleItemChecked(item.getId(), isChecked);
        }
        refreshItemList();
    }

    @Override
    public void onDataSetChanged() {
        if (adapter != null) {
            checkEmptyViewItems(adapter.getCurrentItems().isEmpty());
        }
    }

    @Override
    public void requestItemResort() {
        refreshItemList();
    }

    @Override
    public View getCoordinatorLayout() {
        return activityRootView;
    }

    @Override
    public View getSnackbarAnchorView() {
        return findViewById(R.id.add_item_bar_container);
    }
}