// EinkaufslisteActivity.java
package com.example.einkaufsliste;

import com.example.einkaufsliste.R;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
// LayoutInflater wird nicht mehr für den Add-Dialog benötigt
import android.view.View; // Für Snackbar Root View
import android.widget.EditText; // Wird nicht mehr direkt hier benötigt
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
    // private FloatingActionButton fabAddItem; // Entfernt
    private FloatingActionButton fabClearList;
    private ShoppingList currentShoppingList;
    private TextView toolbarTitleTextView;

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

        Toolbar toolbar = findViewById(R.id.toolbar_einkaufsliste);
        setSupportActionBar(toolbar);
        toolbarTitleTextView = findViewById(R.id.toolbar_title_einkaufsliste);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        shoppingListRepository = new ShoppingListRepository(this);
        currentShoppingListId = getIntent().getLongExtra("LIST_ID", -1L);

        if (currentShoppingListId == -1L) {
            // Verwende einen passenden String aus deiner strings.xml
            Toast.makeText(this, R.string.list_not_found, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        currentShoppingList = shoppingListRepository.getShoppingListById(currentShoppingListId);
        if (currentShoppingList == null) {
            Toast.makeText(this, getString(R.string.list_not_found) + " (ID: " + currentShoppingListId + ")", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (toolbarTitleTextView != null) {
            toolbarTitleTextView.setText(currentShoppingList.getName());
        }

        shoppingItems = shoppingListRepository.getItemsForListId(currentShoppingListId);
        if (shoppingItems == null) {
            shoppingItems = new ArrayList<>();
            Toast.makeText(this, R.string.error_loading_lists, Toast.LENGTH_SHORT).show(); // Beispiel für einen passenden String
        }

        recyclerView = findViewById(R.id.item_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyRecyclerViewAdapter(this, shoppingItems, shoppingListRepository, currentShoppingListId, this);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        // fabAddItem und sein Listener wurden entfernt

        fabClearList = findViewById(R.id.button_clear_list_new_position);
        fabClearList.setOnClickListener(v -> showClearListOptionsDialog());
    }

    // showAddItemDialog() wird nicht mehr benötigt, da wir den Inline-Editor haben
    /*
    private void showAddItemDialog() {
        // ... alter Code ...
    }
    */

    private void showClearListOptionsDialog() {
        // Titel und Nachricht für den Dialog - diese Strings müssen in deiner strings.xml sein!
        // Basierend auf deinem Wunschtext:
        // <string name="clear_list_dialog_title">Möchtest du wirklich die Liste Leeren?</string>
        // <string name="clear_list_dialog_message">Wähle eine Option:</string>
        // <string name="clear_list_option_checked">Ja, abgehakte Artikel entfernen</string>
        // <string name="clear_list_option_all">Ja, alle Artikel entfernen</string>
        // <string name="clear_list_option_cancel">Nein, abbrechen</string>
        // (Ich nehme an, du hast ähnliche Strings oder kannst sie anlegen)

        AlertDialog.Builder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this); // Neue Zeile
        builder.setTitle(R.string.dialog_clear_list_title) // Ersetze mit "Möchtest du wirklich die Liste Leeren?" String
                .setMessage(R.string.dialog_clear_list_message); // Ersetze mit "Wähle eine Option:" String

        // Button-Reihenfolge: 1. Abgehakte, 2. Alle, 3. Abbrechen
        // Android platziert Buttons oft in der Reihenfolge: Positive, Negative, Neutral
        // Wir verwenden hier Positive für "Abgehakte", Neutral für "Alle", Negative für "Abbrechen".

        // 1. Button: "Ja, abgehakte Artikel entfernen" (z.B. Positive Action)
        builder.setPositiveButton(R.string.dialog_option_remove_checked , (dialog, which) -> {
            shoppingListRepository.clearCheckedItemsFromList(currentShoppingListId);
            refreshItemList();
            Toast.makeText(EinkaufslisteActivity.this, "Erledigte Artikel entfernt", Toast.LENGTH_SHORT).show(); // Ggf. eigenen String
        });

        // 2. Button: "Ja, alle Artikel entfernen" (z.B. Neutral Action)
        // In der Standard-Darstellung ist der Neutral-Button oft links oder mittig.
        // Wenn die Reihenfolge wichtig ist, könnten wir hier setNeutralButton oder setNegativeButton vertauschen,
        // aber die semantische Bedeutung (Positive=Hauptaktion, Negative=Abbruch) ist meist wichtiger.
        builder.setNeutralButton(R.string.dialog_option_remove_all, (dialog, which) -> {
            shoppingListRepository.clearAllItemsFromList(currentShoppingListId);
            refreshItemList();
            Toast.makeText(EinkaufslisteActivity.this, "Alle Artikel entfernt", Toast.LENGTH_SHORT).show(); // Ggf. eigenen String
        });

        // 3. Button: "Nein, abbrechen" (z.B. Negative Action)
        builder.setNegativeButton(R.string.dialog_option_cancel, (dialog, which) -> {
            dialog.dismiss();
        });

        builder.create().show();
    }

    private void refreshItemList() {
        if (shoppingListRepository == null || adapter == null) {
            Log.e("EinkaufslisteActivity", "refreshItemList: Repository oder Adapter ist null.");
            return;
        }
        // Vor dem Aktualisieren die aktuell bearbeitete Position im Adapter zurücksetzen
        if (adapter != null) {
            adapter.resetEditingPosition();
        }

        List<ShoppingItem> updatedItems = shoppingListRepository.getItemsForListId(currentShoppingListId);
        if (updatedItems != null) {
            adapter.setItems(updatedItems);
        } else {
            Log.w("EinkaufslisteActivity", "refreshItemList: updatedItems ist null.");
            Toast.makeText(this, R.string.error_loading_lists, Toast.LENGTH_SHORT).show();
            adapter.setItems(new ArrayList<>());
        }
        checkEmptyViewItems(updatedItems == null || updatedItems.isEmpty());
    }

    private void checkEmptyViewItems(boolean isEmpty) {
        TextView emptyView = findViewById(R.id.empty_view_items);
        if (emptyView != null) {
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
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
        // Wird vom Adapter aufgerufen, requestItemResort sollte die Aktualisierung triggern
    }

    @Override
    public void onDataSetChanged() {
        // Wird vom Adapter aufgerufen, wenn sich die Datenmenge ändert (z.B. nach Löschen).
        // Hier könnten wir auch checkEmptyViewItems aufrufen.
        if (shoppingItems != null) { // shoppingItems sollte hier bereits vom Adapter aktualisiert sein
            checkEmptyViewItems(shoppingItems.isEmpty());
        }
    }

    @Override
    public void requestItemResort() {
        refreshItemList();
    }
}