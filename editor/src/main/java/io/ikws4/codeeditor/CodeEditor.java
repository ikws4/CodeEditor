package io.ikws4.codeeditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import io.ikws4.codeeditor.api.configuration.ColorScheme;
import io.ikws4.codeeditor.api.editor.Editor;
import io.ikws4.codeeditor.api.editor.EditorTextAreaListener;
import io.ikws4.codeeditor.api.editor.component.Component;
import io.ikws4.codeeditor.api.editor.EditorKeyboardListener;
import io.ikws4.codeeditor.api.editor.EditorResizeListener;
import io.ikws4.codeeditor.api.editor.EditorScaleListener;
import io.ikws4.codeeditor.api.editor.EditorScrollListener;
import io.ikws4.codeeditor.api.editor.component.ScrollDelegate;
import io.ikws4.codeeditor.api.editor.component.ScrollableComponent;
import io.ikws4.codeeditor.api.language.Language;
import io.ikws4.codeeditor.component.Gutter;
import io.ikws4.codeeditor.component.TextArea;
import io.ikws4.codeeditor.component.Toolbar;
import io.ikws4.codeeditor.configuration.Configuration;
import io.ikws4.codeeditor.language.java.JavaLanguage;

@SuppressLint("ClickableViewAccessibility")
public class CodeEditor extends FrameLayout implements Editor, ScaleGestureDetector.OnScaleGestureListener, ScrollDelegate {
    static {
        System.loadLibrary("jsitter");
    }

    private static final String TAG = "Editor";

    private Configuration mConfiguration;
    private Language mLanguage;

    private final List<EditorKeyboardListener> mKeyboardListeners;
    private final List<EditorResizeListener> mResizeListeners;
    private final List<EditorScrollListener> mScrollListeners;
    private final List<EditorScaleListener> mScaleListeners;

    private final HorizontalScrollView mHScrollView;
    private final ScrollView mVScrollView;
    private final TextArea mTextArea;
    private final Toolbar mToolbar;
    private final Gutter mGutter;

    private final ScaleGestureDetector mScaleGestureDetector;

    private int mScrollX;
    private int mScrollY;

    private final InputMethodManager mIMM;

    public CodeEditor(@NonNull Context context) {
        this(context, null);
    }

    public CodeEditor(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CodeEditor(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.editor, this, true);

        mConfiguration = new Configuration();
        mLanguage = new JavaLanguage();

        mKeyboardListeners = new ArrayList<>();
        mResizeListeners = new ArrayList<>();
        mScrollListeners = new ArrayList<>();
        mScaleListeners = new ArrayList<>();

        mHScrollView = findViewById(R.id.horizontalScrollView);
        mVScrollView = findViewById(R.id.scrollView);
        mTextArea = findViewById(R.id.textArea);
        mToolbar = findViewById(R.id.toolbar);
        mGutter = findViewById(R.id.gutter);

        // components
        addComponent(mTextArea);
        addComponent(mToolbar);
        addComponent(mGutter);

        // scroll
        mHScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            for (EditorScrollListener l : mScrollListeners) {
                l.onScroll(scrollX, mScrollY, mScrollX, mScrollY);
                mScrollX = scrollX;
            }
        });
        mVScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            for (EditorScrollListener l : mScrollListeners) {
                l.onScroll(mScrollX, scrollY, mScrollX, mScrollY);
                mScrollY = scrollY;
            }
        });
        mHScrollView.setOnTouchListener((v, event) -> onTouchEvent(event));
        mVScrollView.setOnTouchListener((v, event) -> onTouchEvent(event));

        // scale
        mScaleGestureDetector = new ScaleGestureDetector(context, this);

        mIMM = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        for (EditorScaleListener l : mScaleListeners) {
            l.onScale(detector.getScaleFactor());
        }
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        for (EditorResizeListener l : mResizeListeners) {
            l.onResize(w, h, oldw, oldh);
        }
        for (EditorKeyboardListener l : mKeyboardListeners) {
            l.onKeyboardChanged(h < oldh);
        }
    }

    @NonNull
    public Configuration getConfiguration() {
        return mConfiguration;
    }

    public void setConfiguration(@NonNull Configuration configuration) {
        Objects.requireNonNull(configuration);
        mConfiguration = configuration;
    }

    @NonNull
    @Override
    public ColorScheme getColorScheme() {
        return mConfiguration.getColorScheme();
    }

    @NonNull
    public Language getLanguage() {
        return mLanguage;
    }

    @Override
    public void setLangauge(@NonNull Language langauge) {
        Objects.requireNonNull(langauge);
        mLanguage = langauge;
    }

    /** @hide */
    @Override
    public void addComponent(@NonNull Component component) {
        Objects.requireNonNull(component);
        if (component instanceof ScrollableComponent) {
            ((ScrollableComponent) component).setScrollDelegate(this);
        }
        component.attach(this);
    }

    @Override
    public void addScrollListener(@NonNull EditorScrollListener l) {
        Objects.requireNonNull(l);
        mScrollListeners.add(l);
    }

    @Override
    public void removeScrollListener(@NonNull EditorScrollListener l) {
        Objects.requireNonNull(l);
        mScrollListeners.remove(l);
    }

    @Override
    public void addKeyboardListener(@NonNull EditorKeyboardListener l) {
        Objects.requireNonNull(l);
        mKeyboardListeners.add(l);
    }

    @Override
    public void removeKeyboardListener(@NonNull EditorKeyboardListener l) {
        Objects.requireNonNull(l);
        mKeyboardListeners.remove(l);
    }

    @Override
    public void addResizeListener(@Nonnull EditorResizeListener l) {
        Objects.requireNonNull(l);
        mResizeListeners.add(l);
    }

    @Override
    public void removeResizeListener(@Nonnull EditorResizeListener l) {
        Objects.requireNonNull(l);
        mResizeListeners.remove(l);
    }

    @Override
    public void addScaleListener(@Nonnull EditorScaleListener l) {
        Objects.requireNonNull(l);
        mScaleListeners.add(l);
    }

    @Override
    public void removeScaleListener(@Nonnull EditorScaleListener l) {
        Objects.requireNonNull(l);
        mScaleListeners.remove(l);
    }

    @Override
    public void addTextAreaListener(@NonNull EditorTextAreaListener l) {
        Objects.requireNonNull(l);
        mTextArea.addTextAreaListener(l);
    }

    @Override
    public void removeTextAreaListener(@NonNull EditorTextAreaListener l) {
        Objects.requireNonNull(l);
        mTextArea.removeTextAreaListener(l);
    }

    @Override
    public void scrollTo(int x, int y) {
        mVScrollView.scrollTo(0, y);
        mHScrollView.scrollTo(x, 0);
    }

    @Override
    public void scrollBy(int x, int y) {
        mVScrollView.scrollBy(0, y);
        mHScrollView.scrollBy(x, 0);
    }

    public void cut() {
        mTextArea.cut();
    }

    public void paste() {
        mTextArea.paste();
    }

    public void undo() {
        mTextArea.undo();
    }

    public void redo() {
        mTextArea.redo();
    }

    public void replace() {
        mTextArea.replace();
    }

    public void cursorMoveUp() {
        mTextArea.cursorMoveUp();
    }

    public void cursorMoveDown() {
        mTextArea.cursorMoveDown();
    }

    public void cursorMoveLeft() {
        mTextArea.cursorMoveLeft();
    }

    public void cursorMoveRight() {
        mTextArea.cursorMoveRight();
    }

    public void format() {
        mTextArea.format();
    }

    public void setText(String text) {
        mTextArea.setText(text);
    }

    public CharSequence getText() {
        return mTextArea.getText();
    }

    public float getTextSize() {
        return mTextArea.getTextSize();
    }

    public void showSoftInput() {
        mIMM.showSoftInput(mTextArea, 0);
    }

    public void hideSoftInput() {
        mIMM.hideSoftInputFromWindow(getWindowToken(), 0);
    }
}
