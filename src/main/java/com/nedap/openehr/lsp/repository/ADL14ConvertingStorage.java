package com.nedap.openehr.lsp.repository;

import com.google.common.collect.Lists;
import com.nedap.archie.adl14.ADL14ConversionConfiguration;
import com.nedap.archie.adl14.ADL14Converter;
import com.nedap.archie.adl14.ADL14Parser;
import com.nedap.archie.adl14.ADL2ConversionResult;
import com.nedap.archie.adl14.ADL2ConversionResultList;
import com.nedap.archie.aom.Archetype;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.archie.serializer.adl.ADLArchetypeSerializer;
import com.nedap.openehr.lsp.ADL2TextDocumentService;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.openehr.referencemodels.BuiltinReferenceModels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

public class ADL14ConvertingStorage {
    private final ADL2TextDocumentService textService;
    private Map<String, Archetype> adl14Files = new ConcurrentHashMap<>();
    private BroadcastingArchetypeRepository repository;
    private ADL14ConversionConfiguration configuration = new ADL14ConversionConfiguration();//TODO: fill :)

    public ADL14ConvertingStorage(ADL2TextDocumentService service, BroadcastingArchetypeRepository repository) {
        this.repository = repository;
        this.textService = service;
    }

    public void addFile(TextDocumentItem item) {
        ADL14Parser adl14Parser = new ADL14Parser(BuiltinReferenceModels.getMetaModels());
        Archetype archetype = adl14Parser.parse(item.getText(), configuration);
        if(adl14Parser.getErrors().hasErrors()) {
            textService.pushDiagnostics(new VersionedTextDocumentIdentifier(item.getUri(), item.getVersion()),  adl14Parser.getErrors());
        } else {
            textService.pushDiagnostics(new VersionedTextDocumentIdentifier(item.getUri(), item.getVersion()),  null, new ValidationResult(archetype));
            adl14Files.put(item.getUri(), archetype);
        }
    }


    public void convert(String documentUri) {
        ADL14Converter adl14Converter = new ADL14Converter(BuiltinReferenceModels.getMetaModels(), configuration);
        adl14Converter.setExistingRepository(repository);
        Archetype archetype = adl14Files.get(documentUri);
        //find all parent archetypes that must also be converted for this to properly work
        List<Archetype> toConvert = getAllToConvertIncludingParents(archetype);
        ADL2ConversionResultList converted = adl14Converter.convert(toConvert);
        for(ADL2ConversionResult result:converted.getConversionResults()) {
            if(result.getException() != null) {
                textService.pushDiagnostics(new TextDocumentIdentifier(documentUri), result.getException());
            } else {
                String newPath = documentUri.substring(0, documentUri.lastIndexOf("/")) + "/adl2/" + result.getArchetypeId() + ".adls";
                textService.writeFile(newPath, "ADL2 conversion of " + result.getArchetypeId(), ADLArchetypeSerializer.serialize(result.getArchetype()));
            }
        }

    }

    private List<Archetype> getAllToConvertIncludingParents(Archetype archetype) {
        List<Archetype> toConvert = new ArrayList<>();
        toConvert.add(archetype);
        Stack<Archetype> toAdd = new Stack();
        toAdd.push(archetype);
        while(!toAdd.isEmpty()) {
            Archetype a = toAdd.pop();
            //if parent archetype already present in ADL 2 form, do not convert
            //it will be taken from ADL 2 form instead
            //user can override using the convert all adl 1.4 archetypes
            if (a.getParentArchetypeId() != null && repository.getArchetype(a.getParentArchetypeId()) == null) {

                for (Archetype possibleParent : this.adl14Files.values()) {
                    //ADL 1.4, so simple string comparison is enough
                    if (a.getParentArchetypeId().equalsIgnoreCase(possibleParent.getArchetypeId().toString())) {
                        toConvert.add(possibleParent);
                        toAdd.push(possibleParent);
                    }
                }
            }
        }
        return toConvert;
    }

    public void convertAll(String rootUri) {
        ADL14Converter adl14Converter = new ADL14Converter(BuiltinReferenceModels.getMetaModels(), configuration);
        adl14Converter.setExistingRepository(repository);
        ADL2ConversionResultList converted = adl14Converter.convert(new ArrayList<>(adl14Files.values()));
        for(ADL2ConversionResult result:converted.getConversionResults()) {
            if(result.getException() != null) {
                textService.pushDiagnostics(new TextDocumentIdentifier(rootUri), result.getException());
            } else {
                String newPath = rootUri.substring(0, rootUri.lastIndexOf("/")) + "/out/" + result.getArchetypeId() + ".adls";
                textService.writeFile(newPath, "ADL2 conversion of " + result.getArchetypeId(), ADLArchetypeSerializer.serialize(result.getArchetype()));
            }
        }

    }

    public Archetype getArchetype(TextDocumentIdentifier textDocument) {
        return this.adl14Files.get(textDocument.getUri());

    }

}
