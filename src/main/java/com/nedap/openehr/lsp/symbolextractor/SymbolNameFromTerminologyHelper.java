package com.nedap.openehr.lsp.symbolextractor;

import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.terminology.ArchetypeTerm;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SymbolNameFromTerminologyHelper {

    private static Pattern cComplexobjectPattern = Pattern.compile(".*\\[(?<id>[^\\]\\,]+)(\\s*\\,\\s*(?<archetyperef>[^\\]]+))?\\]");

    public static void giveNames(List<Either<SymbolInformation, DocumentSymbol>> symbols, Archetype archetype, String language) {
        if(archetype.getTerminology() == null || archetype.getTerminology().getTermDefinitions().get(language) == null) {
            //do null check now, so we don't have to do it multiple times later
            return;
        }
        for(Either<SymbolInformation, DocumentSymbol> symbol:symbols) {
            if(symbol.isRight()) {
                processSymbol(symbol.getRight(), archetype, language);
            }
        }
    }

    private static void processSymbol(DocumentSymbol documentSymbol, Archetype archetype, String language) {
        if(documentSymbol.getKind() == SymbolKind.Class) {
            Matcher matcher = cComplexobjectPattern.matcher(documentSymbol.getName());
            if(matcher.matches()) {
                String idCode = matcher.group("id");
                ArchetypeTerm term = archetype.getTerminology().getTermDefinition(language, idCode);
                if(term != null) {
                    documentSymbol.setDetail(documentSymbol.getName());
                    documentSymbol.setName(term.getText());
                    //documentSymbol.setDetail(term.getText());
                }
            }
        }
        if(documentSymbol.getChildren() != null) {
            for(DocumentSymbol child:documentSymbol.getChildren()) {
                processSymbol(child, archetype, language);
            }
        }
    }
}
