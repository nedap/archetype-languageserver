package com.nedap.openehr.lsp.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonPrimitive;
import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.OperationalTemplate;
import com.nedap.archie.flattener.Flattener;
import com.nedap.archie.flattener.FlattenerConfiguration;
import com.nedap.archie.json.JacksonUtil;
import com.nedap.archie.serializer.adl.ADLArchetypeSerializer;
import com.nedap.archie.xml.JAXBUtil;
import com.nedap.openehr.lsp.ADL2TextDocumentService;
import com.nedap.openehr.lsp.document.DocumentInformation;
import com.nedap.openehr.lsp.repository.BroadcastingArchetypeRepository;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.openehr.referencemodels.BuiltinReferenceModels;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Result;
import java.io.StringWriter;

public class ConvertToOptCommand {

    private BroadcastingArchetypeRepository storage;
    private ADL2TextDocumentService textDocumentService;
    private ExecuteCommandParams params;

    public ConvertToOptCommand(BroadcastingArchetypeRepository storage, ADL2TextDocumentService textDocumentService, ExecuteCommandParams params) {
        this.storage = storage;
        this.textDocumentService = textDocumentService;
        this.params = params;
    }

    public void apply() {
        String documentUri = ((JsonPrimitive) params.getArguments().get(0)).getAsString();
        String format = ((JsonPrimitive) params.getArguments().get(1)).getAsString();
        String serializedOpt = null;
        Flattener flattener = new Flattener(storage, BuiltinReferenceModels.getMetaModels(), FlattenerConfiguration.forOperationalTemplate());
        DocumentInformation documentInformation = storage.getDocumentInformation(documentUri);
        Archetype archetype = storage.getArchetype(documentInformation.getArchetypeId());
        OperationalTemplate opt = (OperationalTemplate) flattener.flatten(archetype);
        String extension;
        switch(format) {
            case "adl":
                extension = ".opt2";
                serializedOpt = ADLArchetypeSerializer.serialize(opt);
                break;
            case "json":
                extension = "_opt.json";
                try {
                    serializedOpt = JacksonUtil.getObjectMapper().writeValueAsString(opt);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "xml": {
                extension = "_opt.xml";
                StringWriter sw = new StringWriter();
                try {
                    Marshaller marshaller = JAXBUtil.getArchieJAXBContext().createMarshaller();
                    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                    marshaller.marshal(opt, sw);
                } catch (JAXBException e) {
                    throw new RuntimeException(e);
                }
                serializedOpt = sw.toString();
                break;
            }
                //
            default:
                throw new UnsupportedOperationException("unsupported format: " + format);
        }
        int lastSlash = documentUri.lastIndexOf("/");
        if(lastSlash == -1) {
            lastSlash = 0;
        }
        String uriToWrite = documentUri.substring(0, lastSlash) + "/opt/" + opt.getArchetypeId() + extension;
        textDocumentService.writeFile(uriToWrite, "opt in " + format, serializedOpt);
    }
}
