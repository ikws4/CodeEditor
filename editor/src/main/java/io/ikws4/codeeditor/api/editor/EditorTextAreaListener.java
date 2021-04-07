package io.ikws4.codeeditor.api.editor;

import android.text.Layout;

import androidx.annotation.Nullable;

public interface EditorTextAreaListener {
void onTextAreaChanged(int topLine, int bottomLine, int currentLine, float textSize, @Nullable Layout layout);
}
