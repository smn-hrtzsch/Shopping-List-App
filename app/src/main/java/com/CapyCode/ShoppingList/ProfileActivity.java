package com.CapyCode.ShoppingList;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import android.widget.EditText;
import android.view.Gravity;
import android.view.ViewGroup;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText editTextUsername;
    private MaterialButton buttonSave;
    private MaterialButton buttonDelete;
    private MaterialButton buttonRegisterEmail;
    private MaterialButton buttonRegisterGoogle;
    private MaterialButton buttonSignOut;
    private TextView textViewCurrentUsername;
    private TextView textViewProfileInfo;
    private TextView textViewWarning;
    private LinearLayout layoutLinkedMethods;
    private ImageView imageProfile;
    private ImageView iconEditImage;
    private LinearLayout layoutViewMode;
    private LinearLayout layoutEditMode;
    private LinearLayout layoutAuthButtons;
    private View containerContent;
    private ProgressBar progressBarLoading;
    private UserRepository userRepository;
    private boolean isInitialProfileCreation = false;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> authActivityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
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
                        Toast.makeText(ProfileActivity.this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        View root = findViewById(R.id.profile_root);
        
        Toolbar toolbar = findViewById(R.id.toolbar_profile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userRepository = new UserRepository(this);

        editTextUsername = findViewById(R.id.edit_text_username);
        buttonSave = findViewById(R.id.button_save_profile);
        buttonDelete = findViewById(R.id.button_delete_account);
        buttonRegisterEmail = findViewById(R.id.button_register_email);
        buttonRegisterGoogle = findViewById(R.id.button_register_google);
        buttonSignOut = findViewById(R.id.button_sign_out);
        textViewCurrentUsername = findViewById(R.id.text_view_current_username);
        textViewProfileInfo = findViewById(R.id.text_view_profile_info);
        textViewWarning = findViewById(R.id.text_view_warning_anonymous);
        layoutLinkedMethods = findViewById(R.id.layout_linked_methods);
        imageProfile = findViewById(R.id.image_profile);
        iconEditImage = findViewById(R.id.icon_edit_image);
        layoutViewMode = findViewById(R.id.layout_view_mode);
        layoutEditMode = findViewById(R.id.layout_edit_mode);
        layoutAuthButtons = findViewById(R.id.layout_auth_buttons);
        containerContent = findViewById(R.id.container_content);
        progressBarLoading = findViewById(R.id.progress_bar_loading);

        loadCurrentProfile();

        buttonSave.setOnClickListener(v -> saveProfile());
        buttonDelete.setOnClickListener(v -> confirmDeleteAccount());
        
        buttonRegisterEmail.setOnClickListener(v -> {
            Intent intent = new Intent(this, AuthActivity.class);
            authActivityLauncher.launch(intent);
        });

        buttonRegisterGoogle.setOnClickListener(v -> signInWithGoogle());

        buttonSignOut.setOnClickListener(v -> confirmSignOut());
        
        View.OnClickListener imageClickListener = v -> showImageOptions();
        imageProfile.setOnClickListener(imageClickListener);
        iconEditImage.setOnClickListener(imageClickListener);
    }
    
    private void showImageOptions() {
        String[] options = {"Neues Bild wählen", "Bild entfernen", "Abbrechen"};
        new AlertDialog.Builder(this)
                .setTitle("Profilbild")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickImageLauncher.launch("image/*");
                    } else if (which == 1) {
                        removeImage();
                    }
                })
                .show();
    }


    private void signInWithGoogle() {
        // Sign out first to force account chooser
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        progressBarLoading.setVisibility(View.VISIBLE);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Check if ALREADY linked to Google to prevent overwriting/double linking confusion
            for (UserInfo profile : currentUser.getProviderData()) {
                if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                     progressBarLoading.setVisibility(View.GONE);
                     Toast.makeText(this, "Es ist bereits ein Google-Konto verknüpft. Bitte trenne es zuerst.", Toast.LENGTH_LONG).show();
                     mGoogleSignInClient.signOut(); // Ensure clean state
                     return;
                }
            }

            // Try to link first
            currentUser.linkWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Google verknüpft", Toast.LENGTH_SHORT).show();
                            loadCurrentProfile();
                        } else {
                             if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                 // Link failed because credential already exists on another user
                                 progressBarLoading.setVisibility(View.GONE);
                                 showCustomDialog(
                                     "Konto wechseln?",
                                     "Ein Konto mit der E-Mail dieses Google-Accounts existiert bereits.\n\nMöchtest du dich stattdessen in dieses Konto einloggen? Dabei wird dein Google-Konto mit dem bestehenden Account verknüpft (und deine aktuellen Gast-Daten ersetzt).",
                                     "Wechseln & Verknüpfen",
                                     () -> performGoogleSignIn(credential)
                                 );
                             } else {
                                 progressBarLoading.setVisibility(View.GONE);
                                 String msg = task.getException() != null ? task.getException().getMessage() : "Fehler beim Verknüpfen";
                                 Toast.makeText(ProfileActivity.this, "Verknüpfung fehlgeschlagen: " + msg, Toast.LENGTH_LONG).show();
                             }
                        }
                    });
        } else {
            performGoogleSignIn(credential);
        }
    }

    private void performGoogleSignIn(AuthCredential credential) {
        progressBarLoading.setVisibility(View.VISIBLE);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        loadCurrentProfile();
                    } else {
                        progressBarLoading.setVisibility(View.GONE);
                        Toast.makeText(ProfileActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        MenuItem editItem = menu.findItem(R.id.action_edit_profile);
        if (isInitialProfileCreation || layoutEditMode.getVisibility() == View.VISIBLE) {
            editItem.setVisible(false);
        } else {
            editItem.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_edit_profile) {
            enableEditModeWithCheck();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadCurrentProfile() {
        progressBarLoading.setVisibility(View.VISIBLE);
        containerContent.setVisibility(View.GONE);

        Runnable fetchProfileData = () -> {
            updateAuthUI();
            
            userRepository.getUserProfile(new UserRepository.OnUserProfileLoadedListener() {
                @Override
                public void onLoaded(String username, String imageUrl) {
                    progressBarLoading.setVisibility(View.GONE);
                    containerContent.setVisibility(View.VISIBLE);
                    
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(ProfileActivity.this)
                            .load(imageUrl)
                            .apply(RequestOptions.circleCropTransform())
                            .into(imageProfile);
                         imageProfile.setPadding(0,0,0,0);
                         imageProfile.setBackgroundResource(0);
                    } else {
                         imageProfile.setImageResource(R.drawable.ic_account_circle_24);
                         imageProfile.setPadding(0,0,0,0);
                         imageProfile.setBackgroundResource(android.R.color.transparent);
                    }

                    if (username != null) {
                        isInitialProfileCreation = false;
                        textViewCurrentUsername.setText(username);
                        editTextUsername.setText(username);
                        showViewMode();
                    } else {
                        isInitialProfileCreation = true;
                        editTextUsername.setText(""); // Clear potentially old data
                        showEditMode();
                    }
                    invalidateOptionsMenu();
                }

                @Override
                public void onError(String error) {
                    progressBarLoading.setVisibility(View.GONE);
                    containerContent.setVisibility(View.VISIBLE);
                    Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_LONG).show();
                    isInitialProfileCreation = true;
                    showEditMode();
                }
            });
        };

        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    fetchProfileData.run();
                } else {
                    progressBarLoading.setVisibility(View.GONE);
                    containerContent.setVisibility(View.VISIBLE);
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    Toast.makeText(ProfileActivity.this, "Authentication failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    isInitialProfileCreation = true;
                    showEditMode();
                }
            });
        } else {
            fetchProfileData.run();
        }
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
            
            // Populate Linked Methods
            layoutLinkedMethods.setVisibility(View.VISIBLE);
            // Keep the first child (the label) and remove others
            if (layoutLinkedMethods.getChildCount() > 1) {
                layoutLinkedMethods.removeViews(1, layoutLinkedMethods.getChildCount() - 1);
            }
            
            for (UserInfo profile : user.getProviderData()) {
                if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                    LinearLayout googleRow = new LinearLayout(this);
                    googleRow.setOrientation(LinearLayout.HORIZONTAL);
                    googleRow.setGravity(Gravity.CENTER_VERTICAL);
                    googleRow.setPadding(0, 8, 0, 8);
                    
                    ImageView icon = new ImageView(this);
                    int size = (int) (24 * getResources().getDisplayMetrics().density);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                    lp.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
                    icon.setLayoutParams(lp);
                    
                    if (profile.getPhotoUrl() != null) {
                         Glide.with(this).load(profile.getPhotoUrl()).apply(RequestOptions.circleCropTransform()).into(icon);
                    } else {
                         icon.setImageResource(R.drawable.ic_google_logo);
                    }
                    
                    TextView text = new TextView(this);
                    text.setText("Google: " + profile.getEmail());
                    text.setTextColor(ContextCompat.getColor(this, R.color.text_primary_adaptive)); // Use dynamic color if possible or resolve attr
                    
                    googleRow.addView(icon);
                    googleRow.addView(text);
                    layoutLinkedMethods.addView(googleRow);
                } else if (EmailAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                    LinearLayout emailRow = new LinearLayout(this);
                    emailRow.setOrientation(LinearLayout.HORIZONTAL);
                    emailRow.setGravity(Gravity.CENTER_VERTICAL);
                    emailRow.setPadding(0, 8, 0, 8);
                    
                    ImageView icon = new ImageView(this);
                    int size = (int) (24 * getResources().getDisplayMetrics().density);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                    lp.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
                    icon.setLayoutParams(lp);
                    icon.setImageResource(R.drawable.ic_email);
                    icon.setColorFilter(ContextCompat.getColor(this, R.color.text_primary_adaptive)); // Tint to text color
                    
                    TextView text = new TextView(this);
                    text.setText("E-Mail: " + profile.getEmail());
                    text.setTextColor(ContextCompat.getColor(this, R.color.text_primary_adaptive));
                    
                    emailRow.addView(icon);
                    emailRow.addView(text);
                    layoutLinkedMethods.addView(emailRow);
                }
            }
            
            buttonSignOut.setVisibility(View.VISIBLE);
            layoutAuthButtons.setVisibility(View.VISIBLE);

            boolean isGoogleLinked = false;
            boolean isEmailLinked = false;
            
            for (UserInfo profile : user.getProviderData()) {
                if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) isGoogleLinked = true;
                if (EmailAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) isEmailLinked = true;
            }
            
            // Configure Google Button
            if (isGoogleLinked) {
                buttonRegisterGoogle.setText("Google verknüpft (Trennen)");
                buttonRegisterGoogle.setIconResource(R.drawable.ic_unlink);
                buttonRegisterGoogle.setIconTint(ContextCompat.getColorStateList(this, android.R.color.black));
                if ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                     buttonRegisterGoogle.setIconTint(ContextCompat.getColorStateList(this, android.R.color.white));
                }
                
                buttonRegisterGoogle.setOnClickListener(v -> confirmUnlink(GoogleAuthProvider.PROVIDER_ID));
            } else {
                buttonRegisterGoogle.setText("Mit Google verknüpfen");
                buttonRegisterGoogle.setIconResource(R.drawable.ic_google_logo);
                buttonRegisterGoogle.setIconTint(null);
                buttonRegisterGoogle.setOnClickListener(v -> signInWithGoogle());
            }

            // Configure Email Button
            if (isEmailLinked) {
                buttonRegisterEmail.setText("E-Mail Verknüpfung entfernen");
                buttonRegisterEmail.setIconResource(R.drawable.ic_unlink);
                
                if ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                     buttonRegisterEmail.setIconTint(ContextCompat.getColorStateList(this, android.R.color.white));
                } else {
                     buttonRegisterEmail.setIconTint(ContextCompat.getColorStateList(this, android.R.color.black));
                }

                buttonRegisterEmail.setOnClickListener(v -> confirmUnlink(EmailAuthProvider.PROVIDER_ID));
            } else {
                buttonRegisterEmail.setText("Mit E-Mail verknüpfen");
                buttonRegisterEmail.setIconResource(R.drawable.ic_email); 
                buttonRegisterEmail.setIconTint(ContextCompat.getColorStateList(this, R.color.text_primary_adaptive)); // Use text color
                buttonRegisterEmail.setOnClickListener(v -> {
                    Intent intent = new Intent(this, AuthActivity.class);
                    authActivityLauncher.launch(intent);
                });
            }
            
            iconEditImage.setVisibility(View.VISIBLE);
        } else {
            // Anonymous or Effectively Anonymous
            textViewWarning.setVisibility(View.VISIBLE);
            layoutLinkedMethods.setVisibility(View.GONE);
            
            layoutAuthButtons.setVisibility(View.VISIBLE);
            
            buttonRegisterEmail.setText(R.string.action_sign_in_email);
            buttonRegisterEmail.setIconResource(R.drawable.ic_email);
            buttonRegisterEmail.setIconTint(ContextCompat.getColorStateList(this, R.color.text_primary_adaptive));
            buttonRegisterEmail.setOnClickListener(v -> {
                Intent intent = new Intent(this, AuthActivity.class);
                authActivityLauncher.launch(intent);
            });

            buttonRegisterGoogle.setText(R.string.sign_in_google);
            buttonRegisterGoogle.setIconResource(R.drawable.ic_google_logo);
            buttonRegisterGoogle.setIconTint(null);
            buttonRegisterGoogle.setOnClickListener(v -> signInWithGoogle());
            
            buttonSignOut.setVisibility(View.GONE);
            iconEditImage.setVisibility(View.VISIBLE);
        }
    }
    
    private void confirmUnlink(String providerId) {
        showCustomDialog(
                "Verknüpfung aufheben?",
                "Möchtest du diese Anmelde-Methode wirklich entfernen? Wenn du keine anderen Methoden hast, verlierst du den Zugriff auf dein Konto.",
                "Trennen",
                () -> unlinkProvider(providerId)
        );
    }
    
    private void unlinkProvider(String providerId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.unlink(providerId)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Verknüpfung aufgehoben", Toast.LENGTH_SHORT).show();
                            
                            // If we unlinked Google, sign out of the Google Client to force account chooser next time
                            if (GoogleAuthProvider.PROVIDER_ID.equals(providerId)) {
                                mGoogleSignInClient.signOut();
                            }

                            // If no providers left, user is effectively anonymous/unsecured
                            if (user.getProviderData().isEmpty()) {
                                Toast.makeText(this, "Warnung: Dein Konto hat keine Anmeldemethoden mehr.", Toast.LENGTH_LONG).show();
                            }
                            loadCurrentProfile();
                        } else {
                            if (task.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                                // Re-auth required
                                if (GoogleAuthProvider.PROVIDER_ID.equals(providerId)) {
                                    Toast.makeText(this, "Bitte melde dich erneut mit Google an, um die Verknüpfung zu trennen.", Toast.LENGTH_LONG).show();
                                    signInWithGoogle(); // Will trigger re-auth logic if we handle it
                                } else if (EmailAuthProvider.PROVIDER_ID.equals(providerId)) {
                                    showReauthDialog(providerId);
                                }
                            } else {
                                String msg = task.getException() != null ? task.getException().getMessage() : "Unbekannter Fehler";
                                Toast.makeText(this, "Fehler beim Trennen: " + msg, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    private void showReauthDialog(String providerId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sicherheitsprüfung");
        builder.setMessage("Bitte gib dein Passwort ein, um die Verknüpfung zu entfernen.");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Bestätigen", (dialog, which) -> {
            String password = input.getText().toString();
            if (!password.isEmpty()) {
                reauthenticateAndUnlink(password, providerId);
            }
        });
        builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void reauthenticateAndUnlink(String password, String providerId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    unlinkProvider(providerId); // Retry unlink
                } else {
                    Toast.makeText(this, "Passwort falsch.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void confirmSignOut() {
        showCustomDialog(
            getString(R.string.button_sign_out),
            "Wenn du dich abmeldest, sind deine geteilten Listen nicht mehr sichtbar, bis du dich wieder anmeldest.",
            getString(R.string.button_sign_out),
            () -> {
                 mAuth.signOut();
                 mGoogleSignInClient.signOut();
                 Toast.makeText(this, "Abgemeldet", Toast.LENGTH_SHORT).show();
                 loadCurrentProfile(); 
            }
        );
    }

    private void removeImage() {
        progressBarLoading.setVisibility(View.VISIBLE);
        userRepository.removeProfileImage(new UserRepository.OnProfileActionListener() {
            @Override
            public void onSuccess() {
                 progressBarLoading.setVisibility(View.GONE);
                 loadCurrentProfile();
                 Toast.makeText(ProfileActivity.this, "Bild entfernt", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                progressBarLoading.setVisibility(View.GONE);
                Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadImage(Uri imageUri) {
        progressBarLoading.setVisibility(View.VISIBLE);
        userRepository.uploadProfileImage(imageUri, new UserRepository.OnImageUploadListener() {
            @Override
            public void onSuccess(String downloadUrl) {
                progressBarLoading.setVisibility(View.GONE);
                Glide.with(ProfileActivity.this)
                        .load(downloadUrl)
                        .apply(RequestOptions.circleCropTransform())
                        .into(imageProfile);
                imageProfile.setPadding(0,0,0,0);
                 imageProfile.setBackgroundResource(0);
                Toast.makeText(ProfileActivity.this, "Image uploaded!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                progressBarLoading.setVisibility(View.GONE);
                Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showViewMode() {
        layoutViewMode.setVisibility(View.VISIBLE);
        layoutEditMode.setVisibility(View.GONE);
        
        buttonDelete.setVisibility(View.VISIBLE);
        textViewProfileInfo.setVisibility(View.GONE);
        invalidateOptionsMenu();
    }

    private void showEditMode() {
        layoutViewMode.setVisibility(View.GONE);
        layoutEditMode.setVisibility(View.VISIBLE);
        
        if (isInitialProfileCreation) {
            buttonDelete.setVisibility(View.GONE);
            textViewProfileInfo.setVisibility(View.VISIBLE);
        } else {
            buttonDelete.setVisibility(View.VISIBLE);
            textViewProfileInfo.setVisibility(View.GONE);
        }
        invalidateOptionsMenu();
    }

    private void enableEditModeWithCheck() {
        userRepository.checkUsernameChangeAllowed(new UserRepository.OnProfileActionListener() {
            @Override
            public void onSuccess() {
                showEditMode();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveProfile() {
        String username = editTextUsername.getText().toString().trim();
        if (username.isEmpty()) {
            editTextUsername.setError(getString(R.string.profile_error_empty));
            return;
        }

        if (username.length() < 3) {
            editTextUsername.setError(getString(R.string.profile_error_short));
            return;
        }

        if (username.contains(" ")) {
            editTextUsername.setError(getString(R.string.profile_error_whitespace));
            return;
        }

        buttonSave.setEnabled(false);

        Runnable saveAction = () -> userRepository.setUsername(username, new UserRepository.OnProfileActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(ProfileActivity.this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
                buttonSave.setEnabled(true);

                if (isInitialProfileCreation) {
                    setResult(RESULT_OK);
                    finish();
                } else {
                    textViewCurrentUsername.setText(username);
                    showViewMode();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
                buttonSave.setEnabled(true);
            }
        });

        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    saveAction.run();
                } else {
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    Toast.makeText(ProfileActivity.this, "Authentication failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    buttonSave.setEnabled(true);
                }
            });
        } else {
            saveAction.run();
        }
    }

    private void confirmDeleteAccount() {
        showCustomDialog(
            getString(R.string.dialog_delete_account_title),
            getString(R.string.dialog_delete_account_message),
            getString(R.string.button_delete),
            this::deleteAccount
        );
    }

    private void showCustomDialog(String title, String message, String positiveButtonText, Runnable onPositiveAction) {
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
        
        btnPositive.setOnClickListener(v -> {
            onPositiveAction.run();
            dialog.dismiss();
        });

        btnNegative.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void deleteAccount() {
        buttonDelete.setEnabled(false);
        userRepository.deleteAccount(new UserRepository.OnProfileActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(ProfileActivity.this, R.string.account_deleted, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
                buttonDelete.setEnabled(true);
            }
        });
    }
}
