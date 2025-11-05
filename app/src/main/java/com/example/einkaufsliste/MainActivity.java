package com.example.einkaufsliste;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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
    private FloatingActionButton fab;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedTheme = sharedPreferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedTheme);

        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        View activityRootView = findViewById(R.id.main_activity_root);
        fab = findViewById(R.id.fab_add_list);

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        ViewCompat.setOnApplyWindowInsetsListener(activityRootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            // Apply status bar padding to the app bar
            appBarLayout.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

            // Determine the bottom inset (keyboard or nav bar)
            int bottomInset = Math.max(systemBars.bottom, ime.bottom);

            // Apply the bottom inset as padding to the root view.
            // This will shrink the available space from the bottom up.
            v.setPadding(v.getPaddingLeft(), 0, v.getPaddingRight(), bottomInset);

            // The FAB is a child and will be affected by the parent's padding.
            // No need to adjust its margin anymore.

            return insets;
        });

        shoppingListManager = new ShoppingListManager(this);
        shoppingLists = new ArrayList<>();

        addListInputLayout = findViewById(R.id.add_list_input_layout);
        editTextNewListName = findViewById(R.id.edit_text_new_list_name);
        buttonConfirmAddList = findViewById(R.id.button_confirm_add_list);
        emptyView = findViewById(R.id.empty_view_main);

        recyclerView = findViewById(R.id.list_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListRecyclerViewAdapter(this, shoppingLists, shoppingListManager, this);
        recyclerView.setAdapter(adapter);

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

        setupCloseEditorOnTouchOutside();
        loadShoppingLists();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        if (intent.getAction().equals(Intent.ACTION_VIEW) && intent.getData() != null) {
            android.net.Uri dataUri = intent.getData();
            try {
                String jsonString = readTextFromUri(dataUri);
                if (jsonString != null) {
                    importShoppingList(jsonString);
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error reading file.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String readTextFromUri(android.net.Uri uri) throws java.io.IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
             java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }

    private void importShoppingList(String jsonString) {
        try {
            org.json.JSONObject jsonList = new org.json.JSONObject(jsonString);

            if (!"com.example.einkaufsliste".equals(jsonList.optString("app_id"))) {
                Toast.makeText(this, "Incompatible file.", Toast.LENGTH_SHORT).show();
                return;
            }

            String listName = jsonList.getString("name");
            org.json.JSONArray jsonItems = jsonList.getJSONArray("items");

            int nextPosition = shoppingLists.size();
            long listId = shoppingListManager.addShoppingList(listName, nextPosition);

            if (listId != -1) {
                for (int i = 0; i < jsonItems.length(); i++) {
                    org.json.JSONObject jsonItem = jsonItems.getJSONObject(i);
                    String itemName = jsonItem.getString("name");
                    String quantity = jsonItem.optString("quantity", "1");
                    String unit = jsonItem.optString("unit", "");
                    boolean isDone = jsonItem.optBoolean("done", false);
                    String notes = jsonItem.optString("notes", "");
                    int position = jsonItem.optInt("position", i);

                    ShoppingItem newItem = new ShoppingItem(itemName, quantity, unit, isDone, listId, notes, position);
                    shoppingListManager.addItemToShoppingList(listId, newItem);
                }
                loadShoppingLists();
                Toast.makeText(this, getString(R.string.list_imported, listName), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.error_adding_list, Toast.LENGTH_SHORT).show();
            }
        } catch (org.json.JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing shared data.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCloseEditorOnTouchOutside() {
        emptyView.setOnClickListener(v -> {
            if (addListInputLayout.getVisibility() == View.VISIBLE) {
                toggleAddListInput();
            }
        });

        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                // Diese Bedingung stellt sicher, dass der Klick nicht auf den Editor selbst erfolgt
                if (addListInputLayout.getVisibility() == View.VISIBLE && e.getY() > addListInputLayout.getBottom()) {
                    toggleAddListInput();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (addListInputLayout.getVisibility() == View.VISIBLE) {
            toggleAddListInput();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_switch_theme) {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                sharedPreferences.edit().putInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_NO).apply();
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                sharedPreferences.edit().putInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_YES).apply();
            }
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (addListInputLayout.getVisibility() == View.VISIBLE) {
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

    private void toggleAddListInput() {
        if (addListInputLayout.getVisibility() == View.GONE) {
            addListInputLayout.setVisibility(View.VISIBLE);
            fab.hide();
            editTextNewListName.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editTextNewListName, InputMethodManager.SHOW_IMPLICIT);
        } else {
            addListInputLayout.setVisibility(View.GONE);
            fab.show();
            editTextNewListName.setText("");
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(editTextNewListName.getWindowToken(), 0);
        }
    }

    private void addNewListFromInput() {
        String listName = editTextNewListName.getText().toString().trim();
        if (!listName.isEmpty()) {
            int nextPosition = 0;
            if (!shoppingLists.isEmpty()) {
                nextPosition = shoppingLists.size();
            }
            long newId = shoppingListManager.addShoppingList(listName, nextPosition);
            if (newId != -1) {
                loadShoppingLists();
                Toast.makeText(MainActivity.this, "Liste \"" + listName + "\" erstellt", Toast.LENGTH_SHORT).show();
                toggleAddListInput();
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
    public void onListDataSetChanged() {
        checkEmptyView();
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

        dialogTitle.setText(R.string.confirm_delete_list_title);
        dialogMessage.setText(getString(R.string.confirm_delete_list_message, listToDelete.getName()));
        positiveButton.setText(R.string.delete);
        negativeButton.setText(R.string.cancel);

        positiveButton.setOnClickListener(v -> {
            shoppingListManager.deleteShoppingList(listToDelete.getId());
            Toast.makeText(MainActivity.this, "Liste \"" + listToDelete.getName() + "\" gelöscht", Toast.LENGTH_SHORT).show();
            loadShoppingLists();
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}