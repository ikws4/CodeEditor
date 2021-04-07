package io.ikws4.codeeditor.task;

import android.os.AsyncTask;
import android.text.Editable;

import java.lang.ref.WeakReference;
import java.util.List;

import io.ikws4.codeeditor.component.TextArea;
import io.ikws4.codeeditor.api.configuration.SyntaxColorScheme;
import io.ikws4.codeeditor.api.language.ExtendedSpan;
import io.ikws4.codeeditor.api.language.LanguageStyler;

public class ProcessSpanTask extends AsyncTask<Void, Void, List<ExtendedSpan>> {
    private final WeakReference<TextArea> mEditor;
    private final OnTaskFinishedListener<List<ExtendedSpan>> mListener;

    public ProcessSpanTask(TextArea editor, OnTaskFinishedListener<List<ExtendedSpan>> listener) {
        super();
        mEditor = new WeakReference<>(editor);
        mListener = listener;
    }

    @Override
    protected List<ExtendedSpan> doInBackground(Void... voids) {
        TextArea editor = mEditor.get();
        Editable content = editor.getText();
        LanguageStyler highlighter = editor.getLanguage().getStyler();
        SyntaxColorScheme syntaxScheme = editor.getConfiguration().getColorScheme().getSyntaxColorScheme();
        return highlighter.process(content.toString(), syntaxScheme);
    }

    @Override
    protected void onPostExecute(List<ExtendedSpan> spans) {
        mListener.onFinished(spans);
    }
}
