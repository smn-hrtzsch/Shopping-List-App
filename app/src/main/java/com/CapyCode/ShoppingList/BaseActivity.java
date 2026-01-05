package com.CapyCode.ShoppingList;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    protected View loadingOverlay;
    private TextView loadingTextView;
    private View dot1, dot2, dot3;
    private final android.os.Handler loadingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable showLoadingRunnable;
    private static final int LOADING_DELAY_MS = 250;

    protected void initLoadingOverlay(ViewGroup container) {
        initLoadingOverlay(container, R.layout.skeleton_list_row);
    }

    protected void initLoadingOverlay(ViewGroup container, int skeletonLayoutResId) {
        initLoadingOverlay(container, skeletonLayoutResId, 6);
    }

    protected void initLoadingOverlay(ViewGroup container, int skeletonLayoutResId, int skeletonCount) {
        if (loadingOverlay != null) {
            container.removeView(loadingOverlay);
        }
        loadingOverlay = getLayoutInflater().inflate(R.layout.layout_loading_overlay, container, false);
        
        ViewGroup skeletonContainer = loadingOverlay.findViewById(R.id.skeleton_content_container);
        if (skeletonContainer != null) {
            skeletonContainer.removeAllViews();
            for (int i = 0; i < skeletonCount; i++) {
                getLayoutInflater().inflate(skeletonLayoutResId, skeletonContainer, true);
            }
        }

        loadingOverlay.setVisibility(View.GONE);
        loadingOverlay.setAlpha(0f);
        container.addView(loadingOverlay);

        loadingTextView = loadingOverlay.findViewById(R.id.loading_text);
        dot1 = loadingOverlay.findViewById(R.id.dot1);
        dot2 = loadingOverlay.findViewById(R.id.dot2);
        dot3 = loadingOverlay.findViewById(R.id.dot3);
    }

    public void showLoading() {
        showLoading(getString(R.string.loading));
    }

    public void showLoading(int resId) {
        showLoading(getString(resId));
    }

    public void showLoading(String message) {
        showLoading(message, true, false);
    }

    public void showLoading(String message, boolean showSkeleton) {
        showLoading(message, showSkeleton, false);
    }

    public void showLoading(String message, boolean showSkeleton, boolean immediate) {
        if (isFinishing() || loadingOverlay == null) return;
        
        hideKeyboard();
        
        loadingHandler.removeCallbacks(showLoadingRunnable);
        
        // Immediate show if skeleton or immediate requested
        if (showSkeleton || immediate) {
            showSkeleton(showSkeleton);
            if (loadingOverlay.getVisibility() != View.VISIBLE) {
                loadingOverlay.setVisibility(View.VISIBLE);
                loadingOverlay.setAlpha(1f);
                loadingOverlay.bringToFront();
            }
        }

        showLoadingRunnable = () -> {
            if (loadingTextView != null) {
                loadingTextView.setText(message);
            }
            if (loadingOverlay.getVisibility() != View.VISIBLE) {
                loadingOverlay.setVisibility(View.VISIBLE);
                loadingOverlay.setAlpha(0f);
                loadingOverlay.animate().alpha(1f).setDuration(250).start();
                loadingOverlay.bringToFront();
            }
            startDotsAnimation();
        };
        
        if (showSkeleton || immediate) {
            loadingHandler.post(showLoadingRunnable);
        } else {
            loadingHandler.postDelayed(showLoadingRunnable, LOADING_DELAY_MS);
        }
    }

    protected void showSkeleton(boolean show) {
        if (loadingOverlay != null) {
            View skeleton = loadingOverlay.findViewById(R.id.skeleton_container);
            if (skeleton != null) {
                skeleton.setVisibility(show ? View.VISIBLE : View.GONE);
            }
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
        AnimationSet set = new AnimationSet(true);
        
        // Jump animation (Translate)
        TranslateAnimation move = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1.2f);
        move.setDuration(400);
        move.setStartOffset(delay);
        move.setRepeatMode(Animation.REVERSE);
        move.setRepeatCount(Animation.INFINITE);
        
        // Fade animation (Alpha)
        AlphaAnimation fade = new AlphaAnimation(1.0f, 0.4f);
        fade.setDuration(400);
        fade.setStartOffset(delay);
        fade.setRepeatMode(Animation.REVERSE);
        fade.setRepeatCount(Animation.INFINITE);

        set.addAnimation(move);
        set.addAnimation(fade);
        dot.startAnimation(set);
    }

    public void hideLoading() {
        loadingHandler.removeCallbacks(showLoadingRunnable);
        if (loadingOverlay != null && loadingOverlay.getVisibility() == View.VISIBLE) {
            loadingOverlay.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                loadingOverlay.setVisibility(View.GONE);
                if (dot1 != null) dot1.clearAnimation();
                if (dot2 != null) dot2.clearAnimation();
                if (dot3 != null) dot3.clearAnimation();
                onLoadingHidden();
            }).start();
        } else if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.GONE);
        }
    }

    protected void onLoadingHidden() { }

    protected boolean isLoadingOverlayVisible() {
        return loadingOverlay != null && loadingOverlay.getVisibility() == View.VISIBLE;
    }

    @Override
    protected void onDestroy() {
        loadingHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    protected void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}