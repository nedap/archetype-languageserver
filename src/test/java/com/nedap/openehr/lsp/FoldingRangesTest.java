package com.nedap.openehr.lsp;

import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FoldingRangesTest extends LanguageServerTestBase {

    @Test
    public void testFoldingRanges() throws Exception {
        openResource("test_archetype.adls");
        List<FoldingRange> foldingRanges = adl2LanguageServer.getTextDocumentService().foldingRange(new FoldingRangeRequestParams(new TextDocumentIdentifier("uri"))).get();
        System.out.println(foldingRanges);
        List<FoldingRange> expected = new ArrayList<>();
        expected.add(new FoldingRange(3, 4)); //language section
        expected.add(new FoldingRange(6, 14)); //description section
        expected.add(new FoldingRange(9, 14)); //description detail
        expected.add(new FoldingRange(10, 13)); //description "nl" details

        expected.add(new FoldingRange(16, 27)); //definition
        expected.add(new FoldingRange(17, 27)); //root node
        expected.add(new FoldingRange(18, 26)); //items attribute
        expected.add(new FoldingRange(19, 25)); //ELEMENT[id2]
        expected.add(new FoldingRange(20, 24)); //value
        expected.add(new FoldingRange(21, 23)); //DV_CODED_TEXT

        expected.add(new FoldingRange(29, 45)); //terminology section
        expected.add(new FoldingRange(30, 45)); //term_definitions
        expected.add(new FoldingRange(31, 44)); //"nl"
        expected.add(new FoldingRange(32, 35)); //"id1"
        expected.add(new FoldingRange(36, 39)); //"id2"
        expected.add(new FoldingRange(40, 43)); //"at4"

        assertEquals(expected, foldingRanges.subList(0, expected.size()));
    }

}
