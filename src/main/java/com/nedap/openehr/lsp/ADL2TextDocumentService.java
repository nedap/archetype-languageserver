package com.nedap.openehr.lsp;


import com.google.common.collect.Lists;
import com.google.gson.JsonPrimitive;
import com.nedap.archie.antlr.errors.ANTLRParserErrors;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.openehr.lsp.repository.BroadcastingArchetypeRepository;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.adapters.CodeActionResponseAdapter;
import org.eclipse.lsp4j.jsonrpc.json.ResponseJsonAdapter;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ADL2TextDocumentService implements TextDocumentService, WorkspaceService {

    public static final String ADL2_COMMAND = "source.convert.adl14";
    public static final String ALL_ADL2_COMMAND = "source.convert.alladl14";

    private LanguageClient remoteProxy;
    private final BroadcastingArchetypeRepository storage = new BroadcastingArchetypeRepository(this);

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

    public void pushDiagnostics(TextDocumentItem document, Exception exception) {
        PublishDiagnosticsParams diagnosticsParams = DiagnosticsConverter.createDiagnostics(document, exception);
        remoteProxy.publishDiagnostics(diagnosticsParams);
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

    /**
     * The document links request is sent from the client to the server to request the location of links in a document.
     *
     * Registration Options: DocumentLinkRegistrationOptions
     */
    @Override
    public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
        return CompletableFuture.completedFuture(this.storage.getDocumentLinks(params));
    }

    /**
     * The document link resolve request is sent from the client to the server to resolve the target of a given document link.
     */
    @Override
    public CompletableFuture<DocumentLink> documentLinkResolve(DocumentLink params) {
        //resolving is done on update, so cannot do anything here.
        //might be useful for future purposes
        return CompletableFuture.completedFuture(params);
    }

    /**
     * The code action request is sent from the client to the server to compute
     * commands for a given text document and range. These commands are
     * typically code fixes to either fix problems or to beautify/refactor code.
     *
     * Registration Options: TextDocumentRegistrationOptions
     */
    @JsonRequest
    @ResponseJsonAdapter(CodeActionResponseAdapter.class)
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        if(storage.isADL14(params.getTextDocument())) {
            CodeAction action1 = new CodeAction("convert this file to ADL 2");
            action1.setKind(CodeActionKind.Source + ".convert.adl2");
            {
                Command command = new Command("Convert to ADL 2", ADL2_COMMAND);
                command.setArguments(Lists.newArrayList(params.getTextDocument().getUri()));
                action1.setCommand(command);
            }

            CodeAction actionAll = new CodeAction("convert all ADL 1.4 files to ADL 2");
            actionAll.setKind(CodeActionKind.Source + ".convert.adl2");
            {
                Command command = new Command("Convert all ADL 1.4 files to ADL 2", ALL_ADL2_COMMAND);
                command.setArguments(Lists.newArrayList(params.getTextDocument().getUri()));
                actionAll.setCommand(command);
            }
            return CompletableFuture.completedFuture(Lists.newArrayList(
                    Either.forRight(action1),
                    Either.forRight(actionAll)));
        }
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    /**
     * The workspace/executeCommand request is sent from the client to the
     * server to trigger command execution on the server. In most cases the
     * server creates a WorkspaceEdit structure and applies the changes to the
     * workspace using the request workspace/applyEdit which is sent from the
     * server to the client.
     *
     * Registration Options: ExecuteCommandRegistrationOptions
     */
    @JsonRequest
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        if(params.getCommand().equalsIgnoreCase(ADL2_COMMAND)) {
            String documentUri = ((JsonPrimitive) params.getArguments().get(0)).getAsString();
            storage.convertAdl14(documentUri);
        } else if (params.getCommand().equalsIgnoreCase(ALL_ADL2_COMMAND)) {
            String documentUri = ((JsonPrimitive) params.getArguments().get(0)).getAsString();
            storage.convertAllAdl14(documentUri);
        }
        return CompletableFuture.completedFuture(null);
    }

    public void writeFile(String uri, String label, String content) {
        WorkspaceEdit edit = new WorkspaceEdit();
        List<Either<TextDocumentEdit, ResourceOperation>> changes = new ArrayList<>();
        changes.add(Either.forRight(
                new CreateFile(uri, new CreateFileOptions(true, false)))
        );
        changes.add(Either.forLeft(
                new TextDocumentEdit(new VersionedTextDocumentIdentifier(uri, 1),
                        Lists.newArrayList(
                                new TextEdit(new Range(
                                        new Position(0, 0),
                                        new Position(0, 0)),
                                        content
                                        )
                        )))
        );


        edit.setDocumentChanges(changes);
        remoteProxy.applyEdit(new ApplyWorkspaceEditParams(edit, label));
    }
}
