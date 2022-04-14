package com.nedap.openehr.lsp;

import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RulesCodeLensTest extends LanguageServerTestBase {

    @Test
    public void testRulesCodeLens() throws IOException, ExecutionException, InterruptedException {

        openResource("archetype_with_rules.adls");
        System.out.println(testClient.getDiagnostics());
        CompletableFuture<List<? extends CodeLens>> uri = adl2LanguageServer.getTextDocumentService().codeLens(new CodeLensParams(new TextDocumentIdentifier("uri")));
        List<? extends CodeLens> codeLens = uri.get();
        assertEquals(2, codeLens.size());
        CodeLens lens1 = codeLens.get(0);
        CodeLens lens2 = codeLens.get(1);
        System.out.println(lens1);
        assertEquals(37, lens1.getRange().getStart().getLine());
        assertEquals(11, lens1.getRange().getStart().getCharacter());
        assertEquals(37, lens1.getRange().getEnd().getLine());
        assertEquals(42, lens1.getRange().getEnd().getCharacter());
        assertEquals("Element 1/value\n\nIn Archetype A test cluster (openEHR-EHR-CLUSTER.simple_sum.v0.0.1)", lens1.getCommand().getArguments().get(0));

        assertEquals(37, lens2.getRange().getStart().getLine());
        assertEquals(62, lens2.getRange().getStart().getCharacter());
        assertEquals(37, lens2.getRange().getEnd().getLine());
        assertEquals(73, lens2.getRange().getEnd().getCharacter());
        assertEquals("Element 2\n\nIn Archetype A test cluster (openEHR-EHR-CLUSTER.simple_sum.v0.0.1)", lens2.getCommand().getArguments().get(0));
    }
}
