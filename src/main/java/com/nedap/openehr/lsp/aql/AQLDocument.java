package com.nedap.openehr.lsp.aql;

import com.nedap.openehr.lsp.document.HoverInfo;
import com.nedap.openehr.lsp.paths.ArchetypePathReference;
import org.eclipse.lsp4j.DocumentHighlight;

import java.util.List;
import java.util.Map;

public class AQLDocument {

    private final String uri;
    private final Map<String, String> symbolToArchetypeMap;
    private final List<ArchetypePathReference> archetypePathReferences;
    private final Map<String, DocumentHighlight> variableBoundToArchetypeIdUse;
    private HoverInfo hoverInfo;

    public AQLDocument(String uri, Map<String, String> symbolToArchetypeIdMap, List<ArchetypePathReference> archetypePathReferences, Map<String, DocumentHighlight> variableBoundToArchetypeIdUse) {
        this.uri = uri;
        this.symbolToArchetypeMap = symbolToArchetypeIdMap;
        this.archetypePathReferences = archetypePathReferences;
        this.variableBoundToArchetypeIdUse = variableBoundToArchetypeIdUse;
    }

    public void setHoverInfo(HoverInfo hoverInfo) {
        this.hoverInfo = hoverInfo;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, String> getSymbolToArchetypeMap() {
        return symbolToArchetypeMap;
    }

    public List<ArchetypePathReference> getArchetypePathReferences() {
        return archetypePathReferences;
    }

    public Map<String, DocumentHighlight> getVariableBoundToArchetypeIdUse() {
        return variableBoundToArchetypeIdUse;
    }

    public HoverInfo getHoverInfo() {
        return hoverInfo;
    }
}
