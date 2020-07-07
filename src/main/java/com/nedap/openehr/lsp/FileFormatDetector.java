package com.nedap.openehr.lsp;

import org.eclipse.lsp4j.TextDocumentItem;

public class FileFormatDetector {
    public FileFormat detectFileFormat(TextDocumentItem item) {
        if(item.getUri().endsWith(".aql") || item.getUri().endsWith(".aql/")) {
            return FileFormat.AQL;
        }
        return FileFormat.ADL;
    }

}
