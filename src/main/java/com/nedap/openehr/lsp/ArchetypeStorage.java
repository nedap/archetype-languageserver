package com.nedap.openehr.lsp;

import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.Template;
import com.nedap.archie.aom.TemplateOverlay;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.archie.flattener.InMemoryFullArchetypeRepository;
import org.eclipse.lsp4j.TextDocumentItem;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ArchetypeStorage {

    private InMemoryFullArchetypeRepository repository;

    Map<String, ProcessableDocument> documents = new ConcurrentHashMap<>();
    Map<String, ProcessableDocument> documentsByArchetypeId = new ConcurrentHashMap<>();

    public ArchetypeStorage() {
        repository = new InMemoryFullArchetypeRepository();
    }

    public void invalidateArchetypes(Archetype newArchetype) {
        //now invalidate all related archetypes
        Set<String> archetypesToInvalidate = new HashSet<>();
        archetypesToInvalidate.add(newArchetype.getArchetypeId().toString());

        for(ValidationResult result:repository.getAllValidationResults()) {

            if(isDescendant(result.getSourceArchetype(), newArchetype.getArchetypeId().getFullId())) {
                archetypesToInvalidate.add(result.getArchetypeId());
            }
        }

        for(ValidationResult result:repository.getAllValidationResults()) {
            //this assumes templates cannot be further specialized
            if(result.getSourceArchetype() instanceof Template) {
                Template template = (Template) result.getSourceArchetype();
                for(TemplateOverlay overlay: template.getTemplateOverlays()) {
                    //TODO: not only direct descendants, but also those far below should be checked with (a variant of?) isDescendant
                    if(archetypesToInvalidate.contains(overlay.getParentArchetypeId())) {
                        archetypesToInvalidate.add(result.getSourceArchetype().getArchetypeId().getFullId());
                    }
                }
            }
        }
        for(String archetypeId: archetypesToInvalidate) {
            repository.removeValidationResult(archetypeId);
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
            archetypeToCheck = repository.getArchetype(parentArchetypeId);
            i++;
        }
        return false;
    }

    public ProcessableDocument getDocument(String uri) {
        return documents.get(uri);
    }

    public ProcessableDocument updateDocument(String uri, String text) {
        //TODO: add a proper different save and update?
        return updateDocument(uri, new TextDocumentItem(uri, "language", 1, text));
    }

    public ProcessableDocument updateDocument(String uri, TextDocumentItem text) {
        ProcessableDocument processableDocument = documents.get(uri);
        if(processableDocument != null) {
            Archetype archetype = processableDocument.getArchetype();
            String oldArchetypeId = processableDocument.getArchetypeId();
            if (archetype != null) {
                invalidateArchetypes(archetype);
            }
            processableDocument.setDocumentText(text.getText());
            documentsByArchetypeId.remove(oldArchetypeId);
            documentsByArchetypeId.put(processableDocument.getArchetypeId(), processableDocument);
        } else {
            ProcessableDocument document = new ProcessableDocument(text, repository);
            documents.put(uri, document);
            documentsByArchetypeId.put(document.getArchetypeId(), document);
        }
        return documents.get(uri);
    }

    public void closeDocument(String uri) {
        //TODO: we may have to open from disk now if it's still in the workspace
        documents.remove(uri);
    }
}
