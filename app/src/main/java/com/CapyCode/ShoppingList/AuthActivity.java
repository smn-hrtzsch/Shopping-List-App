package com.CapyCode.ShoppingList;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
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

public class AuthActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword;
    private MaterialButton buttonLogin, buttonRegister;
    private TextView textForgotPassword;
    private ProgressBar progressBar;
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

        editTextEmail = findViewById(R.id.edit_text_email);
        editTextPassword = findViewById(R.id.edit_text_password);
        buttonLogin = findViewById(R.id.button_login);
        buttonRegister = findViewById(R.id.button_register);
        textForgotPassword = findViewById(R.id.text_forgot_password);
        progressBar = findViewById(R.id.progress_bar_auth);

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
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError(getString(R.string.error_password_required));
            return;
        }

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
        
        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
        secondaryAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                showLoading(false);
                if (task.isSuccessful()) {
                    // Credentials are correct. Now show the warning.
                    showCustomDialog(
                        "Konto wechseln?",
                        "Du bist aktuell als Gast angemeldet. Wenn du dich in ein anderes bestehendes Konto einloggst, werden deine aktuellen Gast-Listen von diesem Gerät entfernt und durch die des anderen Kontos ersetzt.\n\nMöchtest du fortfahren?",
                        "Anmelden (Daten verwerfen)",
                        () -> performLogin(email, password)
                    );
                } else {
                    // Verification failed (wrong password or user doesn't exist)
                    handleLoginError(task);
                }
            });
    }

    private void performLogin(String email, String password) {
        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        finishAuth(true);
                    } else {
                        handleLoginError(task);
                    }
                });
    }

    private void handleLoginError(Task<?> task) {
        String errorMsg = "Login fehlgeschlagen.";
        try {
            throw task.getException();
        } catch(FirebaseAuthInvalidUserException e) {
            errorMsg = "Konto existiert nicht oder wurde deaktiviert.";
        } catch(FirebaseAuthInvalidCredentialsException e) {
            errorMsg = "Ungültige E-Mail oder falsches Passwort.";
        } catch(Exception e) {
            errorMsg = e.getMessage();
        }
        Toast.makeText(AuthActivity.this, errorMsg, Toast.LENGTH_LONG).show();
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError(getString(R.string.error_email_required));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError(getString(R.string.error_password_required));
            return;
        }

        showLoading(true);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        // Allow linking if ANY user is logged in (Anonymous OR Google)
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
                        // Success: Account is now linked. Start verification.
                        // Pass 'true' because this is a linking operation (revert = unlink)
                        startVerificationFlow(user, true);
                    } else {
                        showLoading(false);
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(AuthActivity.this, "Ein Konto mit dieser E-Mail existiert bereits als separater Account.", Toast.LENGTH_LONG).show();
                        } else {
                            String msg = task.getException() != null ? task.getException().getMessage() : "Fehler beim Verknüpfen.";
                            Toast.makeText(AuthActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
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
                        String errorMsg = "Registrierung fehlgeschlagen.";
                        try {
                            throw task.getException();
                        } catch(FirebaseAuthUserCollisionException e) {
                            errorMsg = "Ein Konto mit dieser E-Mail existiert bereits.";
                        } catch(Exception e) {
                            errorMsg = e.getMessage();
                        }
                        Toast.makeText(AuthActivity.this, errorMsg, Toast.LENGTH_LONG).show();
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
                        // Email sending failed. Revert changes immediately.
                        Toast.makeText(this, "Konnte Bestätigungs-Email nicht senden: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        revertRegistration(user, isLinkedAccount);
                    }
                });
    }

    private void showVerificationDialog(FirebaseUser user, boolean isLinkedAccount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("E-Mail bestätigen");
        builder.setMessage("Wir haben eine Bestätigungs-E-Mail an " + user.getEmail() + " gesendet.\n\nBitte klicke auf den Link in der E-Mail und bestätige anschließend hier.");
        builder.setCancelable(false); // Prevent dismissal by clicking outside

        builder.setPositiveButton("Ich habe bestätigt", null); // Override later to prevent closing
        builder.setNegativeButton("Abbrechen (Account löschen)", null); // Override later

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override buttons to handle logic without auto-dismissal
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Reload user to get fresh data
            user.reload().addOnCompleteListener(reloadTask -> {
                if (reloadTask.isSuccessful()) {
                    if (user.isEmailVerified()) {
                        dialog.dismiss();
                        finishAuth(true);
                    } else {
                        Toast.makeText(AuthActivity.this, "E-Mail noch nicht bestätigt. Bitte überprüfe dein Postfach.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AuthActivity.this, "Fehler beim Prüfen: " + reloadTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Abbrechen?")
                .setMessage("Wenn du abbrichst, wird der Registrierungsprozess rückgängig gemacht.")
                .setPositiveButton("Ja, abbrechen", (d, w) -> {
                    dialog.dismiss();
                    revertRegistration(user, isLinkedAccount);
                })
                .setNegativeButton("Nein, warten", null)
                .show();
        });
    }

    private void revertRegistration(FirebaseUser user, boolean isLinkedAccount) {
        showLoading(true);
        if (isLinkedAccount) {
            // User was anonymous and we linked email. Unlink email to revert.
            user.unlink(com.google.firebase.auth.EmailAuthProvider.PROVIDER_ID)
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Toast.makeText(AuthActivity.this, "Verknüpfung abgebrochen.", Toast.LENGTH_SHORT).show();
                        } else {
                            // If unlink fails (rare), maybe sign out or show error
                            Toast.makeText(AuthActivity.this, "Fehler beim Rückgängigmachen. Bitte melde dich ab.", Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            // User was newly created. Delete user.
            user.delete()
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Toast.makeText(AuthActivity.this, "Registrierung abgebrochen.", Toast.LENGTH_SHORT).show();
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

        mAuth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    java.util.List<String> methods = task.getResult().getSignInMethods();
                    
                    if (methods == null || methods.isEmpty()) {
                        showLoading(false);
                        Toast.makeText(AuthActivity.this, "Kein Konto mit dieser E-Mail gefunden.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Check if the user has a password provider setup
                    if (methods.contains(com.google.firebase.auth.EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)) {
                        // User has a password, send reset email
                        sendResetEmail(email);
                    } else if (methods.contains(com.google.firebase.auth.GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD)) {
                        // User only has Google (or other providers), warn them
                        showLoading(false);
                        showCustomDialog(
                            "Google Konto",
                            "Diese E-Mail Adresse ist mit einem Google Konto verknüpft. Bitte melde dich über den 'Mit Google anmelden' Button an.\n\nDu hast für dieses Konto kein Passwort festgelegt.",
                            "OK",
                            () -> {}
                        );
                    } else {
                        // Fallback
                        sendResetEmail(email);
                    }
                } else {
                    // If fetch fails (e.g. strict security settings), try sending anyway
                    sendResetEmail(email);
                }
            });
    }

    private void sendResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(resetTask -> {
                showLoading(false);
                if (resetTask.isSuccessful()) {
                    Toast.makeText(AuthActivity.this, getString(R.string.email_sent), Toast.LENGTH_SHORT).show();
                } else {
                     String msg = resetTask.getException() != null ? resetTask.getException().getMessage() : "Fehler";
                     if (resetTask.getException() instanceof FirebaseAuthInvalidUserException) {
                         msg = "Kein Konto mit dieser E-Mail gefunden.";
                     }
                     Toast.makeText(AuthActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonLogin.setEnabled(!show);
        buttonRegister.setEnabled(!show);
    }

    private void finishAuth(boolean success) {
        if (success) {
            Toast.makeText(AuthActivity.this, getString(R.string.auth_success), Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
