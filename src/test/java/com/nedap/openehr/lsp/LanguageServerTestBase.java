package com.nedap.openehr.lsp;

import com.google.gson.JsonPrimitive;
import org.apache.commons.io.IOUtils;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class LanguageServerTestBase {

    protected TestClient testClient;
    protected ADL2LanguageServer adl2LanguageServer;
    protected TextDocumentService textDocumentService;
    protected InitializeResult initializeResult;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException {
        testClient = new TestClient();
        adl2LanguageServer = new ADL2LanguageServer();
        adl2LanguageServer.connect(testClient, null);
        InitializeParams initializeParams = new InitializeParams();
        InitializedParams initializedParams = new InitializedParams();
        initializeResult = adl2LanguageServer.initialize(initializeParams).get();
        adl2LanguageServer.initialized(initializedParams);
        textDocumentService = adl2LanguageServer.getTextDocumentService();
    }

    protected void openResource(String filename) throws IOException {
        DidOpenTextDocumentParams didOpenTextDocumentParams = new DidOpenTextDocumentParams();
        String archetype;
        try (InputStream stream = getClass().getResourceAsStream(filename)) {
            archetype = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
        }

        didOpenTextDocumentParams.setTextDocument(new TextDocumentItem(filename, "ADL", 1, archetype));
        textDocumentService.didOpen(didOpenTextDocumentParams);
    }

    protected void executeCommand(Command command) {
        List<Object> arguments = command.getArguments().stream().map(a -> new JsonPrimitive((String) a)).collect(Collectors.toList());
        adl2LanguageServer.getWorkspaceService().executeCommand(new ExecuteCommandParams(command.getCommand(), arguments));
    }
}
