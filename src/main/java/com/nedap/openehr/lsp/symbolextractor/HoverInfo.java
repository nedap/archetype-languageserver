package com.nedap.openehr.lsp.symbolextractor;

import com.nedap.archie.ArchieLanguageConfiguration;
import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.CAttribute;
import com.nedap.archie.aom.CComplexObject;
import com.nedap.archie.aom.CObject;
import com.nedap.archie.aom.Template;
import com.nedap.archie.aom.TemplateOverlay;
import com.nedap.archie.aom.primitives.CTerminologyCode;
import com.nedap.archie.aom.terminology.ArchetypeTerm;
import com.nedap.archie.aom.terminology.ArchetypeTerminology;
import com.nedap.archie.aom.terminology.TerminologyCodeWithArchetypeTerm;
import com.nedap.archie.aom.terminology.ValueSet;
import com.nedap.archie.aom.utils.AOMUtils;
import com.nedap.archie.base.MultiplicityInterval;
import com.nedap.archie.rminfo.MetaModels;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.openehr.referencemodels.BuiltinReferenceModels;

import java.util.ArrayList;
import java.util.List;

/**
 * Hover info for one archetype. TODO: work on flat archetype so specializations work properly
 */
public class HoverInfo {

    public static final String MARKDOWN = "markdown";
    private CodeRangeIndex<Hover> hoverRanges = new CodeRangeIndex<>();
    private String language;
    public Hover getHoverInfo(HoverParams params) {
        return hoverRanges.getFromCodeRange(params.getPosition());
    }

    private MetaModels metaModels = BuiltinReferenceModels.getMetaModels();

    public HoverInfo(Archetype sourceArchetype, Archetype archetypeForTerms, String language) {
        this.language = language;
        metaModels.selectModel(sourceArchetype);
        extractHoverInfo(sourceArchetype.getDefinition(), archetypeForTerms);

        if(sourceArchetype instanceof Template) {
            Template template = (Template) sourceArchetype;
            for(TemplateOverlay overlay:template.getTemplateOverlays()) {
                extractHoverInfo(overlay.getDefinition(), archetypeForTerms);
            }
        }

    }

    private void extractHoverInfo(CComplexObject definition, Archetype archetypeForTerms) {
        ArchetypeTerm term = archetypeForTerms.getTerm(definition, language);
        if(term != null) {
            String content = "### " + definition.getRmTypeName() + ": " + term.getText() + "\n\n\t" + term.getDescription();
            MultiplicityInterval occurrences = definition.effectiveOccurrences(metaModels::referenceModelPropMultiplicity);
            content += "\n occurrences: " + occurrences.toString();

            Hover hover = new Hover();
            hover.setContents(new MarkupContent(MARKDOWN, content));
            hoverRanges.addRange(
                    new Position(definition.getStartLine()-1, definition.getStartCharInLine()),
                    new Position(definition.getStartLine()-1, definition.getStartCharInLine() + definition.getTokenLength()),
                    hover);

        }

        for(CAttribute attribute:definition.getAttributes()) {
            extractHoverInfo(attribute, archetypeForTerms);
        }
    }

    private void extractHoverInfo(CAttribute attribute, Archetype archetypeForTerms) {
        for(CObject object:attribute.getChildren()) {
            if(object instanceof CComplexObject) {
                extractHoverInfo((CComplexObject) object, archetypeForTerms);
            } else if (object instanceof CTerminologyCode) {
                extractHoverInfo((CTerminologyCode) object, archetypeForTerms);
            }
        }
    }

    private void extractHoverInfo(CTerminologyCode object, Archetype archetypeForTerms) {
        List<TerminologyCodeWithArchetypeTerm> terms = getTerms(object, archetypeForTerms);
        if(terms != null) {
            StringBuilder content = new StringBuilder();
            if(object.getConstraint() != null && object.getConstraint().size() ==1 && AOMUtils.isValueSetCode(object.getConstraint().get(0))) {


                ArchetypeTerm valueSetTerm = archetypeForTerms.getTerm(object, object.getConstraint().get(0), language);
                if(valueSetTerm != null) {
                    content.append("## ");
                    content.append(valueSetTerm.getText());
                    content.append("\n");
                    content.append(valueSetTerm.getDescription());
                    content.append("\n\n### Members:");
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

    public List<TerminologyCodeWithArchetypeTerm> getTerms(CTerminologyCode cTermCode, Archetype archetypeForTerms) {
        List<TerminologyCodeWithArchetypeTerm> result = new ArrayList();
        ArchetypeTerminology terminology = archetypeForTerms.getTerminology(cTermCode);
        String language = ArchieLanguageConfiguration.getMeaningAndDescriptionLanguage();
        String defaultLanguage = ArchieLanguageConfiguration.getDefaultMeaningAndDescriptionLanguage();
        for (String constraint : cTermCode.getConstraint()) {
            if (constraint.startsWith("at")) {
                ArchetypeTerm termDefinition = terminology.getTermDefinition(language, constraint);
                if (termDefinition == null) {
                    termDefinition = terminology.getTermDefinition(defaultLanguage, constraint);
                }
                if (termDefinition != null) {
                    result.add(new TerminologyCodeWithArchetypeTerm(constraint, termDefinition));
                }
            } else if (constraint.startsWith("ac")) {
                ValueSet acValueSet = terminology.getValueSets().get(constraint);
                if (acValueSet != null) {
                    for (String atCode : acValueSet.getMembers()) {
                        ArchetypeTerm termDefinition = terminology.getTermDefinition(language, atCode);
                        if (termDefinition == null) {
                            termDefinition = terminology.getTermDefinition(defaultLanguage, atCode);
                        }
                        if (termDefinition != null) {
                            result.add(new TerminologyCodeWithArchetypeTerm(atCode, termDefinition));
                        }
                    }
                }
            }
        }
        return result;
    }
}
