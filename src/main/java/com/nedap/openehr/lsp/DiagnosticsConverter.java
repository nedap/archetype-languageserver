package com.nedap.openehr.lsp;

import com.nedap.archie.antlr.errors.ANTLRParserErrors;
import com.nedap.archie.antlr.errors.ANTLRParserMessage;
import com.nedap.archie.archetypevalidator.ValidationMessage;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.openehr.lsp.document.DocumentInformation;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticsConverter {

    public static PublishDiagnosticsParams createDiagnostics(TextDocumentIdentifier document, Exception exception) {
        PublishDiagnosticsParams diagnosticsParams = new PublishDiagnosticsParams();
        List<Diagnostic> diagnostics = new ArrayList<>();
        Range range = getTopOfDocumentRange();
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
                pushOverlayValidations(docInfo, validationResult, diagnostics);
            }
        }
        diagnosticsParams.setDiagnostics(diagnostics);
        setBasicDiagnostics(textDocumentItem, diagnosticsParams);
        return diagnosticsParams;
    }

    private static void pushOverlayValidations(DocumentInformation docInfo, ValidationResult validationResult, List<Diagnostic> diagnostics) {
        if(validationResult.getOverlayValidations() != null) {
            for (ValidationResult overlayResult : validationResult.getOverlayValidations()) {
                if(overlayResult.hasWarningsOrErrors()) {
                    for(ValidationMessage message: overlayResult.getErrors()) {
                        DocumentSymbol documentSymbol = null;
                        String templateOverlayId = overlayResult.getArchetypeId();
                        if(message.getPathInArchetype() != null) {
                            documentSymbol = docInfo.lookupCObjectOrAttributeInOverlay(templateOverlayId,
                                    message.getPathInArchetype(),
                                    true);
                        }
                        Range range = getOverlayDocumentRange(docInfo, documentSymbol, templateOverlayId);
                        diagnostics.add(new Diagnostic(range, "Error in template overlay " +overlayResult.getArchetypeId()  + ": " + toMessage(message), message.isWarning() ? DiagnosticSeverity.Warning : DiagnosticSeverity.Error, "ADL validation", message.getType().toString()));
                    }
                }
            }
        }
    }

    private static Range getOverlayDocumentRange(DocumentInformation docInfo, DocumentSymbol documentSymbol, String templateOverlayId) {
        if(documentSymbol != null) {
            return documentSymbol.getSelectionRange();
        } else {
            DocumentSymbol templateOverlayRootSymbol = docInfo.getTemplateOverlayRootSymbol(templateOverlayId);
            if (templateOverlayRootSymbol != null) {
                templateOverlayRootSymbol = getArchetypeIdSymbolFromTemplateOverlay(templateOverlayRootSymbol);
                return templateOverlayRootSymbol.getSelectionRange();
            } else {
                return getTopOfDocumentRange();
            }
        }
    }

    /**
     * From a template overlay DocumentSymbol, get the first Archetype section DocumentSymbol.
     * otherwise it will point to a rather empty bit of ADL file
     * @param templateOverlayRootSymbol
     * @return the archetype section DocumentSymbol within, or null if it cannot be found
     */
    private static DocumentSymbol getArchetypeIdSymbolFromTemplateOverlay(DocumentSymbol templateOverlayRootSymbol) {
        if(templateOverlayRootSymbol != null && !templateOverlayRootSymbol.getChildren().isEmpty()) {
            //the first symbol of a template overlay will be the archetype section + archetype id. Use that here
            templateOverlayRootSymbol = templateOverlayRootSymbol.getChildren().get(0);
        }
        return templateOverlayRootSymbol;
    }

    private static void pushValidationMessages(DocumentInformation docInfo, ValidationResult validationResult, List<Diagnostic> diagnostics) {
        for(ValidationMessage message: validationResult.getErrors()) {
            DocumentSymbol documentSymbol = null;
            if(message.getPathInArchetype() != null) {
                documentSymbol = docInfo.lookupCObjectOrAttribute(message.getPathInArchetype());
            }
            Range range = documentSymbol != null ? documentSymbol.getSelectionRange() : getTopOfDocumentRange();
            diagnostics.add(new Diagnostic(range, toMessage(message), message.isWarning() ? DiagnosticSeverity.Warning : DiagnosticSeverity.Error, "ADL validation", message.getType().toString()));
        }
    }

    private static Range getTopOfDocumentRange() {
        return new Range(
                new Position(0, 1),
                new Position(0, 50)
        );
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
