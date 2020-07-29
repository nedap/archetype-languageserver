package com.nedap.openehr.lsp.aql;

import com.google.common.collect.Lists;
import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.ArchetypeModelObject;
import com.nedap.archie.aom.CAttribute;
import com.nedap.archie.aom.CObject;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.archie.paths.PathSegment;
import com.nedap.healthcare.aqlparser.AQLLexer;
import com.nedap.healthcare.aqlparser.AQLParser;
import com.nedap.healthcare.aqlparser.exception.AQLRuntimeException;
import com.nedap.healthcare.aqlparser.exception.AQLUnsupportedFeatureException;
import com.nedap.healthcare.aqlparser.exception.AQLValidationException;
import com.nedap.healthcare.aqlparser.model.Lookup;
import com.nedap.healthcare.aqlparser.model.clause.QueryClause;
import com.nedap.healthcare.aqlparser.parser.QOMParser;
import com.nedap.openehr.lsp.ADL2TextDocumentService;
import com.nedap.openehr.lsp.document.HoverInfo;
import com.nedap.openehr.lsp.repository.BroadcastingArchetypeRepository;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.lsp4j.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AQLStorage {

    private final BroadcastingArchetypeRepository archetypeRepository;
    private final Map<String, AQLDocument> aqlDocumentsByUri = new LinkedHashMap<>();

    public AQLStorage(BroadcastingArchetypeRepository archetypeRepository) {
        this.archetypeRepository = archetypeRepository;
    }

    public void addOrUpdate(TextDocumentItem textDocumentItem) {
        AQLDocument document = createAQLDocument(textDocumentItem.getUri(), textDocumentItem.getText());

        aqlDocumentsByUri.put(textDocumentItem.getUri(), document);
        extractHoverInfo(document);

    }

    private void extractHoverInfo(AQLDocument document) {
        HoverInfo hoverInfo = new HoverInfo("aql");
        for(ArchetypePathReference reference:document.getArchetypePathReferences()) {
            if(reference.getArchetypeId() == null) {
                continue;
            }
            try {
                ValidationResult validationResult = archetypeRepository.getValidationResult(reference.getArchetypeId());//TODO: get operational template here. I think that should be cached?
                if (validationResult != null) {
                    Archetype flattened = validationResult.getFlattened();
                    if (flattened != null) {
                        PartialAOMPathQuery aomPathQuery = new PartialAOMPathQuery(reference.getPath());
                        PartialAOMPathQuery.PartialMatch partial = aomPathQuery.findPartial(flattened.getDefinition());
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
                            text += "\n\n" + description;
                            text += "\n\nIn Archetype " + flattened.getDefinition().getTerm().getText() + " (" + reference.getArchetypeId() +")";
                            hoverInfo.getHoverRanges().addRange(reference.getRange(), new Hover(new MarkupContent(HoverInfo.MARKDOWN, text)));
                        } else {
                            hoverInfo.getHoverRanges().addRange(reference.getRange(), new Hover(new MarkupContent(HoverInfo.MARKDOWN, "path " + reference.getPath() + " not found")));
                        }
                    } else {
                        hoverInfo.getHoverRanges().addRange(reference.getRange(), new Hover(new MarkupContent(HoverInfo.MARKDOWN, "flattened archetype not found")));
                    }
                } else {
                    hoverInfo.getHoverRanges().addRange(reference.getRange(), new Hover(new MarkupContent(HoverInfo.MARKDOWN, "archetype not found")));
                }
            } catch (Exception e) {
                //ok... report as diagnostics or log
                e.printStackTrace();
            }
        }
        document.setHoverInfo(hoverInfo);
    }

    private String findNearestText(CAttribute attribute) {
        return findNearestText(attribute.getParent()) + "/" + attribute.getRmAttributeName();
    }

    private String findNearestText(CObject cObject) {
        if(cObject.getTerm() != null) {
            return cObject.getTerm().getText();
        }
        return findNearestText(cObject.getParent());
    }

    private String findNearestDescription(CAttribute attribute) {
        return findNearestText(attribute.getParent());
    }

    private String findNearestDescription(CObject cObject) {
        if(cObject.getTerm() != null) {
            return cObject.getTerm().getDescription();
        }
        return findNearestText(cObject.getParent());
    }

    private AQLDocument createAQLDocument(String uri, String text) {
        AQLLexer lexer = new AQLLexer(CharStreams.fromString(text));
        AQLParser aqlParser = new AQLParser(new CommonTokenStream(lexer));
        AQLSymbolListener aqlSymbolListener = new AQLSymbolListener();
        new ParseTreeWalker().walk(aqlSymbolListener, aqlParser.queryClause());

        boolean errorFound = false;
        try {
            Lookup lookup = new Lookup();
            QueryClause parsed = QOMParser.parse(text, lookup);
            //we don't care about the result, not going to use the object model, but this does both parsing and validation

        } catch (AQLUnsupportedFeatureException ex) {
            //I don't WANT this one, I want it to parse whatever it can and ignore this
            List<Diagnostic> diagnostics = new ArrayList<>();
            diagnostics.add(new Diagnostic(
                    new Range(new Position(0, 0), new Position(0, 50)),
                    ex.getMessage()));
            PublishDiagnosticsParams params = new PublishDiagnosticsParams(uri, diagnostics);
            archetypeRepository.getTextDocumentService().publishDiagnostics(params);
            errorFound = true;
        } catch (AQLValidationException ex) {
            List<Diagnostic> diagnostics = new ArrayList<>();
            Range range;
            if(ex.getLineNumber() != null) {
                range = new Range(new Position(ex.getLineNumber()-1, ex.getCharPosition()), new Position(ex.getLineNumber()-1, ex.getCharPosition() + ex.getLength()));
            } else {
                range = new Range(new Position(0, 0), new Position(0, 50));
            }
            diagnostics.add(new Diagnostic(range, ex.getMessageWithoutLineNumbers()));
            PublishDiagnosticsParams params = new PublishDiagnosticsParams(uri, diagnostics);
            archetypeRepository.getTextDocumentService().publishDiagnostics(params);
            errorFound = true;
        } catch (AQLRuntimeException ex) {
            List<Diagnostic> diagnostics = new ArrayList<>();
            diagnostics.add(new Diagnostic(
                    new Range(new Position(0, 0), new Position(0, 50)),
                    ex.getMessage()));
            PublishDiagnosticsParams params = new PublishDiagnosticsParams(uri, diagnostics);
            archetypeRepository.getTextDocumentService().publishDiagnostics(params);
            errorFound = true;
        }
        if(!errorFound) {
            archetypeRepository.getTextDocumentService().publishDiagnostics(new PublishDiagnosticsParams(uri, new ArrayList<>()));
        }

        return new AQLDocument(uri,
                aqlSymbolListener.getSymbolToArchetypeIdMap(),
                aqlSymbolListener.getArchetypePathReferences(),
                new LinkedHashMap<>());
    }

    public Hover getHover(HoverParams hoverParams) {
        String uri = hoverParams.getTextDocument() == null ?
                hoverParams.getUri() : hoverParams.getTextDocument().getUri();
        AQLDocument aqlDocument = aqlDocumentsByUri.get(uri);
        if(aqlDocument == null || aqlDocument.getHoverInfo() == null) {
            return null;
        }
        return aqlDocument.getHoverInfo().getHoverInfo(hoverParams);
    }

    public List<? extends CodeLens> getCodeLens(CodeLensParams params) {
        AQLDocument document = this.aqlDocumentsByUri.get(params.getTextDocument().getUri());
        if(document == null) {
            return new ArrayList<>();
        }
        List<CodeLens> result = new ArrayList<>();
        for(ArchetypePathReference reference:document.getArchetypePathReferences()) {
            if(reference.getArchetypeId() == null) {
                continue;
            }
            try {
                ValidationResult validationResult = archetypeRepository.getValidationResult(reference.getArchetypeId());//TODO: get operational template here. I think that should be cached?
                if (validationResult != null) {
                    Archetype flattened = validationResult.getFlattened();
                    if (flattened != null) {
                        PartialAOMPathQuery aomPathQuery = new PartialAOMPathQuery(reference.getPath());
                        PartialAOMPathQuery.PartialMatch partial = aomPathQuery.findPartial(flattened.getDefinition());
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
                            //result.add(new CodeLens(reference.getRange(), new Command(text, ADL2TextDocumentService.SHOW_INFO_COMMAND, Lists.newArrayList(extraText)), null));
                            result.add(new CodeLens(reference.getRange(), new Command(text, ADL2TextDocumentService.SHOW_INFO_COMMAND, Lists.newArrayList(extraText)), null));
                        } else {

                            //hoverInfo.getHoverRanges().addRange(reference.getRange(), new Hover(new MarkupContent(HoverInfo.MARKDOWN, "path " + reference.getPath() + " not found")));
                        }
                    } else {
                        //hoverInfo.getHoverRanges().addRange(reference.getRange(), new Hover(new MarkupContent(HoverInfo.MARKDOWN, "flattened archetype not found")));
                    }
                } else {
                    //hoverInfo.getHoverRanges().addRange(reference.getRange(), new Hover(new MarkupContent(HoverInfo.MARKDOWN, "archetype not found")));
                }
            } catch (Exception e) {
                //ok... report as diagnostics or log
                e.printStackTrace();
            }
        }
        return result;
    }
}
