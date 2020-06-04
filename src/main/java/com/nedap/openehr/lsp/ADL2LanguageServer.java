package com.nedap.openehr.lsp;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.ServerInfo;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.openehr.referencemodels.BuiltinReferenceModels;

import java.util.concurrent.CompletableFuture;

public class ADL2LanguageServer implements LanguageServer {

    private InitializeParams clientParams;
    private ADL2TextDocumentService textDocumentService = new ADL2TextDocumentService();;
    private ADL2WorkspaceService workspaceService = new ADL2WorkspaceService();
    private LanguageClient remoteProxy;

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        System.out.println("INITIALIZING CLIENT");
        this.clientParams = params;
        CompletableFuture<InitializeResult> completableFuture = new CompletableFuture<InitializeResult>();
        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setDocumentSymbolProvider(true);
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setName("ADL 2 Archetype language server");
        serverInfo.setVersion("0.0.1-alpha");
        completableFuture.complete(new InitializeResult(capabilities, serverInfo));

        return completableFuture;
    }

    @Override
    public void initialized(InitializedParams params) {
        if(clientParams.getWorkspaceFolders() != null && clientParams.getCapabilities().getWorkspace().getWorkspaceFolders()) {
            textDocumentService.getStorage().setCompile(false);
            for(WorkspaceFolder folder:clientParams.getWorkspaceFolders()) {
                textDocumentService.addFolder(folder.getUri());
            }
            textDocumentService.getStorage().setCompile(true);
            textDocumentService.getStorage().compile(BuiltinReferenceModels.getMetaModels());
        }
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return null;
    }

    @Override
    public void exit() {

    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    public void connect(LanguageClient remoteProxy) {
        this.remoteProxy = remoteProxy;
        textDocumentService.setRemoteProxy(remoteProxy);
    }
}
