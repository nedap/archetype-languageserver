package com.nedap.openehr.lsp.document;

import com.nedap.archie.antlr.errors.ANTLRParserErrors;
import com.nedap.archie.paths.PathSegment;
import com.nedap.archie.query.APathQuery;
import com.nedap.openehr.lsp.utils.DocumentSymbolUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentInformation {

    public static final String DEFINITION_SECTION_NAME = "definition section";
    public static final String TERMINOLOGY_SECTION_NAME = "terminology section";
    public static final String RM_OVERLAY_SECTION_NAME = "rm_overlay section";
    public static final String LANGUAGE_SECTION_NAME = "language section";
    public static final String SPECIALISATION_SECTION_NAME = "specialisation section";
    public static final String DESCRIPTION_SECTION_NAME = "description section";
    public static final String ANNOTATIONS_SECTION_NAME = "annotations section";
    public static final String RULES_SECTION_NAME = "rules section";
    public static final String TERM_DEFINITIONS_NAME = "term_definitions";


    private String archetypeId;
    private ADLVersion adlVersion;
    private ANTLRParserErrors errors;
    private List<Either<SymbolInformation, DocumentSymbol>> symbols;
    private List<FoldingRange> foldingRanges;
    private ArchetypeHoverInfo hoverInfo;
    private DocumentLinks documentLinks;
    /** the current list of diagnostics for this archetype file */
    private List<Diagnostic> diagnostics;
    private final CodeRangeIndex<DocumentSymbol> cTerminologyCodes;

    public DocumentInformation(String archetypeId, ADLVersion adlVersion, ANTLRParserErrors errors,
                               List<Either<SymbolInformation, DocumentSymbol>> symbols,
                               List<FoldingRange> foldingRanges,
                               List<DocumentLink> documentLinks) {
        this(archetypeId, adlVersion, errors, symbols, foldingRanges, documentLinks, new CodeRangeIndex<>());
    }

    public DocumentInformation(String archetypeId, ADLVersion adlVersion, ANTLRParserErrors errors, List<Either<SymbolInformation, DocumentSymbol>> symbols, List<FoldingRange> foldingRanges, List<DocumentLink> documentLinks, CodeRangeIndex<DocumentSymbol> cTerminologyCodes) {
        this.archetypeId = archetypeId;
        this.adlVersion = adlVersion;
        this.errors = errors;
        this.symbols = symbols;
        this.foldingRanges = foldingRanges;
        this.documentLinks = new DocumentLinks(documentLinks);
        this.cTerminologyCodes = cTerminologyCodes;
    }

    public String getArchetypeId() {
        return archetypeId;
    }

    public ANTLRParserErrors getErrors() {
        return errors;
    }

    public List<Either<SymbolInformation, DocumentSymbol>> getSymbols() {
        return symbols;
    }

    public List<FoldingRange> getFoldingRanges() {
        return foldingRanges;
    }

    public ArchetypeHoverInfo getHoverInfo() {
        return hoverInfo;
    }

    public Hover getHoverInfo(HoverParams params) {
        if(hoverInfo == null) {
            return null;
        }
        return hoverInfo.getHoverInfo(params);
    }

    public void setHoverInfo(ArchetypeHoverInfo hoverInfo) {
        this.hoverInfo = hoverInfo;
    }

    public DocumentLinks getDocumentLinks() {
        return documentLinks;
    }

    public void setDocumentLinks(DocumentLinks documentLinks) {
        this.documentLinks = documentLinks;
    }

    public List<DocumentLink> getAllDocumentLinks() {
        if(documentLinks == null) {
            return null;
        }
        return documentLinks.getAllDocumentLinks();
    }

    public ADLVersion getADLVersion() {
        return adlVersion;
    }

    public void setDiagnostics(List<Diagnostic> diagnostics) {
        this.diagnostics = diagnostics;
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    private static final Pattern cComplexObjectPattern = Pattern.compile("(?<type>.*)\\[(?<code>.*)\\]");


    public DocumentSymbol lookupCObjectOrAttribute(String path) {
        return lookupCObjectOrAttribute(path, true);
    }

    /**
     * Archetype path lookup. Definition only for now. Sorry for the ugly code, but this works rather well :)
     * @param path
     * @return
     */
    public DocumentSymbol lookupCObjectOrAttribute(String path, boolean returnResultSoFar) {
        APathQuery query = new APathQuery(path);
        List<PathSegment> pathSegments = query.getPathSegments();
        List<DocumentSymbol> documentSymbols = DocumentSymbolUtils.getDocumentSymbols(symbols);

        if(documentSymbols.isEmpty()) {
            return null;
        }
        DocumentSymbol definitionSections = DocumentSymbolUtils.getDocumentSymbolOrThrow(documentSymbols.get(0).getChildren(), DEFINITION_SECTION_NAME);
        DocumentSymbol rootNode = definitionSections.getChildren().get(0);
        DocumentSymbol currentSymbol = rootNode;
        for(int i = 0; i < pathSegments.size(); i++) {
            PathSegment segment = pathSegments.get(i);
            //search attribute
            if(currentSymbol.getChildren() == null || currentSymbol.getChildren().isEmpty()) {
                return currentSymbol;
            }
            {
                DocumentSymbol foundAttribute = findAttribute(currentSymbol, segment, pathSegments, i);
                if (foundAttribute == null) {
                    return returnResultSoFar ? currentSymbol : null;
                } else if(foundAttribute.getName().startsWith("/")) {
                    //differential path. Skip a couple of path segments
                    int numberOfSegments = new APathQuery(foundAttribute.getName()).getPathSegments().size();
                    i = i + numberOfSegments - 1;
                    segment = pathSegments.get(i);
                }


                currentSymbol = foundAttribute;
            }

            if(currentSymbol.getChildren() == null || currentSymbol.getChildren().isEmpty()) {
                return returnCurrentSymbolIfPossible(returnResultSoFar, pathSegments, currentSymbol, i);
            }

            //search for code

            if(segment.getIndex() != null) {
                if(segment.getIndex() > currentSymbol.getChildren().size()) {
                    return returnResultSoFar ? currentSymbol : null;
                }
                currentSymbol = currentSymbol.getChildren().get(segment.getIndex()-1);
            } else if(segment.hasIdCode() || segment.hasArchetypeRef()) {
                DocumentSymbol foundCObject = findCObject(currentSymbol, segment);
                if(foundCObject == null) {
                    return returnCurrentSymbolIfPossible(returnResultSoFar, pathSegments, currentSymbol, i);
                }
                currentSymbol = foundCObject;
            } else {
                if(i == pathSegments.size() -1) {
                    //this just points at an attribute, that's ok.
                    return returnCurrentSymbolIfPossible(returnResultSoFar, pathSegments, currentSymbol, i);
                }
                // let's hope it's size 1
                else if(currentSymbol.getChildren() == null || currentSymbol.getChildren().isEmpty()) {
                    return returnCurrentSymbolIfPossible(returnResultSoFar, pathSegments, currentSymbol, i);
                } else if(currentSymbol.getChildren().size() == 1) {
                    currentSymbol = currentSymbol.getChildren().get(0);
                } else {
                    throw new RuntimeException("cannot find path, it has too many possible values without an id at segment " + segment);
                }
            }
        }
        return currentSymbol;
    }

    private DocumentSymbol findCObject(DocumentSymbol currentSymbol, PathSegment segment) {
        for(DocumentSymbol cObject:currentSymbol.getChildren()) {
            String nameAndCode = cObject.getDetail();
            if(nameAndCode == null) {
                nameAndCode = cObject.getName();
            }
            if(nameAndCode == null) {
                continue;//right...
            }
            Matcher matcher = cComplexObjectPattern.matcher(nameAndCode);
            if (matcher.matches()) {
                String typeName = matcher.group("type"); //don't need this, but ok
                String code = matcher.group("code");
                if(segment.hasIdCode() && segment.getNodeId().equalsIgnoreCase(code)) {
                    return cObject;
                } else if (segment.hasArchetypeRef() && segment.getArchetypeRef().equalsIgnoreCase(code)) {
                    return cObject;
                }
            }
        }
        return null;
    }

    private DocumentSymbol findAttribute(DocumentSymbol currentSymbol, PathSegment segment, List<PathSegment> allSegments, int index) {
        for(DocumentSymbol attribute:currentSymbol.getChildren()) {
            if (attribute.getName().equalsIgnoreCase(segment.getNodeName())) {
                return attribute;
            }
            if(attribute.getName().startsWith("/")) {
                //this is a differential path
                if(checkDifferentialMatch(allSegments.subList(index, allSegments.size()), new APathQuery(attribute.getName()).getPathSegments())) {
                    return attribute;
                }
            }

        }
        return null;
    }

    private boolean checkDifferentialMatch(List<PathSegment> pathSegments, List<PathSegment> differentialPathSegments) {
        if(differentialPathSegments.size() <= pathSegments.size()) {
            for(int i = 0; i < differentialPathSegments.size(); i++) {
                PathSegment segment = pathSegments.get(i);
                PathSegment differentialPathSegment = differentialPathSegments.get(i);
                if(!matches(segment, differentialPathSegment)) {
                    return false;
                }
            }
            return true;
        }
        return false;

    }

    private boolean matches(PathSegment segment, PathSegment differentialPathSegment) {
        if(differentialPathSegment.getNodeId() == null) {
            return segment.getNodeName().equalsIgnoreCase(differentialPathSegment.getNodeName());
        } else {
            return segment.getNodeName().equalsIgnoreCase(differentialPathSegment.getNodeName()) &&
                    segment.getNodeId().equals(differentialPathSegment.getNodeId());
        }
    }

    private DocumentSymbol returnCurrentSymbolIfPossible(boolean returnResultSoFar, List<PathSegment> pathSegments, DocumentSymbol currentSymbol, int index) {
        if (index >= pathSegments.size()-1) {
            return currentSymbol;
        }
        return returnResultSoFar ? currentSymbol : null;
    }

    public CodeRangeIndex<DocumentSymbol> getcTerminologyCodes() {
        return cTerminologyCodes;
    }
}
