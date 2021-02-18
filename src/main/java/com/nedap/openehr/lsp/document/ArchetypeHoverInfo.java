package com.nedap.openehr.lsp.document;

import com.nedap.archie.ArchieLanguageConfiguration;
import com.nedap.archie.adlparser.modelconstraints.BMMConstraintImposer;
import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.ArchetypeSlot;
import com.nedap.archie.aom.CAttribute;
import com.nedap.archie.aom.CComplexObject;
import com.nedap.archie.aom.CObject;
import com.nedap.archie.aom.CPrimitiveObject;
import com.nedap.archie.aom.Template;
import com.nedap.archie.aom.TemplateOverlay;
import com.nedap.archie.aom.primitives.CTerminologyCode;
import com.nedap.archie.aom.terminology.ArchetypeTerm;
import com.nedap.archie.aom.terminology.ArchetypeTerminology;
import com.nedap.archie.aom.terminology.TerminologyCodeWithArchetypeTerm;
import com.nedap.archie.aom.terminology.ValueSet;
import com.nedap.archie.aom.utils.AOMUtils;
import com.nedap.archie.base.Cardinality;
import com.nedap.archie.base.MultiplicityInterval;
import com.nedap.archie.rminfo.MetaModels;
import com.nedap.openehr.lsp.paths.ArchetypePathReference;
import com.nedap.openehr.lsp.paths.PathUtils;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Range;
import org.openehr.bmm.core.BmmClass;
import org.openehr.bmm.core.BmmProperty;
import org.openehr.referencemodels.BuiltinReferenceModels;

import java.util.ArrayList;
import java.util.List;

/**
 * Hover info for one archetype. TODO: work on flat archetype so specializations work properly
 */
public class ArchetypeHoverInfo extends HoverInfo {

    private MetaModels metaModels = BuiltinReferenceModels.getMetaModels();

    public ArchetypeHoverInfo(DocumentInformation documentInformation, Archetype sourceArchetype, Archetype archetypeForTerms, String language) {
        super(language);
        metaModels.selectModel(sourceArchetype);
        extractHoverInfo(documentInformation, sourceArchetype.getDefinition(), archetypeForTerms);

        if(sourceArchetype instanceof Template) {
            Template template = (Template) sourceArchetype;
            for(TemplateOverlay overlay:template.getTemplateOverlays()) {
                extractHoverInfo(documentInformation, overlay.getDefinition(), archetypeForTerms);
            }
        }

        for(ArchetypePathReference reference:documentInformation.getModelReferences()) {
            //no rules in template overlays, so no hoverinfo needed for path references from rules
            //could become necessary when we add path references from use_node in the same way
            PathUtils.createHoverInfo(this, metaModels, reference, archetypeForTerms);
        }

    }

    private void extractHoverInfo(DocumentInformation documentInformation, CComplexObject definition, Archetype archetypeForTerms) {
        try {
            getHoverInfoForCObject(documentInformation, definition, archetypeForTerms);
        } catch (Exception e) {
            //just continue :)
            e.printStackTrace();
        }

        for(CAttribute attribute:definition.getAttributes()) {
            extractHoverInfo(documentInformation, attribute, archetypeForTerms);
        }
    }

    private void getHoverInfoForCObject(DocumentInformation documentInformation, CObject definition, Archetype archetypeForTerms) {
        ArchetypeTerm term = archetypeForTerms.getTerm(definition, language);
        String content;
        if(term != null) {
            content = "### " + definition.getRmTypeName() + ": " + term.getText() + "\n\n\t" + term.getDescription();
        } else {
            content = "### No term found";
        }
        CObject flattenedObject = (definition instanceof CPrimitiveObject) ? definition : archetypeForTerms.itemAtPath(definition.getPath());
        if(flattenedObject == null) {
            //fallback if something went wrong
            flattenedObject = definition;
        }
        MultiplicityInterval occurrences = flattenedObject.effectiveOccurrences(metaModels::referenceModelPropMultiplicity);
        content += "\n occurrences: " + occurrences;

        content += "\n\n path: " + definition.getPath();

        Hover hover = new Hover();
        hover.setContents(new MarkupContent(MARKDOWN, content.toString()));
        Range range = getHoverRange(documentInformation, definition);
        if(range != null) {
            hoverRanges.addRange(range, hover);
        }
    }

    private void extractHoverInfo(DocumentInformation documentInformation, CAttribute attribute, Archetype archetypeForTerms) {
        for(CObject object:attribute.getChildren()) {
            try {
                if (object instanceof CComplexObject) {
                    extractHoverInfo(documentInformation, (CComplexObject) object, archetypeForTerms);
                } else if (object instanceof CTerminologyCode) {
                    extractHoverInfo(documentInformation, (CTerminologyCode) object, archetypeForTerms);
                } else if (object instanceof ArchetypeSlot) {
                    getHoverInfoForCObject(documentInformation, object, archetypeForTerms);
                }//for the other primitives, hovers should not be important
            } catch (Exception e) {
                //If this fails, fine, continue with the rest of the file!
                e.printStackTrace();//TODO: report to client?
            }
        }
        try {
            CAttribute flatAttribute = archetypeForTerms.itemAtPath(attribute.getPath());
            if (flatAttribute == null) {
                flatAttribute = attribute;
            }
            Cardinality cardinality = null;
            MultiplicityInterval existence = null;
            //TODO: do a proper path lookup through the RM model?
            CAttribute defaults = new BMMConstraintImposer(metaModels.getSelectedBmmModel()).getDefaultAttribute(flatAttribute.getParent().getRmTypeName(), flatAttribute.getRmAttributeName());
            if (flatAttribute.getCardinality() != null) {
                cardinality = flatAttribute.getCardinality();
            } else {
                if (defaults != null) {
                    cardinality = defaults.getCardinality();
                }
            }
            if (flatAttribute.getExistence() != null) {
                existence = flatAttribute.getExistence();
            } else {
                if (defaults != null) {
                    existence = flatAttribute.getExistence();
                }
            }
            boolean multiple = flatAttribute.isMultiple();
            StringBuilder content = new StringBuilder();
            if (cardinality != null) {
                content.append("Cardinality: ");
                content.append(cardinality.toString());
            }
            if (existence != null) {
                content.append(", existence: " + existence.toString());
            }
            content.append(multiple ? "\n multiple valued attribute" : "\n single valued attribute");


            BmmClass classDefinition = metaModels.getSelectedBmmModel().getClassDefinition(flatAttribute.getParent().getRmTypeName());
            if (classDefinition != null) {
                BmmProperty bmmProperty = classDefinition.getFlatProperties().get(flatAttribute.getRmAttributeName());
                if (bmmProperty != null) {
                    content.append("\n\nRM type name: *" + bmmProperty.getType().toDisplayString() + "*");
                }
            }
            content.append("\n\n path: " + attribute.getPath());
            Hover hover = new Hover();
            hover.setContents(new MarkupContent(MARKDOWN, content.toString()));
            Range range = getHoverRange(documentInformation, attribute);
            if(range != null) {
                hoverRanges.addRange(range, hover);
            }
        } catch (Exception e) {
            e.printStackTrace();//TODO: report to client?
        }
    }

    private Range getHoverRange(DocumentInformation documentInformation, CAttribute attribute) {
        DocumentSymbol documentSymbol = documentInformation.lookupCObjectOrAttribute(attribute.path(), false);
        if(documentSymbol == null) {
            return null;
        }
        return documentSymbol.getSelectionRange();
    }

    private Range getHoverRange(DocumentInformation documentInformation, CObject cObject) {
        DocumentSymbol documentSymbol = documentInformation.lookupCObjectOrAttribute(cObject.path(), false);
        if(documentSymbol == null) {
            return null;
        }
        return documentSymbol.getSelectionRange();
    }

    private void extractHoverInfo(DocumentInformation documentInformation, CTerminologyCode object, Archetype archetypeForTerms) {
        DocumentSymbol documentSymbol = documentInformation.lookupCObjectOrAttribute(object.path(), true);//get the closest to an actual location

        //the document symbol tree does not contain terminology codes. So use the separate index
        //perhaps better to add two versions of the tree, one for internal and one for external use?
        //anyway, this works and is fast.
        DocumentSymbol terminologyCodeSymbol = documentInformation.getcTerminologyCodes().getFirstMatchAfter(
                documentSymbol.getSelectionRange().getStart(),
                d -> object.getConstraint().contains(d.getName()));


        if(terminologyCodeSymbol == null) {
            System.err.println("COULD NOT FIND DOCUMENT SYMBOL FOR CTERMINOLOGY CODE");
            return;
        }
        List<TerminologyCodeWithArchetypeTerm> terms = getTerms(object, terminologyCodeSymbol, archetypeForTerms);

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
            Range range = terminologyCodeSymbol.getRange();
            hoverRanges.addRange(range, hover);
        }
    }


    private List<TerminologyCodeWithArchetypeTerm> getTerms(CTerminologyCode cTermCode, DocumentSymbol terminologyCode, Archetype archetypeForTerms) {
        List<TerminologyCodeWithArchetypeTerm> result = new ArrayList();
        ArchetypeTerminology terminology = archetypeForTerms.getTerminology(cTermCode);
        String language = ArchieLanguageConfiguration.getMeaningAndDescriptionLanguage();
        String defaultLanguage = ArchieLanguageConfiguration.getDefaultMeaningAndDescriptionLanguage();
        String constraint = terminologyCode.getName();
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

        return result;
    }
}
