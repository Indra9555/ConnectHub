package com.example.connecthub.activities;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Base class for every screen.
 *
 * Because the app targets Android 15+ (API 35/36), edge-to-edge is enforced by the
 * system: content is drawn behind the status bar and navigation bar. Previously only
 * two screens handled the insets, so the toolbar of every other screen slid under the
 * status bar. This class applies the system-bar (and keyboard) insets as padding on
 * the content root for all screens in one place.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        applyWindowInsets(true);
    }

    /**
     * Applies status/navigation bar + IME insets as padding to the content view.
     *
     * @param includeBottom false lets a screen (e.g. one with a bottom navigation bar)
     *                      handle the bottom inset itself.
     */
    protected void applyWindowInsets(boolean includeBottom) {
        View root = findViewById(android.R.id.content);
        if (root == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            int bottom = includeBottom ? Math.max(bars.bottom, ime.bottom) : ime.bottom;

            view.setPadding(bars.left, bars.top, bars.right, bottom);
            // When the screen handles the bottom inset itself (e.g. a BottomNavigationView),
            // let the insets continue to the children.
            return includeBottom ? WindowInsetsCompat.CONSUMED : insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
