package com.nedap.openehr.lsp;


import com.nedap.archie.aom.ArchetypeModelObject;
import com.nedap.archie.archetypevalidator.ValidationMessage;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.archie.query.AOMPathQuery;
import com.nedap.openehr.lsp.antlr.ANTLRParserMessage;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ADL2TextDocumentService implements TextDocumentService {

    private LanguageClient remoteProxy;
    private ArchetypeStorage storage = new ArchetypeStorage(this);

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        System.out.println("FILE OPENEND");
        storage.updateDocument(params.getTextDocument().getUri(), params.getTextDocument());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        System.out.println("FILE CHANGED");
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

            ProcessableDocument document = storage.updateDocument(uri, params.getTextDocument().getVersion(), changeEvent.getText());

            document.getSymbols();

        }
    }

    public void pushDiagnostics(String uri, Integer version, ProcessableDocument document) {
        PublishDiagnosticsParams diagnosticsParams = new PublishDiagnosticsParams();
        List<Diagnostic> diagnostics = new ArrayList<>();
        for(ANTLRParserMessage warning:document.getErrors().getWarnings()) {
            diagnostics.add(createParserDiagnostic(warning, DiagnosticSeverity.Warning));
        }
        for(ANTLRParserMessage error:document.getErrors().getErrors()) {
            diagnostics.add(createParserDiagnostic(error, DiagnosticSeverity.Error));
        }
        if(document.getExceptionDuringProcessing() != null) {
            //TODO: stacktrace? some extra message to indicate context?
            String message = document.getExceptionDuringProcessing().getMessage() == null ? document.getExceptionDuringProcessing().toString() : document.getExceptionDuringProcessing().getMessage();
            diagnostics.add(new Diagnostic(new Range(new Position(0, 1), new Position(0, 50)), message));
        }
        ValidationResult validationResult = document.getValidationResult();
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
        diagnosticsParams.setUri(uri);
        diagnosticsParams.setVersion(version);
        remoteProxy.publishDiagnostics(diagnosticsParams);
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

        return new Diagnostic(range, error.getMessage(), warning, "ADL2 syntax");
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        System.out.println("FILE CLOSED");
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
        System.out.println("GETTING SYMBOLS");
        CompletableFuture future = new CompletableFuture();
        ProcessableDocument processableDocument = storage.getDocument(params.getTextDocument().getUri());
        future.complete(processableDocument.getSymbols());


        return future;
    }

    public void setRemoteProxy(LanguageClient remoteProxy) {
        this.remoteProxy = remoteProxy;
    }
}
