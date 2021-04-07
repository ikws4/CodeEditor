package io.ikws4.codeeditor.api.editor.component;

import androidx.annotation.NonNull;

public interface ScrollableComponent extends Component {

    @NonNull
    ScrollDelegate getScrollDelegate();

    void setScrollDelegate(@NonNull ScrollDelegate delegate);
}