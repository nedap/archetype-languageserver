package com.nedap.openehr.lsp;

import org.apache.commons.io.IOUtils;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public abstract class LanguageServerTestBase {

    protected TestClient testClient;
    protected ADL2LanguageServer adl2LanguageServer;
    protected TextDocumentService textDocumentService;

    @BeforeEach
    public void setup() {
        testClient = new TestClient();
        adl2LanguageServer = new ADL2LanguageServer();
        adl2LanguageServer.connect(testClient, null);
        InitializeParams initializeParams = new InitializeParams();
        InitializedParams initializedParams = new InitializedParams();
        adl2LanguageServer.initialize(initializeParams);
        adl2LanguageServer.initialized(initializedParams);
        textDocumentService = adl2LanguageServer.getTextDocumentService();
    }

    protected void openResource(String s) throws IOException {
        DidOpenTextDocumentParams didOpenTextDocumentParams = new DidOpenTextDocumentParams();
        String archetype;
        try (InputStream stream = getClass().getResourceAsStream(s)) {
            archetype = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
        }

        didOpenTextDocumentParams.setTextDocument(new TextDocumentItem("uri", "ADL", 1, archetype));
        textDocumentService.didOpen(didOpenTextDocumentParams);
    }
}
