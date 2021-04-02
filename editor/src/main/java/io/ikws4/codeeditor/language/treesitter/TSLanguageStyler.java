package io.ikws4.codeeditor.language.treesitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.ikws4.codeeditor.core.colorscheme.SyntaxColorScheme;
import io.ikws4.codeeditor.core.span.SyntaxHighlightSpan;
import io.ikws4.codeeditor.language.LanguageStyler;
import io.ikws4.jsitter.TSNode;
import io.ikws4.jsitter.TSParser;
import io.ikws4.jsitter.TSQuery;
import io.ikws4.jsitter.TSQueryCapture;
import io.ikws4.jsitter.TSTree;

public abstract class TSLanguageStyler implements LanguageStyler {
    private static final String TAG = "TSLanguageStyler";

    private final static HashMap<String, TSHightlightType> hlmap;
    private final TSParser mParser;
    private final TSQuery mHighlightQuery;
    private final TSQuery mIndentQuery;
    private TSTree mTree;

    private boolean isParsing = false;

    static {
        hlmap = new HashMap<>();
        hlmap.put("annotation", TSHightlightType.Annotation);
        hlmap.put("attribute", TSHightlightType.Attribute);
        hlmap.put("boolean", TSHightlightType.Boolean);
        hlmap.put("character", TSHightlightType.Character);
        hlmap.put("comment", TSHightlightType.Comment);
        hlmap.put("conditional", TSHightlightType.Conditional);
        hlmap.put("constant", TSHightlightType.Constant);
        hlmap.put("constant.builtin", TSHightlightType.ConstBuiltin);
        hlmap.put("constant.macro", TSHightlightType.ConstMacro);
        hlmap.put("constructor", TSHightlightType.Constructor);
        hlmap.put("error", TSHightlightType.Error);
        hlmap.put("exception", TSHightlightType.Exception);
        hlmap.put("field", TSHightlightType.Field);
        hlmap.put("float", TSHightlightType.Float);
        hlmap.put("function", TSHightlightType.Function);
        hlmap.put("function.builtin", TSHightlightType.FuncBuiltin);
        hlmap.put("function.macro", TSHightlightType.FuncMarco);
        hlmap.put("include", TSHightlightType.Include);
        hlmap.put("keyword", TSHightlightType.Keyword);
        hlmap.put("keyword.function", TSHightlightType.KeywordFunction);
        hlmap.put("keyword.operator", TSHightlightType.KeywordOperator);
        hlmap.put("label", TSHightlightType.Label);
        hlmap.put("method", TSHightlightType.Method);
        hlmap.put("namespace", TSHightlightType.Namespace);
        hlmap.put("none", TSHightlightType.None);
        hlmap.put("number", TSHightlightType.Number);
        hlmap.put("operator", TSHightlightType.Operator);
        hlmap.put("parameter", TSHightlightType.Parameter);
        hlmap.put("parameter.reference", TSHightlightType.ParameterReference);
        hlmap.put("property", TSHightlightType.Property);
        hlmap.put("punctuation.delimiter", TSHightlightType.PunctDelimiter);
        hlmap.put("punctuation.bracket", TSHightlightType.PunctBracket);
        hlmap.put("punctuation.special", TSHightlightType.PunctSpecial);
        hlmap.put("repeat", TSHightlightType.Repeat);
        hlmap.put("string", TSHightlightType.String);
        hlmap.put("string.regex", TSHightlightType.StringRegex);
        hlmap.put("string.escape", TSHightlightType.StringEscape);
        hlmap.put("symbol", TSHightlightType.Symbol);
        hlmap.put("tag", TSHightlightType.Tag);
        hlmap.put("tag.delimiter", TSHightlightType.TagDelimiter);
        hlmap.put("text", TSHightlightType.Text);
        hlmap.put("text.strong", TSHightlightType.Strong);
        hlmap.put("text.emphasis", TSHightlightType.Emphasis);
        hlmap.put("text.underline", TSHightlightType.Underline);
        hlmap.put("text.strike", TSHightlightType.Strike);
        hlmap.put("text.title", TSHightlightType.Title);
        hlmap.put("text.literal", TSHightlightType.Literal);
        hlmap.put("text.url", TSHightlightType.URL);
        hlmap.put("text.note", TSHightlightType.Note);
        hlmap.put("text.warning", TSHightlightType.Warning);
        hlmap.put("text.danger", TSHightlightType.Danger);
        hlmap.put("type", TSHightlightType.Type);
        hlmap.put("type.builtin", TSHightlightType.TypeBuiltin);
        hlmap.put("variable", TSHightlightType.Variable);
        hlmap.put("variable.builtin", TSHightlightType.VariableBuiltin);
    }

    /**
     * @param language see {@link io.ikws4.jsitter.TSLanguages}
     */
    public TSLanguageStyler(long language, TSLangaugeQuery queryScm) {
        mParser = new TSParser(language);
        mHighlightQuery = new TSQuery(language, queryScm.highlight());
        mIndentQuery = new TSQuery(language, queryScm.indent());
    }

    @Override
    public void editSyntaxTree(int startByte, int oldEndByte, int newEndByte, int startRow, int startColumn, int oldEndRow, int oldEndColumn, int newEndRow, int newEndColumn) {
        if (mTree == null) return;
        mTree.edit(startByte, oldEndByte, newEndByte, startRow, startColumn, oldEndRow, oldEndColumn, newEndRow, newEndColumn);
    }

    /**
     * Reference <a href="https://github.com/nvim-treesitter/nvim-treesitter/blob/master/lua/nvim-treesitter/indent.lua">https://github.com/nvim-treesitter/nvim-treesitter/blob/master/lua/nvim-treesitter/indent.lua</a>
     */
    @Override
    public int getIndentLevel(String source, int line, int prevnonblankLine) {
        int level = 0;

        parse(source);
        TSNode root = mTree.getRoot();
        TSNode curr = getNodeAtLine(root, line);

        HashMap<TSNode, TSIndentType> queryMap = new HashMap<>();
        for (TSQueryCapture capture : mIndentQuery.captureIter(root)) {
            TSIndentType type = TSIndentType.Ignore;
            switch (capture.getName()) {
                case "indent":
                    type = TSIndentType.Indent;
                    break;
                case "branch":
                    type = TSIndentType.Branch;
                    break;
                case "return":
                    type = TSIndentType.Return;
                    break;
            }
            queryMap.put(capture.getNode(), type);
        }

        if (curr == null) {
            if (prevnonblankLine != line) {
                TSNode prevNode = getNodeAtLine(root, prevnonblankLine);
                boolean usePrev = prevNode != null && prevNode.getEndRow() < line;
                usePrev &= queryMap.get(prevNode) != TSIndentType.Return;
                if (usePrev) {
                    curr = prevNode;
                }
            }
        }

        if (curr == null) {
            TSNode wrapper = root.decendantForRange(line, 0, line, -1);
            assert wrapper != null;

            curr = wrapper.getChild(0);
            if (curr == null) curr = wrapper;

            if (queryMap.get(wrapper) == TSIndentType.Indent && wrapper != root) {
                level = 1;
            }
        }

        while (curr != null && queryMap.get(curr) == TSIndentType.Branch) {
            curr = curr.getParent();
        }

        boolean first = true;
        assert curr != null;

        int prevRow = curr.getStartRow();

        while (curr != null) {
            if (TSIndentType.Ignore == queryMap.get(curr) && curr.getStartRow() < line && curr.getEndRow() > line) {
                return 0;
            }

            int row = curr.getStartRow();
            if (!first && TSIndentType.Indent == queryMap.get(curr) && prevRow != row) {
                level++;
                prevRow = row;
            }

            curr = curr.getParent();
            first = false;
        }

        return level;
    }

    @Override
    public List<SyntaxHighlightSpan> highlight(String source, SyntaxColorScheme scheme) {
        List<SyntaxHighlightSpan> spans = new ArrayList<>();
        parse(source);

        for (TSQueryCapture capture : mHighlightQuery.captureIter(mTree.getRoot())) {
            TSNode node = capture.getNode();
            SyntaxHighlightSpan span = onBuildSpan(hlmap.get(capture.getName()), node.getStartByte(), node.getEndByte(), scheme);
            if (span != null) spans.add(span);
        }

        return spans;
    }

    private TSNode getNodeAtLine(TSNode root, int line) {
        for (TSNode node : root.childrenIter()) {
            int startRow = node.getStartRow();
            int endRow = node.getEndRow();
            if (startRow == line) return node;

            if (node.getChildCount() > 0 && startRow < line && line <= endRow) {
                return getNodeAtLine(node, line);
            }
        }
        return null;
    }

    private void parse(String source) {
        if (!isParsing) {
            isParsing = true;
            mTree = mParser.parse(source, mTree);
            isParsing = false;
        }
    }

    protected abstract SyntaxHighlightSpan onBuildSpan(TSHightlightType type, int start, int end, SyntaxColorScheme scheme);

    protected enum TSHightlightType {
        Annotation,
        Attribute,
        Boolean,
        Character,
        Comment,
        Conditional,
        Constant,
        ConstBuiltin,
        ConstMacro,
        Constructor,
        Error,
        Exception,
        Field,
        Float,
        Function,
        FuncBuiltin,
        FuncMarco,
        Include,
        Keyword,
        KeywordFunction,
        KeywordOperator,
        Label,
        Method,
        Namespace,
        None,
        Number,
        Operator,
        Parameter,
        ParameterReference,
        Property,
        PunctDelimiter,
        PunctBracket,
        PunctSpecial,
        Repeat,
        String,
        StringRegex,
        StringEscape,
        Symbol,
        Tag,
        TagDelimiter,
        Text,
        Strong,
        Emphasis,
        Underline,
        Strike,
        Title,
        Literal,
        URL,
        Note,
        Warning,
        Danger,
        Type,
        TypeBuiltin,
        Variable,
        VariableBuiltin
    }

    enum TSIndentType {
        Indent,
        Branch,
        Return,
        Ignore
    }
}
