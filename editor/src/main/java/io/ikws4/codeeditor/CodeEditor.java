package io.ikws4.codeeditor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.textclassifier.TextClassifier;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.ikws4.codeeditor.api.editor.EditorKeyListener;
import io.ikws4.codeeditor.api.editor.EditorKeyboardEventListener;
import io.ikws4.codeeditor.configuration.Configuration;
import io.ikws4.codeeditor.api.configuration.ColorScheme;
import io.ikws4.codeeditor.completion.SuggestionAdapter;
import io.ikws4.codeeditor.completion.IdentifireTokenizer;
import io.ikws4.codeeditor.span.ExtendedSpan;
import io.ikws4.codeeditor.span.TabSpan;
import io.ikws4.codeeditor.task.FormatTask;
import io.ikws4.codeeditor.task.SyntaxHighlightTask;
import io.ikws4.codeeditor.api.language.Language;
import io.ikws4.codeeditor.language.java.JavaLanguage;
import io.ikws4.codeeditor.language.TSLanguageStyler;

public class CodeEditor extends AppCompatMultiAutoCompleteTextView {
    // load jsitter for syntax highlight and indent.
    static {
        System.loadLibrary("jsitter");
    }

    private static final String TAG = "TextEditor";

    private Language mLanguage;
    private Configuration mConfiguration;

    // Scale
    private final ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactory = 1.0f;

    // Scroll
    private final GestureDetector mScrollGestureDetector;
    private final OverScroller mScroller;

    // Paints
    private final Paint mGutterPaint = new Paint();
    private final Paint mGutterDividerPaint = new Paint();
    private final Paint mGutterTextPaint = new Paint();
    private final Paint mGutterActiveTextPaint = new Paint();
    private final Paint mCursorLinePaint = new Paint();

    // Gutter
    private int mGutterWidth;
    private final int mGutterPaddingLeft = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
    private final int mGutterPaddingRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
    private final int mGutterMarginRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());

    // Highlight
    private SyntaxHighlightTask mSyntaxHighlightTask;
    private final List<ExtendedSpan> mSpans;
    private boolean isUpdatingSpan = false;
    private int mTopVisiableLineStart;
    private int mTopVisiableLineEnd;
    private int mBottomVisiableLineStart;
    private int mBottomVisiableLineEnd;

    // Format
    private FormatTask mFormatTask;

    // Completion menu
    private final SuggestionAdapter mSuggestionAdapter;
    private final IdentifireTokenizer mIdentifireTokenizer;
    private final int mCompletionMenuXOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

    // KeyListener
    private EditorKeyListener mEditorKeyListener;
    private final List<EditorKeyboardEventListener> mEditorKeyboardEventListeners;

    private final InputMethodManager mIMM;


    public CodeEditor(@NonNull Context context) {
        this(context, null);
    }

    public CodeEditor(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.autoCompleteTextViewStyle);
    }

    public CodeEditor(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mConfiguration = new Configuration();
        mLanguage = new JavaLanguage();
        mSpans = new ArrayList<>();
        mScroller = new OverScroller(context);
        mSuggestionAdapter = new SuggestionAdapter(context);
        mIdentifireTokenizer = new IdentifireTokenizer();

        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                CodeEditor.this.onScale(detector.getScaleFactor());
                return true;
            }
        });
        mScrollGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                mScroller.forceFinished(true);
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                fling((int) -velocityX, (int) -velocityY);
                return true;
            }
        });

        mEditorKeyListener = new EditorBaseKeyListener();
        mEditorKeyboardEventListeners = new ArrayList<>();

        mIMM = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        configure();
        colorize();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mConfiguration.isPinchZoom()) {
            mScaleGestureDetector.onTouchEvent(event);
        }
        mScrollGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        post(this::updateSpan);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        post(this::updateSpan);

        // handle keyboard event listener
        if (Math.abs(h - oldh) > 100) {
            for (EditorKeyboardEventListener l : mEditorKeyboardEventListeners) {
                if (oldh > h) {
                    l.onShow(); 
                } else {
                    l.onHide();
                }
            }
        }
    }

    @Override
    protected void onScrollChanged(int horiz, int vert, int oldHoriz, int oldVert) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert);
        post(this::updateSpanWhenScroll);
    }

    @Override
    public void setTextSize(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    @Override
    public void setTextSize(int unit, float size) {
        super.setTextSize(unit, size);
        float s = TypedValue.applyDimension(unit, size, getResources().getDisplayMetrics());
        mGutterTextPaint.setTextSize(s);
        mGutterActiveTextPaint.setTextSize(s);
        mSuggestionAdapter.setTextSize(size);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mEditorKeyListener.onKeyDown(this, keyCode, event);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        mEditorKeyListener.onKeyUp(this, keyCode, event);
        return super.onKeyUp(keyCode, event);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Configuration
    ///////////////////////////////////////////////////////////////////////////
    protected void configure() {
        // Text & font
        setTextSize(mConfiguration.getFontSize());
        setTypeface(Typeface.MONOSPACE);
        setLineSpacing(0, 1.1f);
        setGravity(Gravity.TOP | Gravity.START);
        setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        setInputType(EditorInfo.TYPE_CLASS_TEXT
                | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
                | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        addTextChangedListener(new SyntaxTreeEditWatcher(this));
        addTextChangedListener(new TextWatcher() {
            private int addedTextCount = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                addedTextCount -= count;
                stopHighlightTask();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                addedTextCount += count;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdatingSpan) {
                    TextBuffer.shiftSpans(mSpans, getSelectionStart(), addedTextCount);
                }
                startHighlightTask();
                addedTextCount = 0;
            }
        });
        setIncludeFontPadding(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setTextClassifier(TextClassifier.NO_OP);
        }

        // Wrap
        setHorizontallyScrolling(!mConfiguration.isWrap());

        // Clipboard panel & key handle
        setMovementMethod(EditorMovementMethod.getInstance(this));
        setCustomInsertionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                menu.clear();
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }
        });
        setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                menu.clear();
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }
        });

        // Completion menu
        if (mConfiguration.isCompletion()) {
            setThreshold(1);
            setAdapter(mSuggestionAdapter);
            setTokenizer(mIdentifireTokenizer);
            mSuggestionAdapter.setData(mLanguage.getSuggestionProvider().getAll());
        } else {
            setAdapter(null);
            setTokenizer(null);
        }
    }

    protected void colorize() {
        ColorScheme colorScheme = mConfiguration.getColorScheme();

        // TextEdit configure
        setTextColor(colorScheme.getTextColor());
        setBackgroundColor(colorScheme.getBackgroundColor());
        setHighlightColor(colorScheme.getSelectionColor());

        // Completion menu
        setDropDownBackgroundDrawable(new ColorDrawable(colorScheme.getCompletionMenuBackgroundColor()));
        mSuggestionAdapter.setColorScheme(colorScheme);

        // paints
        Paint basePaint = getPaint();

        mGutterPaint.setColor(colorScheme.getGutterColor());

        mGutterDividerPaint.setColor(colorScheme.getGutterDividerColor());
        mGutterDividerPaint.setStyle(Paint.Style.STROKE);
        mGutterDividerPaint.setStrokeWidth(1.0f);

        mGutterTextPaint.setColor(colorScheme.getGutterTextColor());
        mGutterTextPaint.setTypeface(basePaint.getTypeface());
        mGutterTextPaint.setAntiAlias(basePaint.isAntiAlias());
        mGutterTextPaint.setTextAlign(Paint.Align.RIGHT);

        mGutterActiveTextPaint.setColor(colorScheme.getGutterActiveTextColor());
        mGutterActiveTextPaint.setTypeface(basePaint.getTypeface());
        mGutterActiveTextPaint.setAntiAlias(basePaint.isAntiAlias());
        mGutterActiveTextPaint.setTextAlign(Paint.Align.RIGHT);

        mCursorLinePaint.setColor(colorScheme.getCursorLineColor());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Draw
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected void onDraw(Canvas canvas) {
        if (mConfiguration.isNumber()) {
            drawGutter(canvas);
            drawGutterDivider(canvas);
        }

        if (mConfiguration.isCursorLine()) {
            drawCursorLine(canvas);
        }

        if (mConfiguration.isNumber()) {
            drawLineNumber(canvas);
        }

        super.onDraw(canvas);
    }

    private void drawCursorLine(Canvas canvas) {
        if (!hasLayout() || hasSelection()) return;

        int lineHeight = getLineHeight();
        float left = getScrollX();
        float right = getLayout().getWidth() + getPaddingLeft() + getPaddingRight();
        float top = getLayout().getLineTop(getCurrentLine()) + getPaddingTop() - (lineHeight - (lineHeight / getLineSpacingMultiplier())) / 2;
        float bottom = top + lineHeight;

        canvas.drawRect(left, top, right, bottom, mCursorLinePaint);
    }

    private void drawGutter(Canvas canvas) {
        measureGutterWidth();

        float left = getScrollX();
        float top = getScrollY();

        canvas.drawRect(left, top, left + mGutterWidth, top + getHeight(), mGutterPaint);
    }

    private void drawGutterDivider(Canvas canvas) {
        int x = getScrollX() + mGutterWidth;
        int y = getScrollY();

        canvas.drawLine(x, y, x, y + getHeight(), mGutterDividerPaint);
    }

    private void drawLineNumber(Canvas canvas) {
        if (!hasLayout()) return;

        int currentLine = getCurrentLine();
        int minVisiableLine = getTopVisiableLine();
        int maxVisiableLine = getBottomVisiableLine();

        for (int line = minVisiableLine; line <= maxVisiableLine; line++) {
            float x = getScrollX() + mGutterWidth - mGutterPaddingRight;
            float y = getLayout().getLineBaseline(line) + getPaddingTop();

            canvas.drawText(String.valueOf(line + 1), x, y, line == currentLine ? mGutterActiveTextPaint : mGutterTextPaint);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Scroll & Scale
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
        }
        super.computeScroll();
    }

    protected void onScale(float factor) {
        mScaleFactory *= factor;
        mScaleFactory = Math.max(0.8f, Math.min(mScaleFactory, 1.2f));
        setTextSize(mConfiguration.getFontSize() * mScaleFactory);

        // Need to update the syntax because the visible area changed.
        post(CodeEditor.this::updateSpan);
    }

    private void fling(int velocityX, int velocityY) {
        if (!hasLayout() ||
                getLayout().getHeight() < getHeight() ||
                getLayout().getWidth() < getWidth()) return;

        int maxX = getLayout().getWidth() - getWidth() + getPaddingLeft() + getPaddingRight();
        int maxY = getLayout().getHeight() - getHeight() + getPaddingTop() + getPaddingBottom();
        mScroller.forceFinished(true);
        mScroller.fling(getScrollX(), getScrollY(), velocityX, velocityY, 0, maxX, 0, maxY);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Completion menu
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected void replaceText(CharSequence text) {
        clearComposingText();

        int end = getSelectionEnd();
        int start = mIdentifireTokenizer.findTokenStart(getText(), end);

        Editable editable = getText();
        editable.replace(start, end, mIdentifireTokenizer.terminateToken(text));
    }

    @Override
    public void showDropDown() {
        updateCompletionMenuPosition();
        super.showDropDown();
    }

    private void updateCompletionMenuPosition() {
        if (!hasLayout()) return;

        int extra = (int) (getLineHeight() * (getLineSpacingMultiplier() - 1.0) - getLineSpacingExtra());

        int currentLine = getCurrentLine();
        int x = (int) (getLayout().getPrimaryHorizontal(mIdentifireTokenizer.findTokenStart(getText(), getSelectionStart())) + getPaddingLeft()) - mCompletionMenuXOffset;
        int y = getLayout().getLineBottom(currentLine) - getScrollY() + getPaddingTop() + getDropDownHeight() - extra;

        if (y > getHeight()) {
            scrollBy(0, y - getHeight() + getLineHeight());
        }

        setDropDownHorizontalOffset(x);
        setDropDownVerticalOffset(y);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Span
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Update the span on the current visiable area.
     */
    private void updateSpan() {
        if (!hasLayout()) return;

        int top = getTopVisiableLine();
        int bottom = getBottomVisiableLine();

        updateSpan(getLayout().getLineStart(top), getLayout().getLineEnd(bottom));
    }

    /**
     * Update the span by give a range (start, end).
     */
    private void updateSpan(int start, int end) {
        if (!hasLayout() || isUpdatingSpan) return;

        isUpdatingSpan = true;

        TextBuffer.cleanSpans(getText(), start, end);
        TextBuffer.addSpans(getText(), mSpans, start, end);

        isUpdatingSpan = false;
    }

    /**
     * Update span only the top up bottom part, when scroll changed,
     * this can reduce the update area and imporve the span speed.
     * if you need update the cureen visiable screen span see {@link #updateSpan()}
     */
    private void updateSpanWhenScroll() {
        if (!hasLayout() || isUpdatingSpan) return;

        isUpdatingSpan = true;

        int topVisiableLine = Math.max(0, getTopVisiableLine() - 1);
        int bottomVisiableLine = getBottomVisiableLine();

        int topVisiableLineStart = getLayout().getLineStart(topVisiableLine);
        int topVisiableLineEnd = getLayout().getLineEnd(topVisiableLine);
        int bottomVisiableLineStart = getLayout().getLineStart(bottomVisiableLine);
        int bottomVisiableLineEnd = getLayout().getLineEnd(bottomVisiableLine);

        Editable text = getText();

        // scroll down
        if (topVisiableLineStart >= mTopVisiableLineStart) {
            if (topVisiableLine != 0) {
                TextBuffer.cleanSpans(text, Math.min(mTopVisiableLineStart, topVisiableLineStart), Math.max(mTopVisiableLineEnd, topVisiableLineEnd));
            }
            TextBuffer.addSpans(text, mSpans, Math.min(mBottomVisiableLineStart, bottomVisiableLineStart), Math.max(mBottomVisiableLineEnd, bottomVisiableLineEnd));
        } else {
            if (bottomVisiableLine != getLineCount() - 1) {
                TextBuffer.cleanSpans(text, Math.min(mBottomVisiableLineStart, bottomVisiableLineStart), Math.max(mBottomVisiableLineEnd, bottomVisiableLineEnd));
            }
            TextBuffer.addSpans(text, mSpans, Math.min(mTopVisiableLineStart, topVisiableLineStart), Math.max(mTopVisiableLineEnd, topVisiableLineEnd));
        }

        mTopVisiableLineStart = topVisiableLineStart;
        mTopVisiableLineEnd = topVisiableLineEnd;
        mBottomVisiableLineStart = bottomVisiableLineStart;
        mBottomVisiableLineEnd = bottomVisiableLineEnd;

        isUpdatingSpan = false;
    }


    ///////////////////////////////////////////////////////////////////////////
    // Task
    ///////////////////////////////////////////////////////////////////////////
    private void startHighlightTask() {
        if (mLanguage == null) return;

        stopHighlightTask();

        mSyntaxHighlightTask = new SyntaxHighlightTask(this, spans -> {
            mSpans.clear();
            mSpans.addAll(spans);
            updateSpan();
        });

        mSyntaxHighlightTask.execute();
    }

    private void stopHighlightTask() {
        if (mSyntaxHighlightTask != null) {
            mSyntaxHighlightTask.cancel(true);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Helper function
    ///////////////////////////////////////////////////////////////////////////
    /* package */ int getCurrentLine() {
        return hasLayout() ? getLayout().getLineForOffset(getSelectionStart()) : 0;
    }

    /**
     * Indent by given line
     * @see TSLanguageStyler#getIndentLevel(int, int)
     */
    /* package */ void indent(int line) {
        Editable content = getText();
        int currentLine = getCurrentLine();
        int level = mLanguage.getStyler().getIndentLevel(currentLine, getPrevnonblankLine());
        String tab = mConfiguration.getIndentation().get(level);

        int start = getLayout().getLineStart(line);
        content.insert(start, tab);
        TextBuffer.setSpan(content, new TabSpan(tab, mConfiguration.getColorScheme().getIndentColor(), start, getSelectionEnd()));
    }

    private int getTopVisiableLine() {
        return hasLayout() ? getLayout().getLineForVertical(getScrollY()) : 0;
    }

    private int getBottomVisiableLine() {
        return hasLayout() ? getLayout().getLineForVertical(getScrollY() + getHeight()) : 0;
    }

    /**
     * If line is blank means it only have blank character like space, tab etc.
     * @see Character#isWhitespace(char)
     */
    private boolean isBlankLine(int line) {
        if (line < 0) return true;
        int start = getLayout().getLineStart(line);
        int end = getLayout().getLineEnd(line);
        Editable text = getText();
        while (start < end) {
            if (!Character.isWhitespace(text.charAt(start))) {
                return false;
            }
            start++;
        }
        return true;
    }

    /**
     * @return the previous non blank line
     */
    private int getPrevnonblankLine() {
        int line = getCurrentLine();
        while (!isBlankLine(line)) {
            line--;
        }
        return line;
    }

    private boolean hasLayout() {
        return getLayout() != null;
    }

    private void measureGutterWidth() {
        mGutterWidth = (int) getPaint().measureText(String.valueOf(getLineCount()));
        mGutterWidth += mGutterPaddingLeft + mGutterPaddingRight;

        if (mGutterWidth != getPaddingStart()) {
            setPadding(mGutterWidth + mGutterMarginRight, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public function (api)
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    public Configuration getConfiguration() {
        return mConfiguration;
    }

    public void setConfiguration(@NonNull Configuration configuration) {
        Objects.requireNonNull(configuration);
        mConfiguration = configuration;
    }

    @NonNull
    public Language getLanguage() {
        return mLanguage;
    }

    public void setLanguage(@NonNull Language language) {
        Objects.requireNonNull(language);
        mLanguage = language;
    }

    @Nullable
    public EditorKeyListener getEditorKeyListener() {
        return mEditorKeyListener;
    }

    public void setEditorKeyListener(@Nullable EditorKeyListener editorKeyListener) {
        mEditorKeyListener = editorKeyListener;
    }

    /**
     * To make this listener work, you should put android:windowSoftInputMode="adjustResize" in
     * the AndroidManifest's Activity attribution where your soft keyboard has been used.
     */
    public void addEditorKeyboardEventListener(EditorKeyboardEventListener l) {
        mEditorKeyboardEventListeners.add(l);
    }

    public void removeEditorKeyboardEventListener(EditorKeyboardEventListener l) {
        mEditorKeyboardEventListeners.remove(l);
    }

    public void showSoftInput() {
        mIMM.showSoftInput(this, 0);
    }

    public void hideSoftInput() {
        mIMM.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    public void cut() {
        onTextContextMenuItem(android.R.id.cut);
    }

    public void paste() {
        onTextContextMenuItem(android.R.id.paste);
    }

    public void undo() {
        onTextContextMenuItem(android.R.id.undo);
    }

    public void redo() {
        onTextContextMenuItem(android.R.id.redo);
    }

    public void selectAll() {
        onTextContextMenuItem(android.R.id.selectAll);
    }

    public void replace() {
        // FIXME: seems replace not working
        onTextContextMenuItem(android.R.id.replaceText);
    }

    public void selectionMoveUp() {
        if (hasLayout()) {
            Selection.moveUp(getText(), getLayout());
        }
    }

    public void selectionMoveDown() {
        if (hasLayout()) {
            Selection.moveDown(getText(), getLayout());
        }
    }

    public void selectionMoveLeft() {
        if (hasLayout()) {
            Selection.moveLeft(getText(), getLayout());
        }
    }

    public void selectionMoveRight() {
        if (hasLayout()) {
            Selection.moveRight(getText(), getLayout());
        }
    }

    public void format() {
        if (mFormatTask != null) {
            mFormatTask.cancel(true);
        }

        Editable content = getText();

        mFormatTask = new FormatTask(this, (string) -> {
            content.replace(0, content.length(), string);
        });

        mFormatTask.execute();
    }

}
