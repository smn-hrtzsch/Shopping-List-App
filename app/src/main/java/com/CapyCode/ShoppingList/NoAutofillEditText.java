package com.CapyCode.ShoppingList;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;

/**
 * A custom TextInputEditText that explicitly tells the Android Autofill framework
 * to ignore it by overriding its autofill type and importance.
 */
public class NoAutofillEditText extends TextInputEditText {

    public NoAutofillEditText(@NonNull Context context) {
        super(context);
    }

    public NoAutofillEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NoAutofillEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public int getAutofillType() {
        // Return AUTOFILL_TYPE_NONE to tell the system this view shouldn't be autofilled.
        return View.AUTOFILL_TYPE_NONE;
    }

    @Override
    public int getImportantForAutofill() {
        // Return IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS to ensure neither this view 
        // nor its potential children are considered for autofill.
        return View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS;
    }
}
