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
}
