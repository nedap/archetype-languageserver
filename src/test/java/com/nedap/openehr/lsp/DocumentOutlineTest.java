package com.nedap.openehr.lsp;

import com.nedap.openehr.lsp.document.DocumentInformation;
import com.nedap.openehr.lsp.utils.DocumentSymbolUtils;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DocumentOutlineTest extends LanguageServerTestBase {

    @Test
    public void testDocumentOutline() throws IOException, ExecutionException, InterruptedException {

        openResource("test_archetype.adls");
        CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> uri = adl2LanguageServer.getTextDocumentService().documentSymbol(new DocumentSymbolParams(new TextDocumentIdentifier("test_archetype.adls")));
        List<Either<SymbolInformation, DocumentSymbol>> eitherSymbols = uri.get();
        List<DocumentSymbol> documentSymbols = DocumentSymbolUtils.getDocumentSymbols(eitherSymbols);
        //System.out.println(documentSymbols);
        DocumentSymbol archetype = DocumentSymbolUtils.getDocumentSymbolOrThrow(documentSymbols, "archetype");

        DocumentSymbol definition = DocumentSymbolUtils.getDocumentSymbolOrThrow(archetype, DocumentInformation.DEFINITION_SECTION_NAME);
        DocumentSymbolUtils.getDocumentSymbolOrThrow(archetype, DocumentInformation.DESCRIPTION_SECTION_NAME);
        DocumentSymbolUtils.getDocumentSymbolOrThrow(archetype, DocumentInformation.LANGUAGE_SECTION_NAME);
        DocumentSymbol terminology = DocumentSymbolUtils.getDocumentSymbolOrThrow(archetype, DocumentInformation.TERMINOLOGY_SECTION_NAME);

        DocumentSymbol aTestCluster = DocumentSymbolUtils.getDocumentSymbolOrThrow(definition, "A test cluster");
        DocumentSymbol items = DocumentSymbolUtils.getDocumentSymbolOrThrow(aTestCluster, "items");
        assertEquals(new Range(new Position(18, 8), new Position(26, 8)), items.getRange());
        assertEquals(SymbolKind.Field, items.getKind());

        DocumentSymbol termDefinitions = DocumentSymbolUtils.getDocumentSymbolOrThrow(terminology, "term_definitions");
        DocumentSymbol nl = DocumentSymbolUtils.getDocumentSymbolOrThrow(termDefinitions, "\"nl\"");
        DocumentSymbol id1 = DocumentSymbolUtils.getDocumentSymbolOrThrow(nl, "\"id1\"");
        assertEquals("<\"A test cluster\">", id1.getDetail());//might be better to remove the <""> here, but that's quite annoying code to write
        assertEquals(new Range(new Position(32, 12), new Position(35, 13)), id1.getRange());
    }
}
