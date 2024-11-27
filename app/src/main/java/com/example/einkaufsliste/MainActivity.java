package com.example.einkaufsliste;

import android.os.Bundle;
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

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ShoppingListRepository repository;
    private ListRecyclerViewAdapter adapter;
    private List<ShoppingList> shoppingLists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        repository = new ShoppingListRepository(this);
        shoppingLists = repository.getAllShoppingLists();

        RecyclerView recyclerView = findViewById(R.id.listRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(false); // Ändern der Layout-Richtung
        layoutManager.setStackFromEnd(true); // Items von unten nach oben
        recyclerView.setLayoutManager(layoutManager);

        adapter = new ListRecyclerViewAdapter(this, shoppingLists, repository);
        recyclerView.setAdapter(adapter);

        EditText listNameInput = findViewById(R.id.listNameInput);
        ImageView addListButton = findViewById(R.id.addListButton);

        addListButton.setOnClickListener(v -> addShoppingList(listNameInput));

        listNameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                addShoppingList(listNameInput);
                return true;
            }
            return false;
        });
    }

    private void addShoppingList(EditText listNameInput) {
        String listName = listNameInput.getText().toString();
        if (!listName.isEmpty()) {
            long id = repository.addShoppingList(listName);
            ShoppingList newList = new ShoppingList(id, listName);
            shoppingLists.add(newList);
            adapter.notifyDataSetChanged();
            listNameInput.setText("");
        }
    }

    public void showDeleteListDialog(ShoppingList list) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_custom, null);
        builder.setView(dialogView);

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        TextView message = dialogView.findViewById(R.id.dialogMessage);
        Button positiveButton = dialogView.findViewById(R.id.positiveButton);
        Button negativeButton = dialogView.findViewById(R.id.negativeButton);

        title.setText("Liste löschen");
        message.setText("Möchten Sie die Liste '" + list.getName() + "' wirklich löschen?");

        AlertDialog dialog = builder.create();

        positiveButton.setOnClickListener(v -> {
            repository.deleteShoppingList(list.getId());
            shoppingLists.removeIf(l -> l.getId() == list.getId());
            adapter.notifyDataSetChanged();
            Toast.makeText(this, list.getName() + " gelöscht", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

}
