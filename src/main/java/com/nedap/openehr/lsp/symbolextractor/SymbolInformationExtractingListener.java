package com.nedap.openehr.lsp.symbolextractor;

import com.nedap.archie.adlparser.antlr.AdlBaseListener;
import com.nedap.archie.adlparser.antlr.AdlLexer;
import com.nedap.archie.adlparser.antlr.AdlParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.lsp4j.DocumentSymbol;
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

    private final String documentUri;


    public SymbolInformationExtractingListener(String documentUri, AdlLexer lexer) {
        this.documentUri = documentUri;
        this.lexer = lexer;
    }

    //private Stack<SymbolInformation> symbolStack = new Stack<>();

    public List<Either<SymbolInformation, DocumentSymbol>> getSymbols() {
        return this.symbols;
    }

    /**
     * Enter a parse tree produced by {@link AdlParser#archetype}.
     * @param ctx the parse tree
     */
    @Override
    public void enterArchetype(AdlParser.ArchetypeContext ctx) {
        addSymbol(ctx.SYM_ARCHETYPE(), "archetype", SymbolKind.Constant);
        addSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class);
        this.archetypeId = ctx.ARCHETYPE_HRID().getText();
    }

    @Override
    public void exitArchetype(AdlParser.ArchetypeContext ctx) {
        
    }

    private Range createRange(ParserRuleContext ctx) {
        return new Range(
                new Position(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine()),
                new Position(ctx.getStop().getLine(), ctx.getStop().getCharPositionInLine())
        );
    }

    private SymbolInformation createSymbolInformation(TerminalNode node, String name, SymbolKind kind) {
        SymbolInformation result = createSymbolInformation(name, kind, createRange(node.getSymbol()));
        return result;
    }

    private Range createRange(Token symbol) {
        return new Range(
                new Position(symbol.getLine(), symbol.getCharPositionInLine()),
                new Position(symbol.getLine(), symbol.getText().length())
        );//TODO: scan text for line endings, and determine stop line+ from that!
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void visitTerminal(TerminalNode node) {
        //TODO: get token type and set here as name?
        String displayName = lexer.getVocabulary().getDisplayName(node.getSymbol().getType());
//        addSymbol(node, displayName, SymbolKind.String);
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void visitErrorNode(ErrorNode node) { }

    @Override public void enterTemplate(AdlParser.TemplateContext ctx) {
        addSymbol(ctx.SYM_TEMPLATE(), "archetype", SymbolKind.Constant);
        addSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class);
        this.archetypeId = ctx.ARCHETYPE_HRID().getText();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitTemplate(AdlParser.TemplateContext ctx) {
        
    }


    @Override public void enterTemplate_overlay(AdlParser.Template_overlayContext ctx) {
        addSymbol(ctx.SYM_TEMPLATE_OVERLAY(), "archetype", SymbolKind.Constant);
        addSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class);
    }

    @Override public void exitTemplate_overlay(AdlParser.Template_overlayContext ctx) {
    }

    @Override public void enterOperational_template(AdlParser.Operational_templateContext ctx) {
        addSymbol(ctx.SYM_OPERATIONAL_TEMPLATE(), "operational template", SymbolKind.Constant);
        addSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class);
        this.archetypeId = ctx.ARCHETYPE_HRID().getText();
    }

    private SymbolInformation createAndAddSymbolInformation(String s, SymbolKind module, Range range) {
        return createSymbolInformation(s, module, range);
    }

    private SymbolInformation createSymbolInformation(String s, SymbolKind module, Range range) {
        SymbolInformation SymbolInformation = new SymbolInformation();

        SymbolInformation.setName(s);
        SymbolInformation.setKind(module);

        SymbolInformation.setLocation(new Location(documentUri, range));

        return SymbolInformation;
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterSpecialization_section(AdlParser.Specialization_sectionContext ctx) { }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterLanguage_section(AdlParser.Language_sectionContext ctx) {
        addSymbol(ctx.SYM_LANGUAGE(), "language section", SymbolKind.Module);
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitLanguage_section(AdlParser.Language_sectionContext ctx) {
        
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterDescription_section(AdlParser.Description_sectionContext ctx) {
        addSymbol(ctx.SYM_DESCRIPTION(), "description section", SymbolKind.Module);

    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitDescription_section(AdlParser.Description_sectionContext ctx) {
        
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterDefinition_section(AdlParser.Definition_sectionContext ctx) {
        addSymbol(ctx.SYM_DEFINITION(), "definition section", SymbolKind.Module);
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitDefinition_section(AdlParser.Definition_sectionContext ctx) {
        
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterRules_section(AdlParser.Rules_sectionContext ctx) {
        addSymbol(ctx.SYM_RULES(), "rules section", SymbolKind.Module);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterTerminology_section(AdlParser.Terminology_sectionContext ctx) {
        addSymbol(ctx.SYM_TERMINOLOGY(), "terminology section", SymbolKind.Module);

    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterAnnotations_section(AdlParser.Annotations_sectionContext ctx) {
        addSymbol(ctx.SYM_ANNOTATIONS(), "annotations section", SymbolKind.Module);
    }

    @Override public void enterC_complex_object(AdlParser.C_complex_objectContext ctx) {
        if(ctx.ID_CODE() != null) {
            addSymbol(ctx.type_id().ALPHA_UC_ID(), "complex object " + ctx.type_id().getText() + "[" + ctx.ID_CODE().getText() + "]", SymbolKind.Object);
            addSymbol(ctx.ID_CODE(), "complex object " + ctx.type_id().getText() + "[" + ctx.ID_CODE().getText() + "]", SymbolKind.Object);
        } else if (ctx.ROOT_ID_CODE() != null) {
            addSymbol(ctx.type_id().ALPHA_UC_ID(), "complex object " + ctx.type_id().getText() + "[" + ctx.ROOT_ID_CODE().getText() + "]", SymbolKind.Object);
            addSymbol(ctx.ROOT_ID_CODE(), "[" + ctx.ROOT_ID_CODE().getText() + "]", SymbolKind.Key);
        }
        if(ctx.SYM_MATCHES() != null) {
            addSymbol(ctx.SYM_MATCHES(), ctx.SYM_MATCHES().getText(), SymbolKind.Operator);
        }
    }

    @Override public void enterC_attribute(AdlParser.C_attributeContext ctx) {

    }

    private void addSymbol(TerminalNode node, String tokenName, SymbolKind symbolKind) {

        if(!this.visitedTokens.contains(node.getSymbol())) {
            SymbolInformation symbol = createSymbolInformation(tokenName, symbolKind, createRange(node.getSymbol()));
            symbols.add(Either.forLeft(symbol));
            visitedTokens.add(node.getSymbol());
        }
    }


    public String getArchetypeId() {
        return archetypeId;
    }
}
