package com.nedap.openehr.lsp;

import com.google.common.collect.Lists;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.openehr.referencemodels.BuiltinReferenceModels;

import java.util.concurrent.CompletableFuture;

public class ADL2LanguageServer implements LanguageServer {

    private InitializeParams clientParams;
    private ADL2TextDocumentService textDocumentService = new ADL2TextDocumentService();;
    private LanguageClient remoteProxy;
    private RemoteEndpoint remoteEndpoint;

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        this.clientParams = params;

        CompletableFuture<InitializeResult> completableFuture = new CompletableFuture<InitializeResult>();
        ServerCapabilities capabilities = new ServerCapabilities();
        WorkspaceServerCapabilities workspaceServerCapabilities = new WorkspaceServerCapabilities();
        WorkspaceFoldersOptions workspaceFoldersOptions = new WorkspaceFoldersOptions();
        workspaceFoldersOptions.setChangeNotifications(true);
        workspaceFoldersOptions.setSupported(true);
        workspaceServerCapabilities.setWorkspaceFolders(workspaceFoldersOptions);
        capabilities.setWorkspace(workspaceServerCapabilities);
        capabilities.setDocumentSymbolProvider(true);
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        capabilities.setFoldingRangeProvider(true);
        capabilities.setHoverProvider(true);
        capabilities.setCodeLensProvider(new CodeLensOptions(false));//no resolve provider for now
        capabilities.setDocumentLinkProvider(new DocumentLinkOptions(true));

        capabilities.setCompletionProvider(new CompletionOptions(false, Lists.newArrayList("/")));//add '/' to trigger code completion on paths as well as the usual things.


        capabilities.setCodeActionProvider(new CodeActionOptions(Lists.newArrayList(
                ADL2TextDocumentService.ADL2_COMMAND,
                ADL2TextDocumentService.ALL_ADL2_COMMAND,
                ADL2TextDocumentService.ADD_TO_TERMINOLOGY,
                ADL2TextDocumentService.WRITE_OPT_ADL,
                ADL2TextDocumentService.WRITE_OPT_JSON,
                ADL2TextDocumentService.WRITE_OPT_XML,
                ADL2TextDocumentService.WRITE_EXAMPLE_JSON,
                ADL2TextDocumentService.WRITE_EXAMPLE_FLAT_JSON,
                ADL2TextDocumentService.WRITE_EXAMPLE_XML
        )));
        capabilities.setExecuteCommandProvider(new ExecuteCommandOptions(Lists.newArrayList(
                ADL2TextDocumentService.ADL2_COMMAND,
                ADL2TextDocumentService.ALL_ADL2_COMMAND,
                ADL2TextDocumentService.ADD_TO_TERMINOLOGY,
                ADL2TextDocumentService.WRITE_OPT_COMMAND,
                ADL2TextDocumentService.WRITE_EXAMPLE_COMMAND,
                ADL2TextDocumentService.SHOW_INFO_COMMAND
                )));

        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setName("ADL 2 Archetype language server");
        serverInfo.setVersion("0.0.1-alpha");
        completableFuture.complete(new InitializeResult(capabilities, serverInfo));
        System.err.println(params.getRootUri());
        System.err.println(params.getWorkspaceFolders());

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
        } else if (clientParams.getRootUri() != null) {
            textDocumentService.getStorage().setCompile(false);
            textDocumentService.addFolder(clientParams.getRootUri());
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
        return textDocumentService;
    }

    public void connect(LanguageClient remoteProxy, RemoteEndpoint remoteEndpoint) {
        this.remoteProxy = remoteProxy;
        this.remoteEndpoint = remoteEndpoint;
        textDocumentService.setRemoteProxy(remoteProxy);
        textDocumentService.setRemoteEndPoint(remoteEndpoint);
    }
}
