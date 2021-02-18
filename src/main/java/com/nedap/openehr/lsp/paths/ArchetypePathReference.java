package com.nedap.openehr.lsp.paths;

import org.eclipse.lsp4j.Range;

public class ArchetypePathReference {
    private String archetypeId;
    private String symbolName;
    private String path;
    private Range range;
    private String extraInformation;

    public ArchetypePathReference() {

    }

    public ArchetypePathReference(String symbolName, String path, Range range) {
        this.symbolName = symbolName;
        this.path = path;
        this.range = range;
    }

    public String getArchetypeId() {
        return archetypeId;
    }

    public void setArchetypeId(String archetypeId) {
        this.archetypeId = archetypeId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public String getSymbolName() {
        return symbolName;
    }

    public void setSymbolName(String symbolName) {
        this.symbolName = symbolName;
    }

    public String getExtraInformation() {
        return extraInformation;
    }

    public void setExtraInformation(String extraInformation) {
        this.extraInformation = extraInformation;
    }
}
