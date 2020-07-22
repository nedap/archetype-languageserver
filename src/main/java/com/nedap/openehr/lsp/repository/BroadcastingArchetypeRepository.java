package com.nedap.openehr.lsp.repository;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.nedap.archie.adlparser.ADLParser;
import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.Template;
import com.nedap.archie.aom.TemplateOverlay;
import com.nedap.archie.archetypevalidator.ArchetypeValidator;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.archie.flattener.InMemoryFullArchetypeRepository;
import com.nedap.openehr.lsp.ADL2TextDocumentService;
import com.nedap.openehr.lsp.document.ADLVersion;
import com.nedap.openehr.lsp.document.DocumentInformation;
import com.nedap.openehr.lsp.symbolextractor.ADL2SymbolExtractor;
import com.nedap.openehr.lsp.document.HoverInfo;
import com.nedap.openehr.lsp.symbolextractor.SymbolNameFromTerminologyHelper;
import com.nedap.openehr.lsp.symbolextractor.adl14.ADL14SymbolExtractor;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.openehr.referencemodels.BuiltinReferenceModels;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class BroadcastingArchetypeRepository extends InMemoryFullArchetypeRepository {

    private final ADL2TextDocumentService textDocumentService;
    Map<String, TextDocumentItem> documents = new ConcurrentHashMap<>();
    Map<String, DocumentInformation> symbolsByUri = new ConcurrentHashMap<>();
    Map<String, TextDocumentItem> documentsByArchetypeId = new ConcurrentHashMap<>();
    private ArchetypeValidator validator = new ArchetypeValidator(BuiltinReferenceModels.getMetaModels());
    private final ADL14ConvertingStorage adl14Storage;
    private boolean compile = true;


    public BroadcastingArchetypeRepository(ADL2TextDocumentService textDocumentService) {
        this.textDocumentService = textDocumentService;
        adl14Storage = new ADL14ConvertingStorage(textDocumentService, this) ;

    }

    public void addDocument(TextDocumentItem item) {
        documents.put(item.getUri(), item);
        handleChanged(item);
    }

    public void updateDocument(String uri, int version, String text) {
        TextDocumentItem textDocumentItem = documents.get(uri); //TODO: retrieve old Archetype ID and if changed, remove the old archetype
        if(textDocumentItem == null) {
            textDocumentItem = new TextDocumentItem();
            textDocumentItem.setUri(uri);
            documents.put(uri, textDocumentItem);
        }
        textDocumentItem.setVersion(version);
        textDocumentItem.setText(text);
        handleChanged(textDocumentItem);

    }

    Pattern adl14Pattern = Pattern.compile("[^\n]+adl_version\\s*=\\s*1\\.4.*");

    /**
     * Handles a changed textdocument. Does:
     * 1. parsing the symbols, storing them
     * 2. if parsing succeeds, invalidates the archetype, then compiles again, broadcasting all changes in the process
     * @param textDocumentItem
     */
    private void handleChanged(TextDocumentItem textDocumentItem) {
        if(textDocumentItem.getText().trim().isEmpty()) {
            //not an ADL file
            return;
        }
        boolean adl14 = false;
        if(textDocumentItem.getText().contains("\n")) {
            String firstLine = textDocumentItem.getText().substring(0, textDocumentItem.getText().indexOf("\n"));
            adl14 = adl14Pattern.matcher(firstLine).matches();
        }
        if(adl14) {
            extractADL14Info(textDocumentItem);
            return;
        }
        extractADL2Info(textDocumentItem);

    }

    private void extractADL2Info(TextDocumentItem textDocumentItem) {
        try {
            ADL2SymbolExtractor adl2SymbolExtractor = new ADL2SymbolExtractor();

            DocumentInformation documentInformation = adl2SymbolExtractor.extractSymbols(textDocumentItem.getUri(), textDocumentItem.getText());
            symbolsByUri.put(textDocumentItem.getUri(), documentInformation);
            if(documentInformation.getArchetypeId() != null) {
                documentsByArchetypeId.put(documentInformation.getArchetypeId(), textDocumentItem);
            }
            if (documentInformation.getErrors().hasNoErrors()) {
                ADLParser adlParser = new ADLParser(BuiltinReferenceModels.getMetaModels());
                adlParser.setLogEnabled(false);//no console output please :)
                Archetype archetype = null;
                try {
                    archetype = adlParser.parse(textDocumentItem.getText());

                    addArchetype(archetype);
                    //perform incremental compilation here

                    invalidateAndRecompileArchetypes(archetype);
                    ValidationResult result = getValidationResult(archetype.getArchetypeId().toString());
                    Archetype archetypeForTerms = archetype;
                    if(result != null && result.getFlattened() != null) {
                        archetypeForTerms = result.getFlattened();
                    }
                    String language = archetype.getOriginalLanguage() != null ? archetype.getOriginalLanguage().getCodeString() : null;
                    if(language == null) {
                        language = "en";
                    }
                    documentInformation.setHoverInfo(new HoverInfo(documentInformation, archetype, archetypeForTerms, language));
                    SymbolNameFromTerminologyHelper.giveNames(documentInformation.getSymbols(), archetypeForTerms, language);
                    //diagnostics will now be pushed from within the invalidateArchetypesAndRecompile method
                } catch (Exception ex) {
                    //this particular exce[tion is a parse error, usually when extracting JSON. be sure to post taht
                    textDocumentService.pushDiagnostics(new VersionedTextDocumentIdentifier(textDocumentItem.getUri(), textDocumentItem.getVersion()), ex);
                }


            } else {
                textDocumentService.pushDiagnostics(new VersionedTextDocumentIdentifier(textDocumentItem.getUri(), textDocumentItem.getVersion()), documentInformation.getErrors());
            }
        } catch (IOException e) {
            //shouldn't happen, ever, just in memory processing
            throw new RuntimeException(e);
        }
    }

    private void extractADL14Info(TextDocumentItem textDocumentItem) {
        adl14Storage.addFile(textDocumentItem);
        //make sure any ADL 2 things get removed here!
        ADL14SymbolExtractor adlSymbolExtractor = new ADL14SymbolExtractor();

        try {
            DocumentInformation documentInformation = adlSymbolExtractor.extractSymbols(textDocumentItem.getUri(), textDocumentItem.getText());
            symbolsByUri.put(textDocumentItem.getUri(), documentInformation);
            if (documentInformation.getArchetypeId() != null) {
                documentsByArchetypeId.put(documentInformation.getArchetypeId(), textDocumentItem);
            }
            resolveDocumentLinks();

            Archetype archetype = adl14Storage.getArchetype(new TextDocumentIdentifier(textDocumentItem.getUri()));
            if(archetype != null) {
                String language = archetype.getOriginalLanguage() != null ? archetype.getOriginalLanguage().getCodeString() : null;
                if (language == null) {
                    language = "en";
                }
                SymbolNameFromTerminologyHelper.giveNames(documentInformation.getSymbols(), archetype, language);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return;
    }

    @Override
    public void setValidationResult(ValidationResult result) {
        super.setValidationResult(result);
        TextDocumentItem textDocumentItem = documentsByArchetypeId.get(result.getArchetypeId());
        DocumentInformation documentInformation = getDocumentInformation(textDocumentItem.getUri());
        textDocumentService.pushDiagnostics(new VersionedTextDocumentIdentifier(textDocumentItem.getUri(), textDocumentItem.getVersion()), documentInformation, result);
        //new validation result received! Broadcast it :)

    }

    public void invalidateAndRecompileArchetypes(Archetype newArchetype) {
        //now invalidate all related archetypes
        Set<String> archetypesToInvalidate = new HashSet<>();
        archetypesToInvalidate.add(newArchetype.getArchetypeId().toString());

        for(Archetype result:getAllArchetypes()) {

            if(isDescendant(result, newArchetype.getArchetypeId().getFullId())) {
                archetypesToInvalidate.add(result.getArchetypeId().toString());
            }
            //this assumes templates cannot be further specialized
            if(result instanceof Template) {
                Template template = (Template) result;
                for(TemplateOverlay overlay: template.getTemplateOverlays()) {
                    //TODO: not only direct descendants, but also those far below should be checked with (a variant of?) isDescendant
                    if(archetypesToInvalidate.contains(overlay.getParentArchetypeId())) {
                        archetypesToInvalidate.add(result.getArchetypeId().getFullId());
                    }
                }
            }
        }


        for(String archetypeId: archetypesToInvalidate) {
            removeValidationResult(archetypeId);
        }
        //this should recompile the archetype plus any parents
        //TODO: for operational templates we need to scan way more, all archetype roots as well. future addition
        if(compile) {
            for (String archetypeId : archetypesToInvalidate) {
                Archetype archetype = getArchetype(archetypeId);
                if (archetype != null && getValidationResult(archetypeId) == null) {
                    validator.validate(archetype, this);
                }
            }
            resolveDocumentLinks();
        }
    }

    private boolean isDescendant(Archetype archetype, String parent) {

        Archetype archetypeToCheck = archetype;
        int maxTreeDepth = 75;
        int i= 0;
        while(archetypeToCheck != null && i < maxTreeDepth && archetypeToCheck.getParentArchetypeId() != null) {
            if(archetypeToCheck.getParentArchetypeId().startsWith(parent)) {
                return true;
            }
            String parentArchetypeId = archetypeToCheck.getParentArchetypeId();
            archetypeToCheck = getArchetype(parentArchetypeId);
            i++;
        }
        return false;
    }

    public void closeDocument(String uri) {
        //do nothing right now, but eventually we'll have to start reading from file again if this happens
        //TODO: switch back to filesystem access.
        //this notification is also sent on delete, so need to check for deletion as well
    }

    public List<Either<SymbolInformation, DocumentSymbol>> getSymbols(String uri) {
        DocumentInformation documentInformation = this.symbolsByUri.get(uri);
        return documentInformation == null ? Lists.newArrayList() : documentInformation.getSymbols();
    }

    public Hover getHover(HoverParams params) {
        DocumentInformation documentInformation = this.symbolsByUri.get(params.getTextDocument().getUri());
        return documentInformation == null ? null: documentInformation.getHoverInfo(params);
    }

    public List<FoldingRange> getFoldingRanges(TextDocumentIdentifier textDocument) {
        DocumentInformation documentInformation = this.symbolsByUri.get(textDocument.getUri());
        return documentInformation == null ? null: documentInformation.getFoldingRanges();
    }

    public void addFolder(String uri) {
        //add a folder of files to watch
        addDirectory(new File(URI.create(uri)));

    }

    private void addDirectory(File directory) {
        if(directory.isDirectory()) {
            File[] files = directory.listFiles();
            for(File file:files) {
                if(file.isDirectory()) {
                    addDirectory(file);
                } else {
                    addFile(toUri(file), file);
                }
            }
        } else {
            addFile(toUri(directory), directory);
        }
    }

    private String toUri(File file) {
        String uri = file.toURI().toString();
        //VSCode URIs start with "file:///", as does the LSP spec
        //java only does "file:/".
        //so fix that here so we don't get duplicates in indexes later on
        //alternative is to make the indexes aware of these differences?
        if(uri.startsWith("file:/") && uri.charAt(6) != '/') {
            uri = "file:///" + uri.substring(6);
        }
        return uri;
    }

    public void addFile(String uri, File file) {
        if (hasAdlsExtension(file)) {
            try {
                TextDocumentItem adl = new TextDocumentItem(uri, "adl", 0, new String(Files.readAllBytes(file.toPath()), Charsets.UTF_8));
                addDocument(adl);
            } catch (Exception e) {
                textDocumentService.pushDiagnostics(new TextDocumentIdentifier(uri), e);
                e.printStackTrace();
            }
        }
    }

    private boolean hasAdlsExtension(File file) {
        return file.getName().toLowerCase().endsWith(".adls")
                || file.getName().toLowerCase().endsWith(".adl")
                || file.getName().toLowerCase().endsWith(".adlt")
                || file.getName().toLowerCase().endsWith(".adl2")
                || file.getName().toLowerCase().endsWith(".adlf");
    }

    public void fileChanged(String uri, File file) {
        if(hasAdlsExtension(file)) {
            try {
                updateDocument(uri, 0, new String(Files.readAllBytes(file.toPath()), Charsets.UTF_8));
            } catch (Exception e) {
                textDocumentService.pushDiagnostics(new TextDocumentIdentifier(uri), e);
                e.printStackTrace();
            }
        }
    }


    public boolean isCompile() {
        return compile;
    }

    public void setCompile(boolean compile) {
        this.compile = compile;
    }

    public void compile() {
        compile(validator);
        resolveDocumentLinks();
    }


    private void resolveDocumentLinks() {
        for(DocumentInformation info:symbolsByUri.values()) {
            if(info.getDocumentLinks() != null) {
                info.getDocumentLinks().resolveLinks(this);
            }
        }
    }

    public void fileRemoved(String uri) {
        documents.remove(uri);
        DocumentInformation removedDocumentInfo = symbolsByUri.remove(uri);
        if(removedDocumentInfo != null && removedDocumentInfo.getArchetypeId() != null) {
            this.documentsByArchetypeId.remove(removedDocumentInfo.getArchetypeId());
            super.removeArchetype(removedDocumentInfo.getArchetypeId());
        }
        //incremental compile on remove is just remove all for now
        //TODO: replace with invalidate(archetype) only IF the archetype is available?
        invalidateAll();
    }

    private void invalidateAll() {
        for(ValidationResult result:new ArrayList<>(getAllValidationResults())) {
            removeValidationResult(result.getArchetypeId());
        }
        resolveDocumentLinks();
    }

    public TextDocumentItem getDocument(String archetypeRef) {
        Archetype archetype = getArchetype(archetypeRef);
        if(archetype == null) {
            return null;
        }
        return documentsByArchetypeId.get(archetype.getArchetypeId().toString());
    }

    public DocumentInformation getDocumentInformation(String uri) {
        return this.symbolsByUri.get(uri);
    }

    public List<DocumentLink> getDocumentLinks(DocumentLinkParams params) {
        DocumentInformation documentInformation = this.symbolsByUri.get(params.getTextDocument().getUri());
        if(documentInformation == null) {
            return new ArrayList<>();
        }
        List<DocumentLink> result = documentInformation.getAllDocumentLinks();
        return result;
    }

    public void convertAdl14(String documentUri) {
        this.adl14Storage.convert(documentUri);
    }

    public boolean isADL14(TextDocumentIdentifier textDocument) {
        DocumentInformation info = this.getDocumentInformation(textDocument.getUri());
        return  info != null
                && info.getADLVersion() == ADLVersion.VERSION_1_4;
    }

    public void convertAllAdl14(String rootFileUri) {
        this.adl14Storage.convertAll(rootFileUri);
    }

    public void storeDiagnostics(TextDocumentIdentifier document, List<Diagnostic> diagnostics) {
        DocumentInformation info = getDocumentInformation(document.getUri());
        if(info != null) {
            info.setDiagnostics(diagnostics);
        }
    }
}
