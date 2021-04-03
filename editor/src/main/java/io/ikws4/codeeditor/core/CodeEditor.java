package io.ikws4.codeeditor.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spanned;
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

import io.ikws4.codeeditor.core.colorscheme.ColorScheme;
import io.ikws4.codeeditor.core.completion.SuggestionAdapter;
import io.ikws4.codeeditor.core.completion.SugguestionTokenizer;
import io.ikws4.codeeditor.core.span.SyntaxHighlightSpan;
import io.ikws4.codeeditor.core.span.TabSpan;
import io.ikws4.codeeditor.core.task.FormatTask;
import io.ikws4.codeeditor.core.task.SyntaxHighlightTask;
import io.ikws4.codeeditor.language.Language;
import io.ikws4.codeeditor.language.java.JavaLanguage;
import io.ikws4.codeeditor.language.treesitter.TSLanguageStyler;

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
    private final List<SyntaxHighlightSpan> mSyntaxHighlightSpans;
    private final TextWatcher mSyntaxHighlightTextWatcher;
    private final SyntaxTreeEditTextWatcher mSyntaxTreeEditTextWatcher; // update the syntax tree, keep it in sync witch source code
    private boolean isHighlighting = false;
    private int mTopVisiableLineStart;
    private int mTopVisiableLineEnd;
    private int mBottomVisiableLineStart;
    private int mBottomVisiableLineEnd;

    // Format
    private FormatTask mFormatTask;

    // Completion menu
    private final SuggestionAdapter mSuggestionAdapter;
    private final SugguestionTokenizer mSugguestionTokenizer;
    private final int mCompletionMenuXOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

    // KeyListener
    private EditorKeyListener mEditorKeyListener;
    private final List<EditorKeyboardVisibleListener> mEditorKeyboardVisibleListeners;

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
        mSyntaxHighlightSpans = new ArrayList<>();
        mLanguage = new JavaLanguage();
        mScroller = new OverScroller(context);
        mSuggestionAdapter = new SuggestionAdapter(context);
        mSugguestionTokenizer = new SugguestionTokenizer();

        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                CodeEditor.this.onScale(detector.getScaleFactor());
                post(CodeEditor.this::updateSyntaxHighlight);
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

        mSyntaxTreeEditTextWatcher = new SyntaxTreeEditTextWatcher(this);
        mSyntaxHighlightTextWatcher = new TextWatcher() {
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
                if (!isHighlighting) {
                    shiftSpans(getSelectionStart(), addedTextCount);
                }
                startHighlightTask();
                addedTextCount = 0;
            }
        };

        mEditorKeyListener = new EditorBaseKeyListener();
        mEditorKeyboardVisibleListeners = new ArrayList<>();

        mIMM = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        configure();
        colorize();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        mScrollGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        post(this::updateSyntaxHighlight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        post(this::updateSyntaxHighlight);
        if (Math.abs(h - oldh) > 100) {
            for (EditorKeyboardVisibleListener l : mEditorKeyboardVisibleListeners) {
                l.onChanged(oldh - h > 0);
            }
        }
    }

    @Override
    protected void onScrollChanged(int horiz, int vert, int oldHoriz, int oldVert) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert);
        post(this::updateSyntaxHighlightWhenScroll);
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

    //********************
    //* Section - config *
    //********************
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
        addTextChangedListener(mSyntaxTreeEditTextWatcher);
        addTextChangedListener(mSyntaxHighlightTextWatcher);
        setIncludeFontPadding(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setTextClassifier(TextClassifier.NO_OP);
        }

        // Scroll
        setHorizontallyScrolling(true);

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
            setTokenizer(mSugguestionTokenizer);
            mSuggestionAdapter.setData(mLanguage.getSuggestionProvider().getAll());
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


    //*******************
    //* Section - scale *
    //*******************
    protected void onScale(float factor) {
        mScaleFactory *= factor;
        mScaleFactory = Math.max(0.8f, Math.min(mScaleFactory, 1.2f));
        setTextSize(mConfiguration.getFontSize() * mScaleFactory);
    }


    //********************
    //* Section - scroll *
    //********************
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
        }
        super.computeScroll();
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

    //******************
    //* Section - draw *
    //******************
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


    //******************
    //* Section - task *
    //******************
    private void startHighlightTask() {
        if (mLanguage == null) return;

        stopHighlightTask();

        mSyntaxHighlightTask = new SyntaxHighlightTask(this, spans -> {
            mSyntaxHighlightSpans.clear();
            mSyntaxHighlightSpans.addAll(spans);
            updateSyntaxHighlight();
        });

        mSyntaxHighlightTask.execute();
    }

    private void stopHighlightTask() {
        if (mSyntaxHighlightTask != null) {
            mSyntaxHighlightTask.cancel(true);
        }
    }


    //********************
    //* Section - update *
    //********************
    private void measureGutterWidth() {
        mGutterWidth = (int) getPaint().measureText(String.valueOf(getLineCount()));
        mGutterWidth += mGutterPaddingLeft + mGutterPaddingRight;

        if (mGutterWidth != getPaddingStart()) {
            setPadding(mGutterWidth + mGutterMarginRight, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        }
    }

    //*************************************************************************
    //* Section - Complete mennu
    //*************************************************************************
    @Override
    protected void replaceText(CharSequence text) {
        clearComposingText();

        int end = getSelectionEnd();
        int start = mSugguestionTokenizer.findTokenStart(getText(), end);

        Editable editable = getText();
        editable.replace(start, end, mSugguestionTokenizer.terminateToken(text));
    }

    @Override
    public void showDropDown() {
        updateCompleteMenuPosition();
        super.showDropDown();
    }

    private void updateCompleteMenuPosition() {
        if (!hasLayout()) return;

        int extra = (int) (getLineHeight() * (getLineSpacingMultiplier() - 1.0) - getLineSpacingExtra());

        int currentLine = getCurrentLine();
        int x = (int) (getLayout().getPrimaryHorizontal(mSugguestionTokenizer.findTokenStart(getText(), getSelectionStart())) + getPaddingLeft()) - mCompletionMenuXOffset;
        int y = getLayout().getLineBottom(currentLine) - getScrollY() + getPaddingTop() + getDropDownHeight() - extra;

        if (y > getHeight()) {
            scrollBy(0, y - getHeight() + getLineHeight());
        }

        setDropDownHorizontalOffset(x);
        setDropDownVerticalOffset(y);
    }


    //******************
    //* Section - span *
    //******************

    /**
     * Update the syntax highlight span on the current visiable area.
     */
    private void updateSyntaxHighlight() {
        if (!hasLayout()) return;

        int top = getTopVisiableLine();
        int bottom = getBottomVisiableLine();

        updateSyntaxHighlight(getLayout().getLineStart(top), getLayout().getLineEnd(bottom));
    }

    /**
     * Update the syntax highlight span by give a range (start, end).
     */
    private void updateSyntaxHighlight(int start, int end) {
        if (!hasLayout() || isHighlighting) return;

        isHighlighting = true;

        cleanSyntaxHighlightSpan(start, end);
        addSyntaxHighlightSpan(start, end);

        isHighlighting = false;
    }

    /**
     * Update syntax highlight span only the top up bottom part, when scroll changed,
     * this can reduce the update area and imporve the span speed.
     * if you need update the cureen visiable screen span see {@link #updateSyntaxHighlight()}
     */
    private void updateSyntaxHighlightWhenScroll() {
        if (!hasLayout() || isHighlighting) return;

        isHighlighting = true;

        int topVisiableLine = Math.max(0, getTopVisiableLine() - 1);
        int bottomVisiableLine = getBottomVisiableLine();

        int topVisiableLineStart = getLayout().getLineStart(topVisiableLine);
        int topVisiableLineEnd = getLayout().getLineEnd(topVisiableLine);
        int bottomVisiableLineStart = getLayout().getLineStart(bottomVisiableLine);
        int bottomVisiableLineEnd = getLayout().getLineEnd(bottomVisiableLine);

        // scroll down
        if (topVisiableLineStart >= mTopVisiableLineStart) {
            if (topVisiableLine != 0) {
                cleanSyntaxHighlightSpan(Math.min(mTopVisiableLineStart, topVisiableLineStart), Math.max(mTopVisiableLineEnd, topVisiableLineEnd));
            }
            addSyntaxHighlightSpan(Math.min(mBottomVisiableLineStart, bottomVisiableLineStart), Math.max(mBottomVisiableLineEnd, bottomVisiableLineEnd));
        } else {
            if (bottomVisiableLine != getLineCount() - 1) {
                cleanSyntaxHighlightSpan(Math.min(mBottomVisiableLineStart, bottomVisiableLineStart), Math.max(mBottomVisiableLineEnd, bottomVisiableLineEnd));
            }
            addSyntaxHighlightSpan(Math.min(mTopVisiableLineStart, topVisiableLineStart), Math.max(mTopVisiableLineEnd, topVisiableLineEnd));
        }

        mTopVisiableLineStart = topVisiableLineStart;
        mTopVisiableLineEnd = topVisiableLineEnd;
        mBottomVisiableLineStart = bottomVisiableLineStart;
        mBottomVisiableLineEnd = bottomVisiableLineEnd;

        isHighlighting = false;
    }

    /**
     * Clean syntax highlight span by given range (start, end)
     */
    private void cleanSyntaxHighlightSpan(int start, int end) {
        Editable content = getText();
        SyntaxHighlightSpan[] spans = content.getSpans(start, end ,SyntaxHighlightSpan.class);
        for (SyntaxHighlightSpan span : spans) {
            content.removeSpan(span);
        }
    }

    /**
     * Add syntax highlight span by given range (start, end)
     */
    private void addSyntaxHighlightSpan(int start, int end) {
        Editable content = getText();
        int startIndex = getSpanIndexAt(start);
        int endIndex= getSpanIndexAt(end);
        for (int i = startIndex; i < endIndex; i++) {
            SyntaxHighlightSpan span = mSyntaxHighlightSpans.get(i);
            content.setSpan(span, span.getStart(), span.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private int getSpanIndexAt(int start) {
        int l = 0, r = mSyntaxHighlightSpans.size(), m;
        while (l < r) {
            m = l + (r - l) / 2;
            if (mSyntaxHighlightSpans.get(m).getStart() < start) {
                l = m + 1;
            } else {
                r = m;
            }
        }
        return l;
    }

    private void shiftSpans(int start, int offset) {
        int startIndex = getSpanIndexAt(start);
        for (int i = startIndex; i < mSyntaxHighlightSpans.size(); i++) {
            mSyntaxHighlightSpans.get(i).shift(offset);
        }
    }


    //******************
    //* Section - line *
    //******************
    /* package */ int getCurrentLine() {
        return hasLayout() ? getLayout().getLineForOffset(getSelectionStart()) : 0;
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


    //*************************************************************************
    //* Section - content & selction operation
    //*************************************************************************
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

    /**
     * Indent by given line
     * @see TSLanguageStyler#getIndentLevel(String, int, int)
     */
    /* package */ void indent(int line) {
        Editable text = getText();
        int currentLine = getCurrentLine();
        int level = mLanguage.getStyler().getIndentLevel(text.toString(),  currentLine, getPrevnonblankLine());
        String tab = mConfiguration.getIndentation().get(level);

        int start = getLayout().getLineStart(line);
        text.insert(start, tab);
        text.setSpan(new TabSpan(tab, mConfiguration.getColorScheme().getIndentColor()), start, getSelectionEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }


    //********************
    //* Section - helper *
    //********************
    private boolean hasLayout() {
        return getLayout() != null;
    }

    private int getLineMaxWidth() {
        Layout layout = getLayout();
        int n = layout.getLineCount();
        float max = 0;

        for (int i = 0; i < n; i++) {
            max = Math.max(max, layout.getLineWidth(i));
        }

        return (int) Math.ceil(max);
    }


    //*****************
    //* Section - api *
    //*****************
    public Configuration getConfiguration() {
        return mConfiguration;
    }

    public void setConfiguration(Configuration configuration) {
        mConfiguration = configuration;
    }

    public Language getLanguage() {
        return mLanguage;
    }

    public void setLanguage(Language language) {
        mLanguage = language;
    }

    public EditorKeyListener getEditorKeyListener() {
        return mEditorKeyListener;
    }

    public void setEditorKeyListener(EditorKeyListener editorKeyListener) {
        mEditorKeyListener = editorKeyListener;
    }

    public void addEditorKeyboardVisibleListener(EditorKeyboardVisibleListener l) {
        mEditorKeyboardVisibleListeners.add(l);
    }

    public void removeEditorKeyboardVisibleListener(EditorKeyboardVisibleListener l) {
        mEditorKeyboardVisibleListeners.remove(l);
    }

    public void showSoftInput() {
        mIMM.showSoftInput(this, 0);
    }

    public void hideSoftInput() {
        mIMM.hideSoftInputFromWindow(getWindowToken(), 0);
    }
}
