package com.nedap.openehr.lsp.commands;

import com.google.common.collect.Lists;
import com.google.gson.JsonPrimitive;
import com.nedap.openehr.lsp.ADL2TextDocumentService;
import com.nedap.openehr.lsp.document.DocumentInformation;
import com.nedap.openehr.lsp.repository.BroadcastingArchetypeRepository;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.nedap.openehr.lsp.utils.DocumentSymbolUtils.getDocumentSymbolOrThrow;

public class AddTerminologyCommmand {
    private final BroadcastingArchetypeRepository storage;
    private final ADL2TextDocumentService textDocumentService;
    private final ExecuteCommandParams params;

    public AddTerminologyCommmand(BroadcastingArchetypeRepository storage, ADL2TextDocumentService textDocumentService, ExecuteCommandParams params) {

        this.storage = storage;
        this.textDocumentService = textDocumentService;
        this.params = params;
    }

    Pattern codePattern = Pattern.compile(".*(?<code>[A-Za-z]{2}(\\d|\\.)+).*");

    public void apply() {
        String documentUri = ((JsonPrimitive) params.getArguments().get(0)).getAsString();
        JsonPrimitive message = (JsonPrimitive) params.getArguments().get(1);
        Matcher matcher = codePattern.matcher(message.getAsString());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("First argument must have a valid id, at or ac code somewhere!");
        }
        DocumentInformation documentInformation = storage.getDocumentInformation(documentUri);
        List<DocumentSymbol> symbols = documentInformation.getSymbols().stream().map(e -> e.getRight()).collect(Collectors.toList());
        DocumentSymbol archetypeSymbol = getDocumentSymbolOrThrow(symbols, "archetype");
        DocumentSymbol terminology = getDocumentSymbolOrThrow(archetypeSymbol.getChildren(), DocumentInformation.TERMINOLOGY_SECTION_NAME);
        DocumentSymbol termDefinitions = getDocumentSymbolOrThrow(terminology.getChildren(), DocumentInformation.TERM_DEFINITIONS_NAME);
        List<TextEdit> editCommands = new ArrayList<>();
        for (DocumentSymbol languageSymbol : Lists.reverse(termDefinitions.getChildren())) {
            Position insertBefore = languageSymbol.getRange().getEnd();
            StringBuilder text = new StringBuilder();
            //TODO: better indenting :) perhaps retrieve from last character of symbol position in line?
            text.append("    [\"");
            text.append(matcher.group("code"));
            text.append("\"] = <");
            text.append("\n");
            text.append("                ");
            text.append("text =<\"Missing translation\">\n");
            text.append("                ");
            text.append("description =<\"Missing translation\">\n");
            text.append("            >\n");
            text.append("        ");

            TextEdit insertEdit = new TextEdit(new Range(insertBefore, insertBefore),
                    text.toString());
            editCommands.add(insertEdit);
        }
        this.textDocumentService.applyEdits(documentUri, 0, "Add missing terminology code", editCommands);

    }

}
