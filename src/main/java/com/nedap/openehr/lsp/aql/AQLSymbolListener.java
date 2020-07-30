package com.nedap.openehr.lsp.aql;

import com.nedap.healthcare.tolerantaqlparser.ErrorTolerantAQLBaseListener;
import com.nedap.healthcare.tolerantaqlparser.ErrorTolerantAQLParser;
import com.nedap.openehr.lsp.utils.ANTLRUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AQLSymbolListener extends ErrorTolerantAQLBaseListener {

    private Map<String, String> symbolToArchetypeIdMap = new LinkedHashMap<>();
//    private Map<String, DocumentHighlight> variableBoundToArchetypeIdUse = new LinkedHashMap<>();
    private List<ArchetypePathReference> archetypePathReferences = new ArrayList<>();
//    private Map<String, DocumentHighlight> variableUse;

    /**
     * enter a class expression operand. This is where the archetype ids potentially get bound to variables, or at least noted.
     */
    @Override public void enterClassExprOperand(ErrorTolerantAQLParser.ClassExprOperandContext ctx) {
        if(ctx.archetypePredicate() != null) {
            ErrorTolerantAQLParser.ArchetypePredicateExprContext archetypePredicateExprContext = ctx.archetypePredicate().archetypePredicateExpr();
            if(archetypePredicateExprContext.ARCHETYPEID() != null && ctx.IDENTIFIER().size() == 2) { //we have an archetype and a variable.
                //we have an archetype!
                String variableName = ctx.IDENTIFIER(1).getText();
                symbolToArchetypeIdMap.put(variableName, archetypePredicateExprContext.ARCHETYPEID().getText());
            }
        }

    }

    @Override public void enterIdentifiedPath(ErrorTolerantAQLParser.IdentifiedPathContext ctx) {
        String symbolName = ctx.IDENTIFIER().getText();
        String archetypeId = symbolToArchetypeIdMap.get(symbolName);

        //    variableBoundToArchetypeIdUse.put(archetypeId, new DocumentHighlight(ANTLRUtils.createRange(ctx.IDENTIFIER().getSymbol()), DocumentHighlightKind.Read));
        if(ctx.objectPath() != null) {
            //Look, I found a path within an archetype! Let's do interesting things with it :)
            ArchetypePathReference archetypePathReference = new ArchetypePathReference(symbolName, "/" + ctx.objectPath().getText(), ANTLRUtils.createRange(ctx.objectPath()));
            archetypePathReferences.add(archetypePathReference);
        }

    }

    @Override public void exitQueryClause(ErrorTolerantAQLParser.QueryClauseContext ctx) {
        //finish things up: set the archetype references
        for(ArchetypePathReference reference:archetypePathReferences) {
            String archetypeId = symbolToArchetypeIdMap.get(reference.getSymbolName());
            if(archetypeId != null) {
                reference.setArchetypeId(archetypeId);
            }
        }

    }


    public Map<String, String> getSymbolToArchetypeIdMap() {
        return symbolToArchetypeIdMap;
    }

    public List<ArchetypePathReference> getArchetypePathReferences() {
        return archetypePathReferences;
    }
}
