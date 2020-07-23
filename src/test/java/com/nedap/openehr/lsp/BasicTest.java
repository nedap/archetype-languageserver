package com.nedap.openehr.lsp;

import com.nedap.openehr.lsp.document.DocumentInformation;
import com.nedap.openehr.lsp.utils.DocumentSymbolUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

        openResource("test_archetype.adls");
        System.out.println(testClient.getDiagnostics());
        assertTrue(testClient.getDiagnostics().get("uri").getDiagnostics().isEmpty());
    }


    @Test
    public void testDocumentOutline() throws IOException, ExecutionException, InterruptedException {

        openResource("test_archetype.adls");
        CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> uri = adl2LanguageServer.getTextDocumentService().documentSymbol(new DocumentSymbolParams(new TextDocumentIdentifier("uri")));
        List<Either<SymbolInformation, DocumentSymbol>> eitherSymbols = uri.get();
        List<DocumentSymbol> documentSymbols = DocumentSymbolUtils.getDocumentSymbols(eitherSymbols);
        //System.out.println(documentSymbols);
        DocumentSymbol archetype = DocumentSymbolUtils.getDocumentSymbolOrThrow(documentSymbols, "archetype");

        DocumentSymbol definition = DocumentSymbolUtils.getDocumentSymbolOrThrow(archetype.getChildren(), DocumentInformation.DEFINITION_SECTION_NAME);
        DocumentSymbolUtils.getDocumentSymbolOrThrow(archetype.getChildren(), DocumentInformation.DESCRIPTION_SECTION_NAME);
        DocumentSymbolUtils.getDocumentSymbolOrThrow(archetype.getChildren(), DocumentInformation.LANGUAGE_SECTION_NAME);
        DocumentSymbolUtils.getDocumentSymbolOrThrow(archetype.getChildren(), DocumentInformation.TERMINOLOGY_SECTION_NAME);

        DocumentSymbol aTestCluster = DocumentSymbolUtils.getDocumentSymbolOrThrow(definition.getChildren(), "A test cluster");
        DocumentSymbol items = DocumentSymbolUtils.getDocumentSymbolOrThrow(aTestCluster.getChildren(), "items");
        assertEquals(new Range(new Position(18, 8), new Position(26, 8)), items.getRange());
        assertEquals(SymbolKind.Field, items.getKind());

    }

    private void openResource(String s) throws IOException {
        DidOpenTextDocumentParams didOpenTextDocumentParams = new DidOpenTextDocumentParams();
        String archetype;
        try (InputStream stream = getClass().getResourceAsStream(s)) {
            archetype = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
        }

        didOpenTextDocumentParams.setTextDocument(new TextDocumentItem("uri", "ADL", 1, archetype));
        textDocumentService.didOpen(didOpenTextDocumentParams);
    }


    @Test
    public void syntaxError() throws IOException {

        openResource("syntax_error.adls");
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

        openResource("validation_error.adls");
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

    @Test
    public void jsonError() throws IOException {

        openResource("json_error.adls");
        System.out.println(testClient.getDiagnostics());
        List<Diagnostic> diagnostics = testClient.getDiagnostics().get("uri").getDiagnostics();
        assertFalse(diagnostics.isEmpty());
        Diagnostic diagnostic = diagnostics.get(0);
        assertEquals(DiagnosticSeverity.Error, diagnostic.getSeverity());
        assertEquals("Error processing file", diagnostic.getSource());
        assertEquals(new Position(0, 1), diagnostic.getRange().getStart());
        assertEquals(new Position(0, 50), diagnostic.getRange().getEnd());
        assertTrue(diagnostic.getMessage().contains("com.fasterxml.jackson.databind.JsonMappingException"));
    }

    @Test
    public void adl14() throws Exception {
        openResource("adl14_valid.adl");
        System.out.println(testClient.getDiagnostics());
        List<Diagnostic> diagnostics = testClient.getDiagnostics().get("uri").getDiagnostics();
        assertTrue(diagnostics.isEmpty());
        CompletableFuture<List<Either<Command, CodeAction>>> codeActionsFuture = adl2LanguageServer.getTextDocumentService().codeAction(new CodeActionParams(new TextDocumentIdentifier("uri"), new Range(new Position(1, 1), new Position(1, 1)), new CodeActionContext()));
        List<Either<Command, CodeAction>> codeActions = codeActionsFuture.get();
        System.out.println(codeActions);
        assertEquals(2, codeActions.size());
    }

    @Test
    public void adl14WindowsFileEndings() throws Exception {
        DidOpenTextDocumentParams didOpenTextDocumentParams = new DidOpenTextDocumentParams();
        String archetype;
        try (InputStream stream = getClass().getResourceAsStream("adl14_valid.adl")) {
            archetype = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
        }
        archetype = ensureWindowsLineEndings(archetype);

        didOpenTextDocumentParams.setTextDocument(new TextDocumentItem("uri", "ADL", 1, archetype));
        textDocumentService.didOpen(didOpenTextDocumentParams);
        System.out.println(testClient.getDiagnostics());
        List<Diagnostic> diagnostics = testClient.getDiagnostics().get("uri").getDiagnostics();
        assertTrue(diagnostics.isEmpty());
        CompletableFuture<List<Either<Command, CodeAction>>> codeActionsFuture = adl2LanguageServer.getTextDocumentService().codeAction(new CodeActionParams(new TextDocumentIdentifier("uri"), new Range(new Position(1, 1), new Position(1, 1)), new CodeActionContext()));
        List<Either<Command, CodeAction>> codeActions = codeActionsFuture.get();
        System.out.println(codeActions);
        assertEquals(2, codeActions.size());

    }

    @Test
    public void adl14LinuxFileEndings() throws Exception {
        DidOpenTextDocumentParams didOpenTextDocumentParams = new DidOpenTextDocumentParams();
        String archetype;
        try (InputStream stream = getClass().getResourceAsStream("adl14_valid.adl")) {
            archetype = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
        }
        archetype = normalizeLineEndings(archetype);

        didOpenTextDocumentParams.setTextDocument(new TextDocumentItem("uri", "ADL", 1, archetype));
        textDocumentService.didOpen(didOpenTextDocumentParams);
        System.out.println(testClient.getDiagnostics());
        List<Diagnostic> diagnostics = testClient.getDiagnostics().get("uri").getDiagnostics();
        assertTrue(diagnostics.isEmpty());
        CompletableFuture<List<Either<Command, CodeAction>>> codeActionsFuture = adl2LanguageServer.getTextDocumentService().codeAction(new CodeActionParams(new TextDocumentIdentifier("uri"), new Range(new Position(1, 1), new Position(1, 1)), new CodeActionContext()));
        List<Either<Command, CodeAction>> codeActions = codeActionsFuture.get();
        System.out.println(codeActions);
        assertEquals(2, codeActions.size());

    }

    public static String ensureWindowsLineEndings(String val) {
        String normalized = normalizeLineEndings(val);
        return normalized.replaceAll("\n", "\r\n");
    }

    public static String normalizeLineEndings(String val) {
        return val.replace("\r\n", "\n")
                .replace("\r", "\n");
    }
}
