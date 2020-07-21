package com.nedap.openehr.lsp.utils;

import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DocumentSymbolUtils {

    public static List<DocumentSymbol> getDocumentSymbols(List<Either<SymbolInformation, DocumentSymbol>> symbols) {
        return symbols.stream().map(s -> s.getRight()).collect(Collectors.toList());
    }

    public static Optional<DocumentSymbol> getDocumentSymbol(List<DocumentSymbol> symbols, String name) {
        return symbols.stream().filter(s -> s.getName().equalsIgnoreCase(name)).findAny();
    }

    public static DocumentSymbol getDocumentSymbolOrThrow(List<DocumentSymbol> symbols, String name) {
        Optional<DocumentSymbol> symbol = getDocumentSymbol(symbols, name);
        if (!symbol.isPresent()) {
            throw new RuntimeException(name + " symbol not found");
        }
        return symbol.get();
    }
}
