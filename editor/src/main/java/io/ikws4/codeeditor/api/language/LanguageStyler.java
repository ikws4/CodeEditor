package io.ikws4.codeeditor.api.language;

import java.util.List;

import io.ikws4.codeeditor.api.configuration.SyntaxColorScheme;

public interface LanguageStyler {

    void editSyntaxTree(int startByte, int oldEndByte, int newEndByte, int startRow, int startColumn, int oldEndRow, int oldEndColumn, int newEndRow, int newEndColumn);

    int getIndentLevel(int line, int prevnonblankLine);

    String format(String source);

    List<ExtendedSpan> process(String source, SyntaxColorScheme scheme);
}
