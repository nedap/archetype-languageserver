package com.nedap.openehr.lsp.document;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;

public class HoverInfo {

    public static final String MARKDOWN = "markdown";
    protected CodeRangeIndex<Hover> hoverRanges = new CodeRangeIndex<>();
    protected String language;

    public HoverInfo(String language) {
        this.language = language;
    }


    public Hover getHoverInfo(HoverParams params) {
        return hoverRanges.getFromCodeRange(params.getPosition());
    }

    public CodeRangeIndex<Hover> getHoverRanges() {
        return hoverRanges;
    }

    public String getLanguage() {
        return language;
    }


}
