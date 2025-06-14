package com.example.einkaufsliste;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class EinkaufslisteActivity extends AppCompatActivity implements MyRecyclerViewAdapter.OnItemInteractionListener {

    private RecyclerView recyclerView;
    private MyRecyclerViewAdapter adapter;
    private List<ShoppingItem> shoppingItems;
    private ShoppingListRepository shoppingListRepository;
    private long currentShoppingListId = -1L;
    private TextView toolbarTitleTextView;
    private TextView emptyView;
    private View activityRootView; // Geändert von CoordinatorLayout

    // Views für die Eingabezeile
    private EditText editTextAddItem;
    private ImageButton buttonAddItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_einkaufsliste);

        Toolbar toolbar = findViewById(R.id.toolbar_einkaufsliste);
        setSupportActionBar(toolbar);
        toolbarTitleTextView = findViewById(R.id.toolbar_title_einkaufsliste);
        emptyView = findViewById(R.id.empty_view_items);
        activityRootView = findViewById(R.id.einkaufsliste_activity_root);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        shoppingListRepository = new ShoppingListRepository(this);
        currentShoppingListId = getIntent().getLongExtra("LIST_ID", -1L);

        if (currentShoppingListId == -1L) {
            Toast.makeText(this, R.string.list_not_found, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ShoppingList currentShoppingList = shoppingListRepository.getShoppingListById(currentShoppingListId);
        if (currentShoppingList == null) {
            Toast.makeText(this, getString(R.string.list_not_found) + " (ID: " + currentShoppingListId + ")", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (toolbarTitleTextView != null) {
            toolbarTitleTextView.setText(currentShoppingList.getName());
        }

        shoppingItems = new ArrayList<>();
        recyclerView = findViewById(R.id.item_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyRecyclerViewAdapter(this, shoppingItems, shoppingListRepository, this);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        // FAB-Logik wurde entfernt

        setupAddItemBar();
        refreshItemList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.einkaufsliste_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_list) {
            showClearListOptionsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupAddItemBar() {
        editTextAddItem = findViewById(R.id.edit_text_add_item_bar);
        buttonAddItem = findViewById(R.id.button_add_item_bar);

        // Der Indikator-Strich wird im neuen Design nicht mehr benötigt

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
                long newId = shoppingListRepository.addItemToShoppingList(currentShoppingListId, newItem);

                if (newId != -1) {
                    refreshItemList();
                    editTextAddItem.setText("");
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editTextAddItem.getWindowToken(), 0);
                } else {
                    Toast.makeText(this, R.string.error_adding_item, Toast.LENGTH_SHORT).show();
                }
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
                    shoppingListRepository.clearCheckedItemsFromList(currentShoppingListId);
                    refreshItemList();
                    Toast.makeText(this, "Erledigte Artikel entfernt", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.dialog_option_remove_all, (dialog, which) -> {
                    shoppingListRepository.clearAllItemsFromList(currentShoppingListId);
                    refreshItemList();
                    Toast.makeText(this, "Alle Artikel entfernt", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.dialog_option_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void refreshItemList() {
        if (shoppingListRepository == null || adapter == null) return;

        adapter.resetEditingPosition();

        List<ShoppingItem> updatedItems = shoppingListRepository.getItemsForListId(currentShoppingListId);
        if (updatedItems != null) {
            shoppingItems = updatedItems;
            adapter.setItems(updatedItems);
        } else {
            shoppingItems = new ArrayList<>();
            adapter.setItems(new ArrayList<>());
            Toast.makeText(this, R.string.error_loading_lists, Toast.LENGTH_SHORT).show();
        }
        checkEmptyViewItems(shoppingItems.isEmpty());
    }

    private void checkEmptyViewItems(boolean isEmpty) {
        if (emptyView != null) {
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onItemCheckboxChanged(ShoppingItem item, boolean isChecked) {
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
}