package com.nedap.openehr.lsp.utils;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class ANTLRUtils {

    public static Range createRange(ParserRuleContext ctx, TerminalNode fallback) {
        Position start = new Position(ctx.getStart().getLine()-1, ctx.getStart().getCharPositionInLine());
        Position end = new Position(ctx.getStop().getLine()-1, ctx.getStop().getCharPositionInLine());
        if(start.equals(end)) {
            //not good :)
            return createRange(fallback.getSymbol());
        }
        return new Range(start, end);
        //}
    }

    public static Range createRange(ParserRuleContext ctx) {
        Position start = new Position(ctx.getStart().getLine()-1, ctx.getStart().getCharPositionInLine());
        Position end = new Position(ctx.getStop().getLine()-1, ctx.getStop().getCharPositionInLine() + ctx.getStop().getText().length());
        if(start.equals(end)) {
            //not good :)
            end.setCharacter(end.getCharacter()+1);
        }
        return new Range(start, end);
    }

    public static Range createRange(Token symbol) {
        return new Range(
                new Position(symbol.getLine()-1, symbol.getCharPositionInLine()),
                new Position(symbol.getLine()-1, symbol.getCharPositionInLine() + symbol.getText().length())
        );//TODO: scan text for line endings, and determine stop line+ from that!
    }
}
