package com.nedap.openehr.lsp;

import com.nedap.archie.adlparser.ADLParser;
import com.nedap.archie.adlparser.antlr.AdlLexer;
import com.nedap.archie.adlparser.antlr.AdlParser;
import com.nedap.archie.adlparser.modelconstraints.BMMConstraintImposer;
import com.nedap.archie.adlparser.modelconstraints.ModelConstraintImposer;
import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.utils.ArchetypeParsePostProcesser;
import com.nedap.archie.archetypevalidator.ArchetypeValidator;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.archie.flattener.InMemoryFullArchetypeRepository;
import com.nedap.archie.rminfo.MetaModels;
import com.nedap.openehr.lsp.antlr.ANTLRParserErrors;
import com.nedap.openehr.lsp.antlr.ArchieErrorListener;
import com.nedap.openehr.lsp.symbolextractor.ADL2SymbolExtractor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.openehr.referencemodels.BuiltinReferenceModels;

import java.io.IOException;
import java.util.List;

public class ProcessableDocument {
    private TextDocumentItem document;
    private List<Either<SymbolInformation, DocumentSymbol>> symbols;
    private AdlLexer lexer;
    private AdlParser parser;
    private ArchieErrorListener errorListener;
    private ANTLRParserErrors errors;
    private Exception exceptionDuringProcessing;
    private String archetypeId;

    private InMemoryFullArchetypeRepository repository;

    public ProcessableDocument(TextDocumentItem document, InMemoryFullArchetypeRepository repository) {
        this.document = document;
        this.repository = repository;
    }

    public List<Either<SymbolInformation, DocumentSymbol>> getSymbols() {
        if(symbols == null) {
            synchronized (this) {
                if(symbols == null) {
                    ADL2SymbolExtractor adl2SymbolExtractor = new ADL2SymbolExtractor();
                    try {
                        symbols = adl2SymbolExtractor.extractSymbols(this);
                    } catch (IOException e) {
                        //shouldn't happen, ever, just in memory processing
                        throw new RuntimeException(e);
                    }
                    this.archetypeId = adl2SymbolExtractor.getArchetypeId();
                    if(errors.hasNoErrors()) {
                        try {
                            MetaModels metaModels = BuiltinReferenceModels.getMetaModels();
                            Archetype archetype = new ADLParser(metaModels).parse(this.document.getText());
                            this.archetypeId = archetype.getArchetypeId().toString();
                            repository.addArchetype(archetype);
                            ArchetypeValidator archetypeValidator = new ArchetypeValidator(metaModels);
                            ValidationResult validationResult = archetypeValidator.validate(archetype, repository);
                            //set some values that are not directly in ODIN or ADL
                            ArchetypeParsePostProcesser.fixArchetype(archetype);
                            ModelConstraintImposer imposer = new BMMConstraintImposer(metaModels.getSelectedBmmModel());
                            imposer.setSingleOrMultiple(archetype.getDefinition());
                        } catch (Exception e) {
                            e.printStackTrace();
                            //this will be reported to the client
                            exceptionDuringProcessing = e;
                        }
                    }
                 }
            }
        }
        return symbols;
    }

    public AdlParser getParser() {
        if(parser == null) {
            constructParser();
        }
        return parser;
    }

    public AdlLexer getLexer() {
        if(lexer == null) {
            constructParser();
        }
        return lexer;
    }

    private synchronized void constructParser() {
        lexer = new AdlLexer(CharStreams.fromString(document.getText()));
        parser = new AdlParser(new CommonTokenStream(lexer));
        errors = new ANTLRParserErrors();
        errorListener = new ArchieErrorListener(errors);
        errorListener.setLogEnabled(true);
        parser.addErrorListener(errorListener);

    }

    public TextDocumentItem getDocument() {
        return document;
    }

    public synchronized void setDocumentText(String text, Integer version) {
        document.setText(text);
        document.setVersion(version);
        lexer = null;
        parser = null;
        symbols = null;
        errors = null;
        errorListener = null;
        this.exceptionDuringProcessing = null;
        archetypeId = null;
    }

    public ANTLRParserErrors getErrors() {
        if(errors == null) {
            getSymbols();
        }
        return errors;
    }

    public Archetype getArchetype() {
        if(archetypeId == null || repository.getArchetype(archetypeId) == null) {
            getSymbols();
        }
        if(archetypeId != null) {
            return repository.getArchetype(archetypeId);
        }
        return null;
    }

    public Exception getExceptionDuringProcessing() {
        return exceptionDuringProcessing;
    }

    public ValidationResult getValidationResult() {
        if(archetypeId == null || repository.getValidationResult(archetypeId) == null) {
            getSymbols();
        }
        return repository.getValidationResult(archetypeId);

    }

    public String getArchetypeId() {
        if(archetypeId == null) {
            getSymbols();
        }
        return archetypeId;
    }
}
