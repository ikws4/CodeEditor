package io.ikws4.codeeditor.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import io.ikws4.codeeditor.CodeEditor;
import io.ikws4.codeeditor.R;
import io.ikws4.codeeditor.api.editor.component.Component;
import io.ikws4.codeeditor.widget.KeyButton;

/**
 * Provide such a arrow keys, and Tab key.
 */
public class Toolbar extends FrameLayout implements Component {
    private boolean mKeyboardVisible;

    public Toolbar(Context context) {
        this(context, null);
    }

    public Toolbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Toolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.toolbar, this, true);
    }

    @Override
    public void attach(CodeEditor editor) {
        setBackgroundColor(editor.getColorScheme().getBackgroundColor());

        KeyButton tab = findViewById(R.id.tab);
        KeyButton up = findViewById(R.id.up);
        KeyButton down = findViewById(R.id.down);
        KeyButton left = findViewById(R.id.left);
        KeyButton right = findViewById(R.id.right);
        KeyButton keyboardToggle = findViewById(R.id.keyboard_toggle);

        up.setOnPressedListener(editor::cursorMoveUp);
        down.setOnPressedListener(editor::cursorMoveDown);
        left.setOnPressedListener(editor::cursorMoveLeft);
        right.setOnPressedListener(editor::cursorMoveRight);
        keyboardToggle.setOnPressedListener(() -> {
            if (mKeyboardVisible) {
                editor.hideSoftInput();
            } else {
                editor.showSoftInput();
            }
        });

        editor.addKeyboardListener(visible -> {
            if (visible) {
                mKeyboardVisible = true;
                keyboardToggle.setImageResource(R.drawable.ic_keyboard_hide);
            } else {
                mKeyboardVisible = false;
                keyboardToggle.setImageResource(R.drawable.ic_keyboard);
            }
        });
    }
}
