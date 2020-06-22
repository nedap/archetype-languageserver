package com.nedap.openehr.lsp.symbolextractor;

import com.nedap.archie.adlparser.antlr.AdlBaseListener;
import com.nedap.archie.adlparser.antlr.AdlLexer;
import com.nedap.archie.adlparser.antlr.AdlParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class SymbolInformationExtractingListener extends AdlBaseListener {
    private String archetypeId;
    private final List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();
    private final Set<Token> visitedTokens = new LinkedHashSet<>();
    private final AdlLexer lexer;

    private List<DocumentLink> documentLinks = new ArrayList<>();

    private List<FoldingRange> foldingRanges = new ArrayList<>();

    private final String documentUri;

    private Stack<DocumentSymbol> symbolStack = new Stack<>();


    public SymbolInformationExtractingListener(String documentUri, AdlLexer lexer) {
        this.documentUri = documentUri;
        this.lexer = lexer;
    }

    //private Stack<SymbolInformation> symbolStack = new Stack<>();

    public List<Either<SymbolInformation, DocumentSymbol>> getSymbols() {
        return this.symbols;
    }

    public List<DocumentLink> getDocumentLinks() {
        return documentLinks;
    }

    public List<FoldingRange> getFoldingRanges() {
        return foldingRanges;
    }

    /**
     * Enter a parse tree produced by {@link AdlParser#archetype}.
     * @param ctx the parse tree
     */
    @Override
    public void enterArchetype(AdlParser.ArchetypeContext ctx) {
        addSymbol(ctx.SYM_ARCHETYPE(), ctx, "archetype", SymbolKind.Constant, StackAction.PUSH);
        addSymbol(ctx.ARCHETYPE_HRID(), null, "archetype id", SymbolKind.File);
        this.archetypeId = ctx.ARCHETYPE_HRID().getText();
    }

    @Override
    public void exitArchetype(AdlParser.ArchetypeContext ctx) {
        
    }

    private Range createRange(ParserRuleContext ctx) {
        return new Range(
                new Position(ctx.getStart().getLine()-1, ctx.getStart().getCharPositionInLine()),
                new Position(ctx.getStop().getLine()-1, ctx.getStop().getCharPositionInLine())
        );
    }

    private Range createRange(Token symbol) {
        return new Range(
                new Position(symbol.getLine()-1, symbol.getCharPositionInLine()),
                new Position(symbol.getLine()-1, symbol.getCharPositionInLine() + symbol.getText().length())
        );//TODO: scan text for line endings, and determine stop line+ from that!
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void visitTerminal(TerminalNode node) {
        //TODO: get token type and set here as name?
//        String displayName = lexer.getVocabulary().getDisplayName(node.getSymbol().getType());
//        addSymbol(node, displayName, SymbolKind.String);
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void visitErrorNode(ErrorNode node) { }

    @Override public void enterTemplate(AdlParser.TemplateContext ctx) {
        addSymbol(ctx.SYM_TEMPLATE(), ctx, "archetype", SymbolKind.Constant, StackAction.PUSH);
        addSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class);
        this.archetypeId = ctx.ARCHETYPE_HRID().getText();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitTemplate(AdlParser.TemplateContext ctx) {
        popStack();
    }

    private void popStack() {
        if(!symbolStack.isEmpty()) {
            symbolStack.pop();
        }
    }


    @Override public void enterTemplate_overlay(AdlParser.Template_overlayContext ctx) {
        addSymbol(ctx.SYM_TEMPLATE_OVERLAY(), ctx, "archetype", SymbolKind.Constant, StackAction.PUSH);
        addSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override public void exitTemplate_overlay(AdlParser.Template_overlayContext ctx) {
        popStack();
    }

    @Override public void enterOperational_template(AdlParser.Operational_templateContext ctx) {
        addSymbol(ctx.SYM_OPERATIONAL_TEMPLATE(), ctx, "operational template", SymbolKind.Constant, StackAction.PUSH);
        addSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class);
        this.archetypeId = ctx.ARCHETYPE_HRID().getText();
    }

    @Override public void exitOperational_template(AdlParser.Operational_templateContext ctx) {
        popStack();
    }

    private DocumentSymbol createSymbolInformation(String s, SymbolKind symbolKind, Range range) {
//        SymbolInformation symbolInformation = new SymbolInformation();
//
//        symbolInformation.setName(s);
//        symbolInformation.setKind(module);
//
//        symbolInformation.setLocation(new Location(documentUri, range));
//
//        return symbolInformation;
        DocumentSymbol documentSymbol = new DocumentSymbol();

        documentSymbol.setName(s);
        documentSymbol.setKind(symbolKind);

        documentSymbol.setRange(range);
        documentSymbol.setSelectionRange(range);

        return documentSymbol;
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterSpecialization_section(AdlParser.Specialization_sectionContext ctx) {
        addSymbol(ctx.SYM_SPECIALIZE(), ctx, "specialization section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx);
    }

    @Override public void exitSpecialization_section(AdlParser.Specialization_sectionContext ctx) {
        popStack();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterLanguage_section(AdlParser.Language_sectionContext ctx) {
        addSymbol(ctx.SYM_LANGUAGE(), ctx, "language section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitLanguage_section(AdlParser.Language_sectionContext ctx) {
        popStack();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterDescription_section(AdlParser.Description_sectionContext ctx) {
        addSymbol(ctx.SYM_DESCRIPTION(), ctx, "description section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override public void exitDescription_section(AdlParser.Description_sectionContext ctx) {
        popStack();
    }

    private void addFoldingRange(ParserRuleContext ctx) {
        foldingRanges.add(new FoldingRange(ctx.getStart().getLine()-1, ctx.getStop().getLine()-1));
    }

    private void addFoldingRange(int start, ParserRuleContext stop) {
        foldingRanges.add(new FoldingRange(start, stop.getStop().getLine()-1));
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterDefinition_section(AdlParser.Definition_sectionContext ctx) {
        addSymbol(ctx.SYM_DEFINITION(), ctx, "definition section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitDefinition_section(AdlParser.Definition_sectionContext ctx) {
        popStack();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterRules_section(AdlParser.Rules_sectionContext ctx) {
        addSymbol(ctx.SYM_RULES(), ctx, "rules section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override public void exitRules_section(AdlParser.Rules_sectionContext ctx) {
        popStack();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterTerminology_section(AdlParser.Terminology_sectionContext ctx) {
        addSymbol(ctx.SYM_TERMINOLOGY(), ctx, "terminology section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result

    }

    @Override public void exitTerminology_section(AdlParser.Terminology_sectionContext ctx) {
        popStack();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterAnnotations_section(AdlParser.Annotations_sectionContext ctx) {
        addSymbol(ctx.SYM_ANNOTATIONS(), ctx, "annotations section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override public void exitAnnotations_section(AdlParser.Annotations_sectionContext ctx) {
        popStack();
    }

    @Override public void enterC_complex_object(AdlParser.C_complex_objectContext ctx) {
        if(ctx.ID_CODE() != null) {
            addSymbol(ctx.type_id().ALPHA_UC_ID(), ctx, ctx.type_id().getText() + "[" + ctx.ID_CODE().getText() + "]", SymbolKind.Class, StackAction.PUSH);
           // addSymbol(ctx.ID_CODE(), "complex object " + ctx.type_id().getText() + "[" + ctx.ID_CODE().getText() + "]", SymbolKind.Object);
        } else if (ctx.ROOT_ID_CODE() != null) {
            addSymbol(ctx.type_id().ALPHA_UC_ID(), ctx, ctx.type_id().getText() + "[" + ctx.ROOT_ID_CODE().getText() + "]", SymbolKind.Class, StackAction.PUSH);
         //   addSymbol(ctx.ROOT_ID_CODE(), "[" + ctx.ROOT_ID_CODE().getText() + "]", SymbolKind.Key);
        }
        if(ctx.SYM_MATCHES() != null) {
       //     addSymbol(ctx.SYM_MATCHES(), ctx.SYM_MATCHES().getText(), SymbolKind.Operator);
        }
        addFoldingRange(ctx);
    }

    @Override public void exitC_complex_object(AdlParser.C_complex_objectContext ctx) {
        popStack();
    }

    @Override public void enterC_attribute(AdlParser.C_attributeContext ctx) {
        if(ctx.ADL_PATH() != null) {
            addSymbol(ctx.ADL_PATH(), ctx, ctx.ADL_PATH().getText(), SymbolKind.Field, StackAction.PUSH);
        } else if(ctx.attribute_id() != null) {
            addSymbol(ctx.attribute_id().ALPHA_LC_ID(), ctx, ctx.attribute_id().getText(), SymbolKind.Field, StackAction.PUSH);
        }
        addFoldingRange(ctx);
    }

    @Override public void exitC_attribute(AdlParser.C_attributeContext ctx) {
        popStack();
    }

    @Override public void enterArchetype_slot(AdlParser.Archetype_slotContext ctx) {
        addFoldingRange(ctx);
    }

    @Override public void enterC_archetype_root(AdlParser.C_archetype_rootContext ctx) {
        addSymbol(ctx.SYM_USE_ARCHETYPE(), ctx, ctx.archetype_ref().getText() + "[" + ctx.ID_CODE().getText() + "]", SymbolKind.Class, StackAction.PUSH);
        addFoldingRange(ctx);
        String archetypeRef = ctx.archetype_ref().getText();
        TerminalNode terminalNode = ctx.archetype_ref().ARCHETYPE_HRID();
        if(terminalNode == null) {
            terminalNode = ctx.archetype_ref().ARCHETYPE_REF();
        }
        if(terminalNode != null) {
            DocumentLink documentLink = new DocumentLink(this.createRange(terminalNode.getSymbol()));
            documentLink.setData(archetypeRef); //we'll resolve the target later
            documentLinks.add(documentLink);
        }
    }

    @Override public void exitC_archetype_root(AdlParser.C_archetype_rootContext ctx) {
        popStack();
    }

    /**
     * An odin &lt; ... &gt; block
     */
    @Override public void enterObject_value_block(AdlParser.Object_value_blockContext ctx) {
        addFoldingRange(ctx);
    }

    private void addSymbol(TerminalNode node, String tokenName, SymbolKind symbolKind) {
        addSymbol(node, null, tokenName, symbolKind);

    }

    private void addSymbol(TerminalNode node, ParserRuleContext entireRule, String tokenName, SymbolKind symbolKind) {
        addSymbol(node, entireRule, tokenName, symbolKind, StackAction.NOTHING);
    }

    private void addSymbol(TerminalNode node, ParserRuleContext entireRule, String tokenName, SymbolKind symbolKind, StackAction stackAction) {

        if(!this.visitedTokens.contains(node.getSymbol())) {
            DocumentSymbol symbol = createSymbolInformation(tokenName, symbolKind, createRange(node.getSymbol()));

            if(node.getText().startsWith("\n")) {
                Range range = symbol.getRange();
                range.getStart().setLine(range.getStart().getLine()+1);
                range.getEnd().setLine(range.getEnd().getLine()+1);
            }
            if(entireRule != null) {
                Range range = createRange(entireRule);
                symbol.setRange(range);
            }
            if(!symbolStack.isEmpty()) {
                List<DocumentSymbol> children = symbolStack.peek().getChildren();
                if(children == null) {
                    symbolStack.peek().setChildren(new ArrayList<>());
                    children = symbolStack.peek().getChildren();
                }
                children.add(symbol);
            } else {
                symbols.add(Either.forRight(symbol));
            }
            if(stackAction == StackAction.PUSH) {
                symbolStack.push(symbol);
            }

            visitedTokens.add(node.getSymbol());
        }
    }


    public String getArchetypeId() {
        return archetypeId;
    }

}
