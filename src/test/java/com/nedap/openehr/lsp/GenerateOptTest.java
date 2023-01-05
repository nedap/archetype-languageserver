package com.nedap.openehr.lsp;

import com.google.gson.JsonPrimitive;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenerateOptTest extends LanguageServerTestBase {

    @Test
    public void generateOpt() throws Exception {
        openResource("test_archetype.adls");
        openResource("correct_template.adlt");
        //check that the archetypes are correct
        System.out.println(testClient.getDiagnostics().get("test_archetype.adls").getDiagnostics());
        assertTrue(testClient.getDiagnostics().get("test_archetype.adls").getDiagnostics().isEmpty());

        System.out.println();
        assertTrue(initializeResult.getCapabilities().getExecuteCommandProvider().getCommands().contains("source.opt"));
        List<Either<Command, CodeAction>> listCompletableFuture = textDocumentService.codeAction(new CodeActionParams(new TextDocumentIdentifier("correct_template.adlt"),
                new Range(new Position(1, 1), new Position(1, 15)),
                new CodeActionContext())).get();
        CodeAction optAdlAction = listCompletableFuture.stream().filter(c -> c.isRight() && c.getRight().getKind().equals("source.opt.adl")).findFirst().get().getRight();
        Command command = optAdlAction.getCommand();

        List<Object> arguments = command.getArguments().stream().map(a -> new JsonPrimitive((String) a)).collect(Collectors.toList());
        adl2LanguageServer.getWorkspaceService().executeCommand(new ExecuteCommandParams(command.getCommand(), arguments));

        assertEquals(1, testClient.appliedEdits.size());
        List<Either<TextDocumentEdit, ResourceOperation>> documentChanges = testClient.appliedEdits.get(0).getEdit().getDocumentChanges();
        assertEquals(2, documentChanges.size());
        //first a create, then an edit
        Either<TextDocumentEdit, ResourceOperation> createChange = documentChanges.get(0);
        Either<TextDocumentEdit, ResourceOperation> optContentChange = documentChanges.get(1);
        assertEquals("create", createChange.getRight().getKind());
        CreateFile createFile = (CreateFile) createChange.getRight();

        assertEquals("/opt/openEHR-EHR-CLUSTER.test_template.v0.1.2.opt2", createFile.getUri());

        TextDocumentEdit edit = optContentChange.getLeft();
        assertEquals("/opt/openEHR-EHR-CLUSTER.test_template.v0.1.2.opt2", edit.getTextDocument().getUri());
        assertTrue(edit.getEdits().get(0).getNewText().startsWith("operational_template"));




    }
}
