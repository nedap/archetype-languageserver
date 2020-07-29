package com.nedap.openehr.lsp.aql;

import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.ArchetypeModelObject;
import com.nedap.archie.aom.CAttribute;
import com.nedap.archie.aom.CObject;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.healthcare.aqlparser.AQLLexer;
import com.nedap.healthcare.aqlparser.AQLParser;
import com.nedap.openehr.lsp.document.HoverInfo;
import com.nedap.openehr.lsp.repository.BroadcastingArchetypeRepository;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.TextDocumentItem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AQLStorage {

    private final BroadcastingArchetypeRepository archetypeRepository;
    private final Map<String, AQLDocument> aqlDocumentsByUri = new LinkedHashMap<>();

    public AQLStorage(BroadcastingArchetypeRepository archetypeRepository) {
        this.archetypeRepository = archetypeRepository;
    }

    public void addOrUpdate(TextDocumentItem textDocumentItem) { AQLDocument document = createAQLDocument(textDocumentItem.getUri(), textDocumentItem.getText());

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
                            String text = content + partial.getRemainingQuery().stream().map(p -> p.toString()).collect(Collectors.joining("/"));
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
}
