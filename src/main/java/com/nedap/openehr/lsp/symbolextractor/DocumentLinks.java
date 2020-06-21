package com.nedap.openehr.lsp.symbolextractor;

import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.CArchetypeRoot;
import com.nedap.archie.aom.CAttribute;
import com.nedap.archie.aom.CComplexObject;
import com.nedap.archie.aom.CObject;
import com.nedap.archie.aom.terminology.ArchetypeTerm;
import com.nedap.archie.flattener.ArchetypeRepository;
import com.nedap.openehr.lsp.BroadcastingArchetypeRepository;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;

import java.util.ArrayList;
import java.util.List;

public class DocumentLinks {
    public static final String MARKDOWN = "markdown";
    private BroadcastingArchetypeRepository repository;
    private final CodeRangeIndex<DocumentLink> documentLinks = new CodeRangeIndex<>();
    private String language;

    public DocumentLinks(Archetype archetype, BroadcastingArchetypeRepository repository) {
        this.repository = repository;
        extractLinks(archetype.getDefinition());
        this.repository = null;
    }

    private void extractLinks(CObject definition) {
        for(CAttribute attribute:definition.getAttributes()) {
            extractLinks(attribute);
        }
        try {
            if(definition instanceof CArchetypeRoot) {
                TextDocumentItem document = repository.getDocument(((CArchetypeRoot) definition).getArchetypeRef());
                if(document != null) {
                    Archetype archetype = repository.getArchetype(((CArchetypeRoot) definition).getArchetypeRef());
                    Range range = new Range(new Position(definition.getStartLine()-1, definition.getStartCharInLine()),
                            new Position(definition.getStartLine()-1, definition.getStartCharInLine() + definition.getTokenLength()));
                    String tooltipText = null;
                    if(archetype.getOriginalLanguage() != null) {
                        ArchetypeTerm term = archetype.getTerm(archetype.getDefinition(), archetype.getOriginalLanguage().getCodeString());
                        if(term != null) {
                            tooltipText = term.getText() + "\n" + term.getDescription();
                        }
                    }

                    DocumentLink documentLink = new DocumentLink(range, document.getUri(), null, tooltipText);
                    documentLinks.addRange(range.getStart(), range.getEnd(), documentLink);
                }
            }
        } catch (Exception e) {
            //TODO: push diagnostics!
            e.printStackTrace();
        }
    }

    private void extractLinks(CAttribute attribute) {
        for(CObject cObject:attribute.getChildren()) {
            extractLinks(cObject);
        }
    }

    public List<DocumentLink> getAllDocumentLinks() {
        return documentLinks.values();
    }
}
