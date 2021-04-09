package io.ikws4.codeeditor.api.editor;

import androidx.annotation.NonNull;

import io.ikws4.codeeditor.api.configuration.ColorScheme;
import io.ikws4.codeeditor.api.language.Language;
import io.ikws4.codeeditor.configuration.Configuration;

public interface Editor {
    @NonNull
    CharSequence getText();

    @NonNull
    Configuration getConfiguration();

    @NonNull
    ColorScheme getColorScheme();

    @NonNull
    Language getLanguage();

    /**
     * Returns the value indicating whether the editor operates in viewer mode, with
     * all modification actions disabled.
     *
     * @return true if the editor works as a viewer, false otherwise
     */
    boolean isViwer();

    /**
     * Returns the selection model for the editor, which can be used to select ranges of text in
     * the document and retrieve information about the selection.
     *
     * @return the selection model instance.
     */
    @NonNull
    SelectionModel getSelectionModel();

    /**
     * Returns the scrolling model for the document, which can be used to scroll the document
     * and retrieve information about the current position of the scrollbars.
     *
     * @return the scrolling model instance.
     */
    @NonNull
    ScrollingModel getScrollingModel();

    @NonNull
    ScaleModel getScacleModel();

    @NonNull
    LayoutModel getLayoutModel();

    void hideSoftInput();

    void showSoftInput();
}