package com.CapyCode.ShoppingList;

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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private UserRepository userRepository;
    private ShoppingListRepository shoppingListRepository;
    private String pendingListName;

    private final androidx.activity.result.ActivityResultLauncher<Intent> profileActivityLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Profile created successfully, retry creating shared list
                    if (pendingListName != null && !pendingListName.isEmpty()) {
                        createSharedListInFirestore(pendingListName);
                        pendingListName = null; // Reset
                    }
                } else {
                    // User cancelled or failed, reset pending list name
                    pendingListName = null;
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedTheme = sharedPreferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedTheme);

        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userRepository = new UserRepository(this);
        shoppingListRepository = new ShoppingListRepository(this);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            mAuth.signInAnonymously()
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("Auth", "signInAnonymously:success");
                            // FirebaseUser user = mAuth.getCurrentUser();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("Auth", "signInAnonymously:failure", task.getException());
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(MainActivity.this, getString(R.string.error_auth_failed, errorMsg),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_account_circle_24);
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

        fab.setOnClickListener(view -> showAddListDialog());

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        setupCloseEditorOnTouchOutside();
        loadShoppingLists();
        handleIntent(getIntent());

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (addListInputLayout.getVisibility() == View.VISIBLE) {
                    toggleAddListInput(false); // Assume local if closed without saving
                } else {
                    if (isEnabled()) {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            }
        });
    }

    private void showAddListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_list_type, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View cardPrivate = dialogView.findViewById(R.id.card_private_list);
        View cardShared = dialogView.findViewById(R.id.card_shared_list);

        cardPrivate.setOnClickListener(v -> {
            toggleAddListInput(false);
            dialog.dismiss();
        });

        cardShared.setOnClickListener(v -> {
            toggleAddListInput(true);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void toggleAddListInput(boolean isShared) {
        if (addListInputLayout.getVisibility() == View.GONE) {
            addListInputLayout.setVisibility(View.VISIBLE);
            fab.hide();
            editTextNewListName.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editTextNewListName, InputMethodManager.SHOW_IMPLICIT);
            buttonConfirmAddList.setOnClickListener(v -> addNewList(isShared));
            editTextNewListName.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addNewList(isShared);
                    return true;
                }
                return false;
            });
        } else {
            addListInputLayout.setVisibility(View.GONE);
            fab.show();
            editTextNewListName.setText("");
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(editTextNewListName.getWindowToken(), 0);
        }
    }

    private void addNewList(boolean isShared) {
        String listName = editTextNewListName.getText().toString().trim();
        if (listName.isEmpty()) {
            Toast.makeText(MainActivity.this, R.string.invalid_list_name, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isShared) {
            createSharedListInFirestore(listName);
        } else {
            int nextPosition = shoppingListRepository.getNextPosition();
            long newId = shoppingListManager.addShoppingList(listName, nextPosition);
            if (newId != -1) {
                loadShoppingLists();
                Toast.makeText(MainActivity.this, getString(R.string.local_list_created, listName), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, R.string.error_adding_list, Toast.LENGTH_SHORT).show();
            }
        }
        // Hide keyboard and input field
        addListInputLayout.setVisibility(View.GONE);
        fab.show();
        editTextNewListName.setText("");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editTextNewListName.getWindowToken(), 0);
    }

    private void ensureAuthenticated(Runnable onSuccess) {
        if (mAuth.getCurrentUser() != null) {
            onSuccess.run();
        } else {
            mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Log.d("Auth", "signInAnonymously:success (retry)");
                    onSuccess.run();
                } else {
                    Log.w("Auth", "signInAnonymously:failure (retry)", task.getException());
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    Toast.makeText(MainActivity.this, getString(R.string.error_auth_failed, errorMsg), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void createSharedListInFirestore(String listName) {
        ensureAuthenticated(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            // Check if user has a username profile
            userRepository.getCurrentUsername(new UserRepository.OnUsernameLoadedListener() {
                @Override
                public void onLoaded(String username) {
                    if (username == null) {
                        // No profile yet
                        pendingListName = listName; // Save for later
                        showCustomDialog(
                                getString(R.string.profile_required_title),
                                getString(R.string.profile_required_message),
                                getString(R.string.create_profile_button),
                                getString(R.string.button_cancel),
                                () -> {
                                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                                    profileActivityLauncher.launch(intent);
                                },
                                () -> pendingListName = null
                        );
                    } else {
                        // User has a profile, proceed
                        String userId = currentUser.getUid();
                        Map<String, Object> shoppingList = new HashMap<>();
                        shoppingList.put("name", listName);
                        shoppingList.put("ownerId", userId);
                        shoppingList.put("members", Arrays.asList(userId));

                        db.collection("shopping_lists")
                                .add(shoppingList)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());
                                    Toast.makeText(MainActivity.this, getString(R.string.shared_list_created, listName), Toast.LENGTH_SHORT).show();
                                    loadShoppingLists(); // Reload lists to show the new shared list
                                })
                                .addOnFailureListener(e -> {
                                    Log.w("Firestore", "Error adding document", e);
                                    Toast.makeText(MainActivity.this, R.string.error_create_shared_list, Toast.LENGTH_SHORT).show();
                                });
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(MainActivity.this, getString(R.string.error_profile_check, error), Toast.LENGTH_SHORT).show();
                    pendingListName = null;
                }
            });
        });
    }
    
    private void showCustomDialog(String title, String message, String positiveButtonText, String negativeButtonText, Runnable onPositiveAction, Runnable onNegativeAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_standard, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView textTitle = dialogView.findViewById(R.id.dialog_title);
        TextView textMessage = dialogView.findViewById(R.id.dialog_message);
        MaterialButton btnPositive = dialogView.findViewById(R.id.dialog_button_positive);
        MaterialButton btnNegative = dialogView.findViewById(R.id.dialog_button_negative);

        textTitle.setText(title);
        textMessage.setText(message);
        btnPositive.setText(positiveButtonText);
        btnNegative.setText(negativeButtonText);

        btnPositive.setOnClickListener(v -> {
            if (onPositiveAction != null) onPositiveAction.run();
            dialog.dismiss();
        });

        btnNegative.setOnClickListener(v -> {
            if (onNegativeAction != null) onNegativeAction.run();
            dialog.dismiss();
        });

        dialog.show();
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
                Toast.makeText(this, R.string.error_reading_file, Toast.LENGTH_SHORT).show();
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

            if (!"com.CapyCode.ShoppingList".equals(jsonList.optString("app_id"))) {
                Toast.makeText(this, R.string.incompatible_file, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, R.string.error_parsing_shared, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCloseEditorOnTouchOutside() {
        emptyView.setOnClickListener(v -> {
            if (addListInputLayout.getVisibility() == View.VISIBLE) {
                toggleAddListInput(false); // Assume local if closed without saving
            }
        });

        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                // Diese Bedingung stellt sicher, dass der Klick nicht auf den Editor selbst erfolgt
                if (addListInputLayout.getVisibility() == View.VISIBLE && e.getY() > addListInputLayout.getBottom()) {
                    toggleAddListInput(false); // Assume local if closed without saving
                    return true;
                }
                return false;
            }
        });
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        MenuItem themeItem = menu.findItem(R.id.action_switch_theme);
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            themeItem.setIcon(R.drawable.ic_moon_filled);
        } else {
            themeItem.setIcon(R.drawable.ic_moon_outlined);
        }
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_switch_theme) {
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
        shoppingListManager.getAllShoppingLists(loadedLists -> {
            this.shoppingLists = shoppingListManager.sortListsBasedOnSavedOrder(loadedLists);
            adapter.updateLists(shoppingLists);
            checkEmptyView();
        });
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

    @Override
    public void onListClicked(ShoppingList list) {
        Intent intent = new Intent(MainActivity.this, EinkaufslisteActivity.class);
        intent.putExtra("LIST_ID", list.getId());
        intent.putExtra("LIST_NAME", list.getName());
        if (list.getFirebaseId() != null) {
            intent.putExtra("FIREBASE_LIST_ID", list.getFirebaseId());
        }
        startActivity(intent);
    }

    @Override
    public void onListDataSetChanged() {
        checkEmptyView();
    }

    @Override
    public void onJoinClicked(ShoppingList list) {
        if (list.getFirebaseId() == null) return;
        shoppingListRepository.acceptInvitation(list.getFirebaseId(), new UserRepository.OnProfileActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, R.string.toast_invitation_accepted, Toast.LENGTH_SHORT).show();
                loadShoppingLists();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, "Fehler: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDeclineClicked(ShoppingList list) {
        if (list.getFirebaseId() == null) return;
        shoppingListRepository.declineInvitation(list.getFirebaseId(), new UserRepository.OnProfileActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, R.string.toast_invitation_declined, Toast.LENGTH_SHORT).show();
                loadShoppingLists();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, "Fehler: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onLeaveClicked(ShoppingList list) {
        if (list.getFirebaseId() == null) return;

        showCustomDialog(
                getString(R.string.dialog_leave_list_title),
                getString(R.string.dialog_leave_list_message, list.getName()),
                getString(R.string.button_leave_confirm),
                getString(R.string.button_cancel),
                () -> {
                    shoppingListRepository.leaveList(list.getFirebaseId(), new UserRepository.OnProfileActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(MainActivity.this, R.string.toast_list_left, Toast.LENGTH_SHORT).show();
                            loadShoppingLists();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(MainActivity.this, "Fehler: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                },
                null
        );
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
            shoppingListManager.deleteShoppingList(listToDelete);
            Toast.makeText(MainActivity.this, getString(R.string.toast_list_deleted_named, listToDelete.getName()), Toast.LENGTH_SHORT).show();
            loadShoppingLists();
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}