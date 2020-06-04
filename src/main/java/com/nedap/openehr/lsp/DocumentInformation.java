package com.nedap.openehr.lsp;

import com.nedap.archie.antlr.errors.ANTLRParserErrors;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.List;

public class DocumentInformation {
    private String archetypeId;
    private ANTLRParserErrors errors;
    private List<Either<SymbolInformation, DocumentSymbol>> symbols;

    public DocumentInformation(String archetypeId, ANTLRParserErrors errors, List<Either<SymbolInformation, DocumentSymbol>> symbols) {
        this.archetypeId = archetypeId;
        this.errors = errors;
        this.symbols = symbols;
    }

    public String getArchetypeId() {
        return archetypeId;
    }

    public List<Either<SymbolInformation, DocumentSymbol>> getSymbols() {
        return symbols;
    }

    public ANTLRParserErrors getErrors() {
        return errors;
    }
}
