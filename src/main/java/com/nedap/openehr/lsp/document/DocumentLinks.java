package com.nedap.openehr.lsp.document;

import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.terminology.ArchetypeTerm;
import com.nedap.openehr.lsp.repository.BroadcastingArchetypeRepository;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.TextDocumentItem;

import java.util.List;

public class DocumentLinks {
    private final CodeRangeIndex<DocumentLink> documentLinks = new CodeRangeIndex<>();
    private String language;

    public DocumentLinks(List<DocumentLink> documentLinks) {

        for(DocumentLink link:documentLinks) {
            this.documentLinks.addRange(link.getRange(), link);
        }
    }

    public List<DocumentLink> getAllDocumentLinks() {
        return documentLinks.values();
    }

    public DocumentLink resolveLink(BroadcastingArchetypeRepository repository, DocumentLink linkParam) {
        for(DocumentLink existingLink:documentLinks.values()) {
            if(existingLink.equals(linkParam)) { //TODO: this will usually work, but theoretically only the URL + range has to match!
                resolveLinkInternal(repository, existingLink);
                return existingLink;
            }
        }
        return linkParam;
    }

    /**
     * Re-resolve all document links.
     * TODO: add incremental compile?
     * @param repository
     * @param documentLink
     * @return
     */
    public DocumentLink resolveLinkInternal(BroadcastingArchetypeRepository repository, DocumentLink documentLink) {
        if(documentLink.getData() != null) {
            String ref = (String) documentLink.getData();
            TextDocumentItem document = repository.getDocument(ref);
            if(document != null) {
                documentLink.setTarget(document.getUri());
                DocumentInformation information = repository.getDocumentInformation(document.getUri());
                if(information != null && information.getArchetypeId() != null) {
                    Archetype archetype = repository.getArchetype(information.getArchetypeId());
                    if(archetype != null && archetype.getOriginalLanguage() != null) {
                        ArchetypeTerm term = archetype.getTerm(archetype.getDefinition(), archetype.getOriginalLanguage().getCodeString());
                        if(term != null) {
                            documentLink.setTooltip(term.getText() + "\n" + term.getDescription());
                        }
                    }
                }
            }
        }

        return documentLink;
    }

    public void resolveLinks(BroadcastingArchetypeRepository repository) {
        for(DocumentLink documentLink:documentLinks.values()) {
            resolveLinkInternal(repository, documentLink);
        }
    }

}
