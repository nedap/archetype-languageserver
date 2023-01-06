package com.nedap.openehr.lsp;

import org.eclipse.lsp4j.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.nedap.openehr.lsp.TestArchetypes.ARCHETYPE_WITH_RULES_ADLS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RulesCodeLensTest extends LanguageServerTestBase {

    @Test
    public void testRulesCodeLens() throws IOException, ExecutionException, InterruptedException {

        openResource(ARCHETYPE_WITH_RULES_ADLS.getFilename());
        System.out.println(testClient.getDiagnostics());
        CompletableFuture<List<? extends CodeLens>> uri = adl2LanguageServer.getTextDocumentService().codeLens(
                new CodeLensParams(new TextDocumentIdentifier(ARCHETYPE_WITH_RULES_ADLS.getFilename())));
        List<? extends CodeLens> codeLens = uri.get();
        assertEquals(2, codeLens.size());
        CodeLens lens1 = codeLens.get(0);
        CodeLens lens2 = codeLens.get(1);

        assertEquals(new Range(new Position(37, 11), new Position(37,42)), lens1.getRange());
        assertEquals("Element 1/value\n\nIn Archetype A test cluster (openEHR-EHR-CLUSTER.simple_sum.v0.0.1)", lens1.getCommand().getArguments().get(0));

        assertEquals(new Range(new Position(37, 62), new Position(37,73)), lens2.getRange());
        assertEquals("Element 2\n\nIn Archetype A test cluster (openEHR-EHR-CLUSTER.simple_sum.v0.0.1)", lens2.getCommand().getArguments().get(0));

        CompletableFuture<Hover> hover = adl2LanguageServer.getTextDocumentService().hover(
                new HoverParams(new TextDocumentIdentifier(ARCHETYPE_WITH_RULES_ADLS.getFilename()), new Position(37, 30)));
        Hover hover1 = hover.get();
        assertEquals("markdown", hover1.getContents().getRight().getKind());
        assertEquals("Element 1/value/defining_code\n" +
                "\n" +
                "\n" +
                "\n" +
                "Element 1/value\n" +
                "\n" +
                "In Archetype A test cluster (openEHR-EHR-CLUSTER.simple_sum.v0.0.1)", hover1.getContents().getRight().getValue());
        assertEquals(new Range(new Position(37, 11), new Position(37,42)), hover1.getRange());
    }
}
