package io.ikws4.codeeditor.api.editor;

import androidx.annotation.NonNull;

import javax.annotation.Nonnull;

import io.ikws4.codeeditor.api.configuration.ColorScheme;
import io.ikws4.codeeditor.api.editor.component.Component;
import io.ikws4.codeeditor.api.language.Language;
import io.ikws4.codeeditor.configuration.Configuration;

public interface Editor {
    @NonNull
    Configuration getConfiguration();

    void setConfiguration(@NonNull Configuration configuration);

    @NonNull
    ColorScheme getColorScheme();

    @NonNull
    Language getLanguage();

    void setLangauge(@NonNull Language langauge);

    void addComponent(@NonNull Component component);

    void addScrollListener(@NonNull EditorScrollListener l);

    void removeScrollListener(@NonNull EditorScrollListener l);

    void addKeyboardListener(@NonNull EditorKeyboardListener l);

    void removeKeyboardListener(@NonNull EditorKeyboardListener l);

    void addResizeListener(@Nonnull EditorResizeListener l);

    void removeResizeListener(@Nonnull EditorResizeListener l);

    void addScaleListener(@Nonnull EditorScaleListener l);

    void removeScaleListener(@Nonnull EditorScaleListener l);

    void addTextAreaListener(@NonNull EditorTextAreaListener l);

    void removeTextAreaListener(@NonNull EditorTextAreaListener l);
}