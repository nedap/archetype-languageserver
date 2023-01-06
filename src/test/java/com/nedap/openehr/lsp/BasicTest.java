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

import static com.nedap.openehr.lsp.TestArchetypes.*;
import static org.junit.jupiter.api.Assertions.*;

public class BasicTest extends LanguageServerTestBase {



    @Test
    public void testBasics() throws IOException {

        openResource(TEST_ARCHETYPE_ADLS.getFilename());
        System.out.println(testClient.getDiagnostics());
        assertTrue(testClient.getDiagnostics().get(TEST_ARCHETYPE_ADLS.getFilename()).getDiagnostics().isEmpty());
    }

    @Test
    public void syntaxError() throws IOException {

        openResource(SYNTAX_ERROR_ADLS.getFilename());
        System.out.println(testClient.getDiagnostics());
        List<Diagnostic> diagnostics = testClient.getDiagnostics().get(SYNTAX_ERROR_ADLS.getFilename()).getDiagnostics();
        assertFalse(diagnostics.isEmpty());
        Diagnostic diagnostic = diagnostics.get(0);
        assertEquals("ADL2 syntax", diagnostic.getSource());
        assertEquals(new Position(17, 18), diagnostic.getRange().getStart());
        assertEquals(new Position(17, 53), diagnostic.getRange().getEnd());
        assertTrue(diagnostic.getMessage().contains("matchess"));
    }

    /**
     * test that a template error gets syntax highlighting at the correct range
     * @throws IOException
     */
    @Test
    public void templateErrorInDefinition() throws IOException {
        openResource(TEST_ARCHETYPE_ADLS.getFilename());
        openResource(TEMPLATE_DEFINITION_ERROR_IN_OVL_ADLT.getFilename());
        System.out.println(testClient.getDiagnostics());
        List<Diagnostic> diagnostics = testClient.getDiagnostics().get(TEMPLATE_DEFINITION_ERROR_IN_OVL_ADLT.getFilename()).getDiagnostics();
        assertFalse(diagnostics.isEmpty());
        //the error that one of the template overlay validations failed
        Diagnostic prettyMuchUselessError = diagnostics.get(0);
        assertEquals("ADL validation", prettyMuchUselessError.getSource());
        assertEquals(new Position(25, 4), prettyMuchUselessError.getRange().getStart());
        assertEquals(new Position(25, 11), prettyMuchUselessError.getRange().getEnd());
        assertTrue(prettyMuchUselessError.getMessage().contains("The validation of a template overlay failed"));
        //the actual error
        Diagnostic diagnostic = diagnostics.get(1);
        assertEquals("ADL validation", diagnostic.getSource());
        assertEquals(new Position(47, 12), diagnostic.getRange().getStart());
        assertEquals(new Position(47, 19), diagnostic.getRange().getEnd());
        assertTrue(diagnostic.getMessage().contains("Attribute CLUSTER.items cannot contain type DV_TEXT"));
    }

    /**
     * test that a template error gets syntax highlighting at the correct range
     * @throws IOException
     */
    @Test
    public void templateErrorElsewhere() throws IOException {
        openResource(TEST_ARCHETYPE_ADLS.getFilename());
        openResource(TEMPLATE_NON_DEFINITION_ERROR_ADLT.getFilename());
        System.out.println(testClient.getDiagnostics());
        List<Diagnostic> diagnostics = testClient.getDiagnostics().get(TEMPLATE_NON_DEFINITION_ERROR_ADLT.getFilename()).getDiagnostics();
        assertFalse(diagnostics.isEmpty());
        //the error that one of the template overlay validations failed
        Diagnostic prettyMuchUselessError = diagnostics.get(0);
        assertEquals("ADL validation", prettyMuchUselessError.getSource());
        assertEquals(new Position(25, 4), prettyMuchUselessError.getRange().getStart());
        assertEquals(new Position(25, 11), prettyMuchUselessError.getRange().getEnd());
        assertTrue(prettyMuchUselessError.getMessage().contains("The validation of a template overlay failed"));
        //the actual error
        Diagnostic diagnostic = diagnostics.get(1);
        assertEquals("ADL validation", diagnostic.getSource());
        assertEquals(new Position(39, 4), diagnostic.getRange().getStart());
        assertEquals(new Position(39, 45), diagnostic.getRange().getEnd());
        assertTrue(diagnostic.getMessage().contains("Id code qf1.1 in terminology is not a valid term code, should be id, ac or at, followed by digits"));
    }

    @Test
    public void validationError() throws IOException {

        openResource(VALIDATION_ERROR_ADLS.getFilename());
        System.out.println(testClient.getDiagnostics());
        List<Diagnostic> diagnostics = testClient.getDiagnostics().get(VALIDATION_ERROR_ADLS.getFilename()).getDiagnostics();
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

        openResource(JSON_ERROR_ADLS.getFilename());
        System.out.println(testClient.getDiagnostics());
        List<Diagnostic> diagnostics = testClient.getDiagnostics().get(JSON_ERROR_ADLS.getFilename()).getDiagnostics();
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
        openResource(ADL14_VALID_ADL.getFilename());
        System.out.println(testClient.getDiagnostics());
        List<Diagnostic> diagnostics = testClient.getDiagnostics().get(ADL14_VALID_ADL.getFilename()).getDiagnostics();
        assertTrue(diagnostics.isEmpty());
        CompletableFuture<List<Either<Command, CodeAction>>> codeActionsFuture = adl2LanguageServer.getTextDocumentService().codeAction(new CodeActionParams(new TextDocumentIdentifier("adl14_valid.adl"), new Range(new Position(1, 1), new Position(1, 1)), new CodeActionContext()));
        List<Either<Command, CodeAction>> codeActions = codeActionsFuture.get();
        System.out.println(codeActions);
        assertEquals(2, codeActions.size());
    }

    @Test
    public void adl14WindowsFileEndings() throws Exception {
        DidOpenTextDocumentParams didOpenTextDocumentParams = new DidOpenTextDocumentParams();
        String archetype;
        try (InputStream stream = getClass().getResourceAsStream(ADL14_VALID_ADL.getFilename())) {
            archetype = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
        }
        archetype = ensureWindowsLineEndings(archetype);

        didOpenTextDocumentParams.setTextDocument(new TextDocumentItem(ADL14_VALID_ADL.getFilename(), "ADL", 1, archetype));
        textDocumentService.didOpen(didOpenTextDocumentParams);
        System.out.println(testClient.getDiagnostics());
        List<Diagnostic> diagnostics = testClient.getDiagnostics().get(ADL14_VALID_ADL.getFilename()).getDiagnostics();
        assertTrue(diagnostics.isEmpty());
        CompletableFuture<List<Either<Command, CodeAction>>> codeActionsFuture = adl2LanguageServer.getTextDocumentService().codeAction(new CodeActionParams(new TextDocumentIdentifier("adl14_valid.adl"), new Range(new Position(1, 1), new Position(1, 1)), new CodeActionContext()));
        List<Either<Command, CodeAction>> codeActions = codeActionsFuture.get();
        System.out.println(codeActions);
        assertEquals(2, codeActions.size());

    }

    @Test
    public void adl14LinuxFileEndings() throws Exception {
        DidOpenTextDocumentParams didOpenTextDocumentParams = new DidOpenTextDocumentParams();
        String archetype;
        try (InputStream stream = getClass().getResourceAsStream(ADL14_VALID_ADL.getFilename())) {
            archetype = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
        }
        archetype = normalizeLineEndings(archetype);

        didOpenTextDocumentParams.setTextDocument(new TextDocumentItem(ADL14_VALID_ADL.getFilename(), "ADL", 1, archetype));
        textDocumentService.didOpen(didOpenTextDocumentParams);
        System.out.println(testClient.getDiagnostics());
        List<Diagnostic> diagnostics = testClient.getDiagnostics().get(ADL14_VALID_ADL.getFilename()).getDiagnostics();
        assertTrue(diagnostics.isEmpty());
        CompletableFuture<List<Either<Command, CodeAction>>> codeActionsFuture = adl2LanguageServer
                .getTextDocumentService().codeAction(new CodeActionParams(new TextDocumentIdentifier(ADL14_VALID_ADL.getFilename()),
                        new Range(new Position(1, 1), new Position(1, 1)), new CodeActionContext()));
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
