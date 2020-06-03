package com.nedap.openehr.lsp.symbolextractor;

import com.nedap.archie.adlparser.antlr.AdlBaseListener;
import com.nedap.archie.adlparser.antlr.AdlParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.lsp4j.DocumentSymbol;
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

public class DocumentSymbolExtractingListener extends AdlBaseListener {
    List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();
    private Set<Token> visitedTokens = new LinkedHashSet<>();

    private Stack<DocumentSymbol> symbolStack = new Stack<>();

    public List<Either<SymbolInformation, DocumentSymbol>> getSymbols() {
        return this.symbols;
    }

    /**
     * Enter a parse tree produced by {@link AdlParser#archetype}.
     * @param ctx the parse tree
     */
    @Override
    public void enterArchetype(AdlParser.ArchetypeContext ctx) {
        DocumentSymbol archetypeSection = createAndAddDocumentSymbol("Archetype section", SymbolKind.Module, createRange(ctx));
        archetypeSection.setSelectionRange(createRange(ctx.SYM_ARCHETYPE().getSymbol()));
        archetypeSection.getChildren().add(createDocumentSymbol(ctx.SYM_ARCHETYPE(), "archetype start token", SymbolKind.Constant));
        visitedTokens.add(ctx.SYM_ARCHETYPE().getSymbol());
        visitedTokens.add(ctx.ARCHETYPE_HRID().getSymbol());
        archetypeSection.getChildren().add(createDocumentSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class));
        symbols.add(Either.forRight(archetypeSection));
    }

    @Override
    public void exitArchetype(AdlParser.ArchetypeContext ctx) {
        symbolStack.pop();
    }

    private Range createRange(ParserRuleContext ctx) {
        return new Range(
                new Position(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine()),
                new Position(ctx.getStop().getLine(), ctx.getStop().getCharPositionInLine())
        );
    }

    private DocumentSymbol createDocumentSymbol(TerminalNode node, String name, SymbolKind kind) {
        DocumentSymbol result = createDocumentSymbol(name, kind, createRange(node.getSymbol()));
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
        if(visitedTokens.contains(node.getSymbol())) {
            return;
        }
        if(!symbolStack.isEmpty()) {
            symbolStack.peek().getChildren().add(createDocumentSymbol(node, "" + node.getSymbol().getType(), SymbolKind.String));
            symbols.add(Either.forRight(createDocumentSymbol(node, "" + node.getSymbol().getType(), SymbolKind.String)));
        } else {
            symbols.add(Either.forRight(createDocumentSymbol(node, "" + node.getSymbol().getType(), SymbolKind.String)));
        }
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void visitErrorNode(ErrorNode node) { }

    @Override public void enterTemplate(AdlParser.TemplateContext ctx) {
        DocumentSymbol archetypeSection = createAndAddDocumentSymbol("Archetype section", SymbolKind.Module, createRange(ctx));
        archetypeSection.setSelectionRange(createRange(ctx.SYM_TEMPLATE().getSymbol()));

        archetypeSection.getChildren().add(createDocumentSymbol(ctx.SYM_TEMPLATE(), "archetype start token", SymbolKind.Constant));
        archetypeSection.getChildren().add(createDocumentSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class));
        symbols.add(Either.forRight(archetypeSection));
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitTemplate(AdlParser.TemplateContext ctx) {
        symbolStack.pop();
    }


    @Override public void enterTemplate_overlay(AdlParser.Template_overlayContext ctx) {
        DocumentSymbol archetypeSection = createAndAddDocumentSymbol("Template overlay section", SymbolKind.Module, createRange(ctx));
        archetypeSection.setSelectionRange(createRange(ctx.SYM_TEMPLATE_OVERLAY().getSymbol()));

        archetypeSection.getChildren().add(createDocumentSymbol(ctx.SYM_TEMPLATE_OVERLAY(), "archetype start token", SymbolKind.Constant));
        archetypeSection.getChildren().add(createDocumentSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class));
        symbols.add(Either.forRight(archetypeSection));
    }

    @Override public void exitTemplate_overlay(AdlParser.Template_overlayContext ctx) {
        symbolStack.pop();
    }

    @Override public void enterOperational_template(AdlParser.Operational_templateContext ctx) {
        DocumentSymbol archetypeSection = createAndAddDocumentSymbol("operational template section", SymbolKind.Module, createRange(ctx));
        archetypeSection.setSelectionRange(createRange(ctx.SYM_OPERATIONAL_TEMPLATE().getSymbol()));

        archetypeSection.getChildren().add(createDocumentSymbol(ctx.SYM_OPERATIONAL_TEMPLATE(), "archetype start token", SymbolKind.Constant));
        archetypeSection.getChildren().add(createDocumentSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class));
        symbols.add(Either.forRight(archetypeSection));
    }

    private DocumentSymbol createAndAddDocumentSymbol(String s, SymbolKind module, Range range) {
        DocumentSymbol documentSymbol = createDocumentSymbol(s, module, range);
        symbolStack.push(documentSymbol);
        return documentSymbol;
    }

    private DocumentSymbol createDocumentSymbol(String s, SymbolKind module, Range range) {
        DocumentSymbol documentSymbol = new DocumentSymbol();

        documentSymbol.setName(s);
        documentSymbol.setKind(module);

        documentSymbol.setRange(range);
        if(!symbolStack.isEmpty()) {
            DocumentSymbol parent = symbolStack.peek();
            parent.getChildren().add(documentSymbol);
        }

        documentSymbol.setChildren(new ArrayList<>());

        return documentSymbol;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitOperational_template(AdlParser.Operational_templateContext ctx) {
        symbolStack.pop();
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
    @Override public void exitSpecialization_section(AdlParser.Specialization_sectionContext ctx) { }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterLanguage_section(AdlParser.Language_sectionContext ctx) {
        DocumentSymbol archetypeSection = createAndAddDocumentSymbol("language section", SymbolKind.Module, createRange(ctx));
        archetypeSection.setSelectionRange(createRange(ctx.SYM_LANGUAGE().getSymbol()));
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitLanguage_section(AdlParser.Language_sectionContext ctx) {
        symbolStack.pop();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterDescription_section(AdlParser.Description_sectionContext ctx) {
        DocumentSymbol archetypeSection = createAndAddDocumentSymbol("description section", SymbolKind.Module, createRange(ctx));
        archetypeSection.setSelectionRange(createRange(ctx.SYM_DESCRIPTION().getSymbol()));

    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitDescription_section(AdlParser.Description_sectionContext ctx) {
        symbolStack.pop();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterDefinition_section(AdlParser.Definition_sectionContext ctx) {
        DocumentSymbol archetypeSection = createAndAddDocumentSymbol("definition section", SymbolKind.Module, createRange(ctx));
        archetypeSection.setSelectionRange(createRange(ctx.SYM_DEFINITION().getSymbol()));
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitDefinition_section(AdlParser.Definition_sectionContext ctx) {
        symbolStack.pop();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterRules_section(AdlParser.Rules_sectionContext ctx) {
        DocumentSymbol archetypeSection = createAndAddDocumentSymbol("rules section", SymbolKind.Module, createRange(ctx));
        archetypeSection.setSelectionRange(createRange(ctx.SYM_RULES().getSymbol()));
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitRules_section(AdlParser.Rules_sectionContext ctx) {
        symbolStack.pop();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterTerminology_section(AdlParser.Terminology_sectionContext ctx) {
        DocumentSymbol archetypeSection = createAndAddDocumentSymbol("terminology section", SymbolKind.Module, createRange(ctx));
        archetypeSection.setSelectionRange(createRange(ctx.SYM_TERMINOLOGY().getSymbol()));
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitTerminology_section(AdlParser.Terminology_sectionContext ctx) {
        symbolStack.pop();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterAnnotations_section(AdlParser.Annotations_sectionContext ctx) {
        DocumentSymbol archetypeSection = createAndAddDocumentSymbol("annotations section", SymbolKind.Module, createRange(ctx));
        archetypeSection.setSelectionRange(createRange(ctx.SYM_ANNOTATIONS().getSymbol()));
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitAnnotations_section(AdlParser.Annotations_sectionContext ctx) {
        symbolStack.pop();
    }

}
