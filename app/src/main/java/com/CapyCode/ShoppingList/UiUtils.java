package com.CapyCode.ShoppingList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class UiUtils {

    public static Toast makeCustomToast(Context context, CharSequence message, int duration) {
        if (context == null) return null;
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            View layout = inflater.inflate(R.layout.toast_custom, null);
            TextView text = layout.findViewById(R.id.toast_text);
            text.setText(message);

            Toast toast = new Toast(context.getApplicationContext());
            toast.setDuration(duration);
            toast.setView(layout);
            return toast;
        } catch (Exception e) {
            // Fallback
            return Toast.makeText(context, message, duration);
        }
    }

    public static Toast makeCustomToast(Context context, int resId, int duration) {
        if (context == null) return null;
        return makeCustomToast(context, context.getText(resId), duration);
    }
    
    // Helper for short toasts if needed, but we stick to signature matching for replacement
}