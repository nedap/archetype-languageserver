package com.nedap.openehr.lsp;


import com.nedap.archie.antlr.errors.ANTLRParserErrors;
import com.nedap.archie.antlr.errors.ANTLRParserMessage;
import com.nedap.archie.aom.ArchetypeModelObject;
import com.nedap.archie.archetypevalidator.ValidationMessage;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.archie.query.AOMPathQuery;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
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



    private String toMessage(ValidationMessage message) {
        if(message.getMessage() != null) {
            return message.getMessage();
        } else {
            return message.getType().getDescription();
        }
    }

    private Diagnostic createParserDiagnostic(ANTLRParserMessage error, DiagnosticSeverity warning) {
        Range range = new Range(
                new Position(error.getLineNumber()-1, error.getColumnNumber()),
                new Position(error.getLineNumber()-1, error.getColumnNumber() + error.getLength())//TODO: archie errors do not keep the position properly
        );

        return new Diagnostic(range, error.getShortMessage(), warning, "ADL2 syntax");
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
        CompletableFuture future = new CompletableFuture();
        future.complete(storage.getSymbols(params.getTextDocument().getUri()));

        return future;
    }

    public void setRemoteProxy(LanguageClient remoteProxy) {
        this.remoteProxy = remoteProxy;
    }

    public void pushDiagnostics(TextDocumentItem document, ANTLRParserErrors errors) {
        PublishDiagnosticsParams diagnosticsParams = new PublishDiagnosticsParams();
        List<Diagnostic> diagnostics = new ArrayList<>();
        for(ANTLRParserMessage warning:errors.getWarnings()) {
            diagnostics.add(createParserDiagnostic(warning, DiagnosticSeverity.Warning));
        }
        for(ANTLRParserMessage error:errors.getErrors()) {
            diagnostics.add(createParserDiagnostic(error, DiagnosticSeverity.Error));
        }
//TODO: replace ANTLRParserErrors with a better class
// if(document.getExceptionDuringProcessing() != null) {
//            //TODO: stacktrace? some extra message to indicate context?
//            String message = document.getExceptionDuringProcessing().getMessage() == null ? document.getExceptionDuringProcessing().toString() : document.getExceptionDuringProcessing().getMessage();
//            diagnostics.add(new Diagnostic(new Range(new Position(0, 1), new Position(0, 50)), message));
//        }

        diagnosticsParams.setDiagnostics(diagnostics);
        diagnosticsParams.setUri(document.getUri());
        diagnosticsParams.setVersion(document.getVersion());
        remoteProxy.publishDiagnostics(diagnosticsParams);
    }

    public void pushDiagnostics(TextDocumentItem textDocumentItem, ValidationResult validationResult) {
        PublishDiagnosticsParams diagnosticsParams = new PublishDiagnosticsParams();
        List<Diagnostic> diagnostics = new ArrayList<>();

        if(validationResult != null) {
            if(validationResult.hasWarningsOrErrors()) {
                for(ValidationMessage message:validationResult.getErrors()) {
                    ArchetypeModelObject withLocation = null;
                    if(message.getPathInArchetype() != null) {
                        try {
                            withLocation = new AOMPathQuery(message.getPathInArchetype()).findMatchingPredicate(validationResult.getSourceArchetype().getDefinition(),
                                    (o) -> o instanceof ArchetypeModelObject && ((ArchetypeModelObject) o).getStartLine() != null);
                        } catch (Exception e) {
                            //we really don't care, but log just in case
                            e.printStackTrace();
                        }
                    }
                    if(withLocation != null) {
                        Range range = new Range(
                                new Position(withLocation.getStartLine()-1, withLocation.getStartCharInLine()),
                                new Position(withLocation.getStartLine()-1, withLocation.getStartCharInLine() + withLocation.getTokenLength())
                        );
                        diagnostics.add(new Diagnostic(range, toMessage(message), message.isWarning() ? DiagnosticSeverity.Warning : DiagnosticSeverity.Error, "ADL validation", message.getType().toString()));
                    } else {
                        Range range = new Range(
                                new Position(0, 1),
                                new Position(0, 50)
                        );
                        diagnostics.add(new Diagnostic(range, toMessage(message), message.isWarning() ? DiagnosticSeverity.Warning : DiagnosticSeverity.Error, "ADL validation", message.getType().toString()));
                    }
                }
            }
        }
        diagnosticsParams.setDiagnostics(diagnostics);
        diagnosticsParams.setUri(textDocumentItem.getUri());
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
}
