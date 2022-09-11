package com.leoiacovini.lox;

public class Reporter {

    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    static void report(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] " + "Error" + where + ": " + message
        );
    }

    static void error(int line, String message) {
        report(line, "", message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.getType() == Token.TokenType.EOF) {
            report(token.getLine(), " at end", message);
        } else {
            report(token.getLine(), " at '" + token.getLexeme() + "'", message);
        }
    }

    public static void runtimeError(Interpreter.RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.getToken().getLine() + "]");
        hadRuntimeError = true;
    }
}
