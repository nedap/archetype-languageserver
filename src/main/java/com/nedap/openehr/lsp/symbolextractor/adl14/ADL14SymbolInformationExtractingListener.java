package com.nedap.openehr.lsp.symbolextractor.adl14;

import com.nedap.archie.adlparser.antlr.Adl14BaseListener;
import com.nedap.archie.adlparser.antlr.Adl14Lexer;
import com.nedap.archie.adlparser.antlr.Adl14Parser;
import com.nedap.openehr.lsp.symbolextractor.StackAction;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ADL14SymbolInformationExtractingListener extends Adl14BaseListener {
    private final String documentUri;
    private String archetypeId;
    private final List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();
    private final Set<Token> visitedTokens = new LinkedHashSet<>();
    private List<DocumentLink> documentLinks = new ArrayList<>();
    private List<FoldingRange> foldingRanges = new ArrayList<>();

    private Map<String, List<LocationLink>> idCodeToTerminologyLocations = new ConcurrentHashMap<>();

    private Stack<DocumentSymbol> symbolStack = new Stack<>();

    public ADL14SymbolInformationExtractingListener(String documentUri, Adl14Lexer lexer) {
        this.documentUri = documentUri;
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
     * Enter a parse tree produced by {@link Adl14Parser#archetype}.
     * @param ctx the parse tree
     */
    @Override
    public void enterArchetype(Adl14Parser.ArchetypeContext ctx) {
        addSymbol(ctx.SYM_ARCHETYPE(), ctx, "archetype", SymbolKind.Constant, StackAction.PUSH);
        addSymbol(ctx.ARCHETYPE_HRID(), null, "archetype id", SymbolKind.File);
        this.archetypeId = ctx.ARCHETYPE_HRID().getText();
    }

    @Override
    public void exitArchetype(Adl14Parser.ArchetypeContext ctx) {

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

    private void popStack() {
        if(!symbolStack.isEmpty()) {
            symbolStack.pop();
        }
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
    @Override public void enterSpecialization_section(Adl14Parser.Specialization_sectionContext ctx) {
        addSymbol(ctx.SYM_SPECIALIZE(), ctx, "specialization section", SymbolKind.Module, StackAction.PUSH);
        TerminalNode terminalNode = ctx.archetype_ref().ARCHETYPE_HRID();
        if(terminalNode == null) {
            terminalNode = ctx.archetype_ref().ARCHETYPE_REF();
        }
        DocumentLink documentLink = new DocumentLink(this.createRange(terminalNode.getSymbol()));
        documentLink.setData(terminalNode.getText()); //we'll resolve the target later
        documentLinks.add(documentLink);
        addFoldingRange(ctx);
    }

    @Override public void exitSpecialization_section(Adl14Parser.Specialization_sectionContext ctx) {
        popStack();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterLanguage_section(Adl14Parser.Language_sectionContext ctx) {
        addSymbol(ctx.SYM_LANGUAGE(), ctx, "language section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitLanguage_section(Adl14Parser.Language_sectionContext ctx) {
        popStack();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterDescription_section(Adl14Parser.Description_sectionContext ctx) {
        addSymbol(ctx.SYM_DESCRIPTION(), ctx, "description section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override public void exitDescription_section(Adl14Parser.Description_sectionContext ctx) {
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
    @Override public void enterDefinition_section(Adl14Parser.Definition_sectionContext ctx) {
        addSymbol(ctx.SYM_DEFINITION(), ctx, "definition section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitDefinition_section(Adl14Parser.Definition_sectionContext ctx) {
        popStack();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterRules_section(Adl14Parser.Rules_sectionContext ctx) {
        addSymbol(ctx.SYM_RULES(), ctx, "rules section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override public void exitRules_section(Adl14Parser.Rules_sectionContext ctx) {
        popStack();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterTerminology_section(Adl14Parser.Terminology_sectionContext ctx) {
        addSymbol(ctx.SYM_TERMINOLOGY(), ctx, "terminology section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result

    }

    @Override public void exitTerminology_section(Adl14Parser.Terminology_sectionContext ctx) {
        popStack();
    }

    @Override public void enterC_complex_object(Adl14Parser.C_complex_objectContext ctx) {
        if(ctx.atTypeId() != null) {
            addSymbol(ctx.type_id().ALPHA_UC_ID(), ctx, ctx.type_id().getText() + "[" + ctx.atTypeId().getText() + "]", SymbolKind.Class, StackAction.PUSH);
            // addSymbol(ctx.ID_CODE(), "complex object " + ctx.type_id().getText() + "[" + ctx.ID_CODE().getText() + "]", SymbolKind.Object);
        }
        if(ctx.SYM_MATCHES() != null) {
            //     addSymbol(ctx.SYM_MATCHES(), ctx.SYM_MATCHES().getText(), SymbolKind.Operator);
        }
        addFoldingRange(ctx);
    }

    @Override public void exitC_complex_object(Adl14Parser.C_complex_objectContext ctx) {
        popStack();
    }

    @Override public void enterC_attribute(Adl14Parser.C_attributeContext ctx) {
        if(ctx.ADL_PATH() != null) {
            addSymbol(ctx.ADL_PATH(), ctx, ctx.ADL_PATH().getText(), SymbolKind.Field, StackAction.PUSH);
        } else if(ctx.attribute_id() != null) {
            addSymbol(ctx.attribute_id().ALPHA_LC_ID(), ctx, ctx.attribute_id().getText(), SymbolKind.Field, StackAction.PUSH);
        }
        addFoldingRange(ctx);
    }

    @Override public void exitC_attribute(Adl14Parser.C_attributeContext ctx) {
        popStack();
    }

    @Override public void enterArchetype_slot(Adl14Parser.Archetype_slotContext ctx) {
        addFoldingRange(ctx);
    }

    @Override public void enterC_archetype_root(Adl14Parser.C_archetype_rootContext ctx) {
        addSymbol(ctx.SYM_USE_ARCHETYPE(), ctx, ctx.archetype_ref().getText(), SymbolKind.Class, StackAction.PUSH);
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

    @Override public void exitC_archetype_root(Adl14Parser.C_archetype_rootContext ctx) {
        popStack();
    }

    /**
     * An odin &lt; ... &gt; block
     */
    @Override public void enterObject_value_block(Adl14Parser.Object_value_blockContext ctx) {
        addFoldingRange(ctx);
    }

    Pattern idCodePattern = Pattern.compile("\"(id|at|ac)(\\d|\\.)+\"");

    @Override public void enterAttr_val(Adl14Parser.Attr_valContext ctx) {
        if(!symbolStack.isEmpty()) {
            DocumentSymbol parent = symbolStack.peek();
            if(parent.getKind() == SymbolKind.Key && idCodePattern.matcher(parent.getName()).matches()) {
                //do not add things like 'text' and 'description', they aren't useful at all!
                //TODO: maybe move this to a post-processor?
                if(ctx.attribute_id().getText().equalsIgnoreCase("text")) {
                    if(ctx.object_block() != null) {
                        parent.setDetail(ctx.object_block().getText());
                    }
                }
            } else {
                addSymbol(ctx.attribute_id().ALPHA_LC_ID(), ctx, ctx.attribute_id().getText(), SymbolKind.Field, StackAction.PUSH);
            }
        } else {
            addSymbol(ctx.attribute_id().ALPHA_LC_ID(), ctx, ctx.attribute_id().getText(), SymbolKind.Field, StackAction.PUSH);
        }
    }

    @Override public void exitAttr_val(Adl14Parser.Attr_valContext ctx) {
        //TODO: move to postprocessor!
        if(!symbolStack.isEmpty()) {
            DocumentSymbol parent = symbolStack.peek();
            if(parent.getKind() == SymbolKind.Key && idCodePattern.matcher(parent.getName()).matches()) {
            } else {
                popStack();
            }
        } else {
            popStack();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterKeyed_object(Adl14Parser.Keyed_objectContext ctx) {
        String key = ctx.primitive_value().getText();
        addSymbol(ctx.primitive_value(), ctx, key, SymbolKind.Key, StackAction.PUSH);
        if(idCodePattern.matcher(key).matches()) {
            List<LocationLink> locationLinks = idCodeToTerminologyLocations.get(key);
            if(locationLinks == null) {
                locationLinks = new ArrayList<>();
                idCodeToTerminologyLocations.put(key, locationLinks);
            }
            locationLinks.add(new LocationLink(documentUri, createRange(ctx), createRange(ctx.primitive_value())));
        }
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitKeyed_object(Adl14Parser.Keyed_objectContext ctx) {
        popStack();
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

    private void addSymbol(ParserRuleContext node, ParserRuleContext entireRule, String tokenName, SymbolKind symbolKind, StackAction stackAction) {

        DocumentSymbol symbol = createSymbolInformation(tokenName, symbolKind, createRange(node));

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
    }


    public String getArchetypeId() {
        return archetypeId;
    }

    public Map<String, List<LocationLink>> getIdCodeToTerminologyLocations() {
        return idCodeToTerminologyLocations;
    }

    public void setIdCodeToTerminologyLocations(Map<String, List<LocationLink>> idCodeToTerminologyLocations) {
        this.idCodeToTerminologyLocations = idCodeToTerminologyLocations;
    }
}
