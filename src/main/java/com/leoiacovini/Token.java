package com.leoiacovini;

public class Token {

    public enum TokenType {
        // Single char tokens
        LEFT_PARENS, RIGHT_PARENS, LEFT_BRACE, RIGHT_BRACE, COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

        // one or two char tokens
        BANG, BANG_EQUAL, EQUAL, EQUAL_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,

        // Literals
        IDENTIFIER, STRING, NUMBER,

        // Keywords
        AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR, PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

        EOF
    }

    private final String lexeme;
    private final Object literal;
    private final int line;
    private final TokenType type;

    public Token(String lexeme, Object literal, int line, TokenType type) {
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.type = type;
    }

    public Object getLiteral() {
        return literal;
    }

    public int getLine() {
        return line;
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    @Override
    public String toString() {
        return "Token{" +
                "lexeme='" + lexeme + '\'' +
                ", literal=" + literal +
                ", line=" + line +
                ", type=" + type +
                '}';
    }

}
