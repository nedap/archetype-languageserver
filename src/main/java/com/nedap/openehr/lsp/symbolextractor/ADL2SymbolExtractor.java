package com.nedap.openehr.lsp.symbolextractor;

import com.nedap.archie.adlparser.antlr.AdlLexer;
import com.nedap.archie.adlparser.antlr.AdlParser;
import com.nedap.archie.antlr.errors.ArchieErrorListener;
import com.nedap.openehr.lsp.document.DocumentInformation;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;

public class ADL2SymbolExtractor {

    private String archetypeId;

    public ADL2SymbolExtractor() {

    }

    public DocumentInformation extractSymbols(String uri, String text) throws IOException {
        AdlLexer lexer = new AdlLexer(CharStreams.fromString(text));
        AdlParser parser = new AdlParser(new CommonTokenStream(lexer));
        ArchieErrorListener listener = new ArchieErrorListener();
        parser.addErrorListener(listener);
        SymbolInformationExtractingListener symbolExtractingListener = new SymbolInformationExtractingListener(uri, lexer);
        //DocumentSymbolExtractingListener symbolExtractingListener = new DocumentSymbolExtractingListener();
        try {
            new ParseTreeWalker().walk(symbolExtractingListener, parser.adl());
        } catch (Exception e) {
            //this is fine. for now
            e.printStackTrace();
        }
        archetypeId = symbolExtractingListener.getArchetypeId();
        return new DocumentInformation(archetypeId, listener.getErrors(),
                symbolExtractingListener.getSymbols(),
                symbolExtractingListener.getFoldingRanges(),
                symbolExtractingListener.getDocumentLinks());
    }

    public String getArchetypeId() {
        return archetypeId;
    }
}
