// MainActivity.java
package com.example.einkaufsliste;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
// import android.text.InputType; // Nicht mehr für Dialog benötigt
// import android.view.LayoutInflater; // Nicht mehr für Dialog benötigt
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton; // Import für ImageButton
import android.widget.LinearLayout; // Import für LinearLayout
import android.widget.TextView;
import android.widget.Toast;
import android.database.sqlite.SQLiteException;
import android.util.Log;

// import androidx.appcompat.app.AlertDialog; // Nicht mehr benötigt
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import für Toolbar
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ListRecyclerViewAdapter.OnListInteractionListener {

    private RecyclerView recyclerView;
    private ListRecyclerViewAdapter adapter;
    private List<ShoppingList> shoppingLists;
    private ShoppingListManager shoppingListManager;
    private TextView emptyView;
    private LinearLayout addListInputLayout;
    private EditText editTextNewListName;
    private ImageButton buttonConfirmAddList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        shoppingListManager = new ShoppingListManager(this);
        shoppingLists = new ArrayList<>();

        try {
            shoppingListManager.getWritableDatabase().close();
            Log.d("MainActivity", "Database opened/upgraded successfully on startup.");
        } catch (SQLiteException e) {
            Log.e("MainActivity", "FATAL: Error getting writable database on startup", e);
            Toast.makeText(this, "Datenbankfehler beim Start. Bitte App neu installieren.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        addListInputLayout = findViewById(R.id.add_list_input_layout);
        editTextNewListName = findViewById(R.id.edit_text_new_list_name);
        buttonConfirmAddList = findViewById(R.id.button_confirm_add_list);

        recyclerView = findViewById(R.id.list_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListRecyclerViewAdapter(this, shoppingLists, shoppingListManager, this);
        recyclerView.setAdapter(adapter);

        emptyView = findViewById(R.id.empty_view_main);

        FloatingActionButton fab = findViewById(R.id.fab_add_list);
        fab.setOnClickListener(view -> toggleAddListInput());

        buttonConfirmAddList.setOnClickListener(v -> addNewListFromInput());
        editTextNewListName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addNewListFromInput();
                return true;
            }
            return false;
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        loadShoppingLists(); // Zeile 70 in deinem Logcat (kann leicht variieren)
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (addListInputLayout.getVisibility() == View.VISIBLE) {
            addListInputLayout.setVisibility(View.GONE);
            editTextNewListName.setText(""); // Sicherstellen, dass das Feld leer ist
        }
        loadShoppingLists();
    }

    private void loadShoppingLists() { // Zeile 81 in deinem Logcat
        shoppingLists = shoppingListManager.getAllShoppingLists();
        adapter.updateLists(shoppingLists); // Ruft intern interactionListener.onListDataSetChanged() auf
        // checkEmptyView() wird jetzt zuverlässig von onListDataSetChanged aufgerufen
    }

    private void checkEmptyView() {
        // Stelle sicher, dass shoppingLists initialisiert ist, bevor isEmpty() aufgerufen wird.
        // shoppingLists wird in onCreate initialisiert und in loadShoppingLists neu zugewiesen.
        if (shoppingLists != null && shoppingLists.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
        }
    }

    private void toggleAddListInput() {
        if (addListInputLayout.getVisibility() == View.GONE) {
            addListInputLayout.setVisibility(View.VISIBLE);
            editTextNewListName.requestFocus();
        } else {
            addListInputLayout.setVisibility(View.GONE);
            editTextNewListName.setText("");
        }
    }

    private void addNewListFromInput() {
        String listName = editTextNewListName.getText().toString().trim();
        if (!listName.isEmpty()) {
            long newId = shoppingListManager.addShoppingList(listName);
            if (newId != -1) {
                loadShoppingLists(); // Lade die Listen neu, um die neue Liste und den korrekten Zähler anzuzeigen
                Toast.makeText(MainActivity.this, "Liste \"" + listName + "\" erstellt", Toast.LENGTH_SHORT).show();
                addListInputLayout.setVisibility(View.GONE);
                editTextNewListName.setText("");
            } else {
                Toast.makeText(MainActivity.this, R.string.error_adding_list, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, R.string.invalid_list_name, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onListClicked(ShoppingList list) {
        Intent intent = new Intent(MainActivity.this, EinkaufslisteActivity.class);
        intent.putExtra("LIST_ID", list.getId());
        intent.putExtra("LIST_NAME", list.getName());
        startActivity(intent);
    }

    @Override
    public void onListDataSetChanged() { // Zeile 147 in deinem Logcat
        // NICHT loadShoppingLists() hier aufrufen, um die Rekursion zu vermeiden!
        // Der Adapter hat seine Daten bereits. Wir müssen nur die Empty-View aktualisieren.
        checkEmptyView();
        Log.d("MainActivity", "onListDataSetChanged called, only checked empty view.");
    }

    @Override
    public void requestListDeleteConfirmation(final ShoppingList listToDelete) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete, null);

        // Verwende androidx.appcompat.app.AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        // Verwende androidx.appcompat.app.AlertDialog
        final AlertDialog dialog = builder.create();

        // Optional: Transparenten Hintergrund für Dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button positiveButton = dialogView.findViewById(R.id.positiveButton);
        Button negativeButton = dialogView.findViewById(R.id.negativeButton);

        // Sicherheitscheck: Sind alle Views im Layout vorhanden?
        if (dialogTitle == null || dialogMessage == null || positiveButton == null || negativeButton == null) {
            Log.e("MainActivity", "Konnte nicht alle Views in dialog_delete.xml finden!");
            Toast.makeText(this, "Fehler beim Anzeigen des Dialogs (Layout-Problem).", Toast.LENGTH_LONG).show();
            return; // Verhindert Absturz, falls Views fehlen
        }

        // Texte setzen (stelle sicher, dass die Strings in strings.xml existieren)
        try {
            dialogTitle.setText(R.string.confirm_delete_list_title);
            dialogMessage.setText(getString(R.string.confirm_delete_list_message, listToDelete.getName()));
            positiveButton.setText(R.string.delete);
            negativeButton.setText(R.string.cancel);
        } catch (android.content.res.Resources.NotFoundException e) {
            Log.e("MainActivity", "Fehlende String-Ressource für Löschdialog", e);
            Toast.makeText(this, "Textressource für Dialog fehlt.", Toast.LENGTH_SHORT).show();
            // Setze Fallback-Texte oder handle den Fehler anders
            dialogTitle.setText("Liste löschen?"); // Beispiel
            dialogMessage.setText("Möchtest du die Liste '" + listToDelete.getName() + "' wirklich löschen?"); // Beispiel
            positiveButton.setText("Löschen"); // Beispiel
            negativeButton.setText("Abbrechen"); // Beispiel
        }

        // --- KORRIGIERTER LISTENER FÜR POSITIVEN BUTTON ---
        positiveButton.setOnClickListener(v -> {
            try {
                // Rufe die void-Methode direkt auf
                shoppingListManager.deleteShoppingList(listToDelete.getId());

                // Wenn keine Exception auftritt, gehen wir von Erfolg aus
                Toast.makeText(MainActivity.this, "Liste \"" + listToDelete.getName() + "\" gelöscht", Toast.LENGTH_SHORT).show();
                loadShoppingLists(); // Liste neu laden, damit die UI aktualisiert wird

            } catch (Exception e) {
                // Optional, aber empfohlen: Fange mögliche Fehler (z.B. Datenbankfehler) ab
                Log.e("MainActivity", "Fehler beim Ausführen von deleteShoppingList für ID: " + listToDelete.getId(), e);
                Toast.makeText(MainActivity.this, "Fehler beim Löschen der Liste.", Toast.LENGTH_SHORT).show();
            } finally {
                // Schließe den Dialog in jedem Fall (Erfolg oder Fehler)
                dialog.dismiss();
            }
        });

        // Listener für negativen Button (Abbrechen)
        negativeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}