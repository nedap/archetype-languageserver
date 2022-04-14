package com.nedap.openehr.lsp.paths;

import com.google.common.collect.Lists;
import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.ArchetypeModelObject;
import com.nedap.archie.aom.CAttribute;
import com.nedap.archie.aom.CObject;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.archie.paths.PathSegment;
import com.nedap.archie.rminfo.MetaModels;
import com.nedap.openehr.lsp.ADL2TextDocumentService;
import com.nedap.openehr.lsp.document.HoverInfo;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.openehr.bmm.core.BmmClass;
import org.openehr.bmm.core.BmmProperty;
import org.openehr.bmm.persistence.validation.BmmDefinitions;

import java.util.List;
import java.util.stream.Collectors;

public class PathUtils {

    public static void createCodeLenses(List<CodeLens> result, ArchetypePathReference reference, ValidationResult validationResult) {
        Archetype flattened = validationResult.getFlattened();
        if (flattened != null) {
            PartialAOMPathQuery aomPathQuery = new PartialAOMPathQuery(reference.getPath());
            PartialAOMPathQuery.PartialMatch partial = aomPathQuery.findLSPPartial(flattened.getDefinition());
            if (partial.getMatches().size() > 0) {
                ArchetypeModelObject archetypeModelObject = partial.getMatches().get(0);
                String content = null;
                String description = null;
                if(archetypeModelObject instanceof CAttribute) {
                    content = findNearestText((CAttribute) archetypeModelObject);
                    description = findNearestDescription((CAttribute) archetypeModelObject);
                } else if (archetypeModelObject instanceof CObject){
                    content = findNearestText((CObject) archetypeModelObject);
                    description = findNearestDescription((CObject) archetypeModelObject);
                }
                String text = content + partial.getRemainingQuery().stream().map(PathSegment::toString).collect(Collectors.joining("/"));
                String extraText = description;
                extraText += "\n\nIn Archetype " + flattened.getDefinition().getTerm().getText() + " (" + reference.getArchetypeId() +")";
                if(reference.getExtraInformation() != null) {
                    text += " " + reference.getExtraInformation();
                }

                //result.add(new CodeLens(reference.getRange(), new Command(text, ADL2TextDocumentService.SHOW_INFO_COMMAND, Lists.newArrayList(extraText)), null));
                result.add(new CodeLens(reference.getRange(), new Command(text, ADL2TextDocumentService.SHOW_INFO_COMMAND, Lists.newArrayList(extraText)), null));
            } else {

                //hoverInfo.getHoverRanges().addRange(reference.getRange(), new Hover(new MarkupContent(HoverInfo.MARKDOWN, "path " + reference.getPath() + " not found")));
            }
        } else {
            //hoverInfo.getHoverRanges().addRange(reference.getRange(), new Hover(new MarkupContent(HoverInfo.MARKDOWN, "flattened archetype not found")));
        }
    }

    public static void createHoverInfo(HoverInfo hoverInfo, MetaModels metaModels, ArchetypePathReference reference, Archetype flattened) {
        metaModels.selectModel(flattened);
        if (flattened != null) {
            PartialAOMPathQuery aomPathQuery = new PartialAOMPathQuery(reference.getPath());
            PartialAOMPathQuery.PartialMatch partial = aomPathQuery.findLSPPartial(flattened.getDefinition());
            if (partial.getMatches().size() > 0) {
                ArchetypeModelObject archetypeModelObject = partial.getMatches().get(0);
                String content = null;
                String description = null;
                String typeName = "";
                if(archetypeModelObject instanceof CAttribute) {
                    CAttribute attribute = (CAttribute) archetypeModelObject;
                    content = findNearestText((CAttribute) archetypeModelObject);
                    description = findNearestDescription((CAttribute) archetypeModelObject);
                    CObject parent = attribute.getParent();
                    if(partial.getRemainingQuery().isEmpty()) { //TODO: proper path lookup here
                        BmmClass classDefinition = metaModels.getSelectedBmmModel().getClassDefinition(BmmDefinitions.typeNameToClassKey(parent.getRmTypeName()));
                        if (classDefinition != null) {
                            BmmProperty bmmProperty = classDefinition.getFlatProperties().get(attribute.getRmAttributeName());
                            if (bmmProperty != null) {
                                bmmProperty.getType().toDisplayString();
                            }
                        }
                    }
                } else if (archetypeModelObject instanceof CObject){
                    content = findNearestText((CObject) archetypeModelObject);
                    description = findNearestDescription((CObject) archetypeModelObject);
                    if(partial.getRemainingQuery().isEmpty()) { //TODO: proper path lookup here.
                        typeName = ((CObject) archetypeModelObject).getRmTypeName();
                    }
                }
                String text = content + partial.getRemainingQuery().stream().map(PathSegment::toString).collect(Collectors.joining("/"));
                text += "\n\n" + typeName;
                text += "\n\n" + description;
                text += "\n\nIn Archetype " + flattened.getDefinition().getTerm().getText() + " (" + reference.getArchetypeId() +")";
                if(reference.getExtraInformation() != null) {
                    text += "\n\n" + reference.getExtraInformation();
                }
                hoverInfo.getHoverRanges().addRange(reference.getRange(), new Hover(new MarkupContent(HoverInfo.MARKDOWN, text), reference.getRange()));
            } else {
                hoverInfo.getHoverRanges().addRange(reference.getRange(), new Hover(new MarkupContent(HoverInfo.MARKDOWN, "path " + reference.getPath() + " not found"), reference.getRange()));
            }
        } else {
            hoverInfo.getHoverRanges().addRange(reference.getRange(), new Hover(new MarkupContent(HoverInfo.MARKDOWN, "flattened archetype not found"), reference.getRange()));
        }
    }

    public static String findNearestText(CAttribute attribute) {
        return findNearestText(attribute.getParent()) + "/" + attribute.getRmAttributeName();
    }

    public static String findNearestText(CObject cObject) {
        if(cObject.getTerm() != null) {
            return cObject.getTerm().getText();
        }
        return findNearestText(cObject.getParent());
    }

    public static String findNearestDescription(CAttribute attribute) {
        return findNearestText(attribute.getParent());
    }

    public static String findNearestDescription(CObject cObject) {
        if(cObject.getTerm() != null) {
            return cObject.getTerm().getDescription();
        }
        return findNearestText(cObject.getParent());
    }

}
