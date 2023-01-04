package com.nedap.openehr.lsp.aql;

import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.ArchetypeModelObject;
import com.nedap.archie.aom.CAttribute;
import com.nedap.archie.aom.CObject;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.archie.paths.PathSegment;
import com.nedap.archie.rminfo.MetaModels;
import com.nedap.healthcare.aqlparser.exception.AQLRuntimeException;
import com.nedap.healthcare.aqlparser.exception.AQLUnsupportedFeatureException;
import com.nedap.healthcare.aqlparser.exception.AQLValidationException;
import com.nedap.healthcare.aqlparser.model.Lookup;
import com.nedap.healthcare.aqlparser.model.clause.QueryClause;
import com.nedap.healthcare.aqlparser.parser.QOMParser;
import com.nedap.healthcare.tolerantaqlparser.ErrorTolerantAQLLexer;
import com.nedap.healthcare.tolerantaqlparser.ErrorTolerantAQLParser;
import com.nedap.openehr.lsp.document.HoverInfo;
import com.nedap.openehr.lsp.paths.ArchetypePathReference;
import com.nedap.openehr.lsp.paths.PartialAOMPathQuery;
import com.nedap.openehr.lsp.paths.PathUtils;
import com.nedap.openehr.lsp.repository.BroadcastingArchetypeRepository;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.util.Ranges;
import org.openehr.bmm.core.BmmClass;
import org.openehr.bmm.core.BmmProperty;
import org.openehr.bmm.persistence.validation.BmmDefinitions;
import org.openehr.referencemodels.BuiltinReferenceModels;

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
        MetaModels metaModels = BuiltinReferenceModels.getMetaModels();
        for(ArchetypePathReference reference:document.getArchetypePathReferences()) {
            if(reference.getArchetypeId() == null) {
                continue;
            }
            try {
                ValidationResult validationResult = archetypeRepository.getValidationResult(reference.getArchetypeId());//TODO: get operational template here. I think that should be cached?
                if (validationResult != null) {
                    PathUtils.createHoverInfo(hoverInfo, metaModels, reference, validationResult.getFlattened());
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



    private AQLDocument createAQLDocument(String uri, String text) {
        ErrorTolerantAQLLexer lexer = new ErrorTolerantAQLLexer(CharStreams.fromString(text));
        ErrorTolerantAQLParser aqlParser = new ErrorTolerantAQLParser(new CommonTokenStream(lexer));
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
        } catch (Exception ex) {
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
        String uri = hoverParams.getTextDocument().getUri();
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
                    PathUtils.createCodeLenses(result, reference, validationResult);
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

    public Either<List<CompletionItem>, CompletionList> completion(CompletionParams position) {
        String uri = position.getTextDocument().getUri();
        AQLDocument aqlDocument = this.aqlDocumentsByUri.get(uri);
        if(aqlDocument == null) {
            return Either.forRight(new CompletionList());
        }
        List<CompletionItem> items = new ArrayList<>();
        CompletionList result = new CompletionList(items);
        MetaModels metaModels = BuiltinReferenceModels.getMetaModels();
        for(ArchetypePathReference reference:aqlDocument.getArchetypePathReferences()) {
            //copy the range, adding one because typing at the end of course
            Range range = new Range();
            range.setStart(new Position(reference.getRange().getStart().getLine(), reference.getRange().getStart().getCharacter()));
            range.setEnd(new Position(reference.getRange().getEnd().getLine(), reference.getRange().getEnd().getCharacter()+1));
            if(reference.getArchetypeId() != null && Ranges.containsPosition(range, position.getPosition())) {
                ValidationResult validationResult = archetypeRepository.getValidationResult(reference.getArchetypeId());//TODO: get operational template here. I think that should be cached?
                if(validationResult != null && validationResult.getFlattened() != null) {
                    //ok we have code completion!
                    //now to find the correct archetype path
                    Archetype flat = validationResult.getFlattened();
                    metaModels.selectModel(flat);
                    PartialAOMPathQuery partialAOMPathQuery = new PartialAOMPathQuery(reference.getPath());
                    PartialAOMPathQuery.PartialMatch partial = partialAOMPathQuery.findLSPPartial(flat.getDefinition());//TODO: get path UP TO where we are typing!
                    //TODO: add BMM path traversal here as well, so you can do /value/magnitude
                    for(ArchetypeModelObject object:partial.getMatches()) {
                        if(object instanceof CObject) {
                            for(CAttribute attribute:((CObject) object).getAttributes()) {
                                for(CObject child:attribute.getChildren()) {
                                    if(child.getNodeId() != null && !"id9999".equalsIgnoreCase(child.getNodeId())) {
                                        String text = child.getTerm() == null ?
                                                attribute.getRmAttributeName() + "[" + child.getNodeId() + "] (" + child.getRmTypeName() + ")" :
                                                child.getTerm().getText() + " (" + attribute.getRmAttributeName() + " " + child.getRmTypeName() + "[" + child.getNodeId()+"])";
                                        CompletionItem completionItem = new CompletionItem(text);
                                        completionItem.setInsertText(attribute.getRmAttributeName() + "[" + child.getNodeId() + "]");
                                        completionItem.setFilterText(attribute.getRmAttributeName() + "[" + child.getNodeId() + "]" + text);
                                        completionItem.setSortText(attribute.getRmAttributeName()+ "0" + text); //the 0 is to sort this before others
                                        completionItem.setKind(CompletionItemKind.Reference);
                                        items.add(completionItem);
                                    }
                                }
                            }
                            BmmClass classDefinition = metaModels.getSelectedBmmModel().getClassDefinition(BmmDefinitions.typeNameToClassKey(((CObject) object).getRmTypeName()));
                            if(classDefinition != null) {
                                for (BmmProperty property : classDefinition.getFlatProperties().values()) {
                                    CompletionItem completionItem = new CompletionItem(property.getName() + "(" + property.getType().toDisplayString() + ")");
                                    completionItem.setInsertText(property.getName());
                                    completionItem.setSortText(property.getName() + "zzzz"); //sort this last please
                                    completionItem.setKind(CompletionItemKind.Reference);
                                    items.add(completionItem);
                                }
                            }
                        } else if (object instanceof CAttribute) {
                            for(CObject child: ((CAttribute) object).getChildren()) {
                                if(child.getNodeId() != null && !child.getNodeId().equalsIgnoreCase("id9999")) {
                                    CompletionItem completionItem = new CompletionItem();
                                    completionItem.setLabel(child.getTerm() == null ? child.getRmTypeName() : child.getTerm().getText());
                                    completionItem.setInsertText("[" + child.getNodeId() + "]");
                                    completionItem.setKind(CompletionItemKind.Reference);
                                    //completionItem.setInsertText();
                                    items.add(completionItem);
                                }
                            }
                        }

                    }

                }
            }
        }
        return Either.forRight(result);
    }
}
