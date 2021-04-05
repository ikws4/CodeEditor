package io.ikws4.codeeditor.internal;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.Spanned;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.ikws4.codeeditor.api.language.Language;
import io.ikws4.codeeditor.api.language.LanguageStyler;
import io.ikws4.codeeditor.language.java.JavaLanguage;
import io.ikws4.codeeditor.span.SyntaxSpan;
import io.ikws4.jsitter.TSLanguages;
import io.ikws4.jsitter.TSParser;
import io.ikws4.jsitter.TSQuery;
import io.ikws4.jsitter.TSQueryCursor;
import io.ikws4.jsitter.TSTree;
import io.ikws4.jsitter.TSTreeCursor;

abstract class SyntaxHighlightEditText extends NumberLineEditText {
    static {
        System.loadLibrary("jsitter");
    }

    // TreeSitter Parser
    private final TSParser mTSParser = new TSParser();

    private SyntaxHighlightTask mSyntaxHighlightTask;

    // TODO: Remove add None Language for default value
    private Language mLanguage;

    // Spans
    private final List<SyntaxSpan> mSyntaxSpans = new ArrayList<>();

    public SyntaxHighlightEditText(@NonNull Context context) {
        this(context, null);
    }

    public SyntaxHighlightEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.autoCompleteTextViewStyle);
    }

    public SyntaxHighlightEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLanguage(new JavaLanguage());
    }

    @Override
    public void afterTextChanged(Editable s) {
        super.afterTextChanged(s);
        highlight();
    }

    public Language getLanguage() {
        return mLanguage;
    }

    public void setLanguage(Language language) {
        mLanguage = language;
        mTSParser.setLanguage(language.getTSLanguage());
    }

    public void updateSyntaxHighlighting() {
        if (getLayout() == null) return;
        cleanSyntaxHighlightSpan();

        Editable text = getText();
        int start = getLayout().getLineStart(getMinVisibleLine());
        int end = getLayout().getLineEnd(getMaxVisibleLine());
        int n = syntaxHighlightSpanIndexOf(end);
        for (int i = syntaxHighlightSpanIndexOf(start); i < n; i++) {
            SyntaxSpan span = mSyntaxSpans.get(i);
            text.setSpan(span, span.getStart(), span.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private int syntaxHighlightSpanIndexOf(int pos) {
        int l = 0, r = mSyntaxSpans.size();
        while (l < r) {
            int m = l + (r - l) / 2;
            SyntaxSpan span = mSyntaxSpans.get(m);
            if (span.getStart() < pos) {
                l = m + 1;
            } else {
                r = m;
            }
        }
        return l;
    }

    private void cleanSyntaxHighlightSpan() {
        SyntaxSpan[] spans = getText().getSpans(0, getText().length(), SyntaxSpan.class);
        Editable text = getText();
        for (SyntaxSpan span : spans) {
            text.removeSpan(span);
        }
    }

    private void highlight() {
        if (mLanguage == null) return;
        // Stop the previous task
        if (mSyntaxHighlightTask != null) {
            mSyntaxHighlightTask.cancel(true);
        }
        mSyntaxHighlightTask = new SyntaxHighlightTask(this);
        mSyntaxHighlightTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        TSTree tree = mTSParser.parseString(getText().toString());

        TSTreeCursor cursor = tree.getRootNode().walk();
    }

    private static class SyntaxHighlightTask extends AsyncTask<Void, Void, List<SyntaxSpan>> {
        private final WeakReference<SyntaxHighlightEditText> mEditor;

        public SyntaxHighlightTask(SyntaxHighlightEditText editor) {
            super();
            mEditor = new WeakReference<>(editor);
        }

        @Override
        protected List<SyntaxSpan> doInBackground(Void... voids) {
            SyntaxHighlightEditText editor = mEditor.get();
            LanguageStyler h = editor.getLanguage().getStyler();
            return h.highlight(editor.getText().toString(), editor.getColorScheme().getSyntaxScheme());
        }

        @Override
        protected void onPostExecute(List<SyntaxSpan> spans) {
            if (spans == null) return;
            SyntaxHighlightEditText editor = mEditor.get();
            editor.mSyntaxSpans.clear();
            editor.mSyntaxSpans.addAll(spans);
            editor.updateSyntaxHighlighting();
        }
    }
}