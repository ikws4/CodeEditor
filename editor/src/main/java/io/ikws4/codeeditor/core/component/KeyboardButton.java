package io.ikws4.codeeditor.core.component;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class KeyboardButton extends androidx.appcompat.widget.AppCompatImageButton implements Runnable, View.OnLongClickListener, View.OnClickListener {
    private boolean mLongPressed;
    private OnPressedListener mOnPressedListener;

    public KeyboardButton(@NonNull Context context) {
        this(context, null);
    }

    public KeyboardButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.imageButtonStyle);
    }

    public KeyboardButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedValue value = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);
        setBackgroundResource(value.resourceId);
        setOnClickListener(this);
        if (isLongClickable()) {
            setOnLongClickListener(this);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("", "onTouchEvent: " + event.getAction());
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLongPressed = false;
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void run() {
        if (mOnPressedListener != null) {
            mOnPressedListener.onPressed();
            if (mLongPressed) {
                postDelayed(this, 50);
            }
        }
    }

    @Override
    public void onClick(View v) {
        post(this);
    }

    @Override
    public boolean onLongClick(View v) {
        mLongPressed = true;
        post(this);
        return true;
    }

    public void setOnPressedListener(OnPressedListener l) {
        mOnPressedListener = l;
    }

    public interface OnPressedListener {
        void onPressed();
    }
}
