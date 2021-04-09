package io.ikws4.codeeditor.component;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.Touch;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.textclassifier.TextClassifier;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;

import io.ikws4.codeeditor.R;
import io.ikws4.codeeditor.api.configuration.ColorScheme;
import io.ikws4.codeeditor.api.editor.Editor;
import io.ikws4.codeeditor.api.editor.component.Component;
import io.ikws4.codeeditor.api.editor.listener.ScaleListener;
import io.ikws4.codeeditor.api.editor.listener.VisibleAreaListener;
import io.ikws4.codeeditor.api.language.ExtendedSpan;
import io.ikws4.codeeditor.api.language.Suggestion;
import io.ikws4.codeeditor.language.TSLanguageStyler;
import io.ikws4.codeeditor.span.ReplacedSpan;
import io.ikws4.codeeditor.span.TabSpan;
import io.ikws4.codeeditor.task.FormatTask;
import io.ikws4.codeeditor.task.ParsingSpanTask;
import io.ikws4.codeeditor.util.TextBuffer;

public class TextArea extends AppCompatMultiAutoCompleteTextView implements Component, VisibleAreaListener, ScaleListener {// load jsitter for syntax highlight and indent.
    private Editor mEditor;

    private int mScrollX = 0;
    private int mScrollY = 0;
    private int mWidth = 0;
    private int mHeight = 0;

    private OnSelectionChangedListener mSelectionChangedListener;

    private final Paint mCursorLinePaint = new Paint();

    private FormatTask mFormatTask;

    // Span
    private ParsingSpanTask mParsingSpanTask;
    private final List<ExtendedSpan> mSpans;
    private boolean isUpdatingSpan = false;
    private int mTopLineStart;
    private int mTopLineEnd;
    private int mBottomLineStart;
    private int mBottomLineEnd;

    // Completion menu
    private final SuggestionAdapter mSuggestionAdapter;
    private final WordTokenizer mWordTokenizer;
    private final int mCompletionMenuHorizontalPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

    public TextArea(@NonNull Context context) {
        this(context, null);
    }

    public TextArea(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.autoCompleteTextViewStyle);
    }

    public TextArea(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mSpans = new ArrayList<>();
        mSuggestionAdapter = new SuggestionAdapter(context);
        mWordTokenizer = new WordTokenizer();
    }

    @Override
    public void attach(Editor editor) {
        mEditor = editor;

        editor.getScacleModel().addScaleListener(this);
        editor.getScrollingModel().addVisibleAreaListener(this);

        configure();
        colorize();
    }

    @Override
    public int getComponentWidth() {
        return mWidth;
    }

    @Override
    public int getComponentHeight() {
        return mHeight;
    }

    @Override
    public void onVisibleAreaChanged(Rect rect, Rect oldRect) {
        if (rect.width() != mWidth || rect.height() != mHeight) {
            mWidth = rect.width();
            mHeight = rect.height();

            updateSpan();
        }

        if (rect.top != mScrollY || rect.right != mScrollX) {
            // notify EditText scroll changed.
            super.onScrollChanged(rect.left, rect.top, oldRect.left, oldRect.top);

            mScrollX = rect.left;
            mScrollY = rect.top;

            updateSpanWhenScroll();
        }
    }

    @Override
    public void onScaleChanged(float factor) {
        if (mEditor.getConfiguration().isPinchZoom()) {
            setTextSize(mEditor.getConfiguration().getFontSize() * factor);
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (mSelectionChangedListener != null) {
            mSelectionChangedListener.onSelectionChanged(selStart, selEnd);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        MovementMethod.getInstance().onKeyDown(this, getText(), keyCode, event);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        MovementMethod.getInstance().onKeyUp(this, getText(), keyCode, event);
        return super.onKeyUp(keyCode, event);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Configuration
    ///////////////////////////////////////////////////////////////////////////
    protected void configure() {
        // Text & font
        setTextSize(mEditor.getConfiguration().getFontSize());
        setTypeface(Typeface.MONOSPACE);
        setLineSpacing(0, 1.1f);
        setGravity(Gravity.TOP | Gravity.START);
        setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        setInputType(EditorInfo.TYPE_CLASS_TEXT
                | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
                | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        addTextChangedListener(new SyntaxTreeEditWatcher());
        addTextChangedListener(new HighlightUpdateWatcher());
        setIncludeFontPadding(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setTextClassifier(TextClassifier.NO_OP);
        }

        // Wrap
        setHorizontallyScrolling(!mEditor.getConfiguration().isWrap());

        // Clipboard panel & key handle
        setMovementMethod(MovementMethod.getInstance());
        setCustomInsertionActionModeCallback(new InsertionActionModeCallback());
        setCustomSelectionActionModeCallback(new SelectionActiomModeCallback());

        // Completion menu
        if (mEditor.getConfiguration().isCompletion()) {
            setThreshold(1);
            setAdapter(mSuggestionAdapter);
            setTokenizer(mWordTokenizer);
            mSuggestionAdapter.setData(mEditor.getLanguage().getSuggestionProvider().getAll());
        } else {
            setAdapter(null);
            setTokenizer(null);
        }
    }

    protected void colorize() {
        ColorScheme colorScheme = mEditor.getColorScheme();

        // TextEdit configure
        setTextColor(colorScheme.getTextColor());
        setBackgroundColor(colorScheme.getBackgroundColor());
        setHighlightColor(colorScheme.getSelectionColor());

        // Completion menu
        setDropDownBackgroundDrawable(new ColorDrawable(colorScheme.getCompletionMenuBackgroundColor()));
        mSuggestionAdapter.setColorScheme(colorScheme);

        mCursorLinePaint.setColor(colorScheme.getCursorLineColor());
    }

    ///////////////////////////////////////////////////////////////////////////
    // TextWatcher
    ///////////////////////////////////////////////////////////////////////////
    private class SyntaxTreeEditWatcher implements TextWatcher {
        private int startByte;
        private int oldEndByte;
        private int newEndByte;
        private int startRow;
        private int startColumn;
        private int oldEndRow;
        private int oldEndColumn;
        private int newEndRow;
        private int newEndColumn;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (hasLayout()) {
                startByte = start;
                oldEndByte = start + count;

                startRow = getLayout().getLineForOffset(startByte);
                startColumn = startByte - getLayout().getLineStart(startRow);

                oldEndRow = getLayout().getLineForOffset(oldEndByte);
                oldEndColumn = oldEndByte - getLayout().getLineStart(oldEndRow);
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (hasLayout()) {
                newEndByte = start + count;

                newEndRow = getLayout().getLineForOffset(newEndByte);
                newEndColumn = newEndByte - getLayout().getLineStart(newEndRow);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            mEditor.getLanguage().getStyler().editSyntaxTree(startByte, oldEndByte, newEndByte,
                    startRow, startColumn,
                    oldEndRow, oldEndColumn,
                    newEndRow, newEndColumn);
        }
    }

    private class HighlightUpdateWatcher implements TextWatcher {
        private int addedTextCount = 0;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            addedTextCount -= count;
            stopParsingSpan();
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
            startParsingSpan();
            addedTextCount = 0;
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // ActionMode callback
    ///////////////////////////////////////////////////////////////////////////
    private static class InsertionActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                menu.removeItem(android.R.id.autofill);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    }

    private static class SelectionActiomModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            menu.removeItem(android.R.id.shareText);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // MovementMethod
    ///////////////////////////////////////////////////////////////////////////
    private static class MovementMethod extends ArrowKeyMovementMethod {

        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            return Touch.onTouchEvent(widget, buffer, event);
        }

        @Override
        public boolean onKeyDown(TextView widget, Spannable text, int keyCode, KeyEvent event) {
            handleReplacedSpan((TextArea) widget, keyCode, event);
            return super.onKeyDown(widget, text, keyCode, event);
        }

        @Override
        public boolean onKeyUp(TextView widget, Spannable text, int keyCode, KeyEvent event) {
            handleIndent((TextArea) widget, keyCode, event);
            return super.onKeyUp(widget, text, keyCode, event);
        }

        private void handleIndent(TextArea textArea, int keyCode, KeyEvent event) {
            if (!textArea.mEditor.getConfiguration().isAutoIndent()) return;

            if (keyCode == KeyEvent.KEYCODE_ENTER && (event.hasNoModifiers())) {
                textArea.indent(textArea.getCurrentLine());
            }
        }

        private void handleReplacedSpan(TextArea textArea, int keyCode, KeyEvent event) {
            Editable content = textArea.getText();
            int start = textArea.getSelectionStart();
            if (keyCode == KeyEvent.KEYCODE_DEL && (event.hasNoModifiers() || event.hasModifiers(KeyEvent.META_ALT_ON))) {
                ReplacedSpan[] repl = content.getSpans(start - 1, start, ReplacedSpan.class);
                if (repl.length > 0) {
                    int st = content.getSpanStart(repl[0]);
                    int en = content.getSpanEnd(repl[0]);
                    String old = new String(repl[0].getText());
                    content.removeSpan(repl[0]);
                    if (start >= en) {
                        content.replace(st, en, old);
                    }
                }
            }
        }

//        private void handleTapSpan(TextProcessor editor, int keyCode) {
//        Editable content = editor.getText();
//        int nextLineStart = editor.getLayout().getLineStart(editor.getCurrentLine() + 1);
//
//        int start = editor.getSelectionStart();
//        if (keyCode == KeyEvent.KEYCODE_ENTER) {
//            TabSpan[] tabs = content.getSpans(start, content.length(), TabSpan.class);
//
//            for (TabSpan tab : tabs) {
//                int s = content.getSpanStart(tab);
//
//                content.setSpan();
//            }
//        }
//        }

        @Override
        public boolean canSelectArbitrarily() {
            return true;
        }

        public static MovementMethod getInstance() {
            if (sInstance == null) {
                sInstance = new MovementMethod();
            }
            return sInstance;
        }

        private static MovementMethod sInstance;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Draw
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected void onDraw(Canvas canvas) {
        if (mEditor.getConfiguration().isCursorLine() && !mEditor.isViwer()) {
            drawCursorLine(canvas);
        }
        super.onDraw(canvas);
    }

    private void drawCursorLine(Canvas canvas) {
        if (!hasLayout() || hasSelection()) return;

        int lineHeight = getLineHeight();
        float left = 0;
        float right = getWidth() + getPaddingLeft() + getPaddingRight();
        float top = getLayout().getLineTop(getCurrentLine()) + getPaddingTop() - (lineHeight - (lineHeight / getLineSpacingMultiplier())) / 2;
        float bottom = top + lineHeight;

        canvas.drawRect(left, top, right, bottom, mCursorLinePaint);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Completion menu
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected void replaceText(CharSequence text) {
        clearComposingText();

        int end = getSelectionEnd();
        int start = mWordTokenizer.findTokenStart(getText(), end);

        Editable editable = getText();
        editable.replace(start, end, mWordTokenizer.terminateToken(text));
    }

    @Override
    public void showDropDown() {
        updateCompletionMenuPosition();
        super.showDropDown();
    }

    private void updateCompletionMenuPosition() {
        if (!hasLayout()) return;

        int currentLine = getCurrentLine();
        int x = (int) (getLayout().getPrimaryHorizontal(mWordTokenizer.findTokenStart(getText(), getSelectionStart())) + getPaddingLeft()) - mCompletionMenuHorizontalPadding;
        int y = getLayout().getLineBottom(currentLine) + getDropDownHeight();
        int deltaX = x - mScrollX;
        int deltaY = y - mScrollY;

        if (deltaY > mHeight) {
            mEditor.getScrollingModel().scrollBy(0, deltaY - mHeight + getLineHeight());
        }

        // FIXME: completion menu horizontal gap not working well, need fix
        if (deltaX > mWidth - getDropDownWidth()) {
            mEditor.getScrollingModel().scrollBy(x + getDropDownWidth(), 0);
        }

        setDropDownHorizontalOffset(x);
        setDropDownVerticalOffset(y);
    }

    private static class SuggestionAdapter extends ArrayAdapter<Suggestion> {
        private ColorScheme mColorScheme;
        private float mTextSize;

        public SuggestionAdapter(@NonNull Context context) {
            super(context, R.layout.item_suggestion);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Suggestion suggestion = getItem(position);

            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.item_suggestion, parent, false);
                viewHolder.type = convertView.findViewById(R.id.type);
                viewHolder.suggestion = convertView.findViewById(R.id.suggestion);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.type.setText(suggestion.getType().toString());
            viewHolder.suggestion.setText(suggestion.getText());

            viewHolder.type.setTextSize(mTextSize);
            viewHolder.suggestion.setTextSize(mTextSize);

            if (mColorScheme != null) {
                viewHolder.type.setTextColor(mColorScheme.getTextColor());
                viewHolder.suggestion.setTextColor(mColorScheme.getTextColor());
            }

            return convertView;
        }

        public void setData(List<Suggestion> suggestions) {
            clear();
            addAll(suggestions);
        }

        public void setTextSize(float size) {
            mTextSize = size;
        }

        public void setColorScheme(ColorScheme colorScheme) {
            mColorScheme = colorScheme;
        }

        private static class ViewHolder {
            TextView type;
            TextView suggestion;
        }
    }

    private static class WordTokenizer implements MultiAutoCompleteTextView.Tokenizer {
        private final boolean[] NOT_IDENTIFIRE_TOKEN_TABLE = new boolean[128];

        public WordTokenizer() {
            String tokenString = "!?|.:;'+-=/@#$%^&*(){}[]<> \r\n\t";
            for (int i = 0; i < tokenString.length(); i++) {
                NOT_IDENTIFIRE_TOKEN_TABLE[tokenString.charAt(i)] = true;
            }
        }

        @Override
        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;

            while (i > 0 && !isToken(text.charAt(i - 1))) i--;

            return i;
        }

        @Override
        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();

            while (i < len) {
                if (isToken(text.charAt(i))) {
                    return i - 1;
                } else {
                    i++;
                }
            }

            return len;
        }

        @Override
        public CharSequence terminateToken(CharSequence text) {
            return text;
        }

        private boolean isToken(char c) {
            return NOT_IDENTIFIRE_TOKEN_TABLE[c];
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Span
    ///////////////////////////////////////////////////////////////////////////
    private void startParsingSpan() {
        stopParsingSpan();

        mParsingSpanTask = new ParsingSpanTask(mEditor, spans -> {
            mSpans.clear();
            mSpans.addAll(spans);
            updateSpan();
        });

        mParsingSpanTask.execute();
    }

    private void stopParsingSpan() {
        if (mParsingSpanTask != null) {
            mParsingSpanTask.cancel(true);
        }
    }

    /**
     * Update the span on the current visiable area.
     */
    private void updateSpan() {
        if (!hasLayout()) return;

        int top = getTopLine();
        int bottom = getBottomLine();

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

        int topVisiableLine = Math.max(0, getTopLine() - 1);
        int bottomVisiableLine = getBottomLine();

        int topVisiableLineStart = getLayout().getLineStart(topVisiableLine);
        int topVisiableLineEnd = getLayout().getLineEnd(topVisiableLine);
        int bottomVisiableLineStart = getLayout().getLineStart(bottomVisiableLine);
        int bottomVisiableLineEnd = getLayout().getLineEnd(bottomVisiableLine);

        Editable text = getText();

        // scroll down
        if (topVisiableLineStart >= mTopLineStart) {
            if (topVisiableLine != 0) {
                TextBuffer.cleanSpans(text, Math.min(mTopLineStart, topVisiableLineStart), Math.max(mTopLineEnd, topVisiableLineEnd));
            }
            TextBuffer.addSpans(text, mSpans, Math.min(mBottomLineStart, bottomVisiableLineStart), Math.max(mBottomLineEnd, bottomVisiableLineEnd));
        } else {
            if (bottomVisiableLine != getLineCount() - 1) {
                TextBuffer.cleanSpans(text, Math.min(mBottomLineStart, bottomVisiableLineStart), Math.max(mBottomLineEnd, bottomVisiableLineEnd));
            }
            TextBuffer.addSpans(text, mSpans, Math.min(mTopLineStart, topVisiableLineStart), Math.max(mTopLineEnd, topVisiableLineEnd));
        }

        mTopLineStart = topVisiableLineStart;
        mTopLineEnd = topVisiableLineEnd;
        mBottomLineStart = bottomVisiableLineStart;
        mBottomLineEnd = bottomVisiableLineEnd;

        isUpdatingSpan = false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper function
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Indent by given line
     *
     * @see TSLanguageStyler#getIndentLevel(int, int)
     */
    private void indent(int line) {
        Editable content = getText();
        int currentLine = getCurrentLine();
        int level = mEditor.getLanguage().getStyler().getIndentLevel(currentLine, getPrevnonblankLine());
        String tab = mEditor.getConfiguration().getIndentation().get(level);

        int start = getLayout().getLineStart(line);
        content.insert(start, tab);
        TextBuffer.setSpan(content, new TabSpan(tab, mEditor.getColorScheme().getIndentColor(), start, getSelectionEnd()));
    }

    /**
     * If line is blank means it only have blank character like space, tab etc.
     *
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

    ///////////////////////////////////////////////////////////////////////////
    // Public function (api)
    ///////////////////////////////////////////////////////////////////////////
    public int getCurrentLine() {
        return hasLayout() ? getLayout().getLineForOffset(getSelectionStart()) : 0;
    }

    public int getTopLine() {
        return hasLayout() ? getLayout().getLineForVertical(mScrollY) : 0;
    }

    public int getBottomLine() {
        return hasLayout() ? getLayout().getLineForVertical(mScrollY + mHeight) : 0;
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

    public void replace() {
        // FIXME: seems replace not working
        onTextContextMenuItem(android.R.id.replaceText);
    }

    public void caretMoveUp() {
        if (hasLayout()) {
            Selection.moveUp(getText(), getLayout());
        }
    }

    public void caretMoveDown() {
        if (hasLayout()) {
            Selection.moveDown(getText(), getLayout());
        }
    }

    public void caretMoveLeft() {
        if (hasLayout()) {
            Selection.moveLeft(getText(), getLayout());
        }
    }

    public void caretMoveRight() {
        if (hasLayout()) {
            Selection.moveRight(getText(), getLayout());
        }
    }

    public void format() {
        if (mFormatTask != null) {
            mFormatTask.cancel(true);
        }

        mFormatTask = new FormatTask(mEditor, (text) -> {
            TextBuffer.setText(getText(), text);
        });

        mFormatTask.execute();
    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        // Need update the text size of SuggestionAdapter's TextView
        mSuggestionAdapter.setTextSize(size);

        // Need to update the syntax because the visible area was changed.
        post(this::updateSpan);
    }

    public void setOnSelectionChangedListener(@Nullable OnSelectionChangedListener l) {
        mSelectionChangedListener = l;
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int start, int end);
    }
}