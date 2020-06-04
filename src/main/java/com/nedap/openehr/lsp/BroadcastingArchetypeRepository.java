package com.nedap.openehr.lsp;

import com.nedap.archie.adlparser.ADLParser;
import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.Template;
import com.nedap.archie.aom.TemplateOverlay;
import com.nedap.archie.archetypevalidator.ArchetypeValidator;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.archie.flattener.InMemoryFullArchetypeRepository;
import com.nedap.openehr.lsp.symbolextractor.ADL2SymbolExtractor;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.openehr.referencemodels.BuiltinReferenceModels;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BroadcastingArchetypeRepository extends InMemoryFullArchetypeRepository {

    private final ADL2TextDocumentService textDocumentService;
    Map<String, TextDocumentItem> documents = new ConcurrentHashMap<>();
    Map<String, DocumentInformation> symbolsByUri = new ConcurrentHashMap<>();
    Map<String, TextDocumentItem> documentsByArchetypeId = new ConcurrentHashMap<>();
    private ArchetypeValidator validator = new ArchetypeValidator(BuiltinReferenceModels.getMetaModels());

    public BroadcastingArchetypeRepository(ADL2TextDocumentService textDocumentService) {
        this.textDocumentService = textDocumentService;

    }

    public void addDocument(TextDocumentItem item) {
        documents.put(item.getUri(), item);
        handleChanged(item);
    }

    public void updateDocument(String uri, int version, String text) {
        TextDocumentItem textDocumentItem = documents.get(uri); //TODO: retrieve old Archetype ID and if changed, remove the old archetype
        textDocumentItem.setVersion(version);
        textDocumentItem.setText(text);
        handleChanged(textDocumentItem);

    }

    /**
     * Handles a changed textdocument. Does:
     * 1. parsing the symbols, storing them
     * 2. if parsing succeeds, invalidates the archetype, then compiles again, broadcasting all changes in the process
     * @param textDocumentItem
     */
    private void handleChanged(TextDocumentItem textDocumentItem) {

        try {
            ADL2SymbolExtractor adl2SymbolExtractor = new ADL2SymbolExtractor();
            DocumentInformation documentInformation = adl2SymbolExtractor.extractSymbols(textDocumentItem.getUri(), textDocumentItem.getText());
            symbolsByUri.put(textDocumentItem.getUri(), documentInformation);

            documentsByArchetypeId.put(documentInformation.getArchetypeId(), textDocumentItem);
            if (documentInformation.getErrors().hasNoErrors()) {
                ADLParser adlParser = new ADLParser(BuiltinReferenceModels.getMetaModels());
                Archetype archetype = adlParser.parse(textDocumentItem.getText());
                addArchetype(archetype);
                //perform incremental compilation here
                invalidateAndRecompileArchetypes(archetype);
                //diagnostics will now be pushed from within the invalidateArchetypesAndRecompile method
            } else {
                textDocumentService.pushDiagnostics(textDocumentItem, documentInformation.getErrors());
            }
        } catch (IOException e) {
            //shouldn't happen, ever, just in memory processing
            throw new RuntimeException(e);
        }

    }

    @Override
    public void setValidationResult(ValidationResult result) {
        super.setValidationResult(result);
        TextDocumentItem textDocumentItem = documentsByArchetypeId.get(result.getArchetypeId());
        textDocumentService.pushDiagnostics(textDocumentItem, result);
        //new validation result received! Broadcast it :)

    }

    public void invalidateAndRecompileArchetypes(Archetype newArchetype) {
        //now invalidate all related archetypes
        Set<String> archetypesToInvalidate = new HashSet<>();
        archetypesToInvalidate.add(newArchetype.getArchetypeId().toString());

        for(Archetype result:getAllArchetypes()) {

            if(isDescendant(result, newArchetype.getArchetypeId().getFullId())) {
                archetypesToInvalidate.add(result.getArchetypeId().toString());
            }
            //this assumes templates cannot be further specialized
            if(result instanceof Template) {
                Template template = (Template) result;
                for(TemplateOverlay overlay: template.getTemplateOverlays()) {
                    //TODO: not only direct descendants, but also those far below should be checked with (a variant of?) isDescendant
                    if(archetypesToInvalidate.contains(overlay.getParentArchetypeId())) {
                        archetypesToInvalidate.add(result.getArchetypeId().getFullId());
                    }
                }
            }
        }


        for(String archetypeId: archetypesToInvalidate) {
            removeValidationResult(archetypeId);
        }
        //this should recompile the archetype plus any parents
        //TODO: for operational templates we need to scan way more, all archetype roots as well. future addition
        for(String archetypeId: archetypesToInvalidate) {
            Archetype archetype = getArchetype(archetypeId);
            if(archetype != null && getValidationResult(archetypeId) == null) {
                System.out.println("validating " + archetypeId);
                validator.validate(archetype, this);
            }
        }
    }

    private boolean isDescendant(Archetype archetype, String parent) {

        Archetype archetypeToCheck = archetype;
        int maxTreeDepth = 75;
        int i= 0;
        while(archetypeToCheck != null && i < maxTreeDepth && archetypeToCheck.getParentArchetypeId() != null) {
            if(archetypeToCheck.getParentArchetypeId().startsWith(parent)) {
                return true;
            }
            String parentArchetypeId = archetypeToCheck.getParentArchetypeId();
            archetypeToCheck = getArchetype(parentArchetypeId);
            i++;
        }
        return false;
    }

    public void closeDocument(String uri) {
        //do nothing right now, but eventually we'll have to start reading from file again if this happens
    }

    public List<Either<SymbolInformation, DocumentSymbol>> getSymbols(String uri) {
        return this.symbolsByUri.get(uri).getSymbols();
    }
}