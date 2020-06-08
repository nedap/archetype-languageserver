package com.nedap.openehr.lsp.symbolextractor;

import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.CAttribute;
import com.nedap.archie.aom.CComplexObject;
import com.nedap.archie.aom.CObject;
import com.nedap.archie.aom.Template;
import com.nedap.archie.aom.TemplateOverlay;
import com.nedap.archie.aom.primitives.CTerminologyCode;
import com.nedap.archie.aom.terminology.ArchetypeTerm;
import com.nedap.archie.aom.terminology.TerminologyCodeWithArchetypeTerm;
import com.nedap.archie.aom.utils.AOMUtils;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;

import java.util.List;

/**
 * Hover info for one archetype. TODO: work on flat archetype so specializations work properly
 */
public class HoverInfo {

    public static final String MARKDOWN = "markdown";
    private CodeRangeIndex<Hover> hoverRanges = new CodeRangeIndex<>();

    public Hover getHoverInfo(HoverParams params) {
        return hoverRanges.getFromCodeRange(params.getPosition());
    }


    public HoverInfo(Archetype archetype) {
        extractHoverInfo(archetype.getDefinition());

        if(archetype instanceof Template) {
            Template template = (Template) archetype;
            for(TemplateOverlay overlay:template.getTemplateOverlays()) {
                extractHoverInfo(overlay.getDefinition());
            }
        }

    }

    private void extractHoverInfo(CComplexObject definition) {
        ArchetypeTerm term = definition.getTerm();
        if(term != null) {
            String content = "### " + definition.getRmTypeName() + ": " + term.getText() + "\n\n\t" + term.getDescription();
            Hover hover = new Hover();
            hover.setContents(new MarkupContent(MARKDOWN, content));
            hoverRanges.addRange(
                    new Position(definition.getStartLine()-1, definition.getStartCharInLine()),
                    new Position(definition.getStartLine()-1, definition.getStartCharInLine() + definition.getTokenLength()),
                    hover);
        }

        for(CAttribute attribute:definition.getAttributes()) {
            extractHoverInfo(attribute);
        }
    }

    private void extractHoverInfo(CAttribute attribute) {
        for(CObject object:attribute.getChildren()) {
            if(object instanceof CComplexObject) {
                extractHoverInfo((CComplexObject) object);
            } else if (object instanceof CTerminologyCode) {
                extractHoverInfo((CTerminologyCode) object);
            }
        }
    }

    private void extractHoverInfo(CTerminologyCode object) {
        List<TerminologyCodeWithArchetypeTerm> terms = object.getTerms();
        if(terms != null) {
            StringBuilder content = new StringBuilder();
//TODO: value set name if present!
            if(object.getConstraint() != null && object.getConstraint().size() ==1 && AOMUtils.isValueSetCode(object.getConstraint().get(0))) {
                Archetype archetype = object.getArchetype();
                if(archetype.getOriginalLanguage() != null && archetype.getOriginalLanguage().getCodeString() != null) {
                    ArchetypeTerm valueSetTerm = archetype.getTerm(object, object.getConstraint().get(0), archetype.getOriginalLanguage().getCodeString());
                    if(valueSetTerm != null) {
                        content.append("## ");
                        content.append(valueSetTerm.getText());
                        content.append("\n");
                        content.append(valueSetTerm.getDescription());
                        content.append("\n\n### Members:");
                    }
                }
            }
            for(TerminologyCodeWithArchetypeTerm term:terms) {
                content.append("\n\n");
                content.append(term.getCode());
                content.append(": ");
                if(term.getTerm() != null) {
                    content.append("__");
                    content.append(term.getTerm().getText());
                    content.append("__");
                    content.append("\n\t");
                    content.append(term.getTerm().getDescription());
                }
            }
            Hover hover = new Hover();
            hover.setContents(new MarkupContent(MARKDOWN, content.toString()));
            hoverRanges.addRange(
                    new Position(object.getStartLine()-1, object.getStartCharInLine()),
                    new Position(object.getStartLine()-1, object.getStartCharInLine() + object.getTokenLength()),
                    hover);
        }
    }
}
