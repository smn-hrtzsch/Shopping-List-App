package com.example.einkaufsliste;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText editTextUsername;
    private MaterialButton buttonSave;
    private MaterialButton buttonDelete;
    private TextView textViewCurrentUsername;
    private TextView textViewProfileInfo;
    private TextView textViewWarning;
    private LinearLayout layoutViewMode;
    private LinearLayout layoutEditMode;
    private LinearLayout containerContent;
    private ProgressBar progressBarLoading;
    private UserRepository userRepository;
    private boolean isInitialProfileCreation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_profile);

        View root = findViewById(R.id.profile_root);
        
        Toolbar toolbar = findViewById(R.id.toolbar_profile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
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
        textViewCurrentUsername = findViewById(R.id.text_view_current_username);
        textViewProfileInfo = findViewById(R.id.text_view_profile_info);
        textViewWarning = findViewById(R.id.text_view_warning_anonymous);
        layoutViewMode = findViewById(R.id.layout_view_mode);
        layoutEditMode = findViewById(R.id.layout_edit_mode);
        containerContent = findViewById(R.id.container_content);
        progressBarLoading = findViewById(R.id.progress_bar_loading);

        loadCurrentProfile();

        buttonSave.setOnClickListener(v -> saveProfile());
        buttonDelete.setOnClickListener(v -> confirmDeleteAccount());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        MenuItem editItem = menu.findItem(R.id.action_edit_profile);
        // Only show edit icon if we are in view mode (profile exists) AND currently not editing
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

        userRepository.getCurrentUsername(username -> {
            progressBarLoading.setVisibility(View.GONE);
            containerContent.setVisibility(View.VISIBLE);
            
            if (username != null) {
                // User has a profile -> Show View Mode
                isInitialProfileCreation = false;
                textViewCurrentUsername.setText(username);
                editTextUsername.setText(username); 
                showViewMode();
            } else {
                // No profile -> Show Edit Mode
                isInitialProfileCreation = true;
                showEditMode();
            }
            invalidateOptionsMenu(); // Refresh menu visibility
        });
    }

    private void showViewMode() {
        layoutViewMode.setVisibility(View.VISIBLE);
        layoutEditMode.setVisibility(View.GONE);
        
        buttonDelete.setVisibility(View.VISIBLE);
        textViewWarning.setVisibility(View.VISIBLE);
        textViewProfileInfo.setVisibility(View.GONE);
        invalidateOptionsMenu();
    }

    private void showEditMode() {
        layoutViewMode.setVisibility(View.GONE);
        layoutEditMode.setVisibility(View.VISIBLE);
        
        if (isInitialProfileCreation) {
            buttonDelete.setVisibility(View.GONE);
            textViewWarning.setVisibility(View.GONE);
            textViewProfileInfo.setVisibility(View.VISIBLE);
        } else {
            // Editing existing
            buttonDelete.setVisibility(View.VISIBLE);
            textViewWarning.setVisibility(View.VISIBLE);
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
        userRepository.setUsername(username, new UserRepository.OnProfileActionListener() {
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

        // Transparent background for rounded corners
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
        
        // If it's a destructive action (like delete), maybe color it red?
        // For now we use standard themed colors which are adaptive gray.

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
