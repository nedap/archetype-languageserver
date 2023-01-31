package com.nedap.openehr.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
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


    /**
     * A parameterized test that tests generating examples and opts
     * @param archetypeFilenameToTest the filename of the archetype to test
     * @param otherArchetypeFilenames file names of other archetypes to load before the archetypeToTest
     * @param commandIdentifier the identifier of the command to execute, for example source.example
     * @param commandKind the kind of the command: source.example.json for example
     * @param createdFilename the file name that should be created
     * @param startsWith the first characters the created file should have
     * @throws Exception in case something goes wrong and the test fails :)
     */
    @ParameterizedTest
    @MethodSource("exampleArchetypeProvider")
    public void generateFile(String archetypeFilenameToTest,
                                List<String> otherArchetypeFilenames,
                                String commandIdentifier,
                                String commandKind,
                                String createdFilename,
                                String startsWith) throws Exception {
        for(String otherArchetype:otherArchetypeFilenames) {
            openResource(otherArchetype);
        }
        openResource(archetypeFilenameToTest);

        //check that the archetypes are correct
        for(String otherArchetype:otherArchetypeFilenames) {
            assertTrue(testClient.getDiagnostics().get(otherArchetype).getDiagnostics().isEmpty(), () -> testClient.getDiagnostics().toString());
        }
        assertTrue(testClient.getDiagnostics().get(archetypeFilenameToTest).getDiagnostics().isEmpty(), () -> testClient.getDiagnostics().toString());

        runCommand(archetypeFilenameToTest, commandIdentifier, commandKind);

        assertEquals(1, testClient.appliedEdits.size());
        List<Either<TextDocumentEdit, ResourceOperation>> documentChanges = testClient.appliedEdits.get(0).getEdit().getDocumentChanges();
        assertEquals(2, documentChanges.size());
        //first a create, then an edit
        Either<TextDocumentEdit, ResourceOperation> createChange = documentChanges.get(0);
        Either<TextDocumentEdit, ResourceOperation> optContentChange = documentChanges.get(1);
        assertEquals("create", createChange.getRight().getKind());
        CreateFile createFile = (CreateFile) createChange.getRight();

        assertEquals(createdFilename, createFile.getUri());

        TextDocumentEdit edit = optContentChange.getLeft();
        assertEquals(createdFilename, edit.getTextDocument().getUri());
        //we could do a full parse, but that's ok for now I guess?
        assertTrue(edit.getEdits().get(0).getNewText().startsWith(startsWith));
    }

    static Stream<Arguments> exampleArchetypeProvider() {
        return Stream.of(
                arguments("correct_template.adlt",
                        Arrays.asList("test_archetype.adls"),
                        "source.example",
                        "source.example.json",
                        "/example/openEHR-EHR-CLUSTER.test_template.v0.1.2_example.json",
                        "{"
                ),
                arguments("correct_opt.opt2",
                        new ArrayList<>(),
                        "source.example",
                        "source.example.json",
                        "/example/openEHR-EHR-CLUSTER.test_template.v0.1.2_example.json",
                        "{"
                ),
                arguments("correct_template.adlt",
                        Arrays.asList("test_archetype.adls"),
                        "source.opt",
                        "source.opt.adl",
                        "/opt/openEHR-EHR-CLUSTER.test_template.v0.1.2.opt2",
                        "operational_template"
                )
        );
    }

    /**
     * Run the given command from the code actions, asserting that the given command is in the capabilities
     * @param archetypeUriToTest URI of the file to run the command against
     * @param commandIdentifier the identifier of the command in the commands list of the capabilities
     * @param commandKind the kind of the command, to find it in the list of code Actions
     * @throws InterruptedException from the eclipse LSP library
     * @throws ExecutionException from the eclipse LSP library
     */
    private void runCommand(String archetypeUriToTest, String commandIdentifier, String commandKind) throws InterruptedException, ExecutionException {
        assertTrue(initializeResult.getCapabilities().getExecuteCommandProvider().getCommands().contains(commandIdentifier));
        List<Either<Command, CodeAction>> listCompletableFuture = textDocumentService.codeAction(new CodeActionParams(new TextDocumentIdentifier(archetypeUriToTest),
                new Range(new Position(1, 1), new Position(1, 15)),
                new CodeActionContext())).get();
        CodeAction exampleAction = listCompletableFuture.stream().filter(c -> c.isRight() && c.getRight().getKind().equals(commandKind)).findFirst().get().getRight();
        Command command = exampleAction.getCommand();

        executeCommand(command);
    }
}
