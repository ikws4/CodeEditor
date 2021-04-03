package io.ikws4.codeeditor.core.component;

import android.app.Instrumentation;
import android.content.Context;
import android.text.method.QwertyKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.Nullable;

import java.util.concurrent.ConcurrentHashMap;

import io.ikws4.codeeditor.R;
import io.ikws4.codeeditor.core.CodeEditor;

public class KeyboardExtensionBar extends FrameLayout {
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
        setBackgroundColor(editor.getConfiguration().getColorScheme().getBackgroundColor());

        KeyboardButton tab = findViewById(R.id.tab);
        KeyboardButton left = findViewById(R.id.left);
        KeyboardButton down = findViewById(R.id.down);
        KeyboardButton up = findViewById(R.id.up);
        KeyboardButton right = findViewById(R.id.right);
        KeyboardButton keyboardToggle = findViewById(R.id.keyboard_toggle);


        left.setOnPressedListener(editor::selectionMoveLeft);
        down.setOnPressedListener(editor::selectionMoveDown);
        up.setOnPressedListener(editor::selectionMoveUp);
        right.setOnPressedListener(editor::selectionMoveRight);
    }
}
