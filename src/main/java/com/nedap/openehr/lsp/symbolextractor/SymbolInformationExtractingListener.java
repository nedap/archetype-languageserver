package com.nedap.openehr.lsp.symbolextractor;

import com.nedap.archie.adlparser.antlr.Adl14Parser;
import com.nedap.archie.adlparser.antlr.AdlBaseListener;
import com.nedap.archie.adlparser.antlr.AdlLexer;
import com.nedap.archie.adlparser.antlr.AdlParser;
import com.nedap.openehr.lsp.paths.ArchetypePathReference;
import com.nedap.openehr.lsp.document.CodeRangeIndex;
import com.nedap.openehr.lsp.document.DocumentInformation;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.nedap.openehr.lsp.utils.ANTLRUtils.createRange;

public class SymbolInformationExtractingListener extends AdlBaseListener {
    private final String documentUri;
    private String archetypeId;

    private List<DocumentLink> documentLinks = new ArrayList<>();
    private List<FoldingRange> foldingRanges = new ArrayList<>();

    private Map<String, List<LocationLink>> idCodeToTerminologyLocations = new ConcurrentHashMap<>();

    private CodeRangeIndex<DocumentSymbol> cTerminologyCodes = new CodeRangeIndex<>();

    private DocumentSymbolStack stack = new DocumentSymbolStack();

    /** Contains all the references to the archetype for the rules */
    private List<ArchetypePathReference> modelReferences = new ArrayList<>();

    private Map<String, String> variableToPathMap = new LinkedHashMap<>();
    private Stack<String> forAllVariables = new Stack<>();

    private String currentOverlayid;

    private Map<String, DocumentSymbol> templateOverlays = new LinkedHashMap<>();

    public SymbolInformationExtractingListener(String documentUri, AdlLexer lexer) {
        this.documentUri = documentUri;
    }

    //private Stack<SymbolInformation> symbolStack = new Stack<>();

    public List<Either<SymbolInformation, DocumentSymbol>> getSymbols() {
        return stack.getSymbols();
    }

    public List<DocumentLink> getDocumentLinks() {
        return documentLinks;
    }

    public List<FoldingRange> getFoldingRanges() {
        return foldingRanges;
    }

    /**
     * Enter a parse tree produced by {@link AdlParser#archetype}.
     * @param ctx the parse tree
     */
    @Override
    public void enterArchetype(AdlParser.ArchetypeContext ctx) {
        stack.addSymbol(ctx.SYM_ARCHETYPE(), ctx, "archetype", SymbolKind.Constant, StackAction.PUSH);
        stack.addSymbol(ctx.ARCHETYPE_HRID(), null, "archetype id", SymbolKind.File);
        setArchetypeId(ctx.ARCHETYPE_HRID());
    }

    @Override
    public void exitArchetype(AdlParser.ArchetypeContext ctx) {
        
    }



    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void visitTerminal(TerminalNode node) {
        //TODO: get token type and set here as name?
//        String displayName = lexer.getVocabulary().getDisplayName(node.getSymbol().getType());
//        addSymbol(node, displayName, SymbolKind.String);
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void visitErrorNode(ErrorNode node) { }

    @Override public void enterTemplate(AdlParser.TemplateContext ctx) {
        stack.addSymbol(ctx.SYM_TEMPLATE(), ctx, "archetype", SymbolKind.Constant, StackAction.PUSH);
        stack.addSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class);
        setArchetypeId(ctx.ARCHETYPE_HRID());
    }

    private void setArchetypeId(TerminalNode terminalNode) {
        this.archetypeId = terminalNode.getText();
        this.currentOverlayid = archetypeId;
    }

    private void setOverlayId(TerminalNode terminalNode) {
        this.currentOverlayid = terminalNode == null ?
                archetypeId /* shouldn't happen, just defensive programming */ :
                terminalNode.getText();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitTemplate(AdlParser.TemplateContext ctx) {
        popStack();
    }

    private void popStack() {
        stack.pop();
    }


    @Override public void enterTemplateOverlay(AdlParser.TemplateOverlayContext ctx) {
        stack.addSymbol(ctx.SYM_TEMPLATE_OVERLAY(), ctx, "archetype", SymbolKind.Constant, StackAction.PUSH);
        DocumentSymbol templateSymbol = stack.peek();
        stack.addSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class);
        templateOverlays.put(ctx.ARCHETYPE_HRID().getText(), templateSymbol);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
        setOverlayId(ctx.ARCHETYPE_HRID());
    }

    @Override public void exitTemplateOverlay(AdlParser.TemplateOverlayContext ctx) {
        popStack();
    }

    @Override public void enterOperationalTemplate(AdlParser.OperationalTemplateContext ctx) {
        stack.addSymbol(ctx.SYM_OPERATIONAL_TEMPLATE(), ctx, "operational template", SymbolKind.Constant, StackAction.PUSH);
        stack.addSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class);
        setArchetypeId(ctx.ARCHETYPE_HRID());
    }

    @Override public void exitOperationalTemplate(AdlParser.OperationalTemplateContext ctx) {
        popStack();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterSpecializationSection(AdlParser.SpecializationSectionContext ctx) {
        stack.addSymbol(ctx.SYM_SPECIALIZE(), ctx, DocumentInformation.SPECIALISATION_SECTION_NAME, SymbolKind.Module, StackAction.PUSH);
        TerminalNode terminalNode = ctx.archetype_ref().ARCHETYPE_HRID();
        if(terminalNode == null) {
            terminalNode = ctx.archetype_ref().ARCHETYPE_REF();
        }
        DocumentLink documentLink = new DocumentLink(createRange(terminalNode.getSymbol()));
        documentLink.setData(terminalNode.getText()); //we'll resolve the target later
        documentLinks.add(documentLink);
        addFoldingRange(ctx);
    }

    @Override public void exitSpecializationSection(AdlParser.SpecializationSectionContext ctx) {
        popStack();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterLanguageSection(AdlParser.LanguageSectionContext ctx) {
        stack.addSymbol(ctx.SYM_LANGUAGE(), ctx, DocumentInformation.LANGUAGE_SECTION_NAME, SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitLanguageSection(AdlParser.LanguageSectionContext ctx) {
        popStack();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterDescriptionSection(AdlParser.DescriptionSectionContext ctx) {
        stack.addSymbol(ctx.SYM_DESCRIPTION(), ctx, DocumentInformation.DESCRIPTION_SECTION_NAME, SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override public void exitDescriptionSection(AdlParser.DescriptionSectionContext ctx) {
        popStack();
    }

    private void addFoldingRange(ParserRuleContext ctx) {
        FoldingRange range = new FoldingRange(ctx.getStart().getLine() - 1, ctx.getStop().getLine() - 1);
        if(range.getEndLine() != range.getStartLine()) {
            foldingRanges.add(range);
        }
    }

    private void addFoldingRange(int start, ParserRuleContext stop) {
        FoldingRange range = new FoldingRange(start, stop.getStop().getLine() - 1);
        if(range.getEndLine() != range.getStartLine()) {
            foldingRanges.add(range);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterDefinitionSection(AdlParser.DefinitionSectionContext ctx) {
        stack.addSymbol(ctx.SYM_DEFINITION(), ctx, DocumentInformation.DEFINITION_SECTION_NAME, SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitDefinitionSection(AdlParser.DefinitionSectionContext ctx) {
        popStack();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterRulesSection(AdlParser.RulesSectionContext ctx) {
        stack.addSymbol(ctx.SYM_RULES(), ctx, DocumentInformation.RULES_SECTION_NAME, SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override public void exitRulesSection(AdlParser.RulesSectionContext ctx) {
        popStack();
    }

    @Override
    public void enterRmOverlaySection(AdlParser.RmOverlaySectionContext ctx) {
        stack.addSymbol(ctx.SYM_RM_OVERLAY(), ctx, DocumentInformation.RM_OVERLAY_SECTION_NAME, SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override
    public void exitRmOverlaySection(AdlParser.RmOverlaySectionContext ctx) {
        popStack();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterTerminologySection(AdlParser.TerminologySectionContext ctx) {
        stack.addSymbol(ctx.SYM_TERMINOLOGY(), ctx, DocumentInformation.TERMINOLOGY_SECTION_NAME, SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override public void exitTerminologySection(AdlParser.TerminologySectionContext ctx) {
        popStack();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterAnnotationsSection(AdlParser.AnnotationsSectionContext ctx) {
        stack.addSymbol(ctx.SYM_ANNOTATIONS(), ctx, DocumentInformation.ANNOTATIONS_SECTION_NAME, SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override public void exitAnnotationsSection(AdlParser.AnnotationsSectionContext ctx) {
        popStack();
    }

    @Override public void enterC_complex_object(AdlParser.C_complex_objectContext ctx) {
        if(ctx.ID_CODE() != null) {
            stack.addSymbol(ctx.type_id().ALPHA_UC_ID(), ctx, ctx.type_id().getText() + "[" + ctx.ID_CODE().getText() + "]", SymbolKind.Class, StackAction.PUSH);
           // addSymbol(ctx.ID_CODE(), "complex object " + ctx.type_id().getText() + "[" + ctx.ID_CODE().getText() + "]", SymbolKind.Object);
        } else if (ctx.ROOT_ID_CODE() != null) {
            stack.addSymbol(ctx.type_id().ALPHA_UC_ID(), ctx, ctx.type_id().getText() + "[" + ctx.ROOT_ID_CODE().getText() + "]", SymbolKind.Class, StackAction.PUSH);
         //   addSymbol(ctx.ROOT_ID_CODE(), "[" + ctx.ROOT_ID_CODE().getText() + "]", SymbolKind.Key);
        }
        if(ctx.SYM_MATCHES() != null) {
       //     addSymbol(ctx.SYM_MATCHES(), ctx.SYM_MATCHES().getText(), SymbolKind.Operator);
        }
        addFoldingRange(ctx);
    }

    @Override public void exitC_complex_object(AdlParser.C_complex_objectContext ctx) {
        popStack();
    }

    @Override public void enterC_attribute(AdlParser.C_attributeContext ctx) {
        if(ctx.ADL_PATH() != null) {
            stack.addSymbol(ctx.ADL_PATH(), ctx, ctx.ADL_PATH().getText(), SymbolKind.Field, StackAction.PUSH);
        } else if(ctx.attribute_id() != null) {
            stack.addSymbol(ctx.attribute_id().ALPHA_LC_ID(), ctx, ctx.attribute_id().getText(), SymbolKind.Field, StackAction.PUSH);
        } else {
            throw new RuntimeException("unexpected code path");
        }
        addFoldingRange(ctx);
    }

    @Override public void exitC_attribute(AdlParser.C_attributeContext ctx) {
        popStack();
    }

    @Override public void enterArchetype_slot(AdlParser.Archetype_slotContext ctx) {
        addFoldingRange(ctx);
    }

    @Override public void enterC_archetype_root(AdlParser.C_archetype_rootContext ctx) {
        stack.addSymbol(ctx.SYM_USE_ARCHETYPE(), ctx, ctx.archetype_ref().getText() + "[" + ctx.ID_CODE().getText() + "]", SymbolKind.Class, StackAction.PUSH);
        addFoldingRange(ctx);
        String archetypeRef = ctx.archetype_ref().getText();
        TerminalNode terminalNode = ctx.archetype_ref().ARCHETYPE_HRID();
        if(terminalNode == null) {
            terminalNode = ctx.archetype_ref().ARCHETYPE_REF();
        }
        if(terminalNode != null) {
            DocumentLink documentLink = new DocumentLink(createRange(terminalNode.getSymbol()));
            documentLink.setData(archetypeRef); //we'll resolve the target later
            documentLinks.add(documentLink);
        }
    }

    @Override public void exitC_archetype_root(AdlParser.C_archetype_rootContext ctx) {
        popStack();
    }


    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterC_terminology_code(AdlParser.C_terminology_codeContext ctx) {
        String name = null;
        if(ctx.AC_CODE() != null) {
            name = ctx.AC_CODE().getText();
        } else if (ctx.AT_CODE() != null) {
            name = ctx.AT_CODE().getText();
        }
        if(name != null) {
            Range range = createRange(ctx);
            cTerminologyCodes.addRange(range, stack.createSymbolInformation(name, SymbolKind.Constant, range));
        }
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitC_terminology_code(AdlParser.C_terminology_codeContext ctx) { }

    /**
     * An odin &lt; ... &gt; block
     */
    @Override public void enterObject_value_block(AdlParser.Object_value_blockContext ctx) {
        addFoldingRange(ctx);
    }

    Pattern idCodePattern = Pattern.compile("\"(id|at|ac)(\\d|\\.)+\"");

    @Override public void enterAttr_val(AdlParser.Attr_valContext ctx) {
        if(ctx.odin_object_key() != null) {
            if (!stack.isEmpty()) {
                DocumentSymbol parent = stack.peek();
                if (parent.getKind() == SymbolKind.Key && idCodePattern.matcher(parent.getName()).matches()) {
                    //do not add things like 'text' and 'description', they aren't useful at all!
                    //TODO: maybe move this to a post-processor?
                    if (ctx.odin_object_key().getText().equalsIgnoreCase("text")) {
                        if (ctx.object_block() != null) {
                            parent.setDetail(ctx.object_block().object_value_block().getText());
                        }
                    }
                } else {
                    stack.addSymbol(getObjectKeyTerminalNode(ctx), ctx, ctx.odin_object_key().getText(), SymbolKind.Field, StackAction.PUSH);
                }
            } else {
                stack.addSymbol(getObjectKeyTerminalNode(ctx), ctx, ctx.odin_object_key().getText(), SymbolKind.Field, StackAction.PUSH);
            }
        }
    }

    private TerminalNode getObjectKeyTerminalNode(AdlParser.Attr_valContext ctx) {
        TerminalNode node = null;
        if (ctx.odin_object_key().ALPHA_UNDERSCORE_ID() != null) {
            node = ctx.odin_object_key().ALPHA_UNDERSCORE_ID();
        } else if (ctx.odin_object_key().identifier() != null) {
            node = (TerminalNode) ctx.odin_object_key().identifier().getChild(0);
        }
        return node;
    }

    @Override public void exitAttr_val(AdlParser.Attr_valContext ctx) {
        if(ctx.odin_object_key() != null) {
            //TODO: move to postprocessor!
            if (!stack.isEmpty()) {
                DocumentSymbol parent = stack.peek();
                if (parent.getKind() == SymbolKind.Key && idCodePattern.matcher(parent.getName()).matches()) {
                } else {
                    popStack();
                }
            } else {
                popStack();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterKeyed_object(AdlParser.Keyed_objectContext ctx) {
        String key = ctx.primitive_value().getText();
        stack.addSymbol(ctx.primitive_value(), ctx, key, SymbolKind.Key, StackAction.PUSH);
        if(idCodePattern.matcher(key).matches()) {
            List<LocationLink> locationLinks = idCodeToTerminologyLocations.get(key);
            if(locationLinks == null) {
                locationLinks = new ArrayList<>();
                idCodeToTerminologyLocations.put(key, locationLinks);
            }
            locationLinks.add(new LocationLink(documentUri, createRange(ctx), createRange(ctx.primitive_value())));
        }
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitKeyed_object(AdlParser.Keyed_objectContext ctx) {
        popStack();
    }


    @Override public void enterAdlRulesPath(AdlParser.AdlRulesPathContext ctx) {
        //this is a model reference
        String path = ctx.ADL_PATH().getText();
        if(ctx.SYM_VARIABLE_START() != null) {
            //if the path starts with a variable, replace it with the actual path if present in the variable map
            int firstSlash = path.indexOf('/');
            String variable = path.substring(0, firstSlash);
            String mappedpath = variableToPathMap.get("$" + variable);
            path = path.substring(firstSlash);
            if(mappedpath != null) {
                path = mappedpath + path;
                addArchetypePathReference(ctx, path);
            } else {
                addArchetypePathReference(ctx, path, "variable " + variable + " could not be resolved");
            }
        } else {
            addArchetypePathReference(ctx, path);
        }

        if(currentVariableDeclaration != null && ctx.SYM_VARIABLE_START() == null) {
            currentVariablePaths.add(path);
        }
    }

    private void addArchetypePathReference(AdlParser.AdlRulesPathContext ctx, String path) {
        addArchetypePathReference(ctx, path, null);
    }

    private void addArchetypePathReference(AdlParser.AdlRulesPathContext ctx, String path, String extraInformation) {
        ArchetypePathReference archetypePathReference = new ArchetypePathReference(null, path, createRange(ctx));
        archetypePathReference.setExtraInformation(extraInformation);
        archetypePathReference.setArchetypeId(currentOverlayid);
        this.modelReferences.add(archetypePathReference);
    }

    @Override public void enterBooleanForAllExpression(AdlParser.BooleanForAllExpressionContext ctx) {
        if(ctx.SYM_FOR_ALL() != null && ctx.identifier() != null && ctx.adlRulesPath() != null) {
            variableToPathMap.put("$" + ctx.identifier().getText(), ctx.adlRulesPath().getText());
            forAllVariables.push("$" + ctx.identifier().getText());
        }
    }

    @Override public void exitBooleanForAllExpression(AdlParser.BooleanForAllExpressionContext ctx) {
        if(ctx.SYM_FOR_ALL() != null && ctx.identifier() != null && ctx.adlRulesPath() != null) {
            variableToPathMap.remove(forAllVariables.pop());
        }
    }

    private String currentVariableDeclaration;
    private List<String> currentVariablePaths;

    @Override public void enterVariableDeclaration(AdlParser.VariableDeclarationContext ctx) {
        //if a variable declaration contains just one path, store it for later use
        currentVariableDeclaration = ctx.VARIABLE_DECLARATION().getText();
        int splitIndex = currentVariableDeclaration.indexOf(':');
        if(splitIndex >= 0) {
            currentVariableDeclaration = currentVariableDeclaration.substring(0, splitIndex);
        }
        currentVariablePaths = new ArrayList<>();
    }

    @Override public void exitVariableDeclaration(AdlParser.VariableDeclarationContext ctx) {
        if(currentVariablePaths.size() == 1) {
            //store for use in other paths.
            //could be something else, as in variable = path + 3, but that's ok, that won't be used in subpaths
            variableToPathMap.put(currentVariableDeclaration, currentVariablePaths.get(0));
        }
        currentVariableDeclaration = null;
        currentVariablePaths = null;
    }

    public String getArchetypeId() {
        return archetypeId;
    }

    public Map<String, List<LocationLink>> getIdCodeToTerminologyLocations() {
        return idCodeToTerminologyLocations;
    }

    public void setIdCodeToTerminologyLocations(Map<String, List<LocationLink>> idCodeToTerminologyLocations) {
        this.idCodeToTerminologyLocations = idCodeToTerminologyLocations;
    }

    public CodeRangeIndex<DocumentSymbol> getCTerminologyCodes() {
        return cTerminologyCodes;
    }

    public List<ArchetypePathReference> getModelReferences() {
        return modelReferences;
    }

    public Map<String, DocumentSymbol> getTemplateOverlays() {
        return templateOverlays;
    }
}
