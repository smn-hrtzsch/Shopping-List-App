package com.CapyCode.ShoppingList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

import java.util.List;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "prefs_theme";
    private static final String KEY_LANGUAGE = "prefs_language";
    private SharedPreferences sharedPreferences;

    private MaterialButton buttonDelete;
    private MaterialButton buttonRegisterEmail;
    private MaterialButton buttonRegisterGoogle;
    private MaterialButton buttonSignOut;
    private TextView textViewCurrentUsername;
    private TextView textViewWarning;
    private LinearLayout layoutLinkedMethods;
    private LinearLayout layoutViewMode;
    private LinearLayout layoutEditMode;
    private EditText editTextUsernameInline;
    private MaterialButton buttonSaveUsernameInline;
    private ImageView imageProfile;
    private View containerContent;
    private View cardSyncPreferences;
    private View cardLinkedMethods;
    private com.google.android.material.switchmaterial.SwitchMaterial switchSyncPrivate;
    private UserRepository userRepository;
    private String currentLoadedUsername = null;
    private boolean isInitialProfileCreation = false;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private String currentImageUrl = null;
    private boolean isSigningOut = false;

    private final ActivityResultLauncher<Intent> authActivityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    showLoading(getString(R.string.loading_profile), true, true);
                    loadCurrentProfile();
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadImage(uri);
                }
            });

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        UiUtils.makeCustomToast(ProfileActivity.this, getString(R.string.error_google_sign_in, e.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lang = prefs.getString(KEY_LANGUAGE, "en"); // Default to English or system? Let's say 'en' or maybe check system.
        // Better: check system default if not set. But for toggle we need explicit.
        // Let's assume 'en' as default fall back or check system locale.
        // For simplicity in this step, I'll load what's saved or default to 'en'.
        
        java.util.Locale locale = new java.util.Locale(lang);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(locale);
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedTheme = sharedPreferences.getInt(KEY_THEME, androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(savedTheme);

        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Toolbar toolbar = findViewById(R.id.toolbar_profile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        
        View root = findViewById(R.id.profile_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userRepository = new UserRepository(this);
        buttonDelete = findViewById(R.id.button_delete_account);
        buttonRegisterEmail = findViewById(R.id.button_register_email);
        buttonRegisterGoogle = findViewById(R.id.button_register_google);
        buttonSignOut = findViewById(R.id.button_sign_out);
        textViewCurrentUsername = findViewById(R.id.text_view_current_username);
        textViewWarning = findViewById(R.id.text_view_warning_anonymous);
        layoutLinkedMethods = findViewById(R.id.layout_linked_methods);
        layoutViewMode = findViewById(R.id.layout_view_mode);
        layoutEditMode = findViewById(R.id.layout_edit_mode);
        View includeInline = findViewById(R.id.include_username_input_inline);
        editTextUsernameInline = includeInline.findViewById(R.id.username_edit_text);
        buttonSaveUsernameInline = includeInline.findViewById(R.id.username_action_button);
        imageProfile = findViewById(R.id.image_profile);
        cardSyncPreferences = findViewById(R.id.card_sync_preferences);
        cardLinkedMethods = findViewById(R.id.card_linked_methods);
        switchSyncPrivate = findViewById(R.id.switch_sync_private);
        containerContent = findViewById(R.id.container_content);
        initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_profile);
        showSkeleton(false);

        loadCurrentProfile();

        if (getIntent().getBooleanExtra("EXTRA_OPEN_EDIT_PROFILE", false)) {
            // Delay slightly to ensure UI is ready or just call it. 
            // Better to check after profile load? 
            // Actually, if we want to set username, we can show it immediately, 
            // but pre-filling with current username requires profile load.
            // Let's rely on loadCurrentProfile finishing or just show it blank/pending.
            // Since loadCurrentProfile is async, we can't wait for it easily here without callbacks.
            // But showEditProfileDialog uses currentLoadedUsername.
            // Let's hook into the success of loadCurrentProfile? 
            // Or just set a flag.
            
            // Simpler: Just post it.
            containerContent.postDelayed(this::showEditProfileDialog, 500); 
        }

        buttonDelete.setOnClickListener(v -> confirmDeleteAccount());
        buttonRegisterEmail.setOnClickListener(v -> {
            Intent intent = new Intent(this, AuthActivity.class);
            authActivityLauncher.launch(intent);
        });
        buttonRegisterGoogle.setOnClickListener(v -> signInWithGoogle());
        buttonSignOut.setOnClickListener(v -> confirmSignOut());
        buttonSaveUsernameInline.setOnClickListener(v -> saveUsername(editTextUsernameInline.getText().toString().trim(), null));
        
        imageProfile.setOnClickListener(v -> {
            if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                showImagePreviewDialog(currentImageUrl, currentLoadedUsername);
            }
        });

        setupSyncSwitch();
    }

    private void showImagePreviewDialog(String imageUrl, String username) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_preview, null);
        builder.setView(dialogView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        
        TextView titleView = dialogView.findViewById(R.id.dialog_title_preview);
        ImageView imageView = dialogView.findViewById(R.id.dialog_image_preview);
        TextView usernameView = dialogView.findViewById(R.id.dialog_username_preview);
        View btnClose = dialogView.findViewById(R.id.dialog_button_close_preview);

        titleView.setText(R.string.profile_picture);
        if (username != null) {
            usernameView.setText(username);
        } else {
            usernameView.setVisibility(View.GONE);
        }
        
        Glide.with(this)
             .load(imageUrl)
             .apply(RequestOptions.circleCropTransform())
             .into(imageView);
             
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    private void setupSyncSwitch() {
        SharedPreferences prefs = getSharedPreferences(ShoppingListManager.SETTINGS_PREFS, MODE_PRIVATE);
        switchSyncPrivate.setChecked(prefs.getBoolean(ShoppingListManager.KEY_SYNC_PRIVATE_DEFAULT, true));
        switchSyncPrivate.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean(ShoppingListManager.KEY_SYNC_PRIVATE_DEFAULT, isChecked).apply();
        });
    }

    private void showEditProfileDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        View includeDialog = dialogView.findViewById(R.id.include_username_input_dialog);
        EditText editText = includeDialog.findViewById(R.id.username_edit_text);
        View btnNew = dialogView.findViewById(R.id.button_option_new_image);
        View btnRemove = dialogView.findViewById(R.id.button_option_remove_image);
        View btnSave = includeDialog.findViewById(R.id.username_action_button);
        View btnClose = dialogView.findViewById(R.id.button_dialog_close);

        editText.setText(currentLoadedUsername != null ? currentLoadedUsername : "");

        btnNew.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
            dialog.dismiss();
        });
        btnRemove.setOnClickListener(v -> {
            removeImage();
            dialog.dismiss();
        });
        btnSave.setOnClickListener(v -> {
            saveUsername(editText.getText().toString().trim(), dialog);
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void saveUsername(String username, androidx.appcompat.app.AlertDialog dialog) {
        if (username.isEmpty()) { UiUtils.makeCustomToast(this, R.string.profile_error_empty, Toast.LENGTH_SHORT).show(); return; }
        if (username.length() < 3) { UiUtils.makeCustomToast(this, R.string.profile_error_short, Toast.LENGTH_SHORT).show(); return; }
        if (username.contains(" ")) { UiUtils.makeCustomToast(this, R.string.profile_error_whitespace, Toast.LENGTH_SHORT).show(); return; }
        
        showLoading(getString(R.string.loading_saving), false);
        userRepository.setUsername(username, new UserRepository.OnProfileActionListener() {
            @Override
            public void onSuccess() {
                hideLoading();
                UiUtils.makeCustomToast(ProfileActivity.this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
                currentLoadedUsername = username;
                textViewCurrentUsername.setText(username);
                setResult(RESULT_OK);
                if (dialog != null) dialog.dismiss();
                updateUIForUsername();
                if (isInitialProfileCreation) { loadCurrentProfile(); }
            }
            @Override
            public void onError(String message) { 
                hideLoading();
                UiUtils.makeCustomToast(ProfileActivity.this, message, Toast.LENGTH_LONG).show(); 
            }
        });
    }

    private void updateUIForUsername() {
        if (currentLoadedUsername != null && !currentLoadedUsername.isEmpty()) {
            layoutViewMode.setVisibility(View.VISIBLE);
            layoutEditMode.setVisibility(View.GONE);
        } else {
            layoutViewMode.setVisibility(View.GONE);
            layoutEditMode.setVisibility(View.VISIBLE);
        }
    }

    private void signInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        showLoading();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            for (UserInfo profile : currentUser.getProviderData()) {
                if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                     hideLoading();
                     UiUtils.makeCustomToast(this, R.string.error_google_already_linked, Toast.LENGTH_LONG).show();
                     mGoogleSignInClient.signOut();
                     return;
                }
            }
            currentUser.linkWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            UiUtils.makeCustomToast(ProfileActivity.this, R.string.toast_google_linked, Toast.LENGTH_SHORT).show();
                            syncGoogleProfilePicture();
                            triggerSync();
                            loadCurrentProfile();
                        } else {
                             if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                 checkIfAccountIsEmpty(isEmpty -> {
                                     if (isEmpty) {
                                         performGoogleSignIn(credential);
                                     } else {
                                         hideLoading();
                                         int messageId = mAuth.getCurrentUser().isAnonymous() 
                                             ? R.string.dialog_switch_account_message_guest 
                                             : R.string.dialog_switch_account_message_conflict;
                                         
                                         showCustomDialog(
                                             getString(R.string.dialog_switch_account_title),
                                             getString(messageId),
                                             getString(R.string.button_switch_and_link),
                                             () -> performGoogleSignIn(credential)
                                         );
                                     }
                                 });
                             } else {
                                 hideLoading();
                                 String msg = task.getException() != null ? task.getException().getMessage() : "Error";
                                 UiUtils.makeCustomToast(ProfileActivity.this, getString(R.string.error_link_failed, msg), Toast.LENGTH_LONG).show();
                             }
                        }
                    });
        } else {
            performGoogleSignIn(credential);
        }
    }

    private interface OnAccountEmptyCheckListener {
        void onResult(boolean isEmpty);
    }

    private void checkIfAccountIsEmpty(OnAccountEmptyCheckListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || !user.isAnonymous()) {
            listener.onResult(false);
            return;
        }
        userRepository.getUserProfile(new UserRepository.OnUserProfileLoadedListener() {
            @Override
            public void onLoaded(String username, String imageUrl) {
                if (username != null && !username.isEmpty()) {
                    listener.onResult(false);
                    return;
                }
                ShoppingListRepository listRepo = new ShoppingListRepository(ProfileActivity.this);
                if (listRepo.getLocalListCount() > 0) {
                     listener.onResult(false);
                } else {
                     listener.onResult(true);
                }
            }
            @Override
            public void onError(String error) {
                listener.onResult(false);
            }
        });
    }

    private void triggerSync() {
        UiUtils.makeCustomToast(this, getString(R.string.syncing_data), Toast.LENGTH_SHORT).show();
        ShoppingListRepository repository = new ShoppingListRepository(getApplicationContext());
        repository.migrateLocalListsToCloud(() -> {
            android.util.Log.d("Profile", "Local lists migrated to cloud.");
        });
    }

    private void performGoogleSignIn(AuthCredential credential) {
        showLoading();
        ShoppingListRepository repo = new ShoppingListRepository(getApplicationContext());
        repo.clearLocalDatabase();
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        syncGoogleProfilePicture();
                        loadCurrentProfile();
                    } else {
                        hideLoading();
                        String msg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        UiUtils.makeCustomToast(ProfileActivity.this, getString(R.string.error_auth_failed, msg), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void syncGoogleProfilePicture() {
        userRepository.getUserProfile(new UserRepository.OnUserProfileLoadedListener() {
            @Override
            public void onLoaded(String username, String imageUrl) {
                if (imageUrl == null || imageUrl.isEmpty()) {
                    // No image set, try to get from Google
                    for (UserInfo profile : mAuth.getCurrentUser().getProviderData()) {
                        if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId()) && profile.getPhotoUrl() != null) {
                            userRepository.updateProfileImage(profile.getPhotoUrl().toString(), new UserRepository.OnProfileActionListener() {
                                @Override public void onSuccess() { loadCurrentProfile(); }
                                @Override public void onError(String message) {}
                            });
                            break;
                        }
                    }
                }
            }
            @Override public void onError(String error) {}
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile, menu);

        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        MenuItem themeItem = menu.findItem(R.id.action_switch_theme);
        if (themeItem != null) {
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                themeItem.setIcon(R.drawable.ic_moon_filled);
            } else {
                themeItem.setIcon(R.drawable.ic_moon_outlined);
            }
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
        if (item.getItemId() == R.id.action_edit_profile) {
            showEditProfileDialog();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_switch_theme) {
            int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                sharedPreferences.edit().putInt(KEY_THEME, androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO).apply();
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                sharedPreferences.edit().putInt(KEY_THEME, androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES).apply();
            }
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadCurrentProfile() {
        showLoading(getString(R.string.loading_profile), false, true);
        containerContent.setVisibility(View.GONE);
        Runnable fetchProfileData = () -> {
            userRepository.getUserProfile(new UserRepository.OnUserProfileLoadedListener() {
                @Override
                public void onLoaded(String username, String imageUrl) {
                    hideLoading();
                    containerContent.setVisibility(View.VISIBLE);
                    
                    currentImageUrl = imageUrl;

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(ProfileActivity.this)
                             .load(imageUrl)
                             .apply(RequestOptions.circleCropTransform())
                             .into(imageProfile);
                        imageProfile.setPadding(0,0,0,0);
                        imageProfile.clearColorFilter();
                    } else {
                        imageProfile.setImageResource(R.drawable.ic_account_circle_24);
                        imageProfile.setPadding(0,0,0,0);
                        int tintColor = ContextCompat.getColor(ProfileActivity.this, R.color.icon_tint_adaptive);
                        imageProfile.setColorFilter(tintColor);
                    }
                    
                    if (username != null) {
                        isInitialProfileCreation = false;
                        currentLoadedUsername = username;
                        textViewCurrentUsername.setText(username);
                    } else {
                        isInitialProfileCreation = true;
                        currentLoadedUsername = null;
                        textViewCurrentUsername.setText("...");
                    }
                    isSigningOut = false;
                    updateUIForUsername();
                    updateAuthUI();
                }
                @Override
                public void onError(String error) {
                    hideLoading();
                    containerContent.setVisibility(View.VISIBLE);
                    // Ignore "offline" error for new users who might not have a doc yet
                    if (error != null && !error.toLowerCase().contains("offline")) {
                        UiUtils.makeCustomToast(ProfileActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                    isInitialProfileCreation = true;
                    isSigningOut = false;
                    // Ensure UI updates even if fetch failed (e.g. assume no profile data)
                    if (currentLoadedUsername == null) textViewCurrentUsername.setText("...");
                    updateUIForUsername();
                    updateAuthUI();
                }
            });
        };
        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        user.reload().addOnCompleteListener(t -> fetchProfileData.run());
                    } else {
                        fetchProfileData.run();
                    }
                } else {
                    hideLoading();
                    containerContent.setVisibility(View.VISIBLE);
                    UiUtils.makeCustomToast(ProfileActivity.this, "Auth failed", Toast.LENGTH_LONG).show();
                }
            });
        } else fetchProfileData.run();
    }

    private void updateAuthUI() {
        FirebaseUser user = mAuth.getCurrentUser();
        boolean isEffectivelyAnonymous = user == null || user.isAnonymous();
        if (!isEffectivelyAnonymous) {
             boolean hasProvider = false;
             for (UserInfo profile : user.getProviderData()) {
                 if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId()) || 
                     EmailAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                     hasProvider = true;
                     break;
                 }
             }
             if (!hasProvider) isEffectivelyAnonymous = true;
        }

        if (user != null && !isEffectivelyAnonymous) {
            textViewWarning.setVisibility(View.GONE);
            cardSyncPreferences.setVisibility(View.VISIBLE);
            if (cardLinkedMethods != null) cardLinkedMethods.setVisibility(View.VISIBLE);
            
            layoutLinkedMethods.removeAllViews();
            for (UserInfo profile : user.getProviderData()) {
                if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId()) || EmailAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    row.setPadding(0, 8, 0, 8);
                    ImageView icon = new ImageView(this);
                    int size = (int) (24 * getResources().getDisplayMetrics().density);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                    lp.setMarginEnd((int) (16 * getResources().getDisplayMetrics().density));
                    icon.setLayoutParams(lp);
                    TextView text = new TextView(this);
                    text.setTextColor(ContextCompat.getColor(this, R.color.text_primary_adaptive));
                    if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                        if (profile.getPhotoUrl() != null) Glide.with(this).load(profile.getPhotoUrl()).apply(RequestOptions.circleCropTransform()).into(icon);
                        else icon.setImageResource(R.drawable.ic_google_logo);
                        text.setText(getString(R.string.linked_google, profile.getEmail()));
                    } else {
                        icon.setImageResource(R.drawable.ic_email);
                        icon.setColorFilter(ContextCompat.getColor(this, R.color.text_primary_adaptive));
                        text.setText(getString(R.string.linked_email, profile.getEmail()));
                    }
                    row.addView(icon);
                    row.addView(text);
                    layoutLinkedMethods.addView(row);
                }
            }
            buttonSignOut.setVisibility(View.VISIBLE);
            findViewById(R.id.layout_auth_buttons).setVisibility(View.VISIBLE);
            boolean isGoogleLinked = false; boolean isEmailLinked = false;
            for (UserInfo profile : user.getProviderData()) {
                if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) isGoogleLinked = true;
                if (EmailAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) isEmailLinked = true;
            }
            if (isGoogleLinked) {
                buttonRegisterGoogle.setText(R.string.google_linked_disconnect);
                buttonRegisterGoogle.setIconResource(R.drawable.ic_unlink);
                buttonRegisterGoogle.setIconTint(ContextCompat.getColorStateList(this, android.R.color.black));
                if ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES)
                     buttonRegisterGoogle.setIconTint(ContextCompat.getColorStateList(this, android.R.color.white));
                buttonRegisterGoogle.setOnClickListener(v -> confirmUnlink(GoogleAuthProvider.PROVIDER_ID));
            } else {
                buttonRegisterGoogle.setText(R.string.action_link_google);
                buttonRegisterGoogle.setIconResource(R.drawable.ic_google_logo);
                buttonRegisterGoogle.setIconTint(null);
                buttonRegisterGoogle.setOnClickListener(v -> signInWithGoogle());
            }
            if (isEmailLinked) {
                buttonRegisterEmail.setText(R.string.email_linked_disconnect);
                buttonRegisterEmail.setIconResource(R.drawable.ic_unlink);
                if ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES)
                     buttonRegisterEmail.setIconTint(ContextCompat.getColorStateList(this, android.R.color.white));
                else buttonRegisterEmail.setIconTint(ContextCompat.getColorStateList(this, android.R.color.black));
                buttonRegisterEmail.setOnClickListener(v -> confirmUnlink(EmailAuthProvider.PROVIDER_ID));
            } else {
                buttonRegisterEmail.setText(R.string.action_link_email);
                buttonRegisterEmail.setIconResource(R.drawable.ic_email);
                int colorOnSurface = com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, android.graphics.Color.BLACK);
                buttonRegisterEmail.setIconTint(android.content.res.ColorStateList.valueOf(colorOnSurface));
                buttonRegisterEmail.setOnClickListener(v -> {
                    Intent intent = new Intent(this, AuthActivity.class);
                    authActivityLauncher.launch(intent);
                });
            }
        } else {
            textViewWarning.setVisibility(View.VISIBLE);
            cardSyncPreferences.setVisibility(View.GONE);
            if (cardLinkedMethods != null) cardLinkedMethods.setVisibility(View.GONE);
            findViewById(R.id.layout_auth_buttons).setVisibility(View.VISIBLE);
            boolean shouldShowLinkText = currentLoadedUsername != null && !currentLoadedUsername.isEmpty();
            buttonRegisterEmail.setText(shouldShowLinkText ? R.string.action_link_email : R.string.action_sign_in_email);
            buttonRegisterEmail.setIconResource(R.drawable.ic_email);
            int colorOnSurface = com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, android.graphics.Color.BLACK);
            buttonRegisterEmail.setIconTint(android.content.res.ColorStateList.valueOf(colorOnSurface));
            buttonRegisterEmail.setOnClickListener(v -> {
                Intent intent = new Intent(this, AuthActivity.class);
                authActivityLauncher.launch(intent);
            });
            buttonRegisterGoogle.setText(shouldShowLinkText ? R.string.action_link_google : R.string.sign_in_google);
            buttonRegisterGoogle.setIconResource(R.drawable.ic_google_logo);
            buttonRegisterGoogle.setIconTint(null);
            buttonRegisterGoogle.setOnClickListener(v -> signInWithGoogle());
            buttonSignOut.setVisibility(View.GONE);
        }
    }

    private void confirmUnlink(String providerId) {
        showCustomDialog(getString(R.string.dialog_unlink_title), getString(R.string.dialog_unlink_message), getString(R.string.button_unlink), () -> unlinkProvider(providerId));
    }

    private void unlinkProvider(String providerId) {
        showLoading(R.string.loading_saving);
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.unlink(providerId).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    UiUtils.makeCustomToast(this, R.string.toast_unlinked, Toast.LENGTH_SHORT).show();
                    if (GoogleAuthProvider.PROVIDER_ID.equals(providerId)) mGoogleSignInClient.signOut();
                    
                    user.reload().addOnCompleteListener(reloadTask -> {
                        // Check if we need to unsync all lists
                        boolean hasProvider = false;
                        for (UserInfo profile : user.getProviderData()) {
                            if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId()) || 
                                EmailAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                                hasProvider = true;
                                break;
                            }
                        }
                        
                        if (!hasProvider) {
                             showLoading();
                             ShoppingListRepository repo = new ShoppingListRepository(getApplicationContext());
                             repo.unsyncAllLists(() -> {
                                 hideLoading();
                                 loadCurrentProfile();
                             });
                        } else {
                             loadCurrentProfile();
                        }
                    });
                } else {
                    if (task.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                        if (GoogleAuthProvider.PROVIDER_ID.equals(providerId)) {
                            signInWithGoogle();
                        } else if (EmailAuthProvider.PROVIDER_ID.equals(providerId)) showReauthDialog(providerId);
                    } else {
                        UiUtils.makeCustomToast(this, "Unlink failed", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private void showReauthDialog(String providerId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_security_check_title);
        builder.setMessage(R.string.dialog_security_check_message);
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton(R.string.button_confirm, (dialog, which) -> {
            String password = input.getText().toString();
            if (!password.isEmpty()) reauthenticateAndUnlink(password, providerId);
        });
        builder.setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void reauthenticateAndUnlink(String password, String providerId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) unlinkProvider(providerId);
                else UiUtils.makeCustomToast(this, R.string.error_password_wrong, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void confirmSignOut() {
        showCustomDialog(getString(R.string.button_sign_out), getString(R.string.dialog_sign_out_message_safe), getString(R.string.button_sign_out), this::performSafeSignOut);
    }

    private void performSafeSignOut() {
        showLoading(getString(R.string.loading), true, true);
        isSigningOut = true;
        ShoppingListRepository repo = new ShoppingListRepository(getApplicationContext());
        repo.migrateLocalListsToCloud(() -> {
             repo.clearLocalDatabase();
             mAuth.signOut();
             mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                 UiUtils.makeCustomToast(this, R.string.toast_signed_out, Toast.LENGTH_SHORT).show();
                 loadCurrentProfile(); 
             });
        });
    }

    private void removeImage() {
        showLoading();
        userRepository.removeProfileImage(new UserRepository.OnProfileActionListener() {
            @Override
            public void onSuccess() {
                 hideLoading();
                 loadCurrentProfile();
                 UiUtils.makeCustomToast(ProfileActivity.this, R.string.toast_image_removed, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(String message) {
                hideLoading();
                UiUtils.makeCustomToast(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadImage(Uri imageUri) {
        showLoading(getString(R.string.loading_uploading), false);
        userRepository.uploadProfileImage(imageUri, new UserRepository.OnImageUploadListener() {
            @Override
            public void onSuccess(String downloadUrl) {
                hideLoading();
                Glide.with(ProfileActivity.this).load(downloadUrl).apply(RequestOptions.circleCropTransform()).into(imageProfile);
                imageProfile.setPadding(0,0,0,0);
                imageProfile.clearColorFilter();
                UiUtils.makeCustomToast(ProfileActivity.this, R.string.toast_image_uploaded, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(String message) {
                hideLoading();
                UiUtils.makeCustomToast(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDeleteAccount() {
        showCustomDialog(getString(R.string.dialog_delete_account_title), getString(R.string.dialog_delete_account_message), getString(R.string.button_delete), this::deleteAccount);
    }

    private void showCustomDialog(String title, String message, String positiveButtonText, Runnable onPositiveAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_standard, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        TextView textTitle = dialogView.findViewById(R.id.dialog_title);
        TextView textMessage = dialogView.findViewById(R.id.dialog_message);
        MaterialButton btnPositive = dialogView.findViewById(R.id.dialog_button_positive);
        MaterialButton btnNegative = dialogView.findViewById(R.id.dialog_button_negative);
        textTitle.setText(title); textMessage.setText(message); btnPositive.setText(positiveButtonText);
        btnPositive.setOnClickListener(v -> { onPositiveAction.run(); dialog.dismiss(); });
        btnNegative.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void deleteAccount() {
        buttonDelete.setEnabled(false);
        ShoppingListRepository repository = new ShoppingListRepository(getApplicationContext());
        repository.deleteAllUserData(() -> {
            userRepository.deleteAccount(new UserRepository.OnProfileActionListener() {
                @Override
                public void onSuccess() {
                    UiUtils.makeCustomToast(ProfileActivity.this, R.string.account_deleted, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
                @Override
                public void onError(String message) {
                    UiUtils.makeCustomToast(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
                    buttonDelete.setEnabled(true);
                }
            });
        });
    }
}