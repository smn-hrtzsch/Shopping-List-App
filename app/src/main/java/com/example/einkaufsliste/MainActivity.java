// main/java/com/example/einkaufsliste/MainActivity.java
package com.example.einkaufsliste;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager; // Import für die Tastatursteuerung
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ListRecyclerViewAdapter.OnListInteractionListener {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "prefs_theme";

    private RecyclerView recyclerView;
    private ListRecyclerViewAdapter adapter;
    private List<ShoppingList> shoppingLists;
    private ShoppingListManager shoppingListManager;
    private TextView emptyView;
    private LinearLayout addListInputLayout;
    private EditText editTextNewListName;
    private ImageButton buttonConfirmAddList;
    private SharedPreferences sharedPreferences;
    private FloatingActionButton fab; // NEU: FAB als Klassenvariable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedTheme = sharedPreferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        shoppingListManager = new ShoppingListManager(this);
        shoppingLists = new ArrayList<>();

        // ... (Datenbank-Initialisierung bleibt gleich) ...

        addListInputLayout = findViewById(R.id.add_list_input_layout);
        editTextNewListName = findViewById(R.id.edit_text_new_list_name);
        buttonConfirmAddList = findViewById(R.id.button_confirm_add_list);
        emptyView = findViewById(R.id.empty_view_main);

        recyclerView = findViewById(R.id.list_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListRecyclerViewAdapter(this, shoppingLists, shoppingListManager, this);
        recyclerView.setAdapter(adapter);

        // NEU: FAB initialisieren
        fab = findViewById(R.id.fab_add_list);
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

        loadShoppingLists();
    }

    // ... (onCreateOptionsMenu, onOptionsItemSelected, toggleTheme bleiben gleich) ...

    @Override
    protected void onResume() {
        super.onResume();
        if (addListInputLayout.getVisibility() == View.VISIBLE) {
            // Sicherstellen, dass der Zustand konsistent ist
            addListInputLayout.setVisibility(View.GONE);
            fab.show();
            editTextNewListName.setText("");
        }
        loadShoppingLists();
    }

    private void loadShoppingLists() {
        shoppingLists = shoppingListManager.getAllShoppingLists();
        adapter.updateLists(shoppingLists);
        checkEmptyView();
    }

    private void checkEmptyView() {
        if (shoppingLists != null && shoppingLists.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
        }
    }

    // ANPASSUNG: Methode überarbeitet
    private void toggleAddListInput() {
        if (addListInputLayout.getVisibility() == View.GONE) {
            // Editor wird geöffnet
            addListInputLayout.setVisibility(View.VISIBLE);
            fab.hide(); // FAB verstecken
            editTextNewListName.requestFocus(); // Fokus auf das Eingabefeld setzen
            // Tastatur öffnen
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editTextNewListName, InputMethodManager.SHOW_IMPLICIT);
        } else {
            // Editor wird geschlossen
            addListInputLayout.setVisibility(View.GONE);
            fab.show(); // FAB wieder anzeigen
            editTextNewListName.setText("");
            // Tastatur schließen
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(editTextNewListName.getWindowToken(), 0);
        }
    }

    // ANPASSUNG: Methode überarbeitet, um die Position zu bestimmen
    private void addNewListFromInput() {
        String listName = editTextNewListName.getText().toString().trim();
        if (!listName.isEmpty()) {
            // NEU: Position für die neue Liste bestimmen (immer am Ende)
            int nextPosition = 0;
            if (!shoppingLists.isEmpty()) {
                // Die Position ist die Größe der aktuellen Liste,
                // da die Zählung bei 0 beginnt (z.B. bei 3 Listen ist die letzte Position 2, die neue wird 3)
                nextPosition = shoppingLists.size();
            }

            long newId = shoppingListManager.addShoppingList(listName, nextPosition);

            if (newId != -1) {
                loadShoppingLists(); // Lädt die Listen neu, inklusive der neuen am Ende
                Toast.makeText(MainActivity.this, "Liste \"" + listName + "\" erstellt", Toast.LENGTH_SHORT).show();

                // Editor schließen und Tastatur verstecken (wie in toggleAddListInput)
                addListInputLayout.setVisibility(View.GONE);
                fab.show();
                editTextNewListName.setText("");
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editTextNewListName.getWindowToken(), 0);

            } else {
                Toast.makeText(MainActivity.this, R.string.error_adding_list, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, R.string.invalid_list_name, Toast.LENGTH_SHORT).show();
        }
    }

    // ... (Rest der Klasse bleibt unverändert) ...
    @Override
    public void onListClicked(ShoppingList list) {
        Intent intent = new Intent(MainActivity.this, EinkaufslisteActivity.class);
        intent.putExtra("LIST_ID", list.getId());
        intent.putExtra("LIST_NAME", list.getName());
        startActivity(intent);
    }

    @Override
    public void onListDataSetChanged() {
        checkEmptyView();
        Log.d("MainActivity", "onListDataSetChanged called, only checked empty view.");
    }

    @Override
    public void requestListDeleteConfirmation(final ShoppingList listToDelete) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button positiveButton = dialogView.findViewById(R.id.positiveButton);
        Button negativeButton = dialogView.findViewById(R.id.negativeButton);

        if (dialogTitle == null || dialogMessage == null || positiveButton == null || negativeButton == null) {
            Log.e("MainActivity", "Could not find all views in dialog_delete.xml!");
            Toast.makeText(this, "Error showing dialog (Layout-Problem).", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            dialogTitle.setText(R.string.confirm_delete_list_title);
            dialogMessage.setText(getString(R.string.confirm_delete_list_message, listToDelete.getName()));
            positiveButton.setText(R.string.delete);
            negativeButton.setText(R.string.cancel);
        } catch (android.content.res.Resources.NotFoundException e) {
            Log.e("MainActivity", "Missing string resource for delete dialog", e);
            Toast.makeText(this, "Text resource for dialog is missing.", Toast.LENGTH_SHORT).show();
            dialogTitle.setText("Delete list?");
            dialogMessage.setText("Do you really want to delete the list '" + listToDelete.getName() + "'?");
            positiveButton.setText("Delete");
            negativeButton.setText("Cancel");
        }

        positiveButton.setOnClickListener(v -> {
            try {
                shoppingListManager.deleteShoppingList(listToDelete.getId());
                Toast.makeText(MainActivity.this, "Liste \"" + listToDelete.getName() + "\" gelöscht", Toast.LENGTH_SHORT).show();
                loadShoppingLists();
            } catch (Exception e) {
                Log.e("MainActivity", "Error executing deleteShoppingList for ID: " + listToDelete.getId(), e);
                Toast.makeText(MainActivity.this, "Error deleting list.", Toast.LENGTH_SHORT).show();
            } finally {
                dialog.dismiss();
            }
        });

        negativeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}