package com.nedap.openehr.lsp.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonPrimitive;
import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.OperationalTemplate;
import com.nedap.archie.base.OpenEHRBase;
import com.nedap.archie.creation.ExampleJsonInstanceGenerator;
import com.nedap.archie.flattener.Flattener;
import com.nedap.archie.flattener.FlattenerConfiguration;
import com.nedap.archie.json.JacksonUtil;
import com.nedap.archie.json.flat.DuplicateKeyException;
import com.nedap.archie.json.flat.FlatJsonExampleInstanceGenerator;
import com.nedap.archie.json.flat.FlatJsonFormatConfiguration;
import com.nedap.archie.json.flat.FlatJsonGenerator;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import com.nedap.archie.serializer.adl.ADLArchetypeSerializer;
import com.nedap.archie.xml.JAXBUtil;
import com.nedap.openehr.lsp.ADL2TextDocumentService;
import com.nedap.openehr.lsp.document.DocumentInformation;
import com.nedap.openehr.lsp.repository.BroadcastingArchetypeRepository;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.openehr.referencemodels.BuiltinReferenceModels;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.Map;

public class GenerateExampleCommand {

    private BroadcastingArchetypeRepository storage;
    private ADL2TextDocumentService textDocumentService;
    private ExecuteCommandParams params;

    public GenerateExampleCommand(BroadcastingArchetypeRepository storage, ADL2TextDocumentService textDocumentService, ExecuteCommandParams params) {
        this.storage = storage;
        this.textDocumentService = textDocumentService;
        this.params = params;
    }

    public void apply() {
        String documentUri = ((JsonPrimitive) params.getArguments().get(0)).getAsString();
        String format = ((JsonPrimitive) params.getArguments().get(1)).getAsString();
        String serializedExample = null;
        DocumentInformation documentInformation = storage.getDocumentInformation(documentUri);
        Archetype archetype = storage.getArchetype(documentInformation.getArchetypeId());
        if(archetype == null) {
            archetype = storage.getOperationalTemplate(documentInformation.getArchetypeId());
        }
        OperationalTemplate opt;
        if(archetype instanceof OperationalTemplate) {
            opt = (OperationalTemplate) archetype;
        } else {
            Flattener flattener = new Flattener(storage, BuiltinReferenceModels.getMetaModels(), FlattenerConfiguration.forOperationalTemplate());
            opt = (OperationalTemplate) flattener.flatten(archetype);
        }

        ExampleJsonInstanceGenerator exampleJsonInstanceGenerator = new ExampleJsonInstanceGenerator(BuiltinReferenceModels.getMetaModels(), archetype.getOriginalLanguage().getCodeString());
        Map<String, Object> exampleMap = exampleJsonInstanceGenerator.generate(opt);
        String extension;
        ObjectMapper objectMapper = JacksonUtil.getObjectMapper();
        String jsonRmObject = null;
        OpenEHRBase example;
        try {
            jsonRmObject = objectMapper.writeValueAsString(exampleMap);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        switch(format) {
            case "json":
                extension = "_example.json";
                serializedExample = jsonRmObject;
                break;
            case "flat_json":
                extension = "_flat_example.json";
                FlatJsonGenerator flatJsonGenerator = new FlatJsonGenerator(ArchieRMInfoLookup.getInstance(), FlatJsonFormatConfiguration.nedapInternalFormat());
                try {
                    example = objectMapper.readValue(jsonRmObject, OpenEHRBase.class);
                    Map<String, Object> flatExample = flatJsonGenerator.buildPathsAndValues(example);
                    serializedExample = JacksonUtil.getObjectMapper().writeValueAsString(flatExample);
                } catch (JsonProcessingException | DuplicateKeyException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "xml": {
                extension = "_example.xml";
                try {
                    example = objectMapper.readValue(jsonRmObject, OpenEHRBase.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                StringWriter sw = new StringWriter();
                try {
                    Marshaller marshaller = JAXBUtil.getArchieJAXBContext().createMarshaller();
                    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                    marshaller.marshal(example, sw);
                } catch (JAXBException e) {
                    throw new RuntimeException(e);
                }
                serializedExample = sw.toString();
                break;
            }
            //
            default:
                throw new UnsupportedOperationException("unsupported format: " + format);
        }
        int lastSlash = documentUri.lastIndexOf("/");
        String uriToWrite = documentUri.substring(0, lastSlash == -1 ? 0 : lastSlash) + "/example/" + opt.getArchetypeId() + extension;
        textDocumentService.writeFile(uriToWrite, "opt in " + format, serializedExample);
    }
}
