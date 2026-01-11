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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

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
    private com.google.android.material.card.MaterialCardView layoutEditMode;
    private EditText editTextUsernameInline;
    private MaterialButton buttonSaveUsernameInline;
    private com.google.android.material.card.MaterialCardView layoutAuthInline;
    private EditText editTextEmailInline;
    private EditText editTextPasswordInline;
    private MaterialButton buttonLoginInline;
    private MaterialButton buttonRegisterInline;
    private MaterialButton buttonGoogleInline;
    private ImageView imageProfile;
    private android.widget.ProgressBar progressImage;
    private View containerContent;
    private View cardSyncPreferences;
    private View cardLinkedMethods;
    private com.google.android.material.switchmaterial.SwitchMaterial switchSyncPrivate;
    private UserRepository userRepository;
    private String currentLoadedUsername = null;
    private boolean isInitialProfileCreation = false;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private androidx.appcompat.app.AlertDialog currentAuthDialog = null;
    private androidx.appcompat.app.AlertDialog verificationDialog = null;

    private String currentImageUrl = null;
    private boolean isSigningOut = false;
    private boolean autofillUiShown = false;
    private boolean isTogglingPasswordVisibility = false;

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
        int savedTheme = sharedPreferences.getInt(KEY_THEME, androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(savedTheme);

        if (getIntent().getBooleanExtra("EXTRA_IS_SIGNING_OUT", false)) {
            isSigningOut = true;
            getIntent().removeExtra("EXTRA_IS_SIGNING_OUT");
        }

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
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
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
        editTextUsernameInline = includeInline.findViewById(R.id.field_id_x_secure_no_fill);
        
        AuthUiHelper.disableAutofillForView(editTextUsernameInline);
        
        buttonSaveUsernameInline = includeInline.findViewById(R.id.username_action_button);
        
        layoutAuthInline = findViewById(R.id.layout_auth_inline);
        editTextEmailInline = findViewById(R.id.edit_text_email_inline);
        editTextPasswordInline = findViewById(R.id.edit_text_password_inline);
        TextInputLayout inputLayoutPasswordInline = findViewById(R.id.input_layout_password_inline);
        
        buttonLoginInline = findViewById(R.id.button_login_inline);
        buttonRegisterInline = findViewById(R.id.button_register_inline);
        buttonGoogleInline = findViewById(R.id.button_google_inline);

        AuthUiHelper.setupEmailPasswordFlow(editTextEmailInline, editTextPasswordInline, buttonLoginInline::performClick);
        AuthUiHelper.setupPasswordToggle(inputLayoutPasswordInline, editTextPasswordInline, isVisible -> isTogglingPasswordVisibility = isVisible);

        TextView textForgotPasswordInline = findViewById(R.id.text_forgot_password_inline);
        
        // Setup inline auth listeners
        TextView errorTextEmailInline = findViewById(R.id.error_text_email_inline);
        TextView errorTextPasswordInline = findViewById(R.id.error_text_password_inline);
        TextView errorTextGeneralInline = findViewById(R.id.error_text_general_inline);
        View authLoadingContainerInline = findViewById(R.id.auth_loading_container_inline);
        
        // Find dots inside the included layout. 
        // Note: include id is auth_loading_container_inline, so we need to find dots inside it if possible or just pass the container.
        // Actually the include tag ID is the root of the included layout if merge wasn't used, but here it is a LinearLayout probably.
        // Let's check dialog_loading.xml... it is a LinearLayout. So authLoadingContainerInline IS the view.
        // But we need the dots inside it.
        View dot1Inline = authLoadingContainerInline.findViewById(R.id.dot1);
        View dot2Inline = authLoadingContainerInline.findViewById(R.id.dot2);
        View dot3Inline = authLoadingContainerInline.findViewById(R.id.dot3);

        android.text.TextWatcher clearErrorWatcherInline = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                errorTextEmailInline.setVisibility(View.GONE);
                errorTextPasswordInline.setVisibility(View.GONE);
                errorTextGeneralInline.setVisibility(View.GONE);
                
                // If many characters are added at once, it's very likely an autofill.
                // We check if both fields are now filled.
                if (!isTogglingPasswordVisibility && count > 3 && editTextEmailInline.getText().length() > 0 && editTextPasswordInline.getText().length() > 0) {
                    editTextEmailInline.postDelayed(() -> {
                        hideKeyboard(editTextEmailInline);
                        editTextEmailInline.clearFocus();
                        editTextPasswordInline.clearFocus();
                    }, 100);
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        editTextEmailInline.addTextChangedListener(clearErrorWatcherInline);
        editTextPasswordInline.addTextChangedListener(clearErrorWatcherInline);
        
        View.OnFocusChangeListener scrollListener = (v, hasFocus) -> {
            if (hasFocus) {
                triggerAuthScroll();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    android.view.autofill.AutofillManager afm = getSystemService(android.view.autofill.AutofillManager.class);
                    if (afm != null) afm.requestAutofill(v);
                }
            }
        };
        
        editTextEmailInline.setOnFocusChangeListener(scrollListener);
        editTextPasswordInline.setOnFocusChangeListener(scrollListener);
        
        // Also trigger scroll on click (e.g. if keyboard was closed but focus remained)
        View.OnClickListener clickListener = v -> triggerAuthScroll();
        editTextEmailInline.setOnClickListener(clickListener);
        editTextPasswordInline.setOnClickListener(clickListener);
        
        buttonLoginInline.setOnClickListener(v -> performDialogLogin(null, editTextEmailInline, editTextPasswordInline, errorTextEmailInline, errorTextPasswordInline, errorTextGeneralInline, authLoadingContainerInline, dot1Inline, dot2Inline, dot3Inline, buttonLoginInline, buttonRegisterInline));
        buttonRegisterInline.setOnClickListener(v -> performDialogRegister(null, editTextEmailInline, editTextPasswordInline, errorTextEmailInline, errorTextPasswordInline, errorTextGeneralInline, authLoadingContainerInline, dot1Inline, dot2Inline, dot3Inline, buttonLoginInline, buttonRegisterInline));
        buttonGoogleInline.setOnClickListener(v -> signInWithGoogle());
        textForgotPasswordInline.setOnClickListener(v -> performDialogResetPassword(editTextEmailInline, errorTextEmailInline, authLoadingContainerInline, dot1Inline, dot2Inline, dot3Inline));

        imageProfile = findViewById(R.id.image_profile);
        progressImage = findViewById(R.id.progress_image);
        cardSyncPreferences = findViewById(R.id.card_sync_preferences);
        cardLinkedMethods = findViewById(R.id.card_linked_methods);
        switchSyncPrivate = findViewById(R.id.switch_sync_private);
        containerContent = findViewById(R.id.container_content);
        initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_profile, 1);
        showSkeleton(false);

        loadCurrentProfile();

        if (getIntent().getBooleanExtra("EXTRA_OPEN_EDIT_PROFILE", false)) {
            containerContent.postDelayed(this::showEditProfileDialog, 500); 
        }

        buttonDelete.setOnClickListener(v -> confirmDeleteAccount());
        buttonRegisterEmail.setOnClickListener(v -> showAuthDialog());
        buttonRegisterGoogle.setOnClickListener(v -> signInWithGoogle());
        buttonSignOut.setOnClickListener(v -> confirmSignOut());
        
        AuthUiHelper.setupActionDone(editTextUsernameInline, () -> buttonSaveUsernameInline.performClick());
        buttonSaveUsernameInline.setOnClickListener(v -> saveUsername(editTextUsernameInline.getText().toString().trim(), null));
        
        imageProfile.setOnClickListener(v -> {
            if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                showImagePreviewDialog(currentImageUrl, currentLoadedUsername);
            }
        });

        // Add GlobalLayoutListener to detect keyboard open and scroll if auth fields are focused
        root.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            private int previousHeight = -1;

            @Override
            public void onGlobalLayout() {
                int height = root.getHeight();
                if (previousHeight != -1) {
                    int diff = previousHeight - height;
                    if (diff > 200) { // Height decreased significantly -> Keyboard likely opened
                        if (editTextEmailInline.hasFocus() || editTextPasswordInline.hasFocus()) {
                            triggerAuthScroll();
                        }
                    }
                }
                previousHeight = height;
            }
        });
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.view.autofill.AutofillManager autofillManager = getSystemService(android.view.autofill.AutofillManager.class);
            if (autofillManager != null) {
                autofillManager.registerCallback(new android.view.autofill.AutofillManager.AutofillCallback() {
                    @Override
                    public void onAutofillEvent(@NonNull View view, int event) {
                        super.onAutofillEvent(view, event);
                    }
                });
            }
        }

        setupSyncSwitch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            user.reload().addOnCompleteListener(task -> {
                if (isFinishing() || isDestroyed()) return;
                checkEmailVerification();
            });
        }
    }

    private void checkEmailVerification() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && !userRepository.isUserVerified()) {
            // Determine if it was a link or a new registration for revert logic
            // If they have other providers, it was likely a link.
            boolean hasOtherProvider = false;
            for (UserInfo profile : user.getProviderData()) {
                if (!EmailAuthProvider.PROVIDER_ID.equals(profile.getProviderId()) &&
                    !profile.getProviderId().equals("firebase")) {
                    hasOtherProvider = true;
                    break;
                }
            }
            showVerificationDialog(user, hasOtherProvider);
        } else if (user != null) {
            userRepository.syncEmailToFirestore();
        }
    }

    private void triggerAuthScroll() {
        // Delay to allow keyboard to appear and layout to resize
        if (containerContent != null) {
            containerContent.postDelayed(() -> {
                if (layoutAuthInline != null && containerContent instanceof android.widget.ScrollView) {
                    android.widget.ScrollView scrollView = (android.widget.ScrollView) containerContent;
                    int bottom = layoutAuthInline.getBottom() + layoutAuthInline.getPaddingBottom();
                    int height = scrollView.getHeight();
                    int targetScrollY = bottom - height + 20; 
                    scrollView.smoothScrollTo(0, Math.max(0, targetScrollY));
                }
            }, 500);
        }
    }

    // --- Auth Dialog Implementation ---

    private void showAuthDialog() {
        cancelAutofill();
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_auth, null);
        builder.setView(dialogView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        EditText editTextEmail = dialogView.findViewById(R.id.edit_text_email);
        EditText editTextPassword = dialogView.findViewById(R.id.edit_text_password);
        TextInputLayout inputLayoutPassword = dialogView.findViewById(R.id.input_layout_password);
        
        View buttonLogin = dialogView.findViewById(R.id.button_login);
        View buttonRegister = dialogView.findViewById(R.id.button_register);

        AuthUiHelper.setupEmailPasswordFlow(editTextEmail, editTextPassword, buttonLogin::performClick);
        AuthUiHelper.setupPasswordToggle(inputLayoutPassword, editTextPassword, isVisible -> isTogglingPasswordVisibility = isVisible);

        TextView errorTextEmail = dialogView.findViewById(R.id.error_text_email);
        TextView errorTextPassword = dialogView.findViewById(R.id.error_text_password);
        TextView errorTextGeneral = dialogView.findViewById(R.id.error_text_general);
        View authLoadingContainer = dialogView.findViewById(R.id.auth_loading_container);
        View dot1 = dialogView.findViewById(R.id.dot1);
        View dot2 = dialogView.findViewById(R.id.dot2);
        View dot3 = dialogView.findViewById(R.id.dot3);
        View textForgotPassword = dialogView.findViewById(R.id.text_forgot_password);
        View buttonClose = dialogView.findViewById(R.id.button_dialog_close);

        android.text.TextWatcher clearErrorWatcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { 
                errorTextEmail.setVisibility(View.GONE);
                errorTextPassword.setVisibility(View.GONE);
                errorTextGeneral.setVisibility(View.GONE);

                // If many characters are added at once, it's very likely an autofill.
                if (!isTogglingPasswordVisibility && count > 3 && editTextEmail.getText().length() > 0 && editTextPassword.getText().length() > 0) {
                    editTextEmail.postDelayed(() -> {
                        hideKeyboard(editTextEmail);
                        editTextEmail.clearFocus();
                        editTextPassword.clearFocus();
                    }, 100);
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        editTextEmail.addTextChangedListener(clearErrorWatcher);
        editTextPassword.addTextChangedListener(clearErrorWatcher);

        buttonLogin.setOnClickListener(v -> performDialogLogin(dialog, editTextEmail, editTextPassword, errorTextEmail, errorTextPassword, errorTextGeneral, authLoadingContainer, dot1, dot2, dot3, buttonLogin, buttonRegister));
        buttonRegister.setOnClickListener(v -> performDialogRegister(dialog, editTextEmail, editTextPassword, errorTextEmail, errorTextPassword, errorTextGeneral, authLoadingContainer, dot1, dot2, dot3, buttonLogin, buttonRegister));
        textForgotPassword.setOnClickListener(v -> performDialogResetPassword(editTextEmail, errorTextEmail, authLoadingContainer, dot1, dot2, dot3));
        buttonClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void performDialogLogin(androidx.appcompat.app.AlertDialog dialog, EditText emailField, EditText passwordField, TextView errorEmail, TextView errorPassword, TextView errorGeneral, View loadingContainer, View d1, View d2, View d3, View btnLogin, View btnRegister) {
        String input = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (input.isEmpty()) { showError(errorEmail, getString(R.string.error_email_or_username_required)); return; }
        if (password.isEmpty()) { showError(errorPassword, getString(R.string.error_password_required)); return; }

        hideKeyboard(emailField);
        emailField.setEnabled(false);
        passwordField.setEnabled(false);
        loadingContainer.setVisibility(View.VISIBLE);
        BaseActivity.startDotsAnimation(d1, d2, d3);
        btnLogin.setEnabled(false);
        btnRegister.setEnabled(false);

        Runnable onLoginSuccess = () -> {
            emailField.setEnabled(true);
            passwordField.setEnabled(true);
            commitAutofill();
            if (dialog != null) dialog.dismiss();
            userRepository.syncEmailToFirestore();
            UiUtils.makeCustomToast(ProfileActivity.this, getString(R.string.auth_success), Toast.LENGTH_SHORT).show();
            ShoppingListRepository repository = new ShoppingListRepository(getApplicationContext());
            repository.migrateLocalListsToCloud(() -> {
                loadCurrentProfile(); 
            });
        };
        
        Runnable onError = () -> {
             loadingContainer.setVisibility(View.GONE);
             d1.clearAnimation(); d2.clearAnimation(); d3.clearAnimation();
             btnLogin.setEnabled(true);
             btnRegister.setEnabled(true);
             emailField.setEnabled(true);
             passwordField.setEnabled(true);
        };

        if (AuthValidator.isValidEmail(input)) {
            performAuthLogin(input, password, onLoginSuccess, errorGeneral, onError, emailField, passwordField);
        } else {
            Runnable findAction = () -> userRepository.findEmailByUsername(input, new UserRepository.OnEmailLoadedListener() {
                @Override public void onLoaded(String email) { performAuthLogin(email, password, onLoginSuccess, errorGeneral, onError, emailField, passwordField); }
                @Override public void onError(String error) { onError.run(); showError(errorEmail, error); }
            });

            if (mAuth.getCurrentUser() == null) {
                mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        findAction.run();
                    } else {
                        onError.run();
                        showError(errorEmail, getString(R.string.error_auth_failed, task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
            } else {
                findAction.run();
            }
        }
    }

    private void performAuthLogin(String email, String password, Runnable onSuccess, TextView errorView, Runnable onError, EditText emailField, EditText passwordField) {
        // Force refresh App Check token to ensure validity before sensitive auth operation
        com.google.firebase.appcheck.FirebaseAppCheck.getInstance().getAppCheckToken(true)
            .addOnCompleteListener(tokenTask -> {
                if (!tokenTask.isSuccessful()) {
                    android.util.Log.e("AppCheck", "Token refresh failed", tokenTask.getException());
                }
                // Proceed regardless of token success (Auth might work if not enforced, or use cached)
                // But mostly this delay ensures the provider is active.
                
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null && currentUser.isAnonymous()) {
                     // Note: verifyCredentialsAndSwitch uses secondary app which might need its own App Check token.
                     // But we removed secondary app logic? Let's check verifyCredentialsAndSwitch implementation.
                     verifyCredentialsAndSwitch(email, password, onSuccess, errorView, onError, emailField, passwordField);
                } else if (currentUser == null) {
                     checkIfAccountIsEmpty(isEmpty -> {
                         if (isEmpty) {
                             mAuth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this, task -> {
                                    if (task.isSuccessful()) {
                                        onSuccess.run();
                                    } else {
                                        onError.run();
                                        showError(errorView, AuthErrorMapper.getErrorMessage(this, task.getException()));
                                    }
                                });
                         } else {
                             hideLoading();
                             showCustomDialog(
                                 getString(R.string.dialog_switch_account_title),
                                 getString(R.string.dialog_switch_account_message),
                                 getString(R.string.button_switch_and_link),
                                 () -> {
                                     initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_profile, 1);
                                     showLoading(getString(R.string.loading), true, true);
                                     performSwitchLogin(email, password, onSuccess, errorView, onError);
                                 },
                                 () -> {
                                     hideLoading();
                                     onError.run();
                                     cancelAutofill();
                                     if (emailField != null) emailField.setText("");
                                     if (passwordField != null) passwordField.setText("");
                                 }
                             );
                         }
                     });
                } else {
                     mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                onSuccess.run();
                            } else {
                                onError.run();
                                showError(errorView, AuthErrorMapper.getErrorMessage(this, task.getException()));
                            }
                        });
                }
            });
    }

    private void verifyCredentialsAndSwitch(String email, String password, Runnable onSuccess, TextView errorView, Runnable onError, EditText emailField, EditText passwordField) {
        com.google.firebase.FirebaseOptions options = com.google.firebase.FirebaseApp.getInstance().getOptions();
        com.google.firebase.FirebaseApp secondaryApp;
        try { secondaryApp = com.google.firebase.FirebaseApp.getInstance("secondary"); } 
        catch (IllegalStateException e) { secondaryApp = com.google.firebase.FirebaseApp.initializeApp(this, options, "secondary"); }
        
        // Install App Check for secondary app to avoid "No AppCheckProvider installed"
        com.google.firebase.appcheck.FirebaseAppCheck secondaryAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance(secondaryApp);
        if (BuildConfig.DEBUG) {
             secondaryAppCheck.installAppCheckProviderFactory(com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance());
        } else {
             secondaryAppCheck.installAppCheckProviderFactory(com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance());
        }

        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
        secondaryAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    checkIfAccountIsEmpty(isEmpty -> {
                        if (isEmpty) {
                            performSwitchLogin(email, password, onSuccess, errorView, onError);
                        } else {
                            hideLoading(); 
                            // Dialog remains hidden here? No, we need to handle this.
                            // If we show a NEW dialog (CustomDialog), the AuthDialog is still hidden.
                            // When CustomDialog closes, what happens?
                            // We should probably close AuthDialog if we proceed to Switch.
                            // But if user cancels switch? We need to show AuthDialog again.
                            
                            // Simplified flow: Close auth dialog, show switch dialog.
                            // But onError callback shows AuthDialog again? No, onError is for failures.
                            
                            showCustomDialog(
                                getString(R.string.dialog_switch_account_title),
                                getString(R.string.dialog_switch_account_message),
                                getString(R.string.button_switch_and_link),
                                () -> {
                                    initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_profile, 1);
                                    showLoading(getString(R.string.loading), true, true);
                                    performSwitchLogin(email, password, onSuccess, errorView, onError);
                                },
                                () -> {
                                    hideLoading();
                                    onError.run();
                                    cancelAutofill();
                                    if (emailField != null) emailField.setText("");
                                    if (passwordField != null) passwordField.setText("");
                                }
                            );
                            // If user cancels custom dialog? Auth dialog is hidden forever.
                            // showCustomDialog doesn't support cancel callback properly here.
                            // We assume user proceeds or we are stuck.
                            // Let's assume switch is the path.
                        }
                    });
                } else {
                    onError.run();
                    showError(errorView, AuthErrorMapper.getErrorMessage(this, task.getException()));
                }
            });
    }

    private void cleanupAnonymousUserAndThen(Runnable nextAction) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.isAnonymous()) {
            ShoppingListRepository repo = new ShoppingListRepository(getApplicationContext());
            repo.deleteAllUserData(() -> {
                userRepository.deleteAccount(new UserRepository.OnProfileActionListener() {
                    @Override
                    public void onSuccess() {
                        nextAction.run();
                    }

                    @Override
                    public void onError(String error) {
                        // Even if deletion fails (e.g. network), we proceed to avoid blocking the user
                        android.util.Log.e("ProfileActivity", "Failed to delete anonymous account: " + error);
                        nextAction.run();
                    }
                });
            });
        } else {
            nextAction.run();
        }
    }

    private void performSwitchLogin(String email, String password, Runnable onSuccess, TextView errorView, Runnable onError) {
        ShoppingListRepository repo = new ShoppingListRepository(getApplicationContext());
        repo.clearLocalDatabase();
        cleanupAnonymousUserAndThen(() -> {
            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) onSuccess.run();
                    else { onError.run(); showError(errorView, AuthErrorMapper.getErrorMessage(this, task.getException())); }
                });
        });
    }

    private void performDialogRegister(androidx.appcompat.app.AlertDialog dialog, EditText emailField, EditText passwordField, TextView errorEmail, TextView errorPassword, TextView errorGeneral, View loadingContainer, View d1, View d2, View d3, View btnLogin, View btnRegister) {
        String input = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (input.isEmpty()) { showError(errorEmail, getString(R.string.error_email_required)); return; }

        Runnable startRegistration = () -> {
            if (password.isEmpty()) { showError(errorPassword, getString(R.string.error_password_required)); return; }
            if (!AuthValidator.isValidPassword(password)) { showError(errorPassword, getString(R.string.error_password_short)); return; }

            hideKeyboard(emailField);
            emailField.setEnabled(false);
            passwordField.setEnabled(false);
            loadingContainer.setVisibility(View.VISIBLE);
            BaseActivity.startDotsAnimation(d1, d2, d3);
            btnLogin.setEnabled(false);
            btnRegister.setEnabled(false);

            Runnable onErrorAction = () -> {
                loadingContainer.setVisibility(View.GONE);
                d1.clearAnimation(); d2.clearAnimation(); d3.clearAnimation();
                btnLogin.setEnabled(true);
                btnRegister.setEnabled(true);
                emailField.setEnabled(true);
                passwordField.setEnabled(true);
            };

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                AuthCredential credential = EmailAuthProvider.getCredential(input, password);
                currentUser.linkWithCredential(credential)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                emailField.setEnabled(true);
                                passwordField.setEnabled(true);
                                commitAutofill();
                                startVerificationFlow(currentUser, true, dialog);
                            } else {
                                onErrorAction.run();
                                String msg = AuthErrorMapper.getErrorMessage(this, task.getException());
                                if (task.getException() instanceof FirebaseAuthUserCollisionException) msg = getString(R.string.error_email_collision_link);
                                showError(errorEmail, msg);
                            }
                        });
            } else {
                mAuth.createUserWithEmailAndPassword(input, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                emailField.setEnabled(true);
                                passwordField.setEnabled(true);
                                commitAutofill();
                                startVerificationFlow(task.getResult().getUser(), false, dialog);
                            } else {
                                onErrorAction.run();
                                showError(errorEmail, AuthErrorMapper.getErrorMessage(this, task.getException()));
                            }
                        });
            }
        };

        if (AuthValidator.isValidEmail(input)) {
            startRegistration.run();
        } else {
            // Not a valid email, check if it's a potential username
            if (!isTogglingPasswordVisibility && !input.contains("@") && input.length() >= 3) {
                hideKeyboard(emailField);
                loadingContainer.setVisibility(View.VISIBLE);
                BaseActivity.startDotsAnimation(d1, d2, d3);
                btnLogin.setEnabled(false);
                btnRegister.setEnabled(false);

                Runnable findAction = () -> userRepository.findEmailByUsername(input, new UserRepository.OnEmailLoadedListener() {
                    @Override
                    public void onLoaded(String email) {
                        loadingContainer.setVisibility(View.GONE);
                        d1.clearAnimation(); d2.clearAnimation(); d3.clearAnimation();
                        btnLogin.setEnabled(true);
                        btnRegister.setEnabled(true);
                        showError(errorEmail, getString(R.string.error_username_exists_during_registration));
                    }

                    @Override
                    public void onError(String error) {
                        loadingContainer.setVisibility(View.GONE);
                        d1.clearAnimation(); d2.clearAnimation(); d3.clearAnimation();
                        btnLogin.setEnabled(true);
                        btnRegister.setEnabled(true);
                        // If it's a "not found" error, it's just an invalid email for registration
                        showError(errorEmail, getString(R.string.error_invalid_email));
                    }
                });

                if (mAuth.getCurrentUser() == null) {
                    mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) findAction.run();
                        else {
                            loadingContainer.setVisibility(View.GONE);
                            d1.clearAnimation(); d2.clearAnimation(); d3.clearAnimation();
                            btnLogin.setEnabled(true);
                            btnRegister.setEnabled(true);
                            showError(errorEmail, getString(R.string.error_invalid_email));
                        }
                    });
                } else {
                    findAction.run();
                }
            } else {
                showError(errorEmail, getString(R.string.error_invalid_email));
            }
        }
    }

    private void performDialogResetPassword(EditText emailField, TextView errorEmail, View loadingContainer, View d1, View d2, View d3) {
        String email = emailField.getText().toString().trim();
        if (email.isEmpty()) { showError(errorEmail, getString(R.string.error_email_required)); return; }
        
        hideKeyboard(emailField);
        emailField.setEnabled(false);
        loadingContainer.setVisibility(View.VISIBLE);
        BaseActivity.startDotsAnimation(d1, d2, d3);
        
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(t -> {
            loadingContainer.setVisibility(View.GONE);
            d1.clearAnimation(); d2.clearAnimation(); d3.clearAnimation();
            emailField.setEnabled(true);
            if (t.isSuccessful()) UiUtils.makeCustomToast(this, getString(R.string.email_sent), Toast.LENGTH_SHORT).show();
            else showError(errorEmail, AuthErrorMapper.getErrorMessage(this, t.getException()));
        });
    }

    private void startVerificationFlow(FirebaseUser user, boolean isLinkedAccount, androidx.appcompat.app.AlertDialog authDialog) {
        userRepository.syncEmailToFirestore();
        userRepository.setSyncPreference(true, null); // Enable sync by default on registration
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    // Dialog remains open but loading finished? Or dismiss dialog and show verification dialog?
                    // We dismiss authDialog here.
                    if (authDialog != null) authDialog.dismiss();
                    if (task.isSuccessful()) {
                        showVerificationDialog(user, isLinkedAccount);
                    } else {
                        UiUtils.makeCustomToast(this, getString(R.string.error_send_email_failed, task.getException().getMessage()), Toast.LENGTH_LONG).show();
                        revertRegistration(user, isLinkedAccount);
                    }
                });
    }

    private void showVerificationDialog(FirebaseUser user, boolean isLinkedAccount) {
        if (verificationDialog != null && verificationDialog.isShowing()) return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_vertical_buttons, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        verificationDialog = builder.create();
        if (verificationDialog.getWindow() != null) verificationDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        TextView textTitle = dialogView.findViewById(R.id.dialog_title);
        TextView textMessage = dialogView.findViewById(R.id.dialog_message);
        MaterialButton btnPositive = dialogView.findViewById(R.id.dialog_button_positive);
        MaterialButton btnNegative = dialogView.findViewById(R.id.dialog_button_negative);

        textTitle.setText(R.string.dialog_verify_email_title);
        textMessage.setText(getString(R.string.dialog_verify_email_message, user.getEmail()));
        btnPositive.setText(R.string.button_confirmed);
        btnNegative.setText(R.string.button_cancel_delete);

        btnPositive.setOnClickListener(v -> {
            user.reload().addOnCompleteListener(reloadTask -> {
                if (reloadTask.isSuccessful()) {
                    if (user.isEmailVerified()) {
                        verificationDialog.dismiss();
                        verificationDialog = null;
                        userRepository.syncEmailToFirestore();
                        UiUtils.makeCustomToast(ProfileActivity.this, getString(R.string.auth_success), Toast.LENGTH_SHORT).show();
                        loadCurrentProfile();
                    } else {
                        UiUtils.makeCustomToast(ProfileActivity.this, R.string.toast_email_not_verified, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    UiUtils.makeCustomToast(ProfileActivity.this, getString(R.string.error_check_failed, reloadTask.getException().getMessage()), Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnNegative.setOnClickListener(v -> {
             verificationDialog.dismiss();
             verificationDialog = null;
             revertRegistration(user, isLinkedAccount);
        });
        verificationDialog.show();
    }

    private void revertRegistration(FirebaseUser user, boolean isLinkedAccount) {
        initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_profile, 1);
        showLoading(getString(R.string.loading), true, true);
        Runnable onComplete = () -> {
            hideLoading();
            loadCurrentProfile();
        };

        if (isLinkedAccount) {
            user.unlink(EmailAuthProvider.PROVIDER_ID).addOnCompleteListener(t -> onComplete.run());
        } else {
            user.delete().addOnCompleteListener(t -> onComplete.run());
        }
    }

    private void showError(TextView errorView, String message) {
        if (errorView != null) {
            errorView.setText(message);
            errorView.setVisibility(View.VISIBLE);
        }
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
        switchSyncPrivate.setOnCheckedChangeListener((v, isChecked) -> {
            userRepository.setSyncPreference(isChecked, new UserRepository.OnProfileActionListener() {
                @Override
                public void onSuccess() {
                    // Also update local prefs for immediate use in other parts of the app if needed
                    SharedPreferences prefs = getSharedPreferences(ShoppingListManager.SETTINGS_PREFS, MODE_PRIVATE);
                    prefs.edit().putBoolean(ShoppingListManager.KEY_SYNC_PRIVATE_DEFAULT, isChecked).apply();
                }

                @Override
                public void onError(String message) {
                    // Optional: revert switch if needed
                }
            });
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
        EditText editText = includeDialog.findViewById(R.id.field_id_x_secure_no_fill);
        
        AuthUiHelper.disableAutofillForView(editText);
        
        View btnNew = dialogView.findViewById(R.id.button_option_new_image);
        View btnRemove = dialogView.findViewById(R.id.button_option_remove_image);
        View btnSave = includeDialog.findViewById(R.id.username_action_button);
        View btnClose = dialogView.findViewById(R.id.button_dialog_close);

        editText.setText(currentLoadedUsername != null ? currentLoadedUsername : "");

        AuthUiHelper.setupActionDone(editText, () -> saveUsername(editText.getText().toString().trim(), dialog));

        btnNew.setOnClickListener(v -> {
            if (currentLoadedUsername == null || currentLoadedUsername.isEmpty()) {
                UiUtils.makeCustomToast(this, R.string.profile_error_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            pickImageLauncher.launch("image/*");
            dialog.dismiss();
        });
        btnRemove.setOnClickListener(v -> {
            if (currentLoadedUsername == null || currentLoadedUsername.isEmpty()) {
                UiUtils.makeCustomToast(this, R.string.profile_error_empty, Toast.LENGTH_SHORT).show();
                return;
            }
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
        
        // Use specific skeleton for anonymous user setting username
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.isAnonymous()) {
            initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_profile_anonymous_with_username, 1);
        } else {
            // Default skeleton for others
            initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_profile, 1);
        }

        showLoading(getString(R.string.loading_saving), true); // showSkeleton=true to trigger immediate visual update
        
        Runnable proceedToSave = () -> {
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
                    
                    // If launched with focus request (likely from MainActivity), finish to return
                    if (getIntent().getBooleanExtra("EXTRA_FOCUS_USERNAME", false)) {
                        finish();
                        return;
                    }
                    
                    if (isInitialProfileCreation) { 
                        loadCurrentProfile(); 
                    }
                }
                @Override
                public void onError(String message) { 
                    hideLoading();
                    UiUtils.makeCustomToast(ProfileActivity.this, message, Toast.LENGTH_LONG).show(); 
                }
            });
        };

        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    proceedToSave.run();
                } else {
                    hideLoading();
                    String msg = task.getException() != null ? task.getException().getMessage() : "Auth failed";
                    UiUtils.makeCustomToast(ProfileActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            proceedToSave.run();
        }
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
        initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_profile, 1);
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
                            userRepository.setSyncPreference(true, null);
                            syncGoogleProfilePicture();
                            triggerSync();
                            loadCurrentProfile();
                        } else {
                             if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                 checkIfAccountIsEmpty(isEmpty -> {
                                     // Only skip dialog if account is truly empty AND anonymous (temp user)
                                     if (isEmpty && mAuth.getCurrentUser().isAnonymous()) {
                                         performGoogleSignIn(credential);
                                     } else {
                                         hideLoading();
                                         int messageId = mAuth.getCurrentUser().isAnonymous() 
                                             ? R.string.dialog_switch_account_message 
                                             : R.string.dialog_switch_account_message_conflict;
                                         
                                         showCustomDialog(
                                             getString(R.string.dialog_switch_account_title),
                                             getString(messageId),
                                             getString(R.string.button_switch_and_link),
                                             () -> performGoogleSignIn(credential),
                                             () -> {
                                                 hideLoading();
                                                 cancelAutofill();
                                             }
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
            checkIfAccountIsEmpty(isEmpty -> {
                if (isEmpty) {
                    performGoogleSignIn(credential);
                } else {
                    hideLoading();
                    showCustomDialog(
                        getString(R.string.dialog_switch_account_title),
                        getString(R.string.dialog_switch_account_message),
                        getString(R.string.button_switch_and_link),
                        () -> performGoogleSignIn(credential),
                        () -> {
                            hideLoading();
                            cancelAutofill();
                        }
                    );
                }
            });
        }
    }

    private interface OnAccountEmptyCheckListener {
        void onResult(boolean isEmpty);
    }

    private void checkIfAccountIsEmpty(OnAccountEmptyCheckListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        ShoppingListRepository listRepo = new ShoppingListRepository(ProfileActivity.this);
        boolean hasLocalLists = listRepo.getLocalListCount() > 0;

        if (user == null) {
            listener.onResult(!hasLocalLists);
            return;
        }

        if (!user.isAnonymous()) {
            // Already logged in with permanent account
            listener.onResult(!hasLocalLists);
            return;
        }

        userRepository.getUserProfile(new UserRepository.OnUserProfileLoadedListener() {
            @Override
            public void onLoaded(String username, String imageUrl, boolean syncPrivate) {
                if (username != null && !username.isEmpty()) {
                    listener.onResult(false);
                    return;
                }
                if (hasLocalLists) {
                     listener.onResult(false);
                } else {
                     listener.onResult(true);
                }
            }
            @Override
            public void onError(String error) {
                listener.onResult(!hasLocalLists);
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
        initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_profile, 1);
        showLoading();
        ShoppingListRepository repo = new ShoppingListRepository(getApplicationContext());
        repo.clearLocalDatabase();
        cleanupAnonymousUserAndThen(() -> {
            mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().getAdditionalUserInfo() != null && task.getResult().getAdditionalUserInfo().isNewUser()) {
                            userRepository.setSyncPreference(true, null);
                        }
                        syncGoogleProfilePicture();
                        loadCurrentProfile();
                    } else {
                        hideLoading();
                        String msg = AuthErrorMapper.getErrorMessage(ProfileActivity.this, task.getException());
                        UiUtils.makeCustomToast(ProfileActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                });
        });
    }

    private void syncGoogleProfilePicture() {
        userRepository.getUserProfile(new UserRepository.OnUserProfileLoadedListener() {
            @Override
            public void onLoaded(String username, String imageUrl, boolean syncPrivate) {
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
        userRepository.syncEmailToFirestore();
        // Reset skeleton to profile type in case it was changed to auth type during sign out
        int skeletonResId = isSigningOut ? R.layout.skeleton_logout : R.layout.skeleton_profile;
        initLoadingOverlay(findViewById(R.id.profile_content_container), skeletonResId, 1);
        showLoading(getString(R.string.loading_profile), true, true);
        containerContent.setVisibility(View.GONE);
        Runnable fetchProfileData = () -> {
            userRepository.getUserProfile(new UserRepository.OnUserProfileLoadedListener() {
                @Override
                public void onLoaded(String username, String imageUrl, boolean syncPrivate) {
                    if (isFinishing() || isDestroyed()) return;
                    hideLoading();
                    containerContent.setVisibility(View.VISIBLE);
                    
                    currentImageUrl = imageUrl;

                    // Update sync switch without triggering listener
                    switchSyncPrivate.setOnCheckedChangeListener(null);
                    switchSyncPrivate.setChecked(syncPrivate);
                    setupSyncSwitch();

                    // Also update local prefs to be in sync
                    SharedPreferences prefs = getSharedPreferences(ShoppingListManager.SETTINGS_PREFS, MODE_PRIVATE);
                    prefs.edit().putBoolean(ShoppingListManager.KEY_SYNC_PRIVATE_DEFAULT, syncPrivate).apply();

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
                        
                        if (getIntent().getBooleanExtra("EXTRA_FOCUS_USERNAME", false)) {
                            getIntent().removeExtra("EXTRA_FOCUS_USERNAME"); // Consume the extra
                            editTextUsernameInline.requestFocus();
                            // Show keyboard
                            containerContent.postDelayed(() -> {
                                if (isFinishing() || isDestroyed()) return;
                                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                                imm.showSoftInput(editTextUsernameInline, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                            }, 200);
                        }
                    }
                    if (isSigningOut) {
                        UiUtils.makeCustomToast(ProfileActivity.this, R.string.toast_signed_out, Toast.LENGTH_SHORT).show();
                        isSigningOut = false;
                    }
                    updateUIForUsername();
                    updateAuthUI();
                }
                @Override
                public void onError(String error) {
                    if (isFinishing() || isDestroyed()) return;
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
        fetchProfileData.run();
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
            layoutAuthInline.setVisibility(View.GONE);
            
            layoutLinkedMethods.removeAllViews();
            
            // Define order: Email, Google, Apple (future)
            UserInfo emailProfile = null;
            UserInfo googleProfile = null;

            for (UserInfo profile : user.getProviderData()) {
                if (EmailAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) emailProfile = profile;
                else if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) googleProfile = profile;
            }

            if (emailProfile != null) addProviderView(emailProfile, R.drawable.ic_email, getString(R.string.linked_email, emailProfile.getEmail()));
            if (googleProfile != null) addProviderView(googleProfile, R.drawable.ic_google_logo, getString(R.string.linked_google, googleProfile.getEmail()));

            buttonSignOut.setVisibility(View.VISIBLE);
            findViewById(R.id.layout_auth_buttons).setVisibility(View.VISIBLE);
            
            // Reset visibility of auth buttons for linked state
            buttonRegisterGoogle.setVisibility(View.VISIBLE);
            buttonRegisterEmail.setVisibility(View.VISIBLE);
            buttonDelete.setVisibility(View.VISIBLE);
            
            boolean isGoogleLinked = googleProfile != null;
            boolean isEmailLinked = emailProfile != null;

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
                buttonRegisterEmail.setOnClickListener(v -> showAuthDialog());
            }
        } else {
            textViewWarning.setVisibility(View.VISIBLE);
            cardSyncPreferences.setVisibility(View.GONE);
            if (cardLinkedMethods != null) cardLinkedMethods.setVisibility(View.GONE);
            findViewById(R.id.layout_auth_buttons).setVisibility(View.VISIBLE);
            
            boolean shouldShowLinkText = currentLoadedUsername != null && !currentLoadedUsername.isEmpty();
            
            if (!shouldShowLinkText) {
                // Anonymous & No Username -> Show Inline Auth
                buttonRegisterEmail.setVisibility(View.GONE);
                layoutAuthInline.setVisibility(View.VISIBLE);
                
                // Show inline Google button, hide the one in standard list
                buttonGoogleInline.setVisibility(View.VISIBLE);
                buttonRegisterGoogle.setVisibility(View.GONE);
                
                // Clear fields if they were used previously
                if (editTextEmailInline.getText().length() > 0) editTextEmailInline.setText("");
                if (editTextPasswordInline.getText().length() > 0) editTextPasswordInline.setText("");
                
                // Ensure loading state is reset
                findViewById(R.id.auth_loading_container_inline).setVisibility(View.GONE);
                buttonLoginInline.setEnabled(true);
                buttonRegisterInline.setEnabled(true);
                buttonGoogleInline.setEnabled(true);
                editTextEmailInline.setEnabled(true);
                editTextPasswordInline.setEnabled(true);
            } else {
                buttonRegisterEmail.setVisibility(View.VISIBLE);
                layoutAuthInline.setVisibility(View.GONE);
                
                // Standard logic:
                buttonRegisterEmail.setText(R.string.action_sign_in_email);
                
                // Show standard Google button (will be handled by logic below)
                buttonRegisterGoogle.setVisibility(View.VISIBLE);
            }
            
            // Adjust button text based on username presence? 
            if (buttonRegisterEmail.getVisibility() == View.VISIBLE) {
                buttonRegisterEmail.setText(shouldShowLinkText ? R.string.action_link_email : R.string.action_sign_in_email);
                buttonRegisterEmail.setIconResource(R.drawable.ic_email);
                int colorOnSurface = com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, android.graphics.Color.BLACK);
                buttonRegisterEmail.setIconTint(android.content.res.ColorStateList.valueOf(colorOnSurface));
                buttonRegisterEmail.setOnClickListener(v -> showAuthDialog());
            }

            // Only update standard Google button if visible (it might be hidden if inline is shown)
            if (buttonRegisterGoogle.getVisibility() == View.VISIBLE) {
                buttonRegisterGoogle.setText(shouldShowLinkText ? R.string.action_link_google : R.string.sign_in_google);
                buttonRegisterGoogle.setIconResource(R.drawable.ic_google_logo);
                buttonRegisterGoogle.setIconTint(null);
                buttonRegisterGoogle.setOnClickListener(v -> signInWithGoogle());
            }
            buttonSignOut.setVisibility(View.GONE);
            
            // Hide delete account button and warning if no username set (fresh anonymous user)
            if (!shouldShowLinkText) {
                buttonDelete.setVisibility(View.GONE);
                textViewWarning.setVisibility(View.GONE);
            } else {
                buttonDelete.setVisibility(View.VISIBLE);
                textViewWarning.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void addProviderView(UserInfo profile, int iconRes, String textContent) {
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
        
        if (iconRes == R.drawable.ic_google_logo && profile.getPhotoUrl() != null) {
            Glide.with(this).load(profile.getPhotoUrl()).apply(RequestOptions.circleCropTransform()).into(icon);
        } else {
            icon.setImageResource(iconRes);
            if (iconRes == R.drawable.ic_email) {
                icon.setColorFilter(ContextCompat.getColor(this, R.color.text_primary_adaptive));
            }
        }
        
        text.setText(textContent);
        row.addView(icon);
        row.addView(text);
        layoutLinkedMethods.addView(row);
    }

    private void confirmUnlink(String providerId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        int providerCount = 0;
        for (UserInfo profile : user.getProviderData()) {
            if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId()) ||
                EmailAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                providerCount++;
            }
        }

        boolean isLastProvider = providerCount <= 1;
        boolean hasUsername = currentLoadedUsername != null && !currentLoadedUsername.isEmpty();

        if (isLastProvider && !hasUsername) {
            showVerticalDialog(
                getString(R.string.dialog_delete_account_title),
                getString(R.string.dialog_unlink_last_no_username_message),
                getString(R.string.button_delete),
                this::deleteAccount
            );
        } else {
            showVerticalDialog(getString(R.string.dialog_unlink_title), getString(R.string.dialog_unlink_message), getString(R.string.button_unlink), () -> unlinkProvider(providerId));
        }
    }

    private void unlinkProvider(String providerId) {
        initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_profile, 1);
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
                             initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_profile, 1);
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
        ShoppingListRepository repo = new ShoppingListRepository(getApplicationContext());
        if (repo.hasUnsyncedLists()) {
             showThreeOptionDialog(
                 getString(R.string.button_sign_out),
                 getString(R.string.dialog_sign_out_message_unsaved),
                 getString(R.string.button_upload_and_sign_out),
                 getString(R.string.button_delete_and_sign_out),
                 this::performSafeSignOut, // Positive: Upload & Out
                 this::performDestructiveSignOut // Neutral: Delete & Out
             );
        } else {
             showCustomDialog(getString(R.string.button_sign_out), getString(R.string.dialog_sign_out_message_safe), getString(R.string.button_sign_out), this::performSimpleSignOut);
        }
    }

    private void showThreeOptionDialog(String title, String message, String positiveText, String neutralText, Runnable onPositive, Runnable onNeutral) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Use vertical layout to accommodate 3 buttons without cramped text
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_vertical_buttons, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        TextView textTitle = dialogView.findViewById(R.id.dialog_title);
        TextView textMessage = dialogView.findViewById(R.id.dialog_message);
        MaterialButton btnPositive = dialogView.findViewById(R.id.dialog_button_positive);
        MaterialButton btnNegative = dialogView.findViewById(R.id.dialog_button_negative);
        MaterialButton btnNeutral = dialogView.findViewById(R.id.dialog_button_neutral);

        textTitle.setText(title);
        textMessage.setText(message);
        btnPositive.setText(positiveText);
        btnNeutral.setText(neutralText);
        btnNeutral.setVisibility(View.VISIBLE);
        // Make neutral button red/warning style?
        btnNeutral.setTextColor(ContextCompat.getColor(this, R.color.red));

        btnPositive.setOnClickListener(v -> { onPositive.run(); dialog.dismiss(); });
        btnNeutral.setOnClickListener(v -> { onNeutral.run(); dialog.dismiss(); });
        btnNegative.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showVerticalDialog(String title, String message, String positiveText, Runnable onPositive) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_vertical_buttons, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        TextView textTitle = dialogView.findViewById(R.id.dialog_title);
        TextView textMessage = dialogView.findViewById(R.id.dialog_message);
        MaterialButton btnPositive = dialogView.findViewById(R.id.dialog_button_positive);
        MaterialButton btnNegative = dialogView.findViewById(R.id.dialog_button_negative);
        MaterialButton btnNeutral = dialogView.findViewById(R.id.dialog_button_neutral);

        textTitle.setText(title);
        textMessage.setText(message);
        btnPositive.setText(positiveText);
        btnNeutral.setVisibility(View.GONE); // Hide neutral button for 2-option dialog

        btnPositive.setOnClickListener(v -> { onPositive.run(); dialog.dismiss(); });
        btnNegative.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void performSimpleSignOut() {
        try {
            initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_logout, 1);
            showLoading(getString(R.string.loading), true, true);
            isSigningOut = true;
            commitAutofill();
            cancelAutofill();
            ShoppingListRepository repo = new ShoppingListRepository(getApplicationContext());
            repo.clearLocalDatabase();
            mAuth.signOut();
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                 if (!isFinishing() && !isDestroyed()) {
                     restartActivityWithSignOut();
                 }
            });
        } catch (Exception e) {
            e.printStackTrace();
            // Try to recover state
            if (!isFinishing() && !isDestroyed()) {
                loadCurrentProfile();
            }
        }
    }

    private void performDestructiveSignOut() {
        try {
            initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_logout, 1);
            showLoading(getString(R.string.loading), true, true);
            isSigningOut = true;
            commitAutofill();
            cancelAutofill();
            ShoppingListRepository repo = new ShoppingListRepository(getApplicationContext());
            repo.clearLocalDatabase();
            mAuth.signOut();
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                 if (!isFinishing() && !isDestroyed()) {
                     UiUtils.makeCustomToast(this, R.string.toast_local_data_cleared, Toast.LENGTH_SHORT).show();
                     restartActivityWithSignOut();
                 }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (!isFinishing() && !isDestroyed()) {
                loadCurrentProfile();
            }
        }
    }

    private void performSafeSignOut() {
        try {
            initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_logout, 1);
            showLoading(getString(R.string.loading), true, true);
            isSigningOut = true;
            commitAutofill();
            cancelAutofill();
            ShoppingListRepository repo = new ShoppingListRepository(getApplicationContext());
            repo.migrateLocalListsToCloud(() -> {
                 try {
                     repo.clearLocalDatabase();
                     mAuth.signOut();
                     mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                         if (!isFinishing() && !isDestroyed()) {
                             UiUtils.makeCustomToast(this, R.string.toast_lists_uploaded, Toast.LENGTH_SHORT).show();
                             restartActivityWithSignOut();
                         }
                     });
                 } catch (Exception e) {
                     e.printStackTrace();
                     if (!isFinishing() && !isDestroyed()) {
                         loadCurrentProfile();
                     }
                 }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (!isFinishing() && !isDestroyed()) {
                loadCurrentProfile();
            }
        }
    }

    private void removeImage() {
        progressImage.setVisibility(View.VISIBLE);
        imageProfile.setVisibility(View.INVISIBLE);
        userRepository.removeProfileImage(new UserRepository.OnProfileActionListener() {
            @Override
            public void onSuccess() {
                 if (isFinishing() || isDestroyed()) return;
                 progressImage.setVisibility(View.GONE);
                 imageProfile.setVisibility(View.VISIBLE);
                 loadCurrentProfile();
                 UiUtils.makeCustomToast(ProfileActivity.this, R.string.toast_image_removed, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                progressImage.setVisibility(View.GONE);
                imageProfile.setVisibility(View.VISIBLE);
                UiUtils.makeCustomToast(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadImage(Uri imageUri) {
        progressImage.setVisibility(View.VISIBLE);
        imageProfile.setVisibility(View.INVISIBLE);
        userRepository.uploadProfileImage(imageUri, new UserRepository.OnImageUploadListener() {
            @Override
            public void onSuccess(String downloadUrl) {
                if (isFinishing() || isDestroyed()) return;
                progressImage.setVisibility(View.GONE);
                imageProfile.setVisibility(View.VISIBLE);
                currentImageUrl = downloadUrl;
                Glide.with(ProfileActivity.this).load(downloadUrl).apply(RequestOptions.circleCropTransform()).into(imageProfile);
                imageProfile.setPadding(0,0,0,0);
                imageProfile.clearColorFilter();
                UiUtils.makeCustomToast(ProfileActivity.this, R.string.toast_image_uploaded, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                progressImage.setVisibility(View.GONE);
                imageProfile.setVisibility(View.VISIBLE);
                UiUtils.makeCustomToast(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDeleteAccount() {
        showCustomDialog(getString(R.string.dialog_delete_account_title), getString(R.string.dialog_delete_account_message), getString(R.string.button_delete), this::deleteAccount);
    }

    private void showCustomDialog(String title, String message, String positiveButtonText, Runnable onPositiveAction) {
        showCustomDialog(title, message, positiveButtonText, onPositiveAction, null);
    }

    private void showCustomDialog(String title, String message, String positiveButtonText, Runnable onPositiveAction, Runnable onNegativeAction) {
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
        btnNegative.setOnClickListener(v -> { 
            if (onNegativeAction != null) onNegativeAction.run();
            dialog.dismiss(); 
        });
        dialog.show();
    }

    private void restartActivityWithSignOut() {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("EXTRA_IS_SIGNING_OUT", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void commitAutofill() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.view.autofill.AutofillManager afm = getSystemService(android.view.autofill.AutofillManager.class);
            if (afm != null) afm.commit();
        }
    }

    private void cancelAutofill() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.view.autofill.AutofillManager afm = getSystemService(android.view.autofill.AutofillManager.class);
            if (afm != null) afm.cancel();
        }
    }

    private void deleteAccount() {
        initLoadingOverlay(findViewById(R.id.profile_content_container), R.layout.skeleton_logout, 1);
        showLoading(getString(R.string.loading), true, true);
        isSigningOut = true;
        commitAutofill();
        cancelAutofill();
        buttonDelete.setEnabled(false);
        ShoppingListRepository repository = new ShoppingListRepository(getApplicationContext());
        repository.deleteAllUserData(() -> {
            userRepository.deleteAccount(new UserRepository.OnProfileActionListener() {
                @Override
                public void onSuccess() {
                    if (isFinishing() || isDestroyed()) return;
                    hideLoading();
                    UiUtils.makeCustomToast(ProfileActivity.this, R.string.account_deleted, Toast.LENGTH_SHORT).show();
                    // Don't navigate to MainActivity, stay here and restart (recreate) to reset Autofill
                    restartActivityWithSignOut();
                    buttonDelete.setEnabled(true);
                }
                @Override
                public void onError(String message) {
                    if (isFinishing() || isDestroyed()) return;
                    hideLoading();
                    isSigningOut = false;
                    UiUtils.makeCustomToast(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
                    buttonDelete.setEnabled(true);
                    containerContent.setVisibility(View.VISIBLE);
                }
            });
        });
    }
}