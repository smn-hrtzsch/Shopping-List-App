package com.CapyCode.ShoppingList;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class AuthErrorMapper {

    public static String getErrorMessage(Context context, Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException) {
            return context.getString(R.string.error_login_user_not_found);
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return context.getString(R.string.error_login_wrong_password);
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            return context.getString(R.string.error_email_collision);
        } else {
            return context.getString(R.string.error_auth_failed, e != null ? e.getMessage() : "Unknown error");
        }
    }
}
