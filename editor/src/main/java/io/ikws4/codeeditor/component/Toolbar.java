package io.ikws4.codeeditor.component;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import io.ikws4.codeeditor.CodeEditor;
import io.ikws4.codeeditor.R;
import io.ikws4.codeeditor.api.editor.EditorKeyboardEventListener;

/**
 * Provide such a arrow keys, and Tab key.
 */
public class Toolbar extends FrameLayout {
    private boolean mKeyboardShowing;

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
            if (mKeyboardShowing) {
                editor.hideSoftInput();
            } else {
                editor.showSoftInput();
            }
        });

        editor.addEditorKeyboardEventListener(new EditorKeyboardEventListener() {
            @Override
            public void onShow() {
                mKeyboardShowing = true;
                keyboardToggle.setImageResource(R.drawable.ic_keyboard_hide);
            }

            @Override
            public void onHide() {
                mKeyboardShowing = false;
                keyboardToggle.setImageResource(R.drawable.ic_keyboard);
            }
        });
    }
}
