package io.ikws4.codeeditor.language;

public class Suggestion {
    private final Type mType;
    private final String mText;
    private final String mReturnType;

    public Suggestion(Type type, String text) {
        this(type, text, "");
    }

    public Suggestion(Type type, String text, String returnType) {
        mType = type;
        mText = text;
        mReturnType = returnType;
    }

    public Type getType() {
        return mType;
    }

    public String getTypeString() {
        switch (mType) {
            case KEYWORD: return "k";
            case IDENTIFIER: return "w";
            case FUNCTION: return "f";
            case FIELD: return "v";
            case TYPE: return "t";
        }
        return "";
    }

    public String getText() {
        return mText;
    }

    public String getReturnType() {
        return mReturnType;
    }

    @Override
    public String toString() {
        return mText;
    }

    public enum Type {
        KEYWORD,
        IDENTIFIER,
        FUNCTION,
        FIELD,
        TYPE,
    }
}
