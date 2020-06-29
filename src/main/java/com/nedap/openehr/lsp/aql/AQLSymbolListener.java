package com.nedap.openehr.lsp.aql;

import com.nedap.healthcare.aqlparser.AQLBaseListener;
import com.nedap.healthcare.aqlparser.AQLListener;
import com.nedap.healthcare.aqlparser.AQLParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class AQLSymbolListener extends AQLBaseListener {

    private Map<String, String> symbolToArchetypeIdMap = new LinkedHashMap<>();

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterClassExprOperand(AQLParser.ClassExprOperandContext ctx) {
        if(ctx.archetypePredicate() != null) {
            AQLParser.ArchetypePredicateExprContext archetypePredicateExprContext = ctx.archetypePredicate().archetypePredicateExpr();
            if(archetypePredicateExprContext.ARCHETYPEID() != null) {
                //we have an archetype!
                symbolToArchetypeIdMap.put(ctx.IDENTIFIER(0).getText(), archetypePredicateExprContext.ARCHETYPEID().getText());
            }
        }

    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void exitClassExprOperand(AQLParser.ClassExprOperandContext ctx) { }
}
