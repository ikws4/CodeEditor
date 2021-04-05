package io.ikws4.codeeditor.span;

public interface ExtendedSpan {
    int getStart();

    int getEnd();

    void shift(int offset);
}
