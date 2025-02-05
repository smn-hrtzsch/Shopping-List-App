package com.example.einkaufsliste;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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

        try {
            shoppingLists = repository.getAllShoppingLists();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_loading_lists), Toast.LENGTH_SHORT).show();
            return;
        }

        RecyclerView recyclerView = findViewById(R.id.listRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(false);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new ListRecyclerViewAdapter(this, shoppingLists, repository);
        recyclerView.setAdapter(adapter);

        EditText listNameInput = findViewById(R.id.listNameInput);
        ImageView addListButton = findViewById(R.id.addListButton);

        addListButton.setOnClickListener(v -> addShoppingList(listNameInput));

        listNameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                if (!listNameInput.getText().toString().trim().isEmpty()) {
                    addShoppingList(listNameInput);
                }
                return true;
            }
            return false;
        });
    }

    private void addShoppingList(EditText listNameInput) {
        String listName = listNameInput.getText().toString().trim();
        if (listName.isEmpty()) {
            Toast.makeText(this, getString(R.string.invalid_list_name), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            long id = repository.addShoppingList(listName);
            if (id > 0) {
                ShoppingList newList = new ShoppingList(id, listName);
                shoppingLists.add(newList);
                adapter.notifyItemInserted(shoppingLists.size() - 1);
                listNameInput.setText("");
            } else {
                Toast.makeText(this, getString(R.string.error_adding_list), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_adding_list), Toast.LENGTH_SHORT).show();
        }
    }

    public void showDeleteListDialog(ShoppingList list) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_delete, null);
        builder.setView(dialogView);

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        TextView message = dialogView.findViewById(R.id.dialogMessage);
        TextView positiveButton = dialogView.findViewById(R.id.positiveButton);
        TextView negativeButton = dialogView.findViewById(R.id.negativeButton);

        title.setText(getString(R.string.delete_list_title));
        message.setText(getString(R.string.delete_list_message, list.getName()));

        AlertDialog dialog = builder.create();

        positiveButton.setOnClickListener(v -> {
            try {
                repository.deleteShoppingList(list.getId());
                int position = shoppingLists.indexOf(list);
                if (position >= 0) {
                    shoppingLists.remove(position);
                    adapter.notifyDataSetChanged();
                }
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.error_deleting_list), Toast.LENGTH_SHORT).show();
            }
        });

        negativeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
