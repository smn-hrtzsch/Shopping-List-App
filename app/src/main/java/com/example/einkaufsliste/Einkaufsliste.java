package com.example.einkaufsliste;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class Einkaufsliste extends AppCompatActivity {
    private ShoppingListRepository repository;
    private ShoppingList currentList;
    private RecyclerView recyclerView;
    private MyRecyclerViewAdapter adapter;
    private EditText itemInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_einkaufsliste);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        repository = new ShoppingListRepository(this);

        String listName = getIntent().getStringExtra("list_name");
        currentList = null;
        for (ShoppingList list : repository.getAllShoppingLists()) {
            if (list.getName().equals(listName)) {
                currentList = list;
                break;
            }
        }

        if (currentList == null) {
            Toast.makeText(this, getString(R.string.list_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView title = findViewById(R.id.textView2);
        title.setText(currentList.getName());

        recyclerView = findViewById(R.id.recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(false);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        currentList.setItems(repository.getItemsForList(currentList.getId()));
        adapter = new MyRecyclerViewAdapter(this, currentList.getItems(), repository, recyclerView, currentList.getId());
        recyclerView.setAdapter(adapter);

        // Drag & Drop für Items aktivieren
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        itemInput = findViewById(R.id.itemInput);
        ImageView addItemButton = findViewById(R.id.addItemButton);

        addItemButton.setOnClickListener(v -> addItem());

        itemInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String itemName = itemInput.getText().toString().trim();
                if (!itemName.isEmpty()) {
                    addItem();
                } else {
                    Toast.makeText(this, getString(R.string.invalid_item_name), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        FloatingActionButton clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(view -> showClearListDialog());
    }

    private void addItem() {
        String itemName = itemInput.getText().toString().trim();

        if (itemName.isEmpty()) {
            Toast.makeText(this, getString(R.string.invalid_item_name), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            long itemId = repository.addShoppingItem(currentList.getId(), itemName);
            if (itemId > 0) {
                ShoppingItem newItem = new ShoppingItem(itemId, itemName, false, currentList.getItems().size());
                currentList.getItems().add(newItem);

                // Nach dem Hinzufügen die Items neu gruppieren, damit alle abgehakten Items unten stehen.
                adapter.resortItemsByCompletion();

                recyclerView.scrollToPosition(currentList.getItems().size() - 1);
                itemInput.setText("");
            } else {
                Toast.makeText(this, getString(R.string.error_adding_item), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_adding_item), Toast.LENGTH_SHORT).show();
        }
    }

    private void showClearListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_clear, null);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button positiveButton = dialogView.findViewById(R.id.positiveButton);
        Button negativeButton = dialogView.findViewById(R.id.negativeButton);

        dialogTitle.setText(getString(R.string.clear_list_title));
        dialogMessage.setText(getString(R.string.clear_list_message, currentList.getName()));

        AlertDialog alertDialog = builder.create();

        positiveButton.setOnClickListener(v -> {
            try {
                int itemCount = currentList.getItems().size();
                for (ShoppingItem item : currentList.getItems()) {
                    repository.deleteShoppingItem(item.getId());
                }
                currentList.clearItems();
                adapter.notifyItemRangeRemoved(0, itemCount);
                alertDialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.error_clearing_list), Toast.LENGTH_SHORT).show();
            }
        });

        negativeButton.setOnClickListener(v -> alertDialog.dismiss());

        alertDialog.show();

        // Berechne die Breite: Bildschirmbreite minus 2 * 10dp (für links und rechts)
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int margin = dpToPx(10);  // 10dp in Pixel umrechnen
        int dialogWidth = screenWidth - (2 * margin);

        // Setze die Fensterbreite des Dialogs
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
