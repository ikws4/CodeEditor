package io.ikws4.codeeditor.api.language;

public interface ExtendedSpan {
    int getStart();

    int getEnd();

    void shift(int offset);
}
