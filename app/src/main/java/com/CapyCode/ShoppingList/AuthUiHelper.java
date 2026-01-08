package com.CapyCode.ShoppingList;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Checkable;
import android.widget.EditText;
import com.google.android.material.textfield.TextInputLayout;

public class AuthUiHelper {

    public interface OnVisibilityToggleListener {
        void onToggled(boolean isVisible);
    }

    /**
     * Configures a password toggle that doesn't close the keyboard or lose focus.
     */
    public static void setupPasswordToggle(TextInputLayout layout, EditText editText, OnVisibilityToggleListener listener) {
        if (layout == null || editText == null) return;

        // Make end icon non-focusable to prevent focus loss from EditText
        View endIconView = layout.findViewById(com.google.android.material.R.id.text_input_end_icon);
        if (endIconView != null) {
            endIconView.setFocusable(false);
            endIconView.setFocusableInTouchMode(false);
        }

        layout.setEndIconOnClickListener(v -> {
            if (listener != null) listener.onToggled(true);
            
            int selection = editText.getSelectionEnd();
            boolean isPasswordVisible = editText.getTransformationMethod() instanceof HideReturnsTransformationMethod;

            if (isPasswordVisible) {
                editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                if (v instanceof Checkable) ((Checkable) v).setChecked(false);
            } else {
                editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                if (v instanceof Checkable) ((Checkable) v).setChecked(true);
            }

            editText.setSelection(selection);
            editText.requestFocus();
            
            if (listener != null) listener.onToggled(false);
        });
    }

    /**
     * Sets up the IME actions for email and password fields.
     */
    public static void setupEmailPasswordFlow(EditText emailField, EditText passwordField, Runnable onLoginAction) {
        if (emailField != null && passwordField != null) {
            emailField.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    passwordField.requestFocus();
                    return true;
                }
                return false;
            });
        }

        if (passwordField != null && onLoginAction != null) {
            passwordField.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                    onLoginAction.run();
                    return true;
                }
                return false;
            });
        }
    }
    
    /**
     * Sets up the IME action for a single field (e.g., username).
     */
    public static void setupActionDone(EditText field, Runnable action) {
        if (field == null || action == null) return;
        field.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                action.run();
                return true;
            }
            return false;
        });
    }

    /**
     * Aggressively disables autofill for a specific view.
     */
    public static void disableAutofillForView(View view) {
        if (view == null) return;
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            view.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
            
            view.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    android.view.autofill.AutofillManager afm = v.getContext().getSystemService(android.view.autofill.AutofillManager.class);
                    if (afm != null) {
                        afm.cancel();
                    }
                }
            });
        }
    }
}
