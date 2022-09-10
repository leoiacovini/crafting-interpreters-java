package com.leoiacovini.lox;

import java.util.List;
import java.util.Optional;

import com.leoiacovini.lox.Token.TokenType;

public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Optional<Expr> parse() {
        try {
            return Optional.of(expression());
        } catch (ParseError e) {
            return Optional.empty();
        }
    }

    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private Token peek() {
        return tokens.get(current);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }

    private boolean match(TokenType... types) {
        for (final var type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private ParseError error(Token token, String message) {
        Reporter.error(token, message);
        return new ParseError();
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().getType() == TokenType.SEMICOLON) return;
            switch (peek().getType()) {
                case CLASS, FUN, VAR, FOR, WHILE, IF, PRINT, RETURN:
                    return;
            }
            advance();
        }
    }

    private Expr expression() {
        return ternary();
    }

    private Expr ternary() {
        final var conditionExpr = equality();
        if (match(TokenType.QUESTION)) {
            final var questionOperator = previous();
            final var leftExpr = equality();
            consume(TokenType.COLON, "Expected ':' on ternary operator");
            final var rightExpr = equality();
            return new Expr.Ternary(questionOperator, conditionExpr, leftExpr, rightExpr);
        }
        return conditionExpr;
    }

    private Expr equalityRight(Expr left) {
        if (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            final var operator = previous();
            final var rightExpr = comparison();
            return equalityRight(new Expr.Binary(left, operator, rightExpr));
        } else {
            return left;
        }
    }

    private Expr equality() {
        final var leftExpr = comparison();
        return equalityRight(leftExpr);
    }

    private Expr comparison() {
        var expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            final var operator = previous();
            final var rightExpr = term();
            expr = new Expr.Binary(expr, operator, rightExpr);
        }
        return expr;
    }

    private Expr term() {
        var expr = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            final var operator = previous();
            final var rightExpr = factor();
            expr = new Expr.Binary(expr, operator, rightExpr);
        }
        return expr;
    }

    private Expr factor() {
        var expr = unary();
        while (match(TokenType.SLASH, TokenType.STAR)) {
            final var operator = previous();
            final var rightExpr = unary();
            expr = new Expr.Binary(expr, operator, rightExpr);
        }
        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            final var operator = previous();
            final var expr = unary();
            return new Expr.Unary(operator, expr);
        }
        return primary();
    }

    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NIL)) return new Expr.Literal(null);
        if (match(TokenType.STRING, TokenType.NUMBER)) return new Expr.Literal(previous().getLiteral());
        if (match(TokenType.LEFT_PARENS)) {
            final var expr = expression();
            consume(TokenType.RIGHT_PARENS, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression");
    }
}
