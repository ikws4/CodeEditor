package io.ikws4.codeeditor.api.language;

import java.util.List;

import io.ikws4.codeeditor.api.configuration.SyntaxColorScheme;
import io.ikws4.codeeditor.span.SyntaxHighlightSpan;

public interface LanguageStyler {

    void editSyntaxTree(int startByte, int oldEndByte, int newEndByte, int startRow, int startColumn, int oldEndRow, int oldEndColumn, int newEndRow, int newEndColumn);

    int getIndentLevel(String source, int line, int prevnonblankLine);

    String format(String source);

    List<SyntaxHighlightSpan> highlight(String source, SyntaxColorScheme scheme);
}