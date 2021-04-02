package io.ikws4.codeeditor.language;

import java.util.List;
import java.util.Set;

public interface LanguageSuggestionProvider {
    List<Suggestion> getAll();

    void process(int line, String text);

    void delete(int line);

    void clear();
}
