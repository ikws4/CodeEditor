//package io.ikws4.codeeditor;
//
//import android.app.ActionBar;
//import android.content.ClipboardManager;
//import android.content.Context;
//import android.content.res.Resources;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.graphics.Path;
//import android.graphics.Rect;
//import android.graphics.RectF;
//import android.graphics.Typeface;
//import android.media.MediaRoute2Info;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.ResultReceiver;
//import android.os.SystemClock;
//import android.text.BoringLayout;
//import android.text.DynamicLayout;
//import android.text.Editable;
//import android.text.GetChars;
//import android.text.InputFilter;
//import android.text.InputType;
//import android.text.Layout;
//import android.text.ParcelableSpan;
//import android.text.Selection;
//import android.text.SpanWatcher;
//import android.text.Spannable;
//import android.text.Spanned;
//import android.text.SpannedString;
//import android.text.TextPaint;
//import android.text.TextWatcher;
//import android.text.method.MetaKeyKeyListener;
//import android.text.method.MovementMethod;
//import android.text.method.PasswordTransformationMethod;
//import android.text.method.TransformationMethod;
//import android.util.AttributeSet;
//import android.util.FloatMath;
//import android.util.TypedValue;
//import android.view.KeyEvent;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.ViewGroup.LayoutParams;
//import android.view.ViewTreeObserver;
//import android.view.accessibility.AccessibilityManager;
//import android.view.animation.AnimationUtils;
//import android.view.inputmethod.BaseInputConnection;
//import android.view.inputmethod.CompletionInfo;
//import android.view.inputmethod.EditorInfo;
//import android.view.inputmethod.InputConnection;
//import android.view.inputmethod.InputMethodManager;
//import android.widget.Scroller;
//import android.widget.TextView;
//
//import androidx.annotation.ColorInt;
//import androidx.annotation.Dimension;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//
//import java.lang.ref.WeakReference;
//import java.util.ArrayList;
//
//public class TextEditor extends View implements ViewTreeObserver.OnPreDrawListener {
//    private EditorTouchNavigationMethod mTouchNavigationMethod;
//    private EditorInputConnection mInputConnection;
//
//    private TextPaint mTextPaint;
//    private int mHighlightColor;
//
//    private Layout mLayout;
//
//    private Editable mText;
//    private Editable.Factory mEditableFactory;
//
//    private static final InputFilter[] NO_FILTERS = new InputFilter[0];
//    private static final Spanned EMPTY_SPANNED = new SpannedString("");
//    private InputFilter[] mFilters = NO_FILTERS;
//
//    private ArrayList<TextWatcher> mListeners;
//
//    private boolean mHorizontallyScrolling = false;
//
//    private float mSpacingMult = 1.0f;
//    private float mSpacingAdd = 0.0f;
//
//    private static final int PREDRAW_NOT_REGISTERED = 0;
//    private static final int PREDRAW_PENDING = 1;
//    private static final int PREDRAW_DONE = 2;
//    private int mPreDrawState = PREDRAW_NOT_REGISTERED;
//
//    private static int PRIORITY = 100;
//
//    private int mCurTextColor;
//    private int mCurHintTextColor;
//    private boolean mFreezesText;
//    private boolean mFrozenWithFocus;
//    private boolean mTemporaryDetach;
//    private boolean mDispatchTemporaryDetach;
//
//    private boolean mEatTouchRelease = false;
//    private boolean mScrolled = false;
//
//    private CharSequence mError;
//    private boolean mErrorWasChanged;
//    /**
//     * This flag is set if the TextView tries to display an error before it
//     * is attached to the window (so its position is still unknown).
//     * It causes the error to be shown later, when onAttachedToWindow()
//     * is called.
//     */
//    private boolean mShowErrorAfterAttach;
//
//    private CharWrapper mCharWrapper = null;
//
//    private boolean mSelectionMoved = false;
//    private boolean mTouchFocusSelected = false;
//
//    class InputMethodState {
//        Rect mCursorRectInWindow = new Rect();
//        RectF mTmpRectF = new RectF();
//        float[] mTmpOffset = new float[2];
//        int mBatchEditNesting;
//        boolean mCursorChanged;
//        boolean mSelectionModeChanged;
//        boolean mContentChanged;
//        int mChangedStart, mChangedEnd, mChangedDelta;
//    }
//    private InputMethodState mInputMethodState;
//
//
//    public TextEditor(Context context) {
//        this(context, null);
//    }
//
//    public TextEditor(Context context, @Nullable AttributeSet attrs) {
//        this(context, attrs, 0);
//    }
//
//    public TextEditor(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//        mTouchNavigationMethod = new EditorTouchNavigationMethod(this);
//        mInputConnection = new EditorInputConnection(this);
//        setBackgroundColor(Color.WHITE);
//        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
//        setTextSize(16);
//        setTextColor(Color.BLACK);
//
//        mEditableFactory = Editable.Factory.getInstance();
//        mText = mEditableFactory.newEditable("");
//        mLayout = new DynamicLayout(mText, mTextPaint, 1024 * 1024, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
//
//        setLongClickable(true);
//        setFocusableInTouchMode(true);
//        setHapticFeedbackEnabled(true);
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        mTouchNavigationMethod.onTouchEvent(event);
//        return super.onTouchEvent(event);
//    }
//
//    /**
//     * @return the size (in pixels) of the default text size in this TextEditor.
//     */
//    public final float getTextSize() {
//        return mTextPaint.getTextSize();
//    }
//
//    /**
//     * Set the default text size to the given value, interpreted as "scaled
//     * pixel" units.  This size is adjusted based on the current density and
//     * user font size preference.
//     *
//     * @param size The scaled pixel size.
//     */
//    public void setTextSize(@Dimension(unit = Dimension.SP) float size) {
//        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
//    }
//
//    /**
//     * Set the default text size to a given unit and value.  See {@link
//     * TypedValue} for the possible dimension units.
//     * k*
//     *
//     * @param unit The desired dimension unit.
//     * @param size The desired size in the given units.
//     */
//    public void setTextSize(int unit, float size) {
//        Context context = getContext();
//        Resources resources;
//
//        if (context == null) {
//            resources = Resources.getSystem();
//        } else {
//            resources = context.getResources();
//        }
//
//        setRawTextSize(TypedValue.applyDimension(unit, size, resources.getDisplayMetrics()));
//    }
//
//    private void setRawTextSize(float size) {
//        if (size != mTextPaint.getTextSize()) {
//            mTextPaint.setTextSize(size);
//
//            if (mLayout != null) {
//                nullLayout();
//                requestLayout();
//                invalidate();
//            }
//        }
//    }
//
//    /**
//     * Sets the typeface and style in which the text should be displayed.
//     * Note that not all Typeface families actually have bold and italic
//     * variants, so you may need to use
//     * {@link #setTypeface(Typeface, int)} to get the appearance
//     * that you actually want.
//     */
//    public void setTypeface(Typeface tf) {
//        if (tf != mTextPaint.getTypeface()) {
//            mTextPaint.setTypeface(tf);
//
//            if (mLayout != null) {
//                nullLayout();
//                requestLayout();
//                invalidate();
//            }
//        }
//    }
//
//    /**
//     * @return the current typeface and style in which the text is being
//     * displayed.
//     */
//    public Typeface getTypeface() {
//        return mTextPaint.getTypeface();
//    }
//
//    /**
//     * Sets the text color for all the states (normal, selected,
//     * focused) to be this color.
//     */
//    public void setTextColor(@ColorInt int color) {
//        if (color != mTextPaint.getColor()) {
//            mTextPaint.setColor(color);
//
//            invalidate();
//        }
//    }
//
//    public final int getTextColor() {
//        return mTextPaint.getColor();
//    }
//
//    /**
//     * Sets the color used to display the selection highlight.
//     */
//    public final void setHighlightColor(@ColorInt int color) {
//        if (mHighlightColor != color) {
//            mHighlightColor = color;
//            invalidate();
//        }
//    }
//
//
//    /**
//     * @return the base paint used for the text. Please use this only to
//     * consult the Paint's properties and not to change them.
//     */
//    public TextPaint getPaint() {
//        return mTextPaint;
//    }
//
//    /**
//     * Sets whether the text should be allowed to be wider than the
//     * View is.  If false, it will be wrapped to the width of the View.
//     */
//    public void setHorizontallyScrolling(boolean whether) {
//        mHorizontallyScrolling = whether;
//
//        if (mLayout != null) {
//            nullLayout();
//            requestLayout();
//            invalidate();
//        }
//    }
//
//    /**
//     * Sets line spacing for this TextEditor.  Each line will have its height
//     * multiplied by <code>mult</code> and have <code>add</code> added to it.
//     */
//    public void setLineSpacing(float add, float mult) {
//        mSpacingMult = mult;
//        mSpacingAdd = add;
//
//        if (mLayout != null) {
//            nullLayout();
//            requestLayout();
//            invalidate();
//        }
//    }
//
//    /**
//     * Sets the string value of the TextEditor.
//     */
//    public void setText(CharSequence text) {
//        setText(text, true, 0);
//
//        if (mCharWrapper != null) {
//            mCharWrapper.mChars = null;
//        }
//    }
//
//    private void setText(CharSequence text, boolean notifyBefore, int oldLen) {
//        if (text == null) {
//            text = "";
//        }
//
//        for (InputFilter filter : mFilters) {
//            CharSequence out = filter.filter(text, 0, text.length(), EMPTY_SPANNED, 0, 0);
//            if (out != null) {
//                text = out;
//            }
//        }
//
//        if (notifyBefore) {
//            if (mText != null) {
//                oldLen = mText.length();
//                sendBeforeTextChanged(mText, 0, oldLen, text.length());
//            } else {
//                sendBeforeTextChanged("", 0, 0, text.length());
//            }
//        }
//
//        mText = mEditableFactory.newEditable(text);
//        text = mText;
//        setFilters(mFilters);
//        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//        if (imm != null) imm.restartInput(this);
//
//        final int textLength = text.length();
//
//        Spannable sp = (Spannable) text;
//        final ChangeWatcher[] watchers = sp.getSpans(0, sp.length(), ChangeWatcher.class);
//        for (ChangeWatcher watcher : watchers) {
//            sp.removeSpan(watcher);
//        }
//
//        if (mChangeWatcher == null) {
//            mChangeWatcher = new ChangeWatcher();
//        }
//
//        sp.setSpan(mChangeWatcher, 0, textLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE | (PRIORITY << Spanned.SPAN_PRIORITY_SHIFT));
//
//        if (mMovement != null) {
//            mSelectionMoved = false;
//        }
//
//        if (mLayout != null) {
//            checkForRelayout();
//        }
//
//        sendOnTextChanged(text, 0, oldLen, textLength);
//        onTextChanged(text, 0, oldLen, textLength);
//        sendAfterTextChanged((Editable) text);
//    }
//
//    private static class CharWrapper implements CharSequence, GetChars {
//        private char[] mChars;
//        private int mStart, mLength;
//
//        public CharWrapper(char[] chars, int start, int len) {
//            mChars = chars;
//            mStart = start;
//            mLength = len;
//        }
//
//        /* package */ void set(char[] chars, int start, int len) {
//            mChars = chars;
//            mStart = start;
//            mLength = len;
//        }
//
//        @Override
//        public int length() {
//            return mLength;
//        }
//
//        @Override
//        public char charAt(int off) {
//            return mChars[off + mStart];
//        }
//
//        @NonNull
//        @Override
//        public CharSequence subSequence(int start, int end) {
//            if (start < 0 || end < 0 || start > mLength || end > mLength) {
//                throw new IndexOutOfBoundsException(start + ", " + end);
//            }
//
//            return new String(mChars, start + mStart, end - start);
//        }
//
//        @Override
//        public void getChars(int start, int end, char[] buf, int off) {
//            if (start < 0 || end < 0 || start > mLength || end > mLength) {
//                throw new IndexOutOfBoundsException(start + ", " + end);
//            }
//
//            System.arraycopy(mChars, start + mStart, buf, off, end - start);
//        }
//
//        public void drawText(Canvas c, int start, int end, float x, float y, Paint p) {
//            c.drawText(mChars, start + mStart, end - start, x, y, p);
//        }
//
//        public float measureText(int start, int end, Paint p) {
//            return p.measureText(mChars, start + mStart, end - start);
//        }
//
//        public int getTextWidths(int start, int end , float[] widths, Paint p) {
//            return p.getTextWidths(mChars, start + mStart, end - start, widths);
//        }
//
//        @Override
//        public String toString() {
//            return new String(mChars, mStart, mLength);
//        }
//    }
//
//    /**
//     * Sets the list of input filters on the specified Editable,
//     */
//    public void setFilters(InputFilter[] filters) {
//        if (filters == null) {
//            throw new IllegalStateException();
//        }
//
//        mFilters = filters;
//        mText.setFilters(filters);
//    }
//
//    /**
//     * Returns the current list of input filters.
//     */
//    public InputFilter[] getFilters() {
//        return mFilters;
//    }
//
//    private int getVerticalOffset() {
//        return 0;
//    }
//
//    private int getBottomVerticalOffset() {
//        int voffset = 0;
//        int boxht = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
//        int textht = mLayout.getHeight();
//
//        if (textht < boxht) {
//            voffset = boxht - textht;
//        }
//
//        return voffset;
//    }
//
//    private void invalidateCursorPath() {
//        // TODO: implement invalidateCursorPath
//    }
//
//    private void invalidateCursor() {
//        int where = Selection.getSelectionEnd(mText);
//
//        invalidateCursor(where, where, where);
//    }
//
//    private void invalidateCursor(int a, int b, int c) {
//        if (mLayout == null) {
//            invalidate();
//        } else {
//            if (a >= 0 || b >=0 || c >= 0) {
//                int first = Math.min(Math.min(a, b), c);
//                int last = Math.max(Math.max(a, b), c);
//
//                int line = mLayout.getLineForVertical(first);
//                int top = mLayout.getLineTop(line);
//
//                if (line > 0) {
//                    top -= mLayout.getLineDescent(line - 1);
//                }
//
//                int line2;
//
//                if (first == last) {
//                    line2 = line;
//                } else {
//                    line2 = mLayout.getLineForOffset(last);
//                }
//
//                int bottom = mLayout.getLineTop(line2 + 1);
//                int voffset = getVerticalOffset();
//
//                int left = getPaddingLeft() + getScrollX();
//                invalidate(left, top + voffset + getPaddingTop(),
//                           left + getWidth() - getPaddingLeft() - getPaddingRight(),
//                           bottom + voffset + getPaddingTop());
//            }
//        }
//    }
//
//    private void registerForPreDraw() {
//        final ViewTreeObserver observer = getViewTreeObserver();
//        if (observer == null) {
//            return;
//        }
//
//        if (mPreDrawState == PREDRAW_NOT_REGISTERED) {
//            observer.addOnPreDrawListener(this);
//        } else if (mPreDrawState == PREDRAW_DONE) {
//            mPreDrawState = PREDRAW_PENDING;
//        }
//
//        // else state is PREDRAW_PENDING, so keep waiting.
//    }
//
//    @Override
//    public boolean onPreDraw() {
//        if (mPreDrawState != PREDRAW_PENDING) {
//            return true;
//        }
//
//        if (mLayout == null) {
//            assumeLayout();
//        }
//
//        boolean changed = false;
//
//        if (mMovement != null) {
//            int curs = Selection.getSelectionEnd(mText);
//
//            if (curs >= 0) {
//                changed = bringPointIntoView(curs);
//            }
//        } else {
//            changed = bringTextIntoView();
//        }
//
//        mPreDrawState = PREDRAW_DONE;
//        return !changed;
//    }
//
//    @Override
//    protected void onDetachedFromWindow() {
//        super.onDetachedFromWindow();
//
//        if (mPreDrawState != PREDRAW_NOT_REGISTERED) {
//            final ViewTreeObserver observer = getViewTreeObserver();
//            if (observer != null) {
//                observer.removeOnPreDrawListener(this);
//                mPreDrawState = PREDRAW_NOT_REGISTERED;
//            }
//        }
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        // Draw the background for this view
//        super.onDraw(canvas);
//
//        final int paddingLeft = getPaddingLeft();
//        final int paddingTop = getPaddingTop();
//        final int paddingRight = getPaddingRight();
//        final int paddingBottom = getPaddingBottom();
//        final int scrollX = getScrollX();
//        final int scrollY = getScrollY();
//        final int right = getRight();
//        final int left = getLeft();
//        final int bottom = getBottom();
//        final int top = getTop();
//
//        if (mPreDrawState == PREDRAW_DONE) {
//            final ViewTreeObserver observer = getViewTreeObserver();
//            if (observer != null) {
//                observer.removeOnPreDrawListener(this);
//                mPreDrawState = PREDRAW_NOT_REGISTERED;
//            }
//        }
//
//        if (mLayout == null) {
//            assumeLayout();
//        }
//
//        Layout layout = mLayout;
//        int cursorcolor = mCurTextColor;
//        mTextPaint.drawableState = getDrawableState();
//
//        canvas.save();
//
//
//        float clipLeft = paddingLeft + scrollX;
//        float clipTop = paddingTop + scrollY;
//        float clipRight = right - left - paddingRight + scrollX;
//        float clipBottom = bottom - top - paddingBottom + scrollY;
//
//        canvas.clipRect(clipLeft, clipTop, clipRight, clipBottom);
//
//        int voffsetCursor = 0;
//
//        // translate in by our padding
//        canvas.translate(paddingLeft, paddingTop);
//
//        Path highlight = null;
//        int selStart = -1, selEnd = -1;
//
//        //  If there is no movement method, then there can be no selection.
//        //  Check that first and attempt to skip everything having to do with
//        //  the cursor.
//        //  XXX This is not strictly true -- a program could set the
//        //  selection manually if it really wanted to.
//        if (mMovement != null && (isFocused() || isPressed())) {
//            selStart = Selection.getSelectionStart(mText);
//            selEnd = Selection.getSelectionEnd(mText);
//
//            if (mCursorVisible && selStart >= 0) {
//                if (mHighlightPath == null) {
//                    mHighlightPath = new Path();
//                }
//
//                if (selStart == selEnd) {
//                    if ((SystemClock.uptimeMillis() - mShowCursor) % (2 * BLINK) < BLINK) {
//                        if (mHighlightPathBogus) {
//                            mHighlightPath.reset();
//                            mLayout.getCursorPath(selStart, mHighlightPath, mText);
//                            mHighlightPathBogus = false;
//                        }
//
//                        // XXX should pass to skin instead of drawing directly
//
//                        highlight = mHighlightPath;
//                    }
//                } else {
//                    if (mhighlightPathBogus) {
//                        mHighlightPath.reset();
//                        mLayout.getSelectionPath(selStart, selEnd, mHighlightPath);
//                        mHighlightPathBogus = false;
//                    }
//
//                    // XXX should pass to skin instead of drawing directly
//                    mHighlightPaint.setColor(mHighlightColor);
//                    mHighlightPaint.setStyle(Paint.Style.FILL);
//
//                    highlight = mHighlightPath;
//                }
//            }
//        }
//
//        final InputMethodState ims = mInputMethodState;
//        if (ims != null && ims.mBatchEditNesting == 0) {
//            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//            if (imm != null) {
//                if (imm.isActive(this) && highlight != null) {
//                    int candStart = EditorInputConnection.getComposingSpanStart(mText);
//                    int candEnd = EditorInputConnection.getComposingSpanEnd(mText);
//                    imm.updateSelection(this, selStart, selEnd, candStart, candEnd);
//                }
//
//                if (mInputConnection.requestCursorUpdates(0) && highlight != null) {
//                    highlight.computeBounds(ims.mTmpRectF, true);
//                    ims.mTmpOffset[0] = ims.mTmpOffset[1] = 0;
//
//                    canvas.getMatrix().mapPoints(ims.mTmpOffset);
//                    ims.mTmpRectF.offset(ims.mTmpOffset[0], ims.mTmpOffset[1]);
//
//                    ims.mTmpRectF.offset(0, voffsetCursor);
//
//                    ims.mCursorRectInWindow.set((int)(ims.mTmpRectF.left + 0.5),
//                            (int)(ims.mTmpRectF.top + 0.5),
//                            (int)(ims.mTmpRectF.right + 0.5),
//                            (int)(ims.mTmpRectF.bottom + 0.5));
//
//
//                    imm.updateCursor(this,
//                            ims.mCursorRectInWindow.left, ims.mCursorRectInWindow.top,
//                            ims.mCursorRectInWindow.right, ims.mCursorRectInWindow.bottom);
//                }
//            }
//        }
//        layout.draw(canvas, highlight, mHighlightPaint, voffsetCursor);
//
//        canvas.restore();
//    }
//
//    @Override
//    public void getFocusedRect(Rect r) {
//        if (mLayout == null) {
//            super.getFocusedRect(r);
//            return;
//        }
//
//        int sel = getSelectionEnd();
//        if (sel < 0) {
//            super.getFocusedRect(r);
//            return;
//        }
//
//        int line = mLayout.getLineForOffset(sel);
//        r.top = mLayout.getLineTop(line);
//        r.bottom = mLayout.getLineBottom(line);
//        r.left = (int) mLayout.getPrimaryHorizontal(sel);
//        r.right = r.left + 1;
//
//        r.offset(getPaddingLeft(), getPaddingTop());
//    }
//
//    public int getLintCount() {
//        return mLayout != null ? mLayout.getLineCount() : 0;
//    }
//
//    @Override
//    public int getBaseline() {
//        if (mLayout == null) {
//            return super.getBaseline();
//        }
//
//        return getPaddingTop() + mLayout.getLineBaseline(0);
//    }
//
//    @Override
//    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
//        outAttrs.inputType = InputType.TYPE_CLASS_TEXT
//                | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
//        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
//                | EditorInfo.IME_ACTION_NONE
//                | EditorInfo.IME_FLAG_NO_ENTER_ACTION;
//        outAttrs.initialSelStart = Selection.getSelectionStart(mText);
//        outAttrs.initialSelEnd = Selection.getSelectionEnd(mText);
//        return mInputConnection;
//    }
//
//    @Override
//    public boolean onCheckIsTextEditor() {
//        return true;
//    }
//
//
//    /**
//     * Called by the framework in response to a text completion from
//     * the current input method, provided by it calling
//     * {@link InputConnection#commitCompletion
//     * InputConnection.commitCompletion()}.  The default implementation does
//     * nothing; text views that are supporting auto-completion should override
//     * this to do their desired behavior.
//     *
//     * @param text The auto complete text the user has selected.
//     */
//    public void onCommitCompletion(CompletionInfo text) {
//    }
//
//    public void beginBatchEdit() {
//        final InputMethodState ims = mInputMethodState;
//        if (ims != null) {
//            int nesting = ++ims.mBatchEditNesting;
//            if (nesting == 1) {
//                ims.mCursorChanged = false;
//                ims.mChangedDelta = 0;
//                if (ims.mContentChanged) {
//                    // We already have a pending change from somewhere else,
//                    // so turn this into a full update.
//                    ims.mChangedStart = 0;
//                    ims.mChangedEnd = mText.length();
//                } else {
//                    ims.mContentChanged = false;
//                }
//                onBeginBatchEdit();
//            }
//        }
//    }
//
//    public void endBatchEdit() {
//        final InputMethodState ims = mInputMethodState;
//        if (ims != null) {
//            int nesting = --ims.mBatchEditNesting;
//            if (nesting == 0) {
//                finishBatchEdit(ims);
//            }
//        }
//    }
//
//    void ensureEndedBatchEdit() {
//        final InputMethodState ims = mInputMethodState;
//        if (ims != null && ims.mBatchEditNesting != 0) {
//            ims.mBatchEditNesting = 0;
//            finishBatchEdit(ims);
//        }
//    }
//
//    void finishBatchEdit(final InputMethodState ims) {
//        onEndBatchEdit();
//
//        if (ims.mContentChanged || ims.mSelectionModeChanged) {
//            updateAfterEdit();
//        } else if (ims.mCursorChanged) {
//            // Cheezy way to get us to report the current cursor location.
//            invalidateCursor();
//        }
//    }
//
//    void updateAfterEdit() {
//        invalidate();
//        int curs = Selection.getSelectionStart(mText);
//
//        if (curs >= 0) {
//            mHighlightPathBogus = true;
//
//            if (isFocused()) {
//                mShowCursor = SystemClock.uptimeMillis();
//                makeBlink();
//            }
//        }
//
//        checkForResize();
//    }
//
//    /**
//     * Called by the framework in response to a request to begin a batch
//     * of edit operations through a call to link {@link #beginBatchEdit()}.
//     */
//    public void onBeginBatchEdit() {
//    }
//
//    /**
//     * Called by the framework in response to a request to end a batch
//     * of edit operations through a call to link {@link #endBatchEdit}.
//     */
//    public void onEndBatchEdit() {
//    }
//
//    private void nullLayout() {
//        mLayout = null;
//    }
//
//    /**
//     * Make a new Layout based on the already-measured size of the view,
//     * on the assumption that it was measured correctly at some point.
//     */
//    private void assumeLayout() {
//        int width = getRight() - getLeft() - getPaddingLeft() - getPaddingRight();
//
//        if (width < 1) {
//            width = 0;
//        }
//
//        int physicalWidth = width;
//
//        if (mHorizontallyScrolling) {
//            width = VERY_WIDE;
//        }
//
//        makeNewLayout(width, false);
//    }
//
//    private void makeNewLayout(int w, boolean bringIntoView) {
//        mHighlightPathBogus = true;
//
//        mLayout = new DynamicLayout(mText, mTextPaint, Math.max(0, w), Layout.Alignment.ALIGN_NORMAL,
//                mSpacingMult, mSpacingAdd, false);
//
//        if (bringIntoView) {
//            registerForPreDraw();
//        }
//    }
//
//    private static int desired(Layout layout) {
//        int n = layout.getLineCount();
//        CharSequence text = layout.getText();
//        float max = 0;
//
//        for (int i = 0; i < n - 1; i++) {
//            if (text.charAt(layout.getLineEnd(i) - 1) != '\n') {
//                return -1;
//            }
//        }
//
//        for (int i = 0; i < n; i++) {
//            max = Math.max(max, layout.getLineWidth(i));
//        }
//
//        return (int) Math.ceil(max);
//    }
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
//        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
//        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
//        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
//
//        int width = getWidth();
//        int height;
//
//        int des = -1;
//        boolean fromexisting = false;
//
//        if (widthMode == MeasureSpec.EXACTLY) {
//            width = widthSize;
//        } else {
//            int want = width - getPaddingLeft() - getPaddingRight();
//
//            if (mHorizontallyScrolling) {
//                want = VERY_WIDE;
//            }
//
//            if (mLayout == null || mLayout.getWidth() != want) {
//                makeNewLayout(want, false);
//            }
//
//            if (heightMode == MeasureSpec.EXACTLY) {
//                height = heightSize;
//            } else {
//                int desired = getDesiredHeight();
//
//                height = desired;
//
//                if (heightMode == MeasureSpec.AT_MOST) {
//                    height = Math.min(desired, heightSize);
//                }
//            }
//        }
//        if (mMovement != null) {
//            registerForPreDraw();
//        }
//    }
//
//    private int getDesiredHeight() {
//        if (mLayout == null) return 0;
//
//        int lineCount = mLayout.getLineCount();
//        int padding = getPaddingTop() + getPaddingBottom();
//        int desired = mLayout.getLineTop(lineCount);
//
//        return desired + padding;
//    }
//
//    /**
//     * Check whether a change to the existing text layout requires a
//     * new view layout.
//     */
//    private void checkForResize() {
//        boolean sizeChanged = false;
//
//        if (mLayout != null) {
//            if (mLayoutParams.width == LayoutParams.WRAP_CONTENT) {
//                sizeChanged = true;
//                invalidate();
//            }
//
//            if (mLayoutParams.height == LayoutParams.WRAP_CONTENT) {
//                int desiredHeight = getDesiredHeight();
//
//                if (desiredHeight != getHeight()) {
//                    sizeChanged = true;
//                }
//            }
//        }
//
//        if (sizeChanged) {
//            requestLayout();
//        }
//    }
//
//    /**
//     * Check whether entirely new text requires a new view layout
//     * or merely a new text layout.
//     */
//    private void checkForRelayout() {
//        // If we have a fixed width, we can just swap in a new text layout
//        // if the text height stays the same or if the view height is fixed.
//
//        if ((mLayoutParams.width != LayoutParams.WRAP_CONTENT ||
//                (getRight() - getLeft() - getPaddingLeft() - getPaddingRight() > 0))) {
//            // Static width, so try making a new text layout.
//
//            int oldht = mLayout.getHeight();
//            int want = mLayout.getWidth();
//
//            /*
//             * No need to bring the text into view, since the size is not
//             * changing (unless we do the requestLayout(), in which case it
//             * will happen at measure).
//             */
//            makeNewLayout(want,false);
//
//            // We lose: the height has changed and we have a dynamic height.
//            // Request a new view layout using our new text layout.
//            requestLayout();
//            invalidate();
//        } else {
//            // Dynamic width, so we have no choice but to request a new
//            // view layout with a new text layout.
//
//            nullLayout();
//            requestLayout();
//            invalidate();
//        }
//    }
//
//    /**
//     * Returns true if anything changed.
//     */
//    private boolean bringTextIntoView() {
//        int line = 0;
//
//        Layout.Alignment a = mLayout.getParagraphAlignment(line);
//        int dir = mLayout.getParagraphDirection(line);
//        int hspace = getRight() - getLeft() - getPaddingLeft() - getPaddingRight();
//
//        int scrollx, scrolly = 0;
//
//        if (dir < 0) {
//            int right = (int) Math.ceil(mLayout.getLineRight(line));
//            scrollx = right - hspace;
//        } else {
//            scrollx = (int) Math.floor(mLayout.getLineLeft(line));
//        }
//
//        if (scrollx != getScrollX() || scrolly != getScrollY()) {
//            scrollTo(scrollx, scrolly);
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    /**
//     * Move the point, specified by the offset, into the view if it is needed.
//     * This has to be called after layout. Returns true if anything changed.
//     */
//    public boolean bringPointIntoView(int offset) {
//        boolean changed = false;
//
//        int line = mLayout.getLineForOffset(offset);
//
//        // FIXME: Is it okay to truncate this, or should we round?
//        final int x = (int)mLayout.getPrimaryHorizontal(offset);
//        final int top = mLayout.getLineTop(line);
//        final int bottom = mLayout.getLineTop(line+1);
//
//        int left = (int) Math.floor(mLayout.getLineLeft(line));
//        int right = (int) Math.ceil(mLayout.getLineRight(line));
//        int ht = mLayout.getHeight();
//
//        int grav;
//
//        switch (mLayout.getParagraphAlignment(line)) {
//            case ALIGN_NORMAL:
//                grav = 1;
//                break;
//
//            case ALIGN_OPPOSITE:
//                grav = -1;
//                break;
//
//            default:
//                grav = 0;
//        }
//
//        grav *= mLayout.getParagraphDirection(line);
//
//        int hspace = getRight() - getLeft() - getPaddingLeft() - getPaddingRight();
//        int vspace = getBottom() - getTop() - getPaddingBottom() - getPaddingTop();
//
//        int hslack = (bottom - top) / 2;
//        int vslack = hslack;
//
//        if (vslack > vspace / 4)
//            vslack = vspace / 4;
//        if (hslack > hspace / 4)
//            hslack = hspace / 4;
//
//        int hs = getScrollX();
//        int vs = getScrollY();
//
//        if (top - vs < vslack)
//            vs = top - vslack;
//        if (bottom - vs > vspace - vslack)
//            vs = bottom - (vspace - vslack);
//        if (ht - vs < vspace)
//            vs = ht - vspace;
//        if (-vs > 0)
//            vs = 0;
//
//        if (grav != 0) {
//            if (x - hs < hslack) {
//                hs = x - hslack;
//            }
//            if (x - hs > hspace - hslack) {
//                hs = x - (hspace - hslack);
//            }
//        }
//
//        if (grav < 0) {
//            if (left - hs > 0)
//                hs = left;
//            if (right - hs < hspace)
//                hs = right - hspace;
//        } else if (grav > 0) {
//            if (right - hs < hspace)
//                hs = right - hspace;
//            if (left - hs > 0)
//                hs = left;
//        } else /* grav == 0 */ {
//            if (right - left <= hspace) {
//                /*
//                 * If the entire text fits, center it exactly.
//                 */
//                hs = left - (hspace - (right - left)) / 2;
//            } else if (x > right - hslack) {
//                /*
//                 * If we are near the right edge, keep the right edge
//                 * at the edge of the view.
//                 */
//                hs = right - hspace;
//            } else if (x < left + hslack) {
//                /*
//                 * If we are near the left edge, keep the left edge
//                 * at the edge of the view.
//                 */
//                hs = left;
//            } else if (left > hs) {
//                /*
//                 * Is there whitespace visible at the left?  Fix it if so.
//                 */
//                hs = left;
//            } else if (right < hs + hspace) {
//                /*
//                 * Is there whitespace visible at the right?  Fix it if so.
//                 */
//                hs = right - hspace;
//            } else {
//                /*
//                 * Otherwise, float as needed.
//                 */
//                if (x - hs < hslack) {
//                    hs = x - hslack;
//                }
//                if (x - hs > hspace - hslack) {
//                    hs = x - (hspace - hslack);
//                }
//            }
//        }
//
//        if (hs != getScrollX() || vs != getScrollY()) {
//            if (mScroller == null) {
//                scrollTo(hs, vs);
//            } else {
//                long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
//                int dx = hs - getScrollX();
//                int dy = vs - getScrollY();
//
//                if (duration > ANIMATED_SCROLL_GAP) {
//                    mScroller.startScroll(mScrollX, mScrollY, dx, dy);
//                    awakenScrollBars(mScroller.getDuration());
//                    invalidate();
//                } else {
//                    if (!mScroller.isFinished()) {
//                        mScroller.abortAnimation();
//                    }
//
//                    scrollBy(dx, dy);
//                }
//
//                mLastScroll = AnimationUtils.currentAnimationTimeMillis();
//            }
//
//            changed = true;
//        }
//
//        if (isFocused()) {
//            // This offsets because getInterestingRect() is in terms of
//            // viewport coordinates, but requestRectangleOnScreen()
//            // is in terms of content coordinates.
//
//            Rect r = new Rect();
//            getInterestingRect(r, x, top, bottom, line);
//            r.offset(getScrollX(), getScrollY());
//
//            if (requestRectangleOnScreen(r)) {
//                changed = true;
//            }
//        }
//
//        return changed;
//    }
//
//    /**
//     * Move the cursor, if needed, so that it is at an offset that is visible
//     * to the user.  This will not move the cursor if it represents more than
//     * one character (a selection range).  This will only work if the
//     * TextView contains spannable text; otherwise it will do nothing.
//     */
//    public boolean moveCursorToVisibleOffset() {
//        int start = Selection.getSelectionStart(mText);
//        int end = Selection.getSelectionEnd(mText);
//        if (start != end) {
//            return false;
//        }
//
//        // First: make sure the line is visible on screen:
//
//        int line = mLayout.getLineForOffset(start);
//
//        final int top = mLayout.getLineTop(line);
//        final int bottom = mLayout.getLineTop(line+1);
//        final int vspace = getBottom() - getTop() - getPaddingTop() - getPaddingBottom();
//        int vslack = (bottom - top) / 2;
//        if (vslack > vspace / 4)
//            vslack = vspace / 4;
//        final int vs = getScrollY();
//
//        if (top < (vs+vslack)) {
//            line = mLayout.getLineForVertical(vs+vslack+(bottom-top));
//        } else if (bottom > (vspace+vs-vslack)) {
//            line = mLayout.getLineForVertical(vspace+vs-vslack-(bottom-top));
//        }
//
//        // Next: make sure the character is visible on screen:
//
//        final int hspace = getRight() - getLeft() - getPaddingLeft() - getPaddingRight();
//        final int hs = getScrollX();
//        final int leftChar = mLayout.getOffsetForHorizontal(line, hs);
//        final int rightChar = mLayout.getOffsetForHorizontal(line, hspace+hs);
//
//        int newStart = start;
//        if (newStart < leftChar) {
//            newStart = leftChar;
//        } else if (newStart > rightChar) {
//            newStart = rightChar;
//        }
//
//        if (newStart != start) {
//            Selection.setSelection(mText, newStart);
//            return true;
//        }
//
//        return false;
//    }
//
//    @Override
//    public void computeScroll() {
//        if (mScroller != null) {
//            if (mScroller.computeScrollOffset()) {
//                mScrollX = mScroller.getCurrX();
//                mScrollY = mScroller.getCurrY();
//                postInvalidate();  // So we draw again
//            }
//        }
//    }
//
//    private void getInterestingRect(Rect r, int h, int top, int bottom,
//                                    int line) {
//        int paddingTop = getPaddingTop();
//        top += paddingTop;
//        bottom += paddingTop;
//        h += getPaddingLeft();
//
//        if (line == 0)
//            top -= getPaddingTop();
//        if (line == mLayout.getLineCount() - 1)
//            bottom += getPaddingBottom();
//
//        r.set(h, top, h+1, bottom);
//        r.offset(-getScrollX(), -getScrollY());
//    }
//
//    public int getSelectionStart() {
//        return Selection.getSelectionStart(getText());
//    }
//
//    public int getSelectionEnd() {
//        return Selection.getSelectionEnd(getText());
//    }
//
//    public boolean hasSelection() {
//        return getSelectionStart() != getSelectionEnd();
//    }
//
//    /**
//     * This method is called when the text is changed, in case any
//     * subclasses would like to know.
//     *
//     * @param text The text the TextView is displaying.
//     * @param start The offset of the start of the range of the text
//     *              that was modified.
//     * @param before The offset of the former end of the range of the
//     *               text that was modified.  If text was simply inserted,
//     *               this will be the same as <code>start</code>.
//     *               If text was replaced with new text or deleted, the
//     *               length of the old text was <code>before-start</code>.
//     * @param after The offset of the end of the range of the text
//     *              that was modified.  If text was simply deleted,
//     *              this will be the same as <code>start</code>.
//     *              If text was replaced with new text or inserted,
//     *              the length of the new text is <code>after-start</code>.
//     */
//    protected void onTextChanged(CharSequence text,
//                                 int start, int before, int after) {
//    }
//
//    /**
//     * This method is called when the selection has changed, in case any
//     * subclasses would like to know.
//     *
//     * @param selStart The new selection start location.
//     * @param selEnd The new selection end location.
//     */
//    protected void onSelectionChanged(int selStart, int selEnd) {
//    }
//
//    /**
//     * Adds a TextWatcher to the list of those whose methods are called
//     * whenever this TextView's text changes.
//     * <p>
//     * In 1.0, the {@link TextWatcher#afterTextChanged} method was erroneously
//     * not called after {@link #setText} calls.  Now, doing {@link #setText}
//     * if there are any text changed listeners forces the buffer type to
//     * Editable if it would not otherwise be and does call this method.
//     */
//    public void addTextChangedListener(TextWatcher watcher) {
//        if (mListeners == null) {
//            mListeners = new ArrayList<TextWatcher>();
//        }
//
//        mListeners.add(watcher);
//    }
//
//    /**
//     * Removes the specified TextWatcher from the list of those whose
//     * methods are called
//     * whenever this TextView's text changes.
//     */
//    public void removeTextChangedListener(TextWatcher watcher) {
//        if (mListeners != null) {
//            int i = mListeners.indexOf(watcher);
//
//            if (i >= 0) {
//                mListeners.remove(i);
//            }
//        }
//    }
//
//    private void sendBeforeTextChanged(CharSequence text, int start, int before,
//                                       int after) {
//        if (mListeners != null) {
//            final ArrayList<TextWatcher> list = mListeners;
//            final int count = list.size();
//            for (int i = 0; i < count; i++) {
//                list.get(i).beforeTextChanged(text, start, before, after);
//            }
//        }
//    }
//    /**
//     * Not private so it can be called from an inner class without going
//     * through a thunk.
//     */
//    void sendOnTextChanged(CharSequence text, int start, int before,
//                           int after) {
//        if (mListeners != null) {
//            final ArrayList<TextWatcher> list = mListeners;
//            final int count = list.size();
//            for (int i = 0; i < count; i++) {
//                list.get(i).onTextChanged(text, start, before, after);
//            }
//        }
//    }
//
//    /**
//     * Not private so it can be called from an inner class without going
//     * through a thunk.
//     */
//    void sendAfterTextChanged(Editable text) {
//        if (mListeners != null) {
//            final ArrayList<TextWatcher> list = mListeners;
//            final int count = list.size();
//            for (int i = 0; i < count; i++) {
//                list.get(i).afterTextChanged(text);
//            }
//        }
//    }
//
//    /**
//     * Not private so it can be called from an inner class without going
//     * through a thunk.
//     */
//    void handleTextChanged(CharSequence buffer, int start,
//                           int before, int after) {
//        final InputMethodState ims = mInputMethodState;
//        if (ims == null || ims.mBatchEditNesting == 0) {
//            updateAfterEdit();
//        }
//        if (ims != null) {
//            ims.mContentChanged = true;
//            if (ims.mChangedStart < 0) {
//                ims.mChangedStart = start;
//                ims.mChangedEnd = start+before;
//            } else {
//                if (ims.mChangedStart > start) ims.mChangedStart = start;
//                if (ims.mChangedEnd < (start+before)) ims.mChangedEnd = start+before;
//            }
//            ims.mChangedDelta += after-before;
//        }
//
//        sendOnTextChanged(buffer, start, before, after);
//        onTextChanged(buffer, start, before, after);
//    }
//
//    /**
//     * Not private so it can be called from an inner class without going
//     * through a thunk.
//     */
//    void spanChange(Spanned buf, Object what, int oldStart, int newStart,
//                    int oldEnd, int newEnd) {
//        // XXX Make the start and end move together if this ends up
//        // spending too much time invalidating.
//
//        boolean selChanged = false;
//        int newSelStart=-1, newSelEnd=-1;
//
//        final InputMethodState ims = mInputMethodState;
//
//        if (what == Selection.SELECTION_END) {
//            mHighlightPathBogus = true;
//            selChanged = true;
//            newSelEnd = newStart;
//
//            if (!isFocused()) {
//                mSelectionMoved = true;
//            }
//
//            if (oldStart >= 0 || newStart >= 0) {
//                invalidateCursor(Selection.getSelectionStart(buf), oldStart, newStart);
//                registerForPreDraw();
//
//                if (isFocused()) {
//                    mShowCursor = SystemClock.uptimeMillis();
//                    makeBlink();
//                }
//            }
//        }
//
//        if (what == Selection.SELECTION_START) {
//            mHighlightPathBogus = true;
//            selChanged = true;
//            newSelStart = newStart;
//
//            if (!isFocused()) {
//                mSelectionMoved = true;
//            }
//
//            if (oldStart >= 0 || newStart >= 0) {
//                int end = Selection.getSelectionEnd(buf);
//                invalidateCursor(end, oldStart, newStart);
//            }
//        }
//
//        if (selChanged) {
//            if ((buf.getSpanFlags(what)&Spanned.SPAN_INTERMEDIATE) == 0) {
//                if (newSelStart < 0) {
//                    newSelStart = Selection.getSelectionStart(buf);
//                }
//                if (newSelEnd < 0) {
//                    newSelEnd = Selection.getSelectionEnd(buf);
//                }
//                onSelectionChanged(newSelStart, newSelEnd);
//            }
//        }
//
//        if (MetaKeyKeyListener.isMetaTracker(buf, what)) {
//            mHighlightPathBogus = true;
//            if (ims != null && MetaKeyKeyListener.isSelectingMetaTracker(buf, what)) {
//                ims.mSelectionModeChanged = true;
//            }
//
//            if (Selection.getSelectionStart(buf) >= 0) {
//                if (ims == null || ims.mBatchEditNesting == 0) {
//                    invalidateCursor();
//                } else {
//                    ims.mCursorChanged = true;
//                }
//            }
//        }
//
//        if (what instanceof ParcelableSpan) {
//            ims.mContentChanged = true;
//        }
//    }
//
//    private class ChangeWatcher
//            implements TextWatcher, SpanWatcher {
//
//        private CharSequence mBeforeText;
//
//        public void beforeTextChanged(CharSequence buffer, int start,
//                                      int before, int after) {
//            TextEditor.this.sendBeforeTextChanged(buffer, start, before, after);
//        }
//
//        public void onTextChanged(CharSequence buffer, int start,
//                                  int before, int after) {
//                mBeforeText = null;
//        }
//
//        public void afterTextChanged(Editable buffer) {
//        }
//
//        public void onSpanChanged(Spannable buf,
//                                  Object what, int s, int e, int st, int en) {
//            TextEditor.this.spanChange(buf, what, s, st, e, en);
//        }
//
//        public void onSpanAdded(Spannable buf, Object what, int s, int e) {
//            TextEditor.this.spanChange(buf, what, -1, s, -1, e);
//        }
//
//        public void onSpanRemoved(Spannable buf, Object what, int s, int e) {
//            TextEditor.this.spanChange(buf, what, s, -1, e, -1);
//        }
//    }
//
//    private void makeBlink() {
//        if (!mCursorVisible) {
//            if (mBlink != null) {
//                mBlink.removeCallbacks(mBlink);
//            }
//
//            return;
//        }
//
//        if (mBlink == null)
//            mBlink = new Blink(this);
//
//        mBlink.removeCallbacks(mBlink);
//        mBlink.postAtTime(mBlink, mShowCursor + BLINK);
//    }
//
//    /**
//     * Use {@link BaseInputConnection#removeComposingSpans
//     * BaseInputConnection.removeComposingSpans()} to remove any IME composing
//     * state from this text view.
//     */
//    public void clearComposingText() {
//        BaseInputConnection.removeComposingSpans(mText);
//    }
//
//    class CommitSelectionReceiver extends ResultReceiver {
//        int mNewStart;
//        int mNewEnd;
//
//        CommitSelectionReceiver() {
//            super(getHandler());
//        }
//
//        protected void onReceiveResult(int resultCode, Bundle resultData) {
//            if (resultCode != InputMethodManager.RESULT_SHOWN) {
//                final int len = mText.length();
//                if (mNewStart > len) {
//                    mNewStart = len;
//                }
//                if (mNewEnd > len) {
//                    mNewEnd = len;
//                }
//                Selection.setSelection((Spannable)mText, mNewStart, mNewEnd);
//            }
//        }
//    }
//
//    private static class Blink extends Handler implements Runnable {
//        private final WeakReference<TextEditor> mView;
//        private boolean mCancelled;
//
//        public Blink(TextEditor v) {
//            mView = new WeakReference<TextEditor>(v);
//        }
//
//        public void run() {
//            if (mCancelled) {
//                return;
//            }
//
//            removeCallbacks(Blink.this);
//
//            TextEditor tv = mView.get();
//
//            if (tv != null && tv.isFocused()) {
//                int st = Selection.getSelectionStart(tv.mText);
//                int en = Selection.getSelectionEnd(tv.mText);
//
//                if (st == en && st >= 0) {
//                    if (tv.mLayout != null) {
//                        tv.invalidateCursorPath();
//                    }
//
//                    postAtTime(this, SystemClock.uptimeMillis() + BLINK);
//                }
//            }
//        }
//
//        void cancel() {
//            if (!mCancelled) {
//                removeCallbacks(Blink.this);
//                mCancelled = true;
//            }
//        }
//
//        void uncancel() {
//            mCancelled = false;
//        }
//    }
//
//    @Override
//    protected int computeHorizontalScrollRange() {
//        if (mLayout != null)
//            return mLayout.getWidth();
//
//        return super.computeHorizontalScrollRange();
//    }
//
//    @Override
//    protected int computeVerticalScrollRange() {
//        if (mLayout != null)
//            return mLayout.getHeight();
//
//        return super.computeVerticalScrollRange();
//    }
//
//    @Override
//    protected int computeVerticalScrollExtent() {
//        return getHeight() - getPaddingTop() - getPaddingBottom();
//    }
//
//    @Override
//    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
//        switch (keyCode) {
//            case KeyEvent.KEYCODE_A:
//                if (canSelectAll()) {
//                    return onTextContextMenuItem(ID_SELECT_ALL);
//                }
//
//                break;
//
//            case KeyEvent.KEYCODE_X:
//                if (canCut()) {
//                    return onTextContextMenuItem(ID_CUT);
//                }
//
//                break;
//
//            case KeyEvent.KEYCODE_C:
//                if (canCopy()) {
//                    return onTextContextMenuItem(ID_COPY);
//                }
//
//                break;
//
//            case KeyEvent.KEYCODE_V:
//                if (canPaste()) {
//                    return onTextContextMenuItem(ID_PASTE);
//                }
//
//                break;
//        }
//
//        return super.onKeyShortcut(keyCode, event);
//    }
//
//    private boolean canSelectAll() {
//        if (mText != null && mText.length() != 0 &&
//                mMovement != null && mMovement.canSelectArbitrarily()) {
//            return true;
//        }
//
//        return false;
//    }
//
//    private boolean canSelectText() {
//        if (mText != null && mText.length() != 0 &&
//                mMovement != null && mMovement.canSelectArbitrarily()) {
//            return true;
//        }
//
//        return false;
//    }
//
//    private boolean canCut() {
//        if (mText.length() > 0 && getSelectionStart() >= 0) {
//            return mText != null;
//        }
//
//        return false;
//    }
//
//    private boolean canCopy() {
//        if (mText.length() > 0 && getSelectionStart() >= 0) {
//            return true;
//        }
//
//        return false;
//    }
//
//    private boolean canPaste() {
//        if (mText != null &&
//                getSelectionStart() >= 0 && getSelectionEnd() >= 0) {
//            ClipboardManager clip = (ClipboardManager)getContext()
//                    .getSystemService(Context.CLIPBOARD_SERVICE);
//            if (clip.hasPrimaryClip()) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    private static final int ID_SELECT_ALL = android.R.id.selectAll;
//    private static final int ID_START_SELECTING_TEXT = android.R.id.startSelectingText;
//    private static final int ID_STOP_SELECTING_TEXT = android.R.id.stopSelectingText;
//    private static final int ID_CUT = android.R.id.cut;
//    private static final int ID_COPY = android.R.id.copy;
//    private static final int ID_PASTE = android.R.id.paste;
//    private static final int ID_COPY_URL = android.R.id.copyUrl;
//    private static final int ID_SWITCH_INPUT_METHOD = android.R.id.switchInputMethod;
//    private static final int ID_ADD_TO_DICTIONARY = android.R.id.addToDictionary;
//
//    /**
//     * Called when a context menu option for the text view is selected.  Currently
//     * this will be one of: {@link android.R.id#selectAll},
//     * {@link android.R.id#startSelectingText}, {@link android.R.id#stopSelectingText},
//     * {@link android.R.id#cut}, {@link android.R.id#copy},
//     * {@link android.R.id#paste}, {@link android.R.id#copyUrl},
//     * or {@link android.R.id#switchInputMethod}.
//     */
//    public boolean onTextContextMenuItem(int id) {
//        int selStart = getSelectionStart();
//        int selEnd = getSelectionEnd();
//
//        if (!isFocused()) {
//            selStart = 0;
//            selEnd = mText.length();
//        }
//
//        int min = Math.min(selStart, selEnd);
//        int max = Math.max(selStart, selEnd);
//
//        if (min < 0) {
//            min = 0;
//        }
//        if (max < 0) {
//            max = 0;
//        }
//
//        ClipboardManager clip = (ClipboardManager)getContext()
//                .getSystemService(Context.CLIPBOARD_SERVICE);
//
////        switch (id) {
////            case ID_SELECT_ALL:
////                Selection.setSelection((Spannable) mText, 0,
////                        mText.length());
////                return true;
////
////            case ID_START_SELECTING_TEXT:
////                MetaKeyKeyListener.startSelecting(this, (Spannable) mText);
////                return true;
////
////            case ID_STOP_SELECTING_TEXT:
////                MetaKeyKeyListener.stopSelecting(this, (Spannable) mText);
////                Selection.setSelection((Spannable) mText, getSelectionEnd());
////                return true;
////
////            case ID_CUT:
////                MetaKeyKeyListener.stopSelecting(this, (Spannable) mText);
////
////                if (min == max) {
////                    min = 0;
////                    max = mText.length();
////                }
////
////                clip.setText(mTransformed.subSequence(min, max));
////                ((Editable) mText).delete(min, max);
////                return true;
////
////            case ID_COPY:
////                MetaKeyKeyListener.stopSelecting(this, (Spannable) mText);
////
////                if (min == max) {
////                    min = 0;
////                    max = mText.length();
////                }
////
////                clip.setText(mTransformed.subSequence(min, max));
////                return true;
////
////            case ID_PASTE:
////                MetaKeyKeyListener.stopSelecting(this, (Spannable) mText);
////
////                CharSequence paste = clip.getText();
////
////                if (paste != null) {
////                    Selection.setSelection((Spannable) mText, max);
////                    ((Editable) mText).replace(min, max, paste);
////                }
////
////                return true;
////        }
//
//        return false;
//    }
//
//
//    public Editable getText() {
//        return mText;
//    }
//
//
//
//
//
//
//
//
//
//
//
//    private MovementMethod mMovement;
//    private ChangeWatcher           mChangeWatcher;
//
//    private long                    mShowCursor;
//    private Blink                   mBlink;
//    private boolean                 mCursorVisible = true;
//
//    private boolean                 mSelectAllOnFocus = false;
//
//    private int                     mAutoLinkMask;
//    private boolean                 mLinksClickable = true;
//
//    private static final int        LINES = 1;
//    private static final int        EMS = LINES;
//    private static final int        PIXELS = 2;
//
//    // tmp primitives, so we don't alloc them on each draw
//    private Path                    mHighlightPath;
//    private boolean                 mHighlightPathBogus = true;
//    private static final RectF      sTempRect = new RectF();
//
//    // XXX should be much larger
//    private static final int        VERY_WIDE = 16384;
//
//    private static final int        BLINK = 500;
//
//    private static final int ANIMATED_SCROLL_GAP = 250;
//    private long mLastScroll;
//    private Scroller mScroller = null;
//
//
//}
