package com.nedap.openehr.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class RunCommandTest extends LanguageServerTestBase {

    @Test
    public void generateOpt() throws Exception {
        openResource("test_archetype.adls");
        openResource("correct_template.adlt");
        //check that the archetypes are correct
        assertTrue(testClient.getDiagnostics().get("test_archetype.adls").getDiagnostics().isEmpty());
        assertTrue(testClient.getDiagnostics().get("correct_template.adlt").getDiagnostics().isEmpty());

        runCommand("correct_template.adlt", "source.opt", "source.opt.adl");

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

    @ParameterizedTest
    @MethodSource("exampleArchetypeProvider")
    public void generateExample(String archetypeToTest,
                                List<String> otherArchetypeNames,
                                String commandIdentifier,
                                String commandKind,
                                String createdFileName) throws Exception {
        for(String otherArchetype:otherArchetypeNames) {
            openResource(otherArchetype);
        }
        openResource(archetypeToTest);

        //check that the archetypes are correct
        assertTrue(testClient.getDiagnostics().get(archetypeToTest).getDiagnostics().isEmpty());
        for(String otherArchetype:otherArchetypeNames) {
            assertTrue(testClient.getDiagnostics().get(otherArchetype).getDiagnostics().isEmpty());
        }

        runCommand(archetypeToTest, commandIdentifier, commandKind);

        assertEquals(1, testClient.appliedEdits.size());
        List<Either<TextDocumentEdit, ResourceOperation>> documentChanges = testClient.appliedEdits.get(0).getEdit().getDocumentChanges();
        assertEquals(2, documentChanges.size());
        //first a create, then an edit
        Either<TextDocumentEdit, ResourceOperation> createChange = documentChanges.get(0);
        Either<TextDocumentEdit, ResourceOperation> optContentChange = documentChanges.get(1);
        assertEquals("create", createChange.getRight().getKind());
        CreateFile createFile = (CreateFile) createChange.getRight();

        assertEquals(createdFileName, createFile.getUri());

        TextDocumentEdit edit = optContentChange.getLeft();
        assertEquals(createdFileName, edit.getTextDocument().getUri());
        //we could do a full parse, but that's ok for now I guess?
        assertTrue(edit.getEdits().get(0).getNewText().startsWith("{"));
    }

    static Stream<Arguments> exampleArchetypeProvider() {
        return Stream.of(
                arguments("correct_template.adlt",
                        Arrays.asList("test_archetype.adls"),
                        "source.example",
                        "source.example.json",
                        "/example/openEHR-EHR-CLUSTER.test_template.v0.1.2_example.json"
                ),
                arguments("correct_opt.opt2",
                        new ArrayList<>(),
                        "source.example",
                        "source.example.json",
                        "/example/openEHR-EHR-CLUSTER.test_template.v0.1.2_example.json"
                )
        );
    }

    /**
     * Run the given command from the code actions, asserting that the given command is in the capabilities
     * @param archetypeToTest URI of the file to run the command against
     * @param commandIdentifier the identifier of the command in the commands list of the capabilities
     * @param commandKind the kind of the command, to find it in the list of code Actions
     * @throws InterruptedException from the eclipse LSP library
     * @throws ExecutionException from the eclipse LSP library
     */
    private void runCommand(String archetypeToTest, String commandIdentifier, String commandKind) throws InterruptedException, ExecutionException {
        assertTrue(initializeResult.getCapabilities().getExecuteCommandProvider().getCommands().contains(commandIdentifier));
        List<Either<Command, CodeAction>> listCompletableFuture = textDocumentService.codeAction(new CodeActionParams(new TextDocumentIdentifier(archetypeToTest),
                new Range(new Position(1, 1), new Position(1, 15)),
                new CodeActionContext())).get();
        CodeAction exampleAction = listCompletableFuture.stream().filter(c -> c.isRight() && c.getRight().getKind().equals(commandKind)).findFirst().get().getRight();
        Command command = exampleAction.getCommand();

        executeCommand(command);
    }
}
