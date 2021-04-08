package io.ikws4.codeeditor.component;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;

import androidx.annotation.Nullable;

import io.ikws4.codeeditor.CodeEditor;
import io.ikws4.codeeditor.api.configuration.ColorScheme;
import io.ikws4.codeeditor.api.editor.EditorScrollListener;
import io.ikws4.codeeditor.api.editor.EditorTextAreaListener;
import io.ikws4.codeeditor.api.editor.component.Component;

public class Gutter extends View implements Component, EditorScrollListener, EditorTextAreaListener {
    private static final String TAG = "Gutter";

    private int mScrollY;

    private int mCurrentLine;
    private int mTopLine;
    private int mBottomLine;
    private Layout mLayout;

    private final Paint mTextPaint = new Paint();
    private final Paint mActiveTextPaint = new Paint();

    public Gutter(Context context) {
        this(context, null);
    }

    public Gutter(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Gutter(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void attach(CodeEditor editor) {
        ColorScheme colorScheme = editor.getConfiguration().getColorScheme();

        setBackgroundColor(colorScheme.getGutterColor());

        mTextPaint.setTextSize(editor.getTextSize());
        mTextPaint.setColor(colorScheme.getGutterTextColor());
        mTextPaint.setTypeface(Typeface.MONOSPACE);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.RIGHT);

        mActiveTextPaint.setTextSize(editor.getTextSize());
        mActiveTextPaint.setColor(colorScheme.getGutterActiveTextColor());
        mActiveTextPaint.setTypeface(Typeface.MONOSPACE);
        mActiveTextPaint.setAntiAlias(true);
        mActiveTextPaint.setTextAlign(Paint.Align.RIGHT);

        editor.addScrollListener(this);
        editor.addTextAreaListener(this);
    }

    @Override
    public void onScroll(int x, int y, int oldx, int oldy) {
        if (mScrollY != y) {
            mScrollY = y;
            invalidate();
        }
    }

    @Override
    public void onTextAreaChanged(int topLine, int bottomLine, int currentLine, float textSize, @Nullable Layout layout) {
        if (layout == null) return;

        mTopLine = topLine;
        mBottomLine = bottomLine;
        mCurrentLine = currentLine;
        mLayout = layout;
        setTextSize(textSize);

        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = View.resolveSize(getWidth(), widthMeasureSpec);
        int h = View.resolveSize(getHeight(), heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mLayout == null) return;

        measureGutterWidth();
        drawLineNumber(canvas);
    }

    private void drawLineNumber(Canvas canvas) {
        for (int line = mTopLine; line <= mBottomLine; line++) {
            String realLine = String.valueOf(line + 1);
            float x = getWidth() - getPaddingRight();
            float y = mLayout.getLineBaseline(line) - mScrollY;
            canvas.drawText(realLine, x, y, line == mCurrentLine ? mActiveTextPaint : mTextPaint);
        }
    }

    private void measureGutterWidth() {
        int lineCount = mLayout.getLineCount();
        String lineCountText = String.valueOf(lineCount);

        // When the number digits are not equal, we need relayout.
        // eg. when linenumber from 99 become to 100.
        if (Math.log10(lineCount) != Math.log10(lineCount - 1)) {
            requestLayout();
        }

        getLayoutParams().width = (int) (mTextPaint.measureText(lineCountText));
        getLayoutParams().width += getPaddingLeft() + getPaddingRight();
    }

    private void setTextSize(float size) {
        mTextPaint.setTextSize(size);
        mActiveTextPaint.setTextSize(size);
    }
}
