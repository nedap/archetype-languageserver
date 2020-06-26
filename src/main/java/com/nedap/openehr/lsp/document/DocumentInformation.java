package com.nedap.openehr.lsp.document;

import com.nedap.archie.antlr.errors.ANTLRParserErrors;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.List;

public class DocumentInformation {
    private String archetypeId;
    private ADLVersion adlVersion;
    private ANTLRParserErrors errors;
    private List<Either<SymbolInformation, DocumentSymbol>> symbols;
    private List<FoldingRange> foldingRanges;
    private HoverInfo hoverInfo;
    private DocumentLinks documentLinks;
    /** the current list of diagnostics for this archetype file */
    private List<Diagnostic> diagnostics;

    public DocumentInformation(String archetypeId, ADLVersion adlVersion, ANTLRParserErrors errors,
                               List<Either<SymbolInformation, DocumentSymbol>> symbols,
                               List<FoldingRange> foldingRanges,
                               List<DocumentLink> documentLinks) {
        this.archetypeId = archetypeId;
        this.errors = errors;
        this.symbols = symbols;
        this.foldingRanges = foldingRanges;
        this.documentLinks = new DocumentLinks(documentLinks);
    }

    public String getArchetypeId() {
        return archetypeId;
    }

    public ANTLRParserErrors getErrors() {
        return errors;
    }

    public List<Either<SymbolInformation, DocumentSymbol>> getSymbols() {
        return symbols;
    }

    public List<FoldingRange> getFoldingRanges() {
        return foldingRanges;
    }

    public HoverInfo getHoverInfo() {
        return hoverInfo;
    }

    public Hover getHoverInfo(HoverParams params) {
        if(hoverInfo == null) {
            return null;
        }
        return hoverInfo.getHoverInfo(params);
    }

    public void setHoverInfo(HoverInfo hoverInfo) {
        this.hoverInfo = hoverInfo;
    }

    public DocumentLinks getDocumentLinks() {
        return documentLinks;
    }

    public void setDocumentLinks(DocumentLinks documentLinks) {
        this.documentLinks = documentLinks;
    }

    public List<DocumentLink> getAllDocumentLinks() {
        if(documentLinks == null) {
            return null;
        }
        return documentLinks.getAllDocumentLinks();
    }

    public ADLVersion getADLVersion() {
        return adlVersion;
    }

    public void setDiagnostics(List<Diagnostic> diagnostics) {
        this.diagnostics = diagnostics;
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }
}
