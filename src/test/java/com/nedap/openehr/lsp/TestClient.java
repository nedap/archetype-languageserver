package com.nedap.openehr.lsp;

import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TestClient implements LanguageClient {

    LinkedHashMap<String, PublishDiagnosticsParams> diagnostics = new LinkedHashMap<>();

    @Override
    public void telemetryEvent(Object object) {

    }

    @Override
    public synchronized void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
        this.diagnostics.put(diagnostics.getUri(), diagnostics);

    }

    @Override
    public void showMessage(MessageParams messageParams) {

    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void logMessage(MessageParams message) {

    }

    /**
     * The workspace/applyEdit request is sent from the server to the client to modify resource on the client side.
     */
    public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(ApplyWorkspaceEditParams params) {
            return CompletableFuture.completedFuture(null);
    }

    public Map<String, PublishDiagnosticsParams> getDiagnostics() {
        return diagnostics;
    }
}
