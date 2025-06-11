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
    private TextView emptyView; // Hinzugefügt für einfacheren Zugriff

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
        emptyView = findViewById(R.id.empty_view_items); // Initialisieren von emptyView

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

        fabClearList = findViewById(R.id.button_clear_list_new_position);
        fabClearList.setOnClickListener(v -> showClearListOptionsDialog());

        // Stelle sicher, dass der RecyclerView immer sichtbar ist,
        // da die "Hinzufügen"-Zeile Teil davon ist.
        recyclerView.setVisibility(View.VISIBLE);
        checkEmptyViewItems(shoppingItems.isEmpty()); // Aufruf, um ggf. die "Leer"-Nachricht initial anzuzeigen
    }

    private void showClearListOptionsDialog() {
        AlertDialog.Builder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.dialog_clear_list_title)
                .setMessage(R.string.dialog_clear_list_message);

        builder.setPositiveButton(R.string.dialog_option_remove_checked , (dialog, which) -> {
            shoppingListRepository.clearCheckedItemsFromList(currentShoppingListId);
            refreshItemList();
            Toast.makeText(EinkaufslisteActivity.this, "Erledigte Artikel entfernt", Toast.LENGTH_SHORT).show();
        });

        builder.setNeutralButton(R.string.dialog_option_remove_all, (dialog, which) -> {
            shoppingListRepository.clearAllItemsFromList(currentShoppingListId);
            refreshItemList();
            Toast.makeText(EinkaufslisteActivity.this, "Alle Artikel entfernt", Toast.LENGTH_SHORT).show();
        });

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
        if (adapter != null) {
            adapter.resetEditingPosition();
        }

        List<ShoppingItem> updatedItems = shoppingListRepository.getItemsForListId(currentShoppingListId);
        if (updatedItems != null) {
            shoppingItems = updatedItems; // shoppingItems aktualisieren
            adapter.setItems(updatedItems);
        } else {
            Log.w("EinkaufslisteActivity", "refreshItemList: updatedItems ist null.");
            Toast.makeText(this, R.string.error_loading_lists, Toast.LENGTH_SHORT).show();
            shoppingItems = new ArrayList<>(); // shoppingItems leeren
            adapter.setItems(new ArrayList<>());
        }
        // Rufe checkEmptyViewItems mit der korrekten Information auf, ob die *Datenliste* leer ist.
        // Der Adapter hat immer +1 für die Add-Zeile, daher dürfen wir nicht adapter.getItemCount() == 0 prüfen.
        checkEmptyViewItems(shoppingItems.isEmpty());
    }

    /**
     * Zeigt oder verbirgt die "Liste ist leer"-Nachricht.
     * Der RecyclerView selbst bleibt sichtbar, da er die "Artikel hinzufügen"-Zeile enthält.
     * @param isEmpty true, wenn die Datenliste der Artikel leer ist, sonst false.
     */
    private void checkEmptyViewItems(boolean isEmpty) {
        if (emptyView != null) {
            // Zeige die "Leer"-Nachricht, wenn keine *echten* Artikel da sind.
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            // Der RecyclerView bleibt sichtbar, damit die "Hinzufügen"-Zeile immer da ist.
            // Die Sichtbarkeit des RecyclerViews wird jetzt in onCreate() einmalig auf VISIBLE gesetzt.
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
        // shoppingItems sollte hier bereits vom Adapter aktualisiert sein.
        // Prüfe, ob die *reine Datenliste* (ohne Add-Zeile) leer ist.
        if (adapter != null) {
            // Wir brauchen die tatsächliche Anzahl der ShoppingItems, nicht adapter.getItemCount()
            List<ShoppingItem> currentAdapterItems = adapter.getCurrentItems(); // Du müsstest eine Methode im Adapter hinzufügen, um die reine Item-Liste zu bekommen
            checkEmptyViewItems(currentAdapterItems.isEmpty());
        }
    }

    @Override
    public void requestItemResort() {
        refreshItemList();
    }
}