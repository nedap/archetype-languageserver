package com.nedap.openehr.lsp;

import com.nedap.archie.antlr.errors.ANTLRParserErrors;
import com.nedap.archie.antlr.errors.ANTLRParserMessage;
import com.nedap.archie.aom.ArchetypeModelObject;
import com.nedap.archie.archetypevalidator.ValidationMessage;
import com.nedap.archie.archetypevalidator.ValidationResult;
import com.nedap.archie.query.AOMPathQuery;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticsConverter {

    public static PublishDiagnosticsParams createDiagnostics(TextDocumentItem document, Exception exception) {
        PublishDiagnosticsParams diagnosticsParams = new PublishDiagnosticsParams();
        List<Diagnostic> diagnostics = new ArrayList<>();
        Range range = new Range(
                new Position(0, 1),
                new Position(0, 50)
        );
        diagnostics.add(new Diagnostic(range, exception.getMessage() == null ? exception.toString() : exception.getMessage(),  DiagnosticSeverity.Error, "Error processing file"));
        diagnosticsParams.setDiagnostics(diagnostics);
        diagnosticsParams.setUri(document.getUri());
        diagnosticsParams.setVersion(document.getVersion());
        return diagnosticsParams;
    }

    public static PublishDiagnosticsParams createDiagnostics(TextDocumentItem document, ANTLRParserErrors errors) {
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
        diagnosticsParams.setUri(document.getUri());
        diagnosticsParams.setVersion(document.getVersion());
        return diagnosticsParams;
    }

    public static PublishDiagnosticsParams createDiagnosticsFromValidationResult(TextDocumentItem textDocumentItem, ValidationResult validationResult) {
        PublishDiagnosticsParams diagnosticsParams = new PublishDiagnosticsParams();
        List<Diagnostic> diagnostics = new ArrayList<>();

        if(validationResult != null) {
            if(validationResult.hasWarningsOrErrors()) {
                for(ValidationMessage message:validationResult.getErrors()) {
                    ArchetypeModelObject withLocation = null;
                    if(message.getPathInArchetype() != null) {
                        try {
                            withLocation = new AOMPathQuery(message.getPathInArchetype()).findMatchingPredicate(validationResult.getSourceArchetype().getDefinition(),
                                    (o) -> o instanceof ArchetypeModelObject && ((ArchetypeModelObject) o).getStartLine() != null);
                        } catch (Exception e) {
                            //we really don't care, but log just in case
                            e.printStackTrace();
                        }
                    }
                    if(withLocation != null) {
                        Range range = new Range(
                                new Position(withLocation.getStartLine()-1, withLocation.getStartCharInLine()),
                                new Position(withLocation.getStartLine()-1, withLocation.getStartCharInLine() + withLocation.getTokenLength())
                        );
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
        }
        diagnosticsParams.setDiagnostics(diagnostics);
        diagnosticsParams.setUri(textDocumentItem.getUri());
        return diagnosticsParams;
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
