package com.nedap.openehr.lsp.symbolextractor;

import com.nedap.archie.adlparser.antlr.AdlBaseListener;
import com.nedap.archie.adlparser.antlr.AdlLexer;
import com.nedap.archie.adlparser.antlr.AdlParser;
import com.nedap.openehr.lsp.document.CodeRangeIndex;
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
import java.util.List;
import java.util.Map;
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
        this.archetypeId = ctx.ARCHETYPE_HRID().getText();
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
        this.archetypeId = ctx.ARCHETYPE_HRID().getText();
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


    @Override public void enterTemplate_overlay(AdlParser.Template_overlayContext ctx) {
        stack.addSymbol(ctx.SYM_TEMPLATE_OVERLAY(), ctx, "archetype", SymbolKind.Constant, StackAction.PUSH);
        stack.addSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override public void exitTemplate_overlay(AdlParser.Template_overlayContext ctx) {
        popStack();
    }

    @Override public void enterOperational_template(AdlParser.Operational_templateContext ctx) {
        stack.addSymbol(ctx.SYM_OPERATIONAL_TEMPLATE(), ctx, "operational template", SymbolKind.Constant, StackAction.PUSH);
        stack.addSymbol(ctx.ARCHETYPE_HRID(), "archetype id", SymbolKind.Class);
        this.archetypeId = ctx.ARCHETYPE_HRID().getText();
    }

    @Override public void exitOperational_template(AdlParser.Operational_templateContext ctx) {
        popStack();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterSpecialization_section(AdlParser.Specialization_sectionContext ctx) {
        stack.addSymbol(ctx.SYM_SPECIALIZE(), ctx, "specialization section", SymbolKind.Module, StackAction.PUSH);
        TerminalNode terminalNode = ctx.archetype_ref().ARCHETYPE_HRID();
        if(terminalNode == null) {
            terminalNode = ctx.archetype_ref().ARCHETYPE_REF();
        }
        DocumentLink documentLink = new DocumentLink(createRange(terminalNode.getSymbol()));
        documentLink.setData(terminalNode.getText()); //we'll resolve the target later
        documentLinks.add(documentLink);
        addFoldingRange(ctx);
    }

    @Override public void exitSpecialization_section(AdlParser.Specialization_sectionContext ctx) {
        popStack();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterLanguage_section(AdlParser.Language_sectionContext ctx) {
        stack.addSymbol(ctx.SYM_LANGUAGE(), ctx, "language section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitLanguage_section(AdlParser.Language_sectionContext ctx) {
        popStack();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterDescription_section(AdlParser.Description_sectionContext ctx) {
        stack.addSymbol(ctx.SYM_DESCRIPTION(), ctx, "description section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override public void exitDescription_section(AdlParser.Description_sectionContext ctx) {
        popStack();
    }

    private void addFoldingRange(ParserRuleContext ctx) {
        foldingRanges.add(new FoldingRange(ctx.getStart().getLine()-1, ctx.getStop().getLine()-1));
    }

    private void addFoldingRange(int start, ParserRuleContext stop) {
        foldingRanges.add(new FoldingRange(start, stop.getStop().getLine()-1));
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterDefinition_section(AdlParser.Definition_sectionContext ctx) {
        stack.addSymbol(ctx.SYM_DEFINITION(), ctx, "definition section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitDefinition_section(AdlParser.Definition_sectionContext ctx) {
        popStack();
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterRules_section(AdlParser.Rules_sectionContext ctx) {
        stack.addSymbol(ctx.SYM_RULES(), ctx, "rules section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override public void exitRules_section(AdlParser.Rules_sectionContext ctx) {
        popStack();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterTerminology_section(AdlParser.Terminology_sectionContext ctx) {
        stack.addSymbol(ctx.SYM_TERMINOLOGY(), ctx, "terminology section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result

    }

    @Override public void exitTerminology_section(AdlParser.Terminology_sectionContext ctx) {
        popStack();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterAnnotations_section(AdlParser.Annotations_sectionContext ctx) {
        stack.addSymbol(ctx.SYM_ANNOTATIONS(), ctx, "annotations section", SymbolKind.Module, StackAction.PUSH);
        addFoldingRange(ctx.getStart().getLine(), ctx); //starts with \n, which shouldn't be in result
    }

    @Override public void exitAnnotations_section(AdlParser.Annotations_sectionContext ctx) {
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
        if(!stack.isEmpty()) {
            DocumentSymbol parent = stack.peek();
            if(parent.getKind() == SymbolKind.Key && idCodePattern.matcher(parent.getName()).matches()) {
                //do not add things like 'text' and 'description', they aren't useful at all!
                //TODO: maybe move this to a post-processor?
                if(ctx.attribute_id().getText().equalsIgnoreCase("text")) {
                    if(ctx.object_block() != null) {
                        parent.setDetail(ctx.object_block().getText());
                    }
                }
            } else {
                stack.addSymbol(ctx.attribute_id().ALPHA_LC_ID(), ctx, ctx.attribute_id().getText(), SymbolKind.Field, StackAction.PUSH);
            }
        } else {
            stack.addSymbol(ctx.attribute_id().ALPHA_LC_ID(), ctx, ctx.attribute_id().getText(), SymbolKind.Field, StackAction.PUSH);
        }
    }

    @Override public void exitAttr_val(AdlParser.Attr_valContext ctx) {
        //TODO: move to postprocessor!
        if(!stack.isEmpty()) {
            DocumentSymbol parent = stack.peek();
            if(parent.getKind() == SymbolKind.Key && idCodePattern.matcher(parent.getName()).matches()) {
            } else {
                popStack();
            }
        } else {
            popStack();
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
}
