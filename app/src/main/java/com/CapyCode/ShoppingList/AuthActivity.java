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
            showCustomDialog(
                "Konto wechseln?",
                "Du bist aktuell als Gast angemeldet. Wenn du dich in ein anderes bestehendes Konto einloggst, werden deine aktuellen Gast-Listen von diesem Gerät entfernt und durch die des anderen Kontos ersetzt.\n\nMöchtest du fortfahren?",
                "Anmelden (Daten verwerfen)",
                () -> performLogin(email, password)
            );
        } else {
            performLogin(email, password);
        }
    }

    private void performLogin(String email, String password) {
        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        finishAuth(true);
                    } else {
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
                });
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
        if (currentUser != null && currentUser.isAnonymous()) {
            // Link the new credential to the existing anonymous user
            AuthCredential credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password);
            currentUser.linkWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            finishAuth(true);
                        } else {
                            // Linking failed (maybe email already in use), try standard create
                            if (task.getException() != null && task.getException().getMessage().contains("already in use")) {
                                Toast.makeText(AuthActivity.this, "Email already in use. Please sign in.", Toast.LENGTH_LONG).show();
                            } else {
                                // Try creating fresh
                                createAccount(email, password);
                            }
                        }
                    });
        } else {
            createAccount(email, password);
        }
    }
    
    private void createAccount(String email, String password) {
         mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        finishAuth(true);
                    } else {
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
                     if (task.getResult().getSignInMethods() != null && task.getResult().getSignInMethods().isEmpty()) {
                         showLoading(false);
                         Toast.makeText(AuthActivity.this, "Kein Konto mit dieser E-Mail gefunden.", Toast.LENGTH_LONG).show();
                         return;
                     }
                }
                
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
