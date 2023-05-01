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
        hadError = true;
    }

    static void debug(String log) {
        if (System.getenv().get("DEBUG") != null) {
            final StackTraceElement st = Thread.currentThread().getStackTrace()[2];
            final String callerClass = st.getClassName();
            final long callerLine = st.getLineNumber();
            System.out.println("[DEBUG " + callerClass + ":" + callerLine + "]: " + log);
        }
    }

    public static void runtimeError(Interpreter.RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.getToken().getLine() + "]");
        hadRuntimeError = true;
    }
}
