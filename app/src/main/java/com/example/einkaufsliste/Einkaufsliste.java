package com.example.einkaufsliste;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class Einkaufsliste extends AppCompatActivity {
    private static final String TAG = "Einkaufsliste";
    private ShoppingListRepository repository;
    private ShoppingList currentList;
    private RecyclerView recyclerView;
    private MyRecyclerViewAdapter adapter;
    private EditText itemInput;
    private ImageView addItemButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_einkaufsliste);

        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }

            repository = new ShoppingListRepository(this);

            String listName = getIntent().getStringExtra("list_name");
            currentList = repository.getAllShoppingLists().stream()
                    .filter(list -> list.getName().equals(listName))
                    .findFirst()
                    .orElse(null);

            if (currentList == null) {
                finish(); // Beendet die Aktivität, wenn die Liste nicht gefunden wird
                return;
            }

            // Setzen des dynamischen Titels
            TextView title = findViewById(R.id.textView2);
            title.setText(currentList.getName());

            recyclerView = findViewById(R.id.recyclerview);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setReverseLayout(false); // Elemente in normaler Reihenfolge anzeigen
            layoutManager.setStackFromEnd(true); // Starten Sie die Anzeige von unten
            recyclerView.setLayoutManager(layoutManager);

            // Laden der Artikel für die aktuelle Liste
            currentList.setItems(repository.getItemsForList(currentList.getId()));

            adapter = new MyRecyclerViewAdapter(this, currentList.getItems(), repository, recyclerView);
            recyclerView.setAdapter(adapter);

            itemInput = findViewById(R.id.itemInput);
            addItemButton = findViewById(R.id.addItemButton);

            addItemButton.setOnClickListener(v -> {
                String itemName = itemInput.getText().toString();
                if (!itemName.isEmpty()) {
                    long itemId = repository.addShoppingItem(currentList.getId(), itemName);
                    ShoppingItem newItem = new ShoppingItem(itemId, itemName, false, currentList.getItems().size());
                    currentList.getItems().add(newItem); // Element am Ende der Liste hinzufügen
                    adapter.notifyItemInserted(currentList.getItems().size() - 1); // Adapter benachrichtigen
                    recyclerView.scrollToPosition(currentList.getItems().size() - 1); // RecyclerView nach unten scrollen
                    itemInput.setText("");
                }
            });

            itemInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    addItemToList(itemInput);
                    return true;
                }
                return false;
            });

            FloatingActionButton clearButton = findViewById(R.id.clearButton);
            clearButton.setOnClickListener(view -> showClearListDialog());
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }

    private void addItemToList(EditText itemInput) {
        try {
            String itemName = itemInput.getText().toString();
            if (!itemName.isEmpty()) {
                long itemId = repository.addShoppingItem(currentList.getId(), itemName);
                ShoppingItem newItem = new ShoppingItem(itemId, itemName, false, currentList.getItems().size());
                currentList.addItem(newItem);
                adapter.notifyDataSetChanged();
                itemInput.setText("");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in addItemToList", e);
        }
    }

    private void showClearListDialog() {
        // Erstelle den benutzerdefinierten Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_custom, null);
        builder.setView(dialogView);

        // Finde die Views im benutzerdefinierten Layout
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button positiveButton = dialogView.findViewById(R.id.positiveButton);
        Button negativeButton = dialogView.findViewById(R.id.negativeButton);

        // Setze die Texte für die Dialogelemente
        dialogTitle.setText("Liste leeren");
        dialogMessage.setText("Möchten Sie die Liste '" + currentList.getName() + "' wirklich leeren?");
        positiveButton.setText("Leeren");
        negativeButton.setText("Abbrechen");

        // Erstelle und zeige den Dialog
        AlertDialog alertDialog = builder.create();

        // Setze die Klick-Listener für die Buttons
        positiveButton.setOnClickListener(v -> {
            for (ShoppingItem item : currentList.getItems()) {
                repository.deleteShoppingItem(item.getId());
            }
            currentList.clearItems();
            adapter.notifyDataSetChanged();
            Toast.makeText(this, currentList.getName() + " geleert", Toast.LENGTH_SHORT).show();
            alertDialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> alertDialog.dismiss());

        alertDialog.show();
    }

}
