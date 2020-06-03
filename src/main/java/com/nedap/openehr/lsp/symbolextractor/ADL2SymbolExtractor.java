package com.nedap.openehr.lsp.symbolextractor;

import com.nedap.archie.adlparser.antlr.AdlLexer;
import com.nedap.archie.adlparser.antlr.AdlParser;
import com.nedap.openehr.lsp.ProcessableDocument;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.io.IOException;
import java.util.List;

public class ADL2SymbolExtractor {

    private String archetypeId;

    public ADL2SymbolExtractor() {

    }

    public List<Either<SymbolInformation, DocumentSymbol>> extractSymbols(ProcessableDocument document) throws IOException {
        AdlParser parser = document.getParser();
        SymbolInformationExtractingListener symbolExtractingListener = new SymbolInformationExtractingListener(document.getDocument().getUri(), document.getLexer());
        //DocumentSymbolExtractingListener symbolExtractingListener = new DocumentSymbolExtractingListener();
        new ParseTreeWalker().walk(symbolExtractingListener, parser.adl());
        archetypeId = symbolExtractingListener.getArchetypeId();
        return symbolExtractingListener.getSymbols();
    }

    public String getArchetypeId() {
        return archetypeId;
    }
}
