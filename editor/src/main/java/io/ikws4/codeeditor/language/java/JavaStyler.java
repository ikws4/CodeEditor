package io.ikws4.codeeditor.language.java;

import android.util.Log;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.JavaFormatterOptions;

import io.ikws4.codeeditor.api.configuration.SyntaxColorScheme;
import io.ikws4.codeeditor.api.language.ExtendedSpan;
import io.ikws4.codeeditor.language.TSHighlightType;
import io.ikws4.codeeditor.span.SyntaxSpan;
import io.ikws4.codeeditor.language.TSLanguageStyler;
import io.ikws4.codeeditor.span.TabSpan;
import io.ikws4.jsitter.TSLanguages;

class JavaStyler extends TSLanguageStyler {
    private static final String TAG = "JavaStyler";

    private final Formatter mFormatter;
    private static JavaStyler sInstance;

    private JavaStyler() {
        super(TSLanguages.java(), new JavaQuery());
        JavaFormatterOptions options = JavaFormatterOptions.builder()
                .style(JavaFormatterOptions.Style.AOSP)
                .build();
        mFormatter = new Formatter(options);
    }

    public static JavaStyler getInstance() {
        if (sInstance == null) {
            sInstance = new JavaStyler();
        }
        return sInstance;
    }

    @Override
    public String format(String source) {
        try {
            return mFormatter.formatSource(source);
        } catch (FormatterException e) {
            Log.w(TAG, "format: ", e);
        }
        return source;
    }

    @Override
    protected ExtendedSpan onBuildSpan(TSHighlightType type, int start, int end, SyntaxColorScheme scheme) {
        switch (type) {
            case Attribute:
                return new SyntaxSpan(scheme.getAnnotationColor(), start, end);
            case Comment:
                return new SyntaxSpan(scheme.getCommentColor(), start, end);
            case Method:
            case Function:
                return new SyntaxSpan(scheme.getMethodColor(), start, end);
            case Include:
            case ConstBuiltin:
            case Boolean:
            case FuncBuiltin:
            case TypeBuiltin:
            case Conditional:
            case Repeat:
            case KeywordOperator:
            case Exception:
            case Keyword:
                return new SyntaxSpan(scheme.getKeywordColor(), start, end);
            case Float:
            case Number:
                return new SyntaxSpan(scheme.getNumberColor(), start, end);
            case Operator:
                return new SyntaxSpan(scheme.getOperatorColor(), start, end);
            case Character:
            case String:
                return new SyntaxSpan(scheme.getStringColor(), start, end);
            case Type:
                return new SyntaxSpan(scheme.getTypeColor(), start, end);
        }
        return null;
    }
}
