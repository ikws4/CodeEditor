package io.ikws4.codeeditor.language.none;

import java.util.Collections;
import java.util.List;

import io.ikws4.codeeditor.core.colorscheme.SyntaxColorScheme;
import io.ikws4.codeeditor.core.span.SyntaxHighlightSpan;
import io.ikws4.codeeditor.language.Language;
import io.ikws4.codeeditor.language.LanguageParser;
import io.ikws4.codeeditor.language.LanguageStyler;
import io.ikws4.codeeditor.language.LanguageSuggestionProvider;
import io.ikws4.codeeditor.language.ParseException;
import io.ikws4.codeeditor.language.ParseResult;
import io.ikws4.codeeditor.language.Suggestion;

public class NoneLanguage implements Language {
    @Override
    public String getName() {
        return "none";
    }

    @Override
    public LanguageParser getParser() {
        return new LanguageParser() {
            @Override
            public ParseResult parse(String name, String source) {
                ParseException exception = new ParseException("Unable to parse unsupported language", 0, 0);
                return new ParseResult(exception);
            }
        };
    }

    @Override
    public LanguageSuggestionProvider getSuggestionProvider() {
        return new LanguageSuggestionProvider() {
            @Override
            public List<Suggestion> getAll() {
                return Collections.emptyList();
            }

            @Override
            public void process(int line, String text) {

            }

            @Override
            public void delete(int line) {

            }

            @Override
            public void clear() {

            }
        };
    }

    @Override
    public LanguageStyler getStyler() {
        return new LanguageStyler() {
            @Override
            public void editSyntaxTree(int startByte, int oldEndByte, int newEndByte, int startRow, int startColumn, int oldEndRow, int oldEndColumn, int newEndRow, int newEndColumn) {

            }

            @Override
            public int getIndentLevel(String source, int line, int prevnonblankLine) {
                return 0;
            }

            @Override
            public String format(String source) {
                return "";
            }

            @Override
            public List<SyntaxHighlightSpan> highlight(String source, SyntaxColorScheme scheme) {
                return Collections.emptyList();
            }
        };
    }
}
