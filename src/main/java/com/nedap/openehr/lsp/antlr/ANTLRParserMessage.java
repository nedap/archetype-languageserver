package com.nedap.openehr.lsp.antlr;

/**
 * An error, info or warning message from the archetype parsing
 *
 * Created by pieter.bos on 19/10/15.
 */
public class ANTLRParserMessage {

    private Integer lineNumber;
    private Integer columnNumber;
    private String message;
    private Integer length;
    private String offendingSymbol;

    public ANTLRParserMessage(String message) {
        this.message = message;
    }

    public ANTLRParserMessage(String message, Integer lineNumber, Integer columnNumber) {
        this(message, lineNumber, columnNumber, null, null);
    }

    public ANTLRParserMessage(String message, Integer lineNumber, Integer columnNumber, Integer length, String offendingSymbol) {
        this.message = message;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.length = length;
        this.offendingSymbol = offendingSymbol;
    }


    public String getMessage() {
        return message;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public Integer getLength() {
        return length;
    }

    public String getOffendingSymbol() {
        return offendingSymbol;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public void setOffendingSymbol(String offendingSymbol) {
        this.offendingSymbol = offendingSymbol;
    }
}
