package com.nedap.openehr.lsp.symbolextractor.adl14;

import com.nedap.archie.adlparser.antlr.Adl14Lexer;
import com.nedap.archie.adlparser.antlr.Adl14Parser;
import com.nedap.archie.antlr.errors.ArchieErrorListener;
import com.nedap.openehr.lsp.document.ADLVersion;
import com.nedap.openehr.lsp.document.DocumentInformation;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;

public class ADL14SymbolExtractor {

    private String archetypeId;

    public ADL14SymbolExtractor() {

    }

    public DocumentInformation extractSymbols(String uri, String text) throws IOException {
        Adl14Lexer lexer = new Adl14Lexer(CharStreams.fromString(text));
        Adl14Parser parser = new Adl14Parser(new CommonTokenStream(lexer));
        ArchieErrorListener listener = new ArchieErrorListener();
        parser.addErrorListener(listener);
        ADL14SymbolInformationExtractingListener symbolExtractingListener = new ADL14SymbolInformationExtractingListener(uri, lexer);
        //DocumentSymbolExtractingListener symbolExtractingListener = new DocumentSymbolExtractingListener();
        try {
            new ParseTreeWalker().walk(symbolExtractingListener, parser.adl());
        } catch (Exception e) {
            //this is fine. for now
            e.printStackTrace();
        }
        archetypeId = symbolExtractingListener.getArchetypeId();
        return new DocumentInformation(archetypeId, ADLVersion.VERSION_1_4, listener.getErrors(),
                symbolExtractingListener.getSymbols(),
                symbolExtractingListener.getFoldingRanges(),
                symbolExtractingListener.getDocumentLinks());
    }

    public String getArchetypeId() {
        return archetypeId;
    }
}
