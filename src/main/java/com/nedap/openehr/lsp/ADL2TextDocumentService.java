package com.nedap.openehr.lsp;


import com.nedap.archie.antlr.errors.ANTLRParserErrors;
import com.nedap.archie.antlr.errors.ANTLRParserMessage;
import com.nedap.archie.aom.ArchetypeModelObject;
import com.nedap.archie.archetypevalidator.ValidationMessage;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.archie.query.AOMPathQuery;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ADL2TextDocumentService implements TextDocumentService, WorkspaceService {

    private LanguageClient remoteProxy;
    private BroadcastingArchetypeRepository storage = new BroadcastingArchetypeRepository(this);

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        storage.addDocument(params.getTextDocument());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        for (TextDocumentContentChangeEvent changeEvent : params.getContentChanges()) {
            // Will be full update because we specified that is all we support
            //full update requires us to split things into lines first
            if (changeEvent.getRange() != null) {
                throw new UnsupportedOperationException("Range should be null for full document update.");
            }
            if (changeEvent.getRangeLength() != null) {
                throw new UnsupportedOperationException("RangeLength should be null for full document update.");
            }

            storage.updateDocument(params.getTextDocument().getUri(), params.getTextDocument().getVersion(), changeEvent.getText());
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        storage.closeDocument(uri);
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {

    }

    /**
     * The document symbol request is sent from the client to the server to list all
     * symbols found in a given text document.
     *
     * Registration Options: {@link TextDocumentRegistrationOptions}
     *
     * <p>
     * <b>Caveat</b>: although the return type allows mixing the
     * {@link DocumentSymbol} and {@link SymbolInformation} instances into a list do
     * not do it because the clients cannot accept a heterogeneous list. A list of
     * {@code DocumentSymbol} instances is only a valid return value if the
     * {@link DocumentSymbolCapabilities#getHierarchicalDocumentSymbolSupport()
     * textDocument.documentSymbol.hierarchicalDocumentSymbolSupport} is
     * {@code true}. More details on this difference between the LSP and the LSP4J
     * can be found <a href="https://github.com/eclipse/lsp4j/issues/252">here</a>.
     * </p>
     */
    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        return CompletableFuture.completedFuture(storage.getSymbols(params.getTextDocument().getUri()));
    }

    public void setRemoteProxy(LanguageClient remoteProxy) {
        this.remoteProxy = remoteProxy;
    }

    public void pushDiagnostics(TextDocumentItem document, ANTLRParserErrors errors) {
        PublishDiagnosticsParams diagnosticsParams = DiagnosticsConverter.createDiagnostics(document, errors);
        remoteProxy.publishDiagnostics(diagnosticsParams);
    }



    public void pushDiagnostics(TextDocumentItem textDocumentItem, ValidationResult validationResult) {
        PublishDiagnosticsParams diagnosticsParams = DiagnosticsConverter.createDiagnosticsFromValidationResult(textDocumentItem, validationResult);
        // diagnosticsParams.setVersion(textDocumentItem.getVersion());
        remoteProxy.publishDiagnostics(diagnosticsParams);
    }



    public void addFolder(String uri) {
        storage.addFolder(uri);
    }

    public BroadcastingArchetypeRepository getStorage() {
        return storage;
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        storage.setCompile(false);
        for(FileEvent event:params.getChanges()) {
            if(event.getType() == FileChangeType.Created) {
                storage.addFile(event.getUri(), new File(URI.create(event.getUri())));
            } else if (event.getType() == FileChangeType.Changed) {
                storage.fileChanged(event.getUri(), new File(URI.create(event.getUri())));
            } else if (event.getType() == FileChangeType.Deleted) {
                storage.fileRemoved(event.getUri());
            }
        }
        storage.setCompile(true);
        storage.compile();
    }


    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        return CompletableFuture.completedFuture(storage.getHover(params));
    }

    /**
     * The folding range request is sent from the client to the server to return all folding
     * ranges found in a given text document.
     *
     * Since version 3.10.0
     */
    @Override
    public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
        return CompletableFuture.completedFuture(storage.getFoldingRanges(params.getTextDocument()));
    }

    /**
     * The Completion request is sent from the client to the server to compute
     * completion items at a given cursor position. Completion items are
     * presented in the IntelliSense user interface. If computing complete
     * completion items is expensive servers can additional provide a handler
     * for the resolve completion item request. This request is sent when a
     * completion item is selected in the user interface.
     *
     * Registration Options: CompletionRegistrationOptions
     */
    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        throw new UnsupportedOperationException();
    }
}
