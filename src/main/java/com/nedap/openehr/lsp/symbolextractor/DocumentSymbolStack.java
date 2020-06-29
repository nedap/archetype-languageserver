package com.nedap.openehr.lsp.symbolextractor;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import static com.nedap.openehr.lsp.utils.ANTLRUtils.createRange;

public class DocumentSymbolStack {

    private Stack<DocumentSymbol> symbolStack = new Stack<>();
    private final List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();
    private final Set<Token> visitedTokens = new LinkedHashSet<>();

    public DocumentSymbol pop() {
        if(!symbolStack.isEmpty()) {
            return symbolStack.pop();
        }
        return null;
    }

    public void addSymbol(TerminalNode node, String tokenName, SymbolKind symbolKind) {
        addSymbol(node, null, tokenName, symbolKind);
    }

    public void addSymbol(TerminalNode node, ParserRuleContext entireRule, String tokenName, SymbolKind symbolKind) {
        addSymbol(node, entireRule, tokenName, symbolKind, StackAction.NOTHING);
    }

    public void addSymbol(TerminalNode node, ParserRuleContext entireRule, String tokenName, SymbolKind symbolKind, StackAction stackAction) {

        if(!this.visitedTokens.contains(node.getSymbol())) {
            DocumentSymbol symbol = createSymbolInformation(tokenName, symbolKind, createRange(node.getSymbol()));

            if(node.getText().startsWith("\n")) {
                Range range = symbol.getRange();
                range.getStart().setLine(range.getStart().getLine()+1);
                range.getEnd().setLine(range.getEnd().getLine()+1);
            }
            if(entireRule != null) {
                Range range = createRange(entireRule, node);
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

    public void addSymbol(ParserRuleContext node, ParserRuleContext entireRule, String tokenName, SymbolKind symbolKind, StackAction stackAction) {

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

    public DocumentSymbol createSymbolInformation(String name, SymbolKind symbolKind, Range range) {
        DocumentSymbol documentSymbol = new DocumentSymbol();

        documentSymbol.setName(name);
        documentSymbol.setKind(symbolKind);

        documentSymbol.setRange(range);
        documentSymbol.setSelectionRange(range);

        return documentSymbol;
    }

    public List<Either<SymbolInformation, DocumentSymbol>> getSymbols() {
        return symbols;
    }

    public DocumentSymbol peek() {
        return symbolStack.peek();
    }

    public boolean isEmpty() {
        return symbolStack.isEmpty();
    }
}
