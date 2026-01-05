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
        buttonLogin = findViewById(R.id.button_login);
        buttonRegister = findViewById(R.id.button_register);
        textForgotPassword = findViewById(R.id.text_forgot_password);

        buttonLogin.setOnClickListener(v -> loginUser());
        buttonRegister.setOnClickListener(v -> registerUser());
        textForgotPassword.setOnClickListener(v -> resetPassword());
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError(getString(R.string.error_email_required));
            return;
        }
        if (!AuthValidator.isValidEmail(email)) {
            editTextEmail.setError(getString(R.string.error_invalid_email));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError(getString(R.string.error_password_required));
            return;
        }

        showLoading(true, R.string.loading_signing_in);

        // Directly attempt to login/verify, bypassing fetchSignInMethodsForEmail
        // which might be blocked by Email Enumeration Protection.
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
        UiUtils.makeCustomToast(AuthActivity.this, errorMsg, Toast.LENGTH_LONG).show();
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError(getString(R.string.error_email_required));
            return;
        }
        if (!AuthValidator.isValidEmail(email)) {
            editTextEmail.setError(getString(R.string.error_invalid_email));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError(getString(R.string.error_password_required));
            return;
        }
        if (!AuthValidator.isValidPassword(password)) {
            editTextPassword.setError(getString(R.string.error_password_short));
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
                        UiUtils.makeCustomToast(AuthActivity.this, errorMsg, Toast.LENGTH_LONG).show();
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
                        UiUtils.makeCustomToast(AuthActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startVerificationFlow(FirebaseUser user, boolean isLinkedAccount) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        showVerificationDialog(user, isLinkedAccount);
                    } else {
                        UiUtils.makeCustomToast(this, getString(R.string.error_send_email_failed, task.getException().getMessage()), Toast.LENGTH_LONG).show();
                        revertRegistration(user, isLinkedAccount);
                    }
                });
    }

    private void showVerificationDialog(FirebaseUser user, boolean isLinkedAccount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_standard, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

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
                        dialog.dismiss();
                        finishAuth(true, true);
                    } else {
                        UiUtils.makeCustomToast(AuthActivity.this, R.string.toast_email_not_verified, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    UiUtils.makeCustomToast(AuthActivity.this, getString(R.string.error_check_failed, reloadTask.getException().getMessage()), Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnNegative.setOnClickListener(v -> {
            AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);
            View confirmView = getLayoutInflater().inflate(R.layout.dialog_standard, null);
            confirmBuilder.setView(confirmView);
            AlertDialog confirmDialog = confirmBuilder.create();
            
            if (confirmDialog.getWindow() != null) {
                confirmDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }

            TextView confirmTitle = confirmView.findViewById(R.id.dialog_title);
            TextView confirmMessage = confirmView.findViewById(R.id.dialog_message);
            MaterialButton confirmBtnPositive = confirmView.findViewById(R.id.dialog_button_positive);
            MaterialButton confirmBtnNegative = confirmView.findViewById(R.id.dialog_button_negative);

            confirmTitle.setText(R.string.dialog_abort_title);
            confirmMessage.setText(R.string.dialog_abort_message);
            confirmBtnPositive.setText(R.string.button_yes_abort);
            confirmBtnNegative.setText(R.string.button_no_wait);

            confirmBtnPositive.setOnClickListener(confirmV -> {
                confirmDialog.dismiss();
                dialog.dismiss();
                revertRegistration(user, isLinkedAccount);
            });

            confirmBtnNegative.setOnClickListener(confirmV -> confirmDialog.dismiss());
            confirmDialog.show();
        });

        dialog.show();
    }

    private void revertRegistration(FirebaseUser user, boolean isLinkedAccount) {
        showLoading(true);
        if (isLinkedAccount) {
            user.unlink(com.google.firebase.auth.EmailAuthProvider.PROVIDER_ID)
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            UiUtils.makeCustomToast(AuthActivity.this, R.string.toast_link_cancelled, Toast.LENGTH_SHORT).show();
                        } else {
                            UiUtils.makeCustomToast(AuthActivity.this, R.string.error_revert_failed, Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            user.delete()
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            UiUtils.makeCustomToast(AuthActivity.this, R.string.toast_registration_aborted, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void resetPassword() {
        String email = editTextEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError(getString(R.string.error_email_required));
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