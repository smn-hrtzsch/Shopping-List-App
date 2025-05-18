// EinkaufslisteActivity.java
package com.example.einkaufsliste;

// Stelle sicher, dass dieser Import korrekt ist und nicht rot unterstrichen wird.
import com.example.einkaufsliste.R;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
// Stelle sicher, dass diese Klasse im Paket com.example.einkaufsliste liegt
import com.example.einkaufsliste.SimpleItemTouchHelperCallback;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class EinkaufslisteActivity extends AppCompatActivity implements MyRecyclerViewAdapter.OnItemInteractionListener {

    private RecyclerView recyclerView;
    private MyRecyclerViewAdapter adapter;
    private List<ShoppingItem> shoppingItems;
    private ShoppingListRepository shoppingListRepository;
    private long currentShoppingListId = -1L;
    private FloatingActionButton fabAddItem;
    private FloatingActionButton fabClearList;
    private ShoppingList currentShoppingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_einkaufsliste);
        } catch (Exception e) {
            Log.e("EinkaufslisteActivity", "FATAL: Konnte R.layout.activity_einkaufsliste nicht laden. XML-Dateien prüfen!", e);
            Toast.makeText(this, "Layout-Fehler! XML-Dateien prüfen.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        shoppingListRepository = new ShoppingListRepository(this);
        currentShoppingListId = getIntent().getLongExtra("LIST_ID", -1L);

        if (currentShoppingListId == -1L) {
            Toast.makeText(this, "Fehler: Einkaufslisten-ID nicht gefunden.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        currentShoppingList = shoppingListRepository.getShoppingListById(currentShoppingListId);
        if (currentShoppingList == null) {
            Toast.makeText(this, "Fehler: Einkaufsliste (ID: " + currentShoppingListId + ") konnte nicht geladen werden.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setTitle(currentShoppingList.getName());
        shoppingItems = shoppingListRepository.getItemsForListId(currentShoppingListId);
        if (shoppingItems == null) {
            shoppingItems = new ArrayList<>();
            Toast.makeText(this, "Artikel konnten nicht geladen werden (leere Liste wird verwendet).", Toast.LENGTH_SHORT).show();
        }

        recyclerView = findViewById(R.id.item_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyRecyclerViewAdapter(this, shoppingItems, shoppingListRepository, currentShoppingListId, this);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        fabAddItem = findViewById(R.id.fab_add_item);
        fabAddItem.setOnClickListener(view -> showAddItemDialog());

        fabClearList = findViewById(R.id.button_clear_list_new_position);
        fabClearList.setOnClickListener(v -> showClearListOptionsDialog());
    }

    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_item, null); // Dein vorhandenes Layout
        builder.setView(dialogView);

        // Verwende einen String, der in deiner strings.xml existiert (z.B. "Artikel hinzufügen")
        builder.setTitle(R.string.add_item); // <string name="add_item">Artikel hinzufügen</string>

        final EditText itemNameEditText = dialogView.findViewById(R.id.editTextDialog); // ID aus deinem dialog_edit_item.xml

        // Verwende Strings, die in deiner strings.xml existieren
        builder.setPositiveButton(R.string.button_add, (dialog, which) -> { // <string name="button_add">Hinzufügen</string>
            String name = itemNameEditText.getText().toString().trim();
            String quantityStr = "1";
            String unit = "";

            if (name.isEmpty()) {
                // <string name="invalid_item_name">Bitte einen gültigen Artikelnamen eingeben.</string>
                Toast.makeText(EinkaufslisteActivity.this, R.string.invalid_item_name, Toast.LENGTH_SHORT).show();
                return;
            }

            ShoppingItem newItem = new ShoppingItem(name, quantityStr, unit, false, currentShoppingListId, "", 0);
            long itemId = shoppingListRepository.addItemToShoppingList(currentShoppingListId, newItem);

            if (itemId != -1) {
                newItem.setId(itemId);
                refreshItemList();
                // <string name="item_added">Artikel erfolgreich hinzugefügt.</string>
                Toast.makeText(EinkaufslisteActivity.this, R.string.item_added, Toast.LENGTH_SHORT).show();
            } else {
                // <string name="error_adding_item">Fehler beim Hinzufügen des Artikels.</string>
                Toast.makeText(EinkaufslisteActivity.this, R.string.error_adding_item, Toast.LENGTH_SHORT).show();
            }
        });
        // <string name="button_cancel">Abbrechen</string>
        builder.setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showClearListOptionsDialog() {
        new AlertDialog.Builder(this)
                // Annahme: Du hast passende Strings oder kannst sie anlegen/umwidmen
                .setTitle(R.string.clear) // <string name="clear">Leeren</string>
                .setMessage(R.string.confirm_clear_all_items_message) // Wiederverwendung oder neuen String "Erledigte oder alle Artikel entfernen?"
                .setPositiveButton(R.string.button_clear_list_done, (dialog, which) -> { // <string name="button_clear_list_done">Erledigte löschen</string>
                    shoppingListRepository.clearCheckedItemsFromList(currentShoppingListId);
                    refreshItemList();
                    // Eigenen String für "Erledigte Artikel entfernt" Toast erstellen oder wiederverwenden
                    Toast.makeText(EinkaufslisteActivity.this, "Erledigte Artikel entfernt", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.button_clear_list_all, (dialog, which) -> { // <string name="button_clear_list_all">Alle Artikel löschen</string>
                    shoppingListRepository.clearAllItemsFromList(currentShoppingListId);
                    refreshItemList();
                    // Eigenen String für "Alle Artikel entfernt" Toast erstellen oder wiederverwenden
                    Toast.makeText(EinkaufslisteActivity.this, "Alle Artikel entfernt", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.button_cancel, null) // <string name="button_cancel">Abbrechen</string>
                .show();
    }

    private void refreshItemList() {
        if (shoppingListRepository == null || adapter == null) {
            Log.e("EinkaufslisteActivity", "refreshItemList: Repository oder Adapter ist null.");
            return;
        }
        List<ShoppingItem> updatedItems = shoppingListRepository.getItemsForListId(currentShoppingListId);
        if (updatedItems != null) {
            adapter.setItems(updatedItems);
        } else {
            Log.w("EinkaufslisteActivity", "refreshItemList: updatedItems ist null.");
            Toast.makeText(this, "Fehler beim Aktualisieren der Artikelliste.", Toast.LENGTH_SHORT).show();
            adapter.setItems(new ArrayList<>());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshItemList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            refreshItemList();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onItemCheckboxChanged(ShoppingItem item, boolean isChecked) {
        // Die Logik (DB-Update) ist im Adapter/Repository.
        // requestItemResort wird vom Adapter aufgerufen, um die UI zu aktualisieren.
    }

    @Override
    public void onDataSetChanged() {
        // Kann verwendet werden, um z.B. eine "Empty View" Anzeige zu aktualisieren,
        // nachdem Items gelöscht wurden.
    }

    @Override
    public void requestItemResort() {
        refreshItemList();
    }
}