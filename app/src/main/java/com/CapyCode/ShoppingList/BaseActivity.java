package com.CapyCode.ShoppingList;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    private View loadingOverlay;
    private TextView loadingTextView;
    private View dot1, dot2, dot3;
    private final android.os.Handler loadingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable showLoadingRunnable;
    private static final int LOADING_DELAY_MS = 250;

    protected void initLoadingOverlay(android.view.ViewGroup container) {
        if (loadingOverlay != null) {
            container.removeView(loadingOverlay);
        }
        loadingOverlay = getLayoutInflater().inflate(R.layout.layout_loading_overlay, container, false);
        loadingOverlay.setVisibility(View.GONE);
        loadingOverlay.setAlpha(0f);
        container.addView(loadingOverlay);

        loadingTextView = loadingOverlay.findViewById(R.id.loading_text);
        dot1 = loadingOverlay.findViewById(R.id.dot1);
        dot2 = loadingOverlay.findViewById(R.id.dot2);
        dot3 = loadingOverlay.findViewById(R.id.dot3);
    }

    protected void showSkeleton(boolean show) {
        if (loadingOverlay != null) {
            View skeleton = loadingOverlay.findViewById(R.id.skeleton_container);
            if (skeleton != null) {
                skeleton.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }
    }

    public void showLoading() {
        showLoading(getString(R.string.loading));
    }

    public void showLoading(int resId) {
        showLoading(getString(resId));
    }

    public void showLoading(String message) {
        showLoading(message, true);
    }
    
    public void showLoading(String message, boolean showSkeleton) {
        if (isFinishing() || loadingOverlay == null) return;
        
        hideKeyboard();
        
        // Show skeleton immediately if requested to prevent flickering of empty views
        if (showSkeleton) {
            showSkeleton(true);
            loadingOverlay.setVisibility(View.VISIBLE);
            loadingOverlay.setAlpha(1f);
        }

        loadingHandler.removeCallbacks(showLoadingRunnable);
        showLoadingRunnable = () -> {
            if (loadingTextView != null) {
                loadingTextView.setText(message);
            }
            if (loadingOverlay.getVisibility() != View.VISIBLE) {
                loadingOverlay.setVisibility(View.VISIBLE);
                loadingOverlay.animate().alpha(1f).setDuration(300).start();
            }
            startDotsAnimation();
        };
        
        if (showSkeleton) {
            // If skeleton is already shown, just run the text update/animation logic
            loadingHandler.post(showLoadingRunnable);
        } else {
            loadingHandler.postDelayed(showLoadingRunnable, LOADING_DELAY_MS);
        }
    }

    private void startDotsAnimation() {
        animateDot(dot1, 0);
        animateDot(dot2, 150);
        animateDot(dot3, 300);
    }

    private void animateDot(View dot, int delay) {
        if (dot == null) return;
        dot.clearAnimation();
        android.view.animation.AnimationSet set = new android.view.animation.AnimationSet(true);
        
        android.view.animation.TranslateAnimation move = new android.view.animation.TranslateAnimation(0, 0, 0, -15);
        move.setDuration(400);
        move.setStartOffset(delay);
        move.setRepeatMode(android.view.animation.Animation.REVERSE);
        move.setRepeatCount(android.view.animation.Animation.INFINITE);
        
        android.view.animation.AlphaAnimation fade = new android.view.animation.AlphaAnimation(1.0f, 0.4f);
        fade.setDuration(400);
        fade.setStartOffset(delay);
        fade.setRepeatMode(android.view.animation.Animation.REVERSE);
        fade.setRepeatCount(android.view.animation.Animation.INFINITE);

        set.addAnimation(move);
        set.addAnimation(fade);
        dot.startAnimation(set);
    }

    public void hideLoading() {
        loadingHandler.removeCallbacks(showLoadingRunnable);
        if (loadingOverlay != null && loadingOverlay.getVisibility() == View.VISIBLE) {
            loadingOverlay.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                loadingOverlay.setVisibility(View.GONE);
                if (dot1 != null) dot1.clearAnimation();
                if (dot2 != null) dot2.clearAnimation();
                if (dot3 != null) dot3.clearAnimation();
            }).start();
        }
    }

    @Override
    protected void onDestroy() {
        loadingHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    protected void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}