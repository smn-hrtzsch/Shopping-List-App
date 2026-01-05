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

public class MainActivity extends BaseActivity implements ListRecyclerViewAdapter.OnListInteractionListener {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "prefs_theme";
    private static final String KEY_LANGUAGE = "prefs_language";

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
    protected void attachBaseContext(android.content.Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lang = prefs.getString(KEY_LANGUAGE, "en");
        
        java.util.Locale locale = new java.util.Locale(lang);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(locale);
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

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
                            UiUtils.makeCustomToast(MainActivity.this, getString(R.string.error_auth_failed, errorMsg),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        initLoadingOverlay(findViewById(R.id.main_content_container));
        showSkeleton(true);

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
        // editTextNewListName = findViewById(R.id.edit_text_new_list_name); // Unused
        // buttonConfirmAddList = findViewById(R.id.button_confirm_add_list); // Unused
        if(addListInputLayout != null) addListInputLayout.setVisibility(View.GONE); // Hide it permanently if it exists in layout

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
        showLoading(R.string.loading_lists);
        loadShoppingLists();
        handleIntent(getIntent());

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isEnabled()) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
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
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        View cardPrivate = dialogView.findViewById(R.id.card_private_list);
        View cardShared = dialogView.findViewById(R.id.card_shared_list);
        View btnClose = dialogView.findViewById(R.id.button_dialog_close);

        cardPrivate.setOnClickListener(v -> {
            dialog.dismiss();
            showNameInputDialog(false);
        });

        cardShared.setOnClickListener(v -> {
            dialog.dismiss();
            showNameInputDialog(true);
        });

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showNameInputDialog(boolean isShared) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_input_name, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        View btnClose = view.findViewById(R.id.button_dialog_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        View includeInput = view.findViewById(R.id.include_input);
        EditText input = includeInput.findViewById(R.id.username_edit_text);
        input.setHint(R.string.enter_list_name);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        
        Button btnSave = includeInput.findViewById(R.id.username_action_button);
        btnSave.setText(R.string.button_save);

        btnSave.setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                UiUtils.makeCustomToast(MainActivity.this, R.string.invalid_list_name, Toast.LENGTH_SHORT).show();
                return;
            }
            createList(name, isShared);
            dialog.dismiss();
        });
        
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnSave.performClick();
                return true;
            }
            return false;
        });

        dialog.setOnShowListener(d -> {
            input.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });

        dialog.show();
    }

    private void createList(String listName, boolean isShared) {
        if (isShared) {
            createSharedListInFirestore(listName);
        } else {
            int nextPosition = shoppingListRepository.getNextPosition();
            long newId = shoppingListManager.addShoppingList(listName, nextPosition, false);
            if (newId != -1) {
                UiUtils.makeCustomToast(this, getString(R.string.local_list_created, listName), Toast.LENGTH_SHORT).show();
                syncPrivateListIfEnabled();
            } else {
                UiUtils.makeCustomToast(MainActivity.this, R.string.error_adding_list, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void syncPrivateListIfEnabled() {
        android.content.SharedPreferences settings = getSharedPreferences(ShoppingListManager.SETTINGS_PREFS, MODE_PRIVATE);
        boolean syncDefault = settings.getBoolean(ShoppingListManager.KEY_SYNC_PRIVATE_DEFAULT, true);
        FirebaseUser user = mAuth.getCurrentUser();
        boolean hasAccount = false;
        if (user != null && !user.isAnonymous()) hasAccount = true;
        if (user != null && user.isAnonymous()) {
            for (com.google.firebase.auth.UserInfo info : user.getProviderData()) {
                if (!info.getProviderId().equals("firebase")) { hasAccount = true; break; }
            }
        }

        if (syncDefault && hasAccount) {
            UiUtils.makeCustomToast(MainActivity.this, R.string.syncing_data, Toast.LENGTH_SHORT).show();
            shoppingListRepository.migrateLocalListsToCloud(this::loadShoppingLists);
        } else {
            loadShoppingLists();
        }
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
                    hideLoading();
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    UiUtils.makeCustomToast(MainActivity.this, getString(R.string.error_auth_failed, errorMsg), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void createSharedListInFirestore(String listName) {
        showLoading(R.string.loading_saving);
        ensureAuthenticated(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            // Check if user has a username profile
            userRepository.getCurrentUsername(new UserRepository.OnUsernameLoadedListener() {
                @Override
                public void onLoaded(String username) {
                    if (username == null) {
                        hideLoading();
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
                        shoppingList.put("isShared", true);
                        shoppingList.put("members", new ArrayList<>(Arrays.asList(userId)));

                        db.collection("shopping_lists")
                                .add(shoppingList)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());
                                    UiUtils.makeCustomToast(MainActivity.this, getString(R.string.shared_list_created, listName), Toast.LENGTH_SHORT).show();
                                    loadShoppingLists(); // Reload lists (calls hideLoading inside)
                                })
                                .addOnFailureListener(e -> {
                                    hideLoading();
                                    Log.w("Firestore", "Error adding document", e);
                                    UiUtils.makeCustomToast(MainActivity.this, R.string.error_create_shared_list, Toast.LENGTH_SHORT).show();
                                });
                    }
                }

                @Override
                public void onError(String error) {
                    hideLoading();
                    UiUtils.makeCustomToast(MainActivity.this, getString(R.string.error_profile_check, error), Toast.LENGTH_SHORT).show();
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
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
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
                UiUtils.makeCustomToast(this, R.string.error_reading_file, Toast.LENGTH_SHORT).show();
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
                UiUtils.makeCustomToast(this, R.string.incompatible_file, Toast.LENGTH_SHORT).show();
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
                UiUtils.makeCustomToast(this, getString(R.string.list_imported, listName), Toast.LENGTH_SHORT).show();
                syncPrivateListIfEnabled();
            } else {
                UiUtils.makeCustomToast(this, R.string.error_adding_list, Toast.LENGTH_SHORT).show();
            }
        } catch (org.json.JSONException e) {
            e.printStackTrace();
            UiUtils.makeCustomToast(this, R.string.error_parsing_shared, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCloseEditorOnTouchOutside() {
        // No inline editor anymore
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

        MenuItem languageItem = menu.findItem(R.id.action_switch_language);
        if (languageItem != null) {
            View actionView = languageItem.getActionView();
            if (actionView != null) {
                TextView langText = actionView.findViewById(R.id.text_language_code);
                String currentLang = sharedPreferences.getString(KEY_LANGUAGE, "en");
                langText.setText(currentLang.toUpperCase());
                actionView.setOnClickListener(v -> switchLanguage());
            }
        }
        
        return true;
    }

    private void switchLanguage() {
        String currentLang = sharedPreferences.getString(KEY_LANGUAGE, "en");
        String newLang = currentLang.equals("en") ? "de" : "en";
        
        sharedPreferences.edit().putString(KEY_LANGUAGE, newLang).apply();
        recreate();
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
        
        String savedLang = sharedPreferences.getString(KEY_LANGUAGE, "en");
        String currentLang = getResources().getConfiguration().getLocales().get(0).getLanguage();
        if (!savedLang.equals(currentLang)) {
            recreate();
            return;
        }

        if (addListInputLayout.getVisibility() == View.VISIBLE) {
            addListInputLayout.setVisibility(View.GONE);
            fab.show();
            editTextNewListName.setText("");
        }
        showLoading(R.string.loading_lists);
        loadShoppingLists();
    }

    private void loadShoppingLists() {
        shoppingListManager.getAllShoppingLists(loadedLists -> {
            this.shoppingLists = shoppingListManager.sortListsBasedOnSavedOrder(loadedLists);
            adapter.updateLists(shoppingLists);
            checkEmptyView();
            hideLoading();
        });
    }

    @Override
    public void showLoading(String message, boolean showSkeleton) {
        if (emptyView != null) emptyView.setVisibility(View.GONE);
        super.showLoading(message, showSkeleton);
    }

    private void checkEmptyView() {
        if (shoppingLists != null && shoppingLists.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            // Only show empty view if we are not currently showing the loading overlay
            View overlay = findViewById(R.id.loading_overlay_root);
            if (overlay != null && overlay.getVisibility() == View.VISIBLE) {
                if (emptyView != null) emptyView.setVisibility(View.GONE);
                return;
            }
            if (emptyView != null) {
                emptyView.setVisibility(View.VISIBLE);
            }
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
                UiUtils.makeCustomToast(MainActivity.this, R.string.toast_invitation_accepted, Toast.LENGTH_SHORT).show();
                loadShoppingLists();
            }

            @Override
            public void onError(String message) {
                UiUtils.makeCustomToast(MainActivity.this, getString(R.string.error_generic_message, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDeclineClicked(ShoppingList list) {
        if (list.getFirebaseId() == null) return;
        shoppingListRepository.declineInvitation(list.getFirebaseId(), new UserRepository.OnProfileActionListener() {
            @Override
            public void onSuccess() {
                UiUtils.makeCustomToast(MainActivity.this, R.string.toast_invitation_declined, Toast.LENGTH_SHORT).show();
                loadShoppingLists();
            }

            @Override
            public void onError(String message) {
                UiUtils.makeCustomToast(MainActivity.this, getString(R.string.error_generic_message, message), Toast.LENGTH_LONG).show();
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
                            UiUtils.makeCustomToast(MainActivity.this, R.string.toast_list_left, Toast.LENGTH_SHORT).show();
                            loadShoppingLists();
                        }

                        @Override
                        public void onError(String message) {
                            UiUtils.makeCustomToast(MainActivity.this, getString(R.string.error_generic_message, message), Toast.LENGTH_LONG).show();
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
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
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
            UiUtils.makeCustomToast(MainActivity.this, getString(R.string.toast_list_deleted_named, listToDelete.getName()), Toast.LENGTH_SHORT).show();
            loadShoppingLists();
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onEditStarted() {
        if (fab != null) fab.hide();
    }

    @Override
    public void onEditFinished() {
        if (fab != null) fab.show();
    }
}