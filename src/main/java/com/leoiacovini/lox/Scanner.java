package com.leoiacovini.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.leoiacovini.lox.Token.TokenType;

public class Scanner {

    private final String sourceCode;

    private static class Keywords {

        private static final Map<String, TokenType> keywords = Map.ofEntries(
                Map.entry("and", TokenType.AND),
                Map.entry("class", TokenType.CLASS),
                Map.entry("else", TokenType.ELSE),
                Map.entry("false", TokenType.FALSE),
                Map.entry("for", TokenType.FOR),
                Map.entry("fun", TokenType.FUN),
                Map.entry("if", TokenType.IF),
                Map.entry("nil", TokenType.NIL),
                Map.entry("or", TokenType.OR),
                Map.entry("print", TokenType.PRINT),
                Map.entry("return", TokenType.RETURN),
                Map.entry("super", TokenType.SUPER),
                Map.entry("this", TokenType.THIS),
                Map.entry("true", TokenType.TRUE),
                Map.entry("var", TokenType.VAR),
                Map.entry("while", TokenType.WHILE)
        );

        static Optional<TokenType> get(String lexeme) {
            return Optional.ofNullable(keywords.get(lexeme));
        }

    }

    public Scanner(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    private class Tracker {
        int start = 0;
        int current = 0;
        int line = 1;

        private boolean isAtEnd() {
            return current >= sourceCode.length();
        }

        private char advance() {
            return sourceCode.charAt(this.current++);
        }

        private char previous() {
            return sourceCode.charAt(this.current - 1);
        }

        private boolean match(char expected) {
            if (isAtEnd()) return false;
            if (sourceCode.charAt(current) != expected) return false;
            current++;
            return true;
        }

        private char peek() {
            if (isAtEnd()) return '\0';
            return sourceCode.charAt(current);
        }

        private char peekNext() {
            if (isAtEnd()) return '\0';
            if (current + 1 >= sourceCode.length()) return '\0';
            return sourceCode.charAt(current + 1);
        }

        private void incrementLine() {
            this.line++;
        }
    }

    private Token newToken(Tracker tracker, TokenType tokenType, Object literal) {
        final var text = sourceCode.substring(tracker.start, tracker.current);
        return new Token(text, literal, tracker.line, tokenType);
    }

    private Token newToken(Tracker tracker, TokenType tokenType) {
        return newToken(tracker, tokenType, null);
    }

    private Token string(Tracker tracker) {
        final var stringBuilder = new StringBuilder();
        while (tracker.peek() != '"' && !tracker.isAtEnd()) {
            if (tracker.peek() == '\n') tracker.incrementLine();
            final var c = tracker.advance();
            stringBuilder.append(c);
        }
        if (tracker.isAtEnd()) {
            Reporter.error(tracker.line, "Unterminated string.");
            return null;
        }
        tracker.advance();
        final var stringValue = stringBuilder.toString();
        return newToken(tracker, TokenType.STRING, stringValue);
    }

    private Token number(Tracker tracker) {
        final var stringBuilder = new StringBuilder();
        // Add the first digit that was identified in the initial iteration
        stringBuilder.append(tracker.previous());
        while (Character.isDigit(tracker.peek())) {
            stringBuilder.append(tracker.advance());
        }
        if (tracker.peek() == '.' && Character.isDigit(tracker.peekNext())) {
            stringBuilder.append(tracker.advance());
            while (Character.isDigit(tracker.peek())) {
                stringBuilder.append(tracker.advance());
            }
        }
        final var stringValue = stringBuilder.toString();
        return newToken(tracker, TokenType.NUMBER, Double.parseDouble(stringValue));
    }

    private Token identifier(Tracker tracker) {
        final var stringBuilder = new StringBuilder();
        stringBuilder.append(tracker.previous());
        while (Character.isAlphabetic(tracker.peek()) || Character.isDigit(tracker.peek()) || tracker.peek() == '_') {
            stringBuilder.append(tracker.advance());
        }
        final var stringValue = stringBuilder.toString();
        final var possibleKeyword = Keywords.get(stringValue);
        return possibleKeyword
                .map(tokenType -> newToken(tracker, tokenType, stringValue))
                .orElseGet(() -> newToken(tracker, TokenType.IDENTIFIER, stringValue));
    }

    private Optional<Token> scanToken(Tracker tracker) {
        char c = tracker.advance();
        return Optional.ofNullable(switch (c) {
            case '(' -> newToken(tracker, TokenType.LEFT_PARENS);
            case ')' -> newToken(tracker, TokenType.RIGHT_PARENS);
            case '{' -> newToken(tracker, TokenType.LEFT_BRACE);
            case '}' -> newToken(tracker, TokenType.RIGHT_BRACE);
            case ',' -> newToken(tracker, TokenType.COMMA);
            case '.' -> newToken(tracker, TokenType.DOT);
            case '-' -> newToken(tracker, TokenType.MINUS);
            case '+' -> newToken(tracker, TokenType.PLUS);
            case ';' -> newToken(tracker, TokenType.SEMICOLON);
            case '*' -> newToken(tracker, TokenType.STAR);
            case '?' -> newToken(tracker, TokenType.QUESTION);
            case ':' -> newToken(tracker, TokenType.COLON);

            case '!' -> newToken(tracker, tracker.match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
            case '=' -> newToken(tracker, tracker.match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
            case '<' -> newToken(tracker, tracker.match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
            case '>' -> newToken(tracker, tracker.match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);

            case '/' -> {
                if (tracker.match('/')) {
                    while (tracker.peek() != '\n' && !tracker.isAtEnd()) {
                        tracker.advance();
                    }
                    yield null;
                } else {
                    yield newToken(tracker, TokenType.SLASH);
                }
            }

            // Ignore Whitespace
            case ' ', '\r', '\t' -> null;

            // Line Break
            case '\n' -> {
                tracker.incrementLine();
                yield null;
            }

            case '"' -> string(tracker);

            default -> {
                if (Character.isDigit(c)) {
                    yield number(tracker);
                } else if (Character.isAlphabetic(c) || c == '_') {
                    yield identifier(tracker);
                } else {
                    Reporter.error(tracker.line, "Unexpected character '" + c + "'");
                    yield null;
                }
            }
        });
    }

    public List<Token> scanTokens() {
        final var tokenList = new ArrayList<Token>();
        final var tracker = new Tracker();
        while (!tracker.isAtEnd()) {
            tracker.start = tracker.current;
            final var token = scanToken(tracker);
            token.ifPresent(tokenList::add);
        }
        tokenList.add(new Token("", null, tracker.line, TokenType.EOF));
        return tokenList;
    }

}
