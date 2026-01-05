package com.CapyCode.ShoppingList;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class AuthActivity extends BaseActivity {

    private TextInputEditText editTextEmail, editTextPassword;
    private TextView errorTextEmail, errorTextPassword;
    private MaterialButton buttonLogin, buttonRegister;
    private TextView textForgotPassword;
    private FirebaseAuth mAuth;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_auth);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar_auth);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(R.string.account_info);
        }

        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository(this);

        initLoadingOverlay(findViewById(R.id.auth_content_container), R.layout.skeleton_profile);
        showSkeleton(false);

        editTextEmail = findViewById(R.id.edit_text_email);
        editTextPassword = findViewById(R.id.edit_text_password);
        errorTextEmail = findViewById(R.id.error_text_email);
        errorTextPassword = findViewById(R.id.error_text_password);
        buttonLogin = findViewById(R.id.button_login);
        buttonRegister = findViewById(R.id.button_register);
        textForgotPassword = findViewById(R.id.text_forgot_password);

        buttonLogin.setOnClickListener(v -> loginUser());
        buttonRegister.setOnClickListener(v -> registerUser());
        textForgotPassword.setOnClickListener(v -> resetPassword());
        
        // Clear errors on text change
        android.text.TextWatcher clearErrorWatcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { clearErrors(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        editTextEmail.addTextChangedListener(clearErrorWatcher);
        editTextPassword.addTextChangedListener(clearErrorWatcher);
    }

    private void showError(TextView errorView, String message) {
        if (errorView != null) {
            errorView.setText(message);
            errorView.setVisibility(View.VISIBLE);
        }
    }

    private void clearErrors() {
        if (errorTextEmail != null) errorTextEmail.setVisibility(View.GONE);
        if (errorTextPassword != null) errorTextPassword.setVisibility(View.GONE);
    }

    private void loginUser() {
        clearErrors();
        String input = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(input)) {
            showError(errorTextEmail, getString(R.string.error_email_or_username_required));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showError(errorTextPassword, getString(R.string.error_password_required));
            return;
        }

        showLoading(true, R.string.loading_signing_in);

        if (AuthValidator.isValidEmail(input)) {
            // It's an email
            proceedWithLogin(input, password);
        } else {
            // Assume it's a username, resolve email first
            userRepository.findEmailByUsername(input, new UserRepository.OnEmailLoadedListener() {
                @Override
                public void onLoaded(String email) {
                    proceedWithLogin(email, password);
                }

                @Override
                public void onError(String error) {
                    showLoading(false);
                    showError(errorTextEmail, error); // Show "User not found"
                }
            });
        }
    }


    private void proceedWithLogin(String email, String password) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isAnonymous()) {
            verifyCredentialsAndLogin(email, password);
        } else {
            performLogin(email, password);
        }
    }

    private void verifyCredentialsAndLogin(String email, String password) {
        showLoading(true);
        
        // Use a secondary Firebase app to verify credentials without switching the main user yet
        FirebaseOptions options = FirebaseApp.getInstance().getOptions();
        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance("secondary");
        } catch (IllegalStateException e) {
            secondaryApp = FirebaseApp.initializeApp(this, options, "secondary");
        }

        // Install App Check on the secondary app as well
        com.google.firebase.appcheck.FirebaseAppCheck secondaryAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance(secondaryApp);
        if (BuildConfig.DEBUG) {
            secondaryAppCheck.installAppCheckProviderFactory(
                com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
            );
        } else {
            secondaryAppCheck.installAppCheckProviderFactory(
                com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
            );
        }
        
        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
        secondaryAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    checkIfAccountIsEmpty(isEmpty -> {
                        if (isEmpty) {
                            performLogin(email, password);
                        } else {
                            // Credentials are correct. Now show the warning.
                            showCustomDialog(
                                getString(R.string.dialog_switch_account_title),
                                getString(R.string.dialog_switch_account_message_guest),
                                getString(R.string.button_switch_and_link),
                                () -> performLogin(email, password)
                            );
                        }
                    });
                } else {
                    // Verification failed
                    handleLoginError(task);
                }
            });
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

        // Only check for lists. Losing a username on an account with no lists is considered acceptable/trivial.
        ShoppingListRepository listRepo = new ShoppingListRepository(AuthActivity.this);
        if (listRepo.getLocalListCount() > 0) {
             listener.onResult(false);
        } else {
             listener.onResult(true);
        }
    }

    private void performLogin(String email, String password) {
        showLoading(true);
        
        // Clear local data BEFORE login (Switch scenario)
        ShoppingListRepository repo = new ShoppingListRepository(getApplicationContext());
        repo.clearLocalDatabase();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        finishAuth(true, false);
                    } else {
                        showLoading(false);
                        handleLoginError(task);
                    }
                });
    }

    void handleLoginError(Task<?> task) {
        showLoading(false);
        String errorMsg = AuthErrorMapper.getErrorMessage(this, task.getException());
        // Show general login errors under the email field as a primary error location
        showError(errorTextEmail, errorMsg);
    }

    private void registerUser() {
        clearErrors();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            showError(errorTextEmail, getString(R.string.error_email_required));
            return;
        }
        if (!AuthValidator.isValidEmail(email)) {
            showError(errorTextEmail, getString(R.string.error_invalid_email));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showError(errorTextPassword, getString(R.string.error_password_required));
            return;
        }
        if (!AuthValidator.isValidPassword(password)) {
            showError(errorTextPassword, getString(R.string.error_password_short));
            return;
        }

        showLoading(true, R.string.loading_creating_account);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            linkEmailCredential(currentUser, email, password);
        } else {
            createNewAccount(email, password);
        }
    }

    private void linkEmailCredential(FirebaseUser user, String email, String password) {
        AuthCredential credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password);
        user.linkWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startVerificationFlow(user, true);
                    } else {
                        showLoading(false);
                        String errorMsg = AuthErrorMapper.getErrorMessage(this, task.getException());
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            errorMsg = getString(R.string.error_email_collision_link);
                        }
                        showError(errorTextEmail, errorMsg); // Show general error under email field usually
                    }
                });
    }
    
    private void createNewAccount(String email, String password) {
         mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startVerificationFlow(task.getResult().getUser(), false);
                    } else {
                        showLoading(false);
                        String errorMsg = AuthErrorMapper.getErrorMessage(this, task.getException());
                        showError(errorTextEmail, errorMsg);
                    }
                });
    }

    // ... (startVerificationFlow, showVerificationDialog, revertRegistration methods) ...

    private void resetPassword() {
        clearErrors();
        String email = editTextEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            showError(errorTextEmail, getString(R.string.error_email_required));
            return;
        }

        showLoading(true);
        // Directly send reset email. If user doesn't exist, Firebase might still return success
        // or fail depending on settings, but we shouldn't pre-check existence.
        sendResetEmail(email);
    }

    private void sendResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(resetTask -> {
                showLoading(false);
                if (resetTask.isSuccessful()) {
                    UiUtils.makeCustomToast(AuthActivity.this, getString(R.string.email_sent), Toast.LENGTH_SHORT).show();
                } else {
                     String msg = AuthErrorMapper.getErrorMessage(this, resetTask.getException());
                     UiUtils.makeCustomToast(AuthActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showLoading(boolean show) {
        showLoading(show, R.string.loading);
    }

    private void showLoading(boolean show, int messageResId) {
        if (show) {
            super.showLoading(getString(messageResId), true);
        } else {
            super.hideLoading();
        }
        buttonLogin.setEnabled(!show);
        buttonRegister.setEnabled(!show);
    }

    private void finishAuth(boolean success, boolean shouldSync) {
        if (success) {
            // Ensure email is synced to Firestore for future username logins
            userRepository.syncEmailToFirestore();
            
            UiUtils.makeCustomToast(AuthActivity.this, getString(R.string.auth_success), Toast.LENGTH_SHORT).show();
            if (shouldSync) {
                UiUtils.makeCustomToast(AuthActivity.this, getString(R.string.syncing_data), Toast.LENGTH_SHORT).show();
                ShoppingListRepository repository = new ShoppingListRepository(getApplicationContext());
                repository.migrateLocalListsToCloud(() -> {
                    android.util.Log.d("Auth", "Local lists migrated to cloud.");
                });
            }
            setResult(RESULT_OK);
            finish();
        }
    }

    private void showCustomDialog(String title, String message, String positiveButtonText, Runnable onPositiveAction) {
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
        
        btnPositive.setOnClickListener(v -> {
            onPositiveAction.run();
            dialog.dismiss();
        });

        btnNegative.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}