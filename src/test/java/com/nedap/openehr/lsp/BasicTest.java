package com.nedap.openehr.lsp;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class BasicTest {

    TestClient testClient;
    private ADL2LanguageServer adl2LanguageServer;
    private TextDocumentService textDocumentService;

    @BeforeEach
    public void setup() {
        testClient = new TestClient();
        adl2LanguageServer = new ADL2LanguageServer();
        adl2LanguageServer.connect(testClient);
        InitializeParams initializeParams = new InitializeParams();
        InitializedParams initializedParams = new InitializedParams();
        adl2LanguageServer.initialize(initializeParams);
        adl2LanguageServer.initialized(initializedParams);
        textDocumentService = adl2LanguageServer.getTextDocumentService();
    }

    @Test
    public void testBasics() throws IOException {


        DidOpenTextDocumentParams didOpenTextDocumentParams = new DidOpenTextDocumentParams();
        String archetype;
        try(InputStream stream = getClass().getResourceAsStream("test_archetype.adls")) {
            archetype = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
        }

        didOpenTextDocumentParams.setTextDocument(new TextDocumentItem("uri", "ADL", 1, archetype));
        textDocumentService.didOpen(didOpenTextDocumentParams);
        System.out.println(testClient.getDiagnostics());
        assertTrue(testClient.getDiagnostics().get("uri").getDiagnostics().isEmpty());

    }


    @Test
    public void syntaxError() throws IOException {

        DidOpenTextDocumentParams didOpenTextDocumentParams = new DidOpenTextDocumentParams();
        String archetype;
        try(InputStream stream = getClass().getResourceAsStream("syntax_error.adls")) {
            archetype = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
        }

        didOpenTextDocumentParams.setTextDocument(new TextDocumentItem("uri", "ADL", 1, archetype));
        textDocumentService.didOpen(didOpenTextDocumentParams);
        System.out.println(testClient.getDiagnostics());
        List<Diagnostic> diagnostics = testClient.getDiagnostics().get("uri").getDiagnostics();
        assertFalse(diagnostics.isEmpty());
        Diagnostic diagnostic = diagnostics.get(0);
        assertEquals("ADL2 syntax", diagnostic.getSource());
        assertEquals(new Position(17, 18), diagnostic.getRange().getStart());
        assertEquals(new Position(17, 53), diagnostic.getRange().getEnd());
        assertTrue(diagnostic.getMessage().contains("matchess"));
    }

    @Test
    public void validationError() throws IOException {

        DidOpenTextDocumentParams didOpenTextDocumentParams = new DidOpenTextDocumentParams();
        String archetype;
        try(InputStream stream = getClass().getResourceAsStream("validation_error.adls")) {
            archetype = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
        }

        didOpenTextDocumentParams.setTextDocument(new TextDocumentItem("uri", "ADL", 1, archetype));
        textDocumentService.didOpen(didOpenTextDocumentParams);
        System.out.println(testClient.getDiagnostics());
        List<Diagnostic> diagnostics = testClient.getDiagnostics().get("uri").getDiagnostics();
        assertFalse(diagnostics.isEmpty());
        Diagnostic diagnostic = diagnostics.get(0);
        assertEquals(DiagnosticSeverity.Error, diagnostic.getSeverity());
        assertEquals("ADL validation", diagnostic.getSource());
        assertEquals(new Position(21, 20), diagnostic.getRange().getStart());
        assertEquals(new Position(21, 25), diagnostic.getRange().getEnd());
        assertTrue(diagnostic.getMessage().contains("WRONG"));
        assertTrue(diagnostic.getCode().getLeft().contains("VCORM"));
    }
}
