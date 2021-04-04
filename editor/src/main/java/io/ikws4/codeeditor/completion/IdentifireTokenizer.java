package io.ikws4.codeeditor.completion;

import android.widget.MultiAutoCompleteTextView;

public class IdentifireTokenizer implements MultiAutoCompleteTextView.Tokenizer {
    private final boolean[] NOT_IDENTIFIRE_TOKEN_TABLE = new boolean[128];

    public IdentifireTokenizer() {
        String tokenString = "!?|.:;'+-=/@#$%^&*(){}[]<> \r\n\t";
        for (int i = 0; i < tokenString.length(); i++) {
            NOT_IDENTIFIRE_TOKEN_TABLE[tokenString.charAt(i)] = true;
        }
    }

    /**
     * Returns the start of the token that ends at offset
     * <code>cursor</code> within <code>text</code>.
     *
     * @param text
     * @param cursor
     */
    @Override
    public int findTokenStart(CharSequence text, int cursor) {
        int i = cursor;

        while (i > 0 && !isToken(text.charAt(i - 1))) i--;

        return i;
    }

    /**
     * Returns the end of the token (minus trailing punctuation)
     * that begins at offset <code>cursor</code> within <code>text</code>.
     *
     * @param text
     * @param cursor
     */
    @Override
    public int findTokenEnd(CharSequence text, int cursor) {
        int i = cursor;
        int len = text.length();

        while (i < len) {
            if (isToken(text.charAt(i))) {
                return i - 1;
            } else {
                i++;
            }
        }

        return len;
    }

    /**
     * Returns <code>text</code>, modified, if necessary, to ensure that
     * it ends with a token terminator (for example a space or comma).
     *
     * @param text
     */
    @Override
    public CharSequence terminateToken(CharSequence text) {
        return text;
    }

    private boolean isToken(char c) {
        return NOT_IDENTIFIRE_TOKEN_TABLE[c];
    }
}