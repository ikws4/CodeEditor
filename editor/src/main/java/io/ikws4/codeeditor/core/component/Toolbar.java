package io.ikws4.codeeditor.core.component;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import io.ikws4.codeeditor.R;
import io.ikws4.codeeditor.core.CodeEditor;

public class Toolbar extends FrameLayout {
    private boolean mKeyboardVisible;

    public Toolbar(Context context) {
        this(context, null);
    }

    public Toolbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Toolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));
        LayoutInflater.from(context).inflate(R.layout.keyboard_extension_bar, this, true);
    }

    public void attach(CodeEditor editor) {
        setBackgroundColor(editor.getConfiguration().getColorScheme().getBackgroundColor());

        KeyButton tab = findViewById(R.id.tab);
        KeyButton up = findViewById(R.id.up);
        KeyButton down = findViewById(R.id.down);
        KeyButton left = findViewById(R.id.left);
        KeyButton right = findViewById(R.id.right);
        KeyButton keyboardToggle = findViewById(R.id.keyboard_toggle);

        up.setOnPressedListener(editor::selectionMoveUp);
        down.setOnPressedListener(editor::selectionMoveDown);
        left.setOnPressedListener(editor::selectionMoveLeft);
        right.setOnPressedListener(editor::selectionMoveRight);
        keyboardToggle.setOnPressedListener(() -> {
            if (mKeyboardVisible) {
                editor.hideSoftInput();
            } else {
                editor.showSoftInput();
            }
        });

        editor.addEditorKeyboardVisibleListener((visible) -> {
            mKeyboardVisible = visible;
            if (visible) {
                keyboardToggle.setImageResource(R.drawable.ic_keyboard_hide);
            } else {
                keyboardToggle.setImageResource(R.drawable.ic_keyboard);
            }
        });

    }}
