package com.nedap.openehr.lsp;

import com.nedap.archie.antlr.errors.ANTLRParserErrors;
import com.nedap.archie.antlr.errors.ANTLRParserMessage;
import com.nedap.archie.aom.ArchetypeModelObject;
import com.nedap.archie.archetypevalidator.ValidationMessage;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.archie.query.AOMPathQuery;
import com.nedap.openehr.lsp.document.DocumentInformation;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticsConverter {

    public static PublishDiagnosticsParams createDiagnostics(TextDocumentIdentifier document, Exception exception) {
        PublishDiagnosticsParams diagnosticsParams = new PublishDiagnosticsParams();
        List<Diagnostic> diagnostics = new ArrayList<>();
        Range range = new Range(
                new Position(0, 1),
                new Position(0, 50)
        );
        exception.printStackTrace();
        diagnostics.add(new Diagnostic(range, exception.getMessage() == null ? exception.toString() : exception.getMessage(),  DiagnosticSeverity.Error, "Error processing file"));
        diagnosticsParams.setDiagnostics(diagnostics);
        setBasicDiagnostics(document, diagnosticsParams);
        return diagnosticsParams;
    }

    private static void setBasicDiagnostics(TextDocumentIdentifier document, PublishDiagnosticsParams diagnosticsParams) {
        diagnosticsParams.setUri(document.getUri());
        if(document instanceof VersionedTextDocumentIdentifier) {
            diagnosticsParams.setVersion(((VersionedTextDocumentIdentifier) document).getVersion ());
        }
    }

    public static PublishDiagnosticsParams createDiagnostics(TextDocumentIdentifier document, ANTLRParserErrors errors) {
        PublishDiagnosticsParams diagnosticsParams = new PublishDiagnosticsParams();
        List<Diagnostic> diagnostics = new ArrayList<>();
        for(ANTLRParserMessage warning:errors.getWarnings()) {
            diagnostics.add(createParserDiagnostic(warning, DiagnosticSeverity.Warning));
        }
        for(ANTLRParserMessage error:errors.getErrors()) {
            diagnostics.add(createParserDiagnostic(error, DiagnosticSeverity.Error));
        }
//TODO: replace ANTLRParserErrors with a better class
// if(document.getExceptionDuringProcessing() != null) {
//            //TODO: stacktrace? some extra message to indicate context?
//            String message = document.getExceptionDuringProcessing().getMessage() == null ? document.getExceptionDuringProcessing().toString() : document.getExceptionDuringProcessing().getMessage();
//            diagnostics.add(new Diagnostic(new Range(new Position(0, 1), new Position(0, 50)), message));
//        }

        diagnosticsParams.setDiagnostics(diagnostics);
        setBasicDiagnostics(document, diagnosticsParams);
        return diagnosticsParams;
    }

    public static PublishDiagnosticsParams createDiagnosticsFromValidationResult(TextDocumentIdentifier textDocumentItem, DocumentInformation docInfo, ValidationResult validationResult) {
        PublishDiagnosticsParams diagnosticsParams = new PublishDiagnosticsParams();
        List<Diagnostic> diagnostics = new ArrayList<>();

        if(validationResult != null) {
            if(validationResult.hasWarningsOrErrors()) {
                pushValidationMessages(docInfo, validationResult, diagnostics);

                if(validationResult.getOverlayValidations() != null) {
                    for (ValidationResult overlayResult : validationResult.getOverlayValidations()) {
                        if(overlayResult.hasWarningsOrErrors()) {
                            for(ValidationMessage message: overlayResult.getErrors()) {
                                DocumentSymbol documentSymbol = null;
                                //if(message.getPathInArchetype() != null) {
                                //    documentSymbol = docInfo.lookupCObjectOrAttribute(message.getPathInArchetype());
                                //}
                                if(documentSymbol != null) {
                                    Range range = documentSymbol.getSelectionRange();
                                    diagnostics.add(new Diagnostic(range, toMessage(message), message.isWarning() ? DiagnosticSeverity.Warning : DiagnosticSeverity.Error, "ADL validation", message.getType().toString()));
                                } else {
                                    Range range = new Range(
                                            new Position(0, 1),
                                            new Position(0, 50)
                                    );
                                    diagnostics.add(new Diagnostic(range, "Error in templte overlay " +overlayResult.getArchetypeId()  + ": " + toMessage(message), message.isWarning() ? DiagnosticSeverity.Warning : DiagnosticSeverity.Error, "ADL validation", message.getType().toString()));
                                }
                            }
                        }
                    }
                }
            }
        }
        diagnosticsParams.setDiagnostics(diagnostics);
        setBasicDiagnostics(textDocumentItem, diagnosticsParams);
        return diagnosticsParams;
    }

    private static void pushValidationMessages(DocumentInformation docInfo, ValidationResult validationResult, List<Diagnostic> diagnostics) {
        for(ValidationMessage message: validationResult.getErrors()) {
            DocumentSymbol documentSymbol = null;
            if(message.getPathInArchetype() != null) {
                documentSymbol = docInfo.lookupCObjectOrAttribute(message.getPathInArchetype());
            }
            if(documentSymbol != null) {
                Range range = documentSymbol.getSelectionRange();
                diagnostics.add(new Diagnostic(range, toMessage(message), message.isWarning() ? DiagnosticSeverity.Warning : DiagnosticSeverity.Error, "ADL validation", message.getType().toString()));
            } else {
                Range range = new Range(
                        new Position(0, 1),
                        new Position(0, 50)
                );
                diagnostics.add(new Diagnostic(range, toMessage(message), message.isWarning() ? DiagnosticSeverity.Warning : DiagnosticSeverity.Error, "ADL validation", message.getType().toString()));
            }
        }
    }

    private static Diagnostic createParserDiagnostic(ANTLRParserMessage error, DiagnosticSeverity warning) {
        Range range = new Range(
                new Position(error.getLineNumber()-1, error.getColumnNumber()),
                new Position(error.getLineNumber()-1, error.getColumnNumber() + error.getLength())//TODO: archie errors do not keep the position properly
        );

        return new Diagnostic(range, error.getShortMessage(), warning, "ADL2 syntax");
    }

    private static String toMessage(ValidationMessage message) {
        if(message.getMessage() != null) {
            return message.getMessage();
        } else {
            return message.getType().getDescription();
        }
    }

}
