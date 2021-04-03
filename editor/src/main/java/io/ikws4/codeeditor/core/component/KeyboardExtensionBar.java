package io.ikws4.codeeditor.core.component;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import io.ikws4.codeeditor.R;
import io.ikws4.codeeditor.core.CodeEditor;

public class KeyboardExtensionBar extends FrameLayout {
    private boolean mKeyboardVisible;

    public KeyboardExtensionBar(Context context) {
        this(context, null);
    }

    public KeyboardExtensionBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyboardExtensionBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));
        LayoutInflater.from(context).inflate(R.layout.keyboard_extension_bar, this, true);
    }

    public void attach(CodeEditor editor) {
        editor.addEditorKeyboardVisibleListener((visible) -> {
            mKeyboardVisible = visible;
        });

        setBackgroundColor(editor.getConfiguration().getColorScheme().getBackgroundColor());

        KeyboardButton tab = findViewById(R.id.tab);
        KeyboardButton up = findViewById(R.id.up);
        KeyboardButton down = findViewById(R.id.down);
        KeyboardButton left = findViewById(R.id.left);
        KeyboardButton right = findViewById(R.id.right);
        KeyboardButton keyboardToggle = findViewById(R.id.keyboard_toggle);

        up.setOnPressedListener(editor::selectionMoveUp);
        down.setOnPressedListener(editor::selectionMoveDown);
        left.setOnPressedListener(editor::selectionMoveLeft);
        right.setOnPressedListener(editor::selectionMoveRight);
        keyboardToggle.setOnPressedListener(() -> {
            if (mKeyboardVisible) {
                editor.hideSoftInput();
                keyboardToggle.setImageResource(R.drawable.ic_keyboard);
            } else {
                editor.showSoftInput();
                keyboardToggle.setImageResource(R.drawable.ic_keyboard_hide);
            }
        });

    }
}
