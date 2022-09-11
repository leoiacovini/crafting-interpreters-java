package com.leoiacovini.lox;

import java.util.ArrayList;
import java.util.List;

import com.leoiacovini.lox.Token.TokenType;

public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parse() {
        final var statements = new ArrayList<Stmt>();
        while (!isAtEnd()) {
            try {
                statements.add(declaration());
            } catch (ParseError error) {
                synchronize();
            }
        }
        return statements;
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

    private Stmt.Block block() {
        final var stmtList = new ArrayList<Stmt>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            stmtList.add(declaration());
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after block.");
        return new Stmt.Block(stmtList);
    }

    private Stmt.If ifBlock() {
        consume(TokenType.LEFT_PARENS, "Expected '(' after if statement");
        final var conditionExpr = expression();
        consume(TokenType.RIGHT_PARENS, "Expected ')' after expression.");
        final var trueStatement = statement();
        if (match(TokenType.ELSE)) {
            final var falseStatement = statement();
            return new Stmt.If(conditionExpr, trueStatement, falseStatement);
        } else {
            return new Stmt.If(conditionExpr, trueStatement, null);
        }
    }

    private Stmt declaration() {
        if (match(TokenType.VAR)) {
            return varDeclaration();
        } else if (match(TokenType.LEFT_BRACE)) {
            return block();
        } else if (match(TokenType.IF)) {
            return ifBlock();
        }else {
            return statement();
        }
    }

    private Stmt statement() {
        if (match(TokenType.PRINT)) {
            return printStatement();
        } else {
            return expressionStatement();
        }
    }

    private Stmt.Print printStatement() {
        final var expr = expression();
        consume(TokenType.SEMICOLON, "Missing ';' after value.");
        return new Stmt.Print(expr);
    }

    private Stmt.Expression expressionStatement() {
        final var expr = expression();
        consume(TokenType.SEMICOLON, "Missing ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt.Var varDeclaration() {
        final var identifierToken = consume(TokenType.IDENTIFIER, "Expected variable name after 'var'.");
        if (match(TokenType.EQUAL)) {
            final var initializerExpr = expression();
            consume(TokenType.SEMICOLON, "Missing ';' after variable declaration.");
            return new Stmt.Var(identifierToken, initializerExpr);
        } else {
            consume(TokenType.SEMICOLON, "Missing ';' after variable declaration.");
            return new Stmt.Var(identifierToken, null);
        }
    }

    private Expr expression() {
        return ternary();
    }

    private Expr ternary() {
        final var conditionExpr = assignment();
        if (match(TokenType.QUESTION)) {
            final var questionOperator = previous();
            final var leftExpr = assignment();
            consume(TokenType.COLON, "Expected ':' on ternary operator");
            final var rightExpr = assignment();
            return new Expr.Ternary(questionOperator, conditionExpr, leftExpr, rightExpr);
        }
        return conditionExpr;
    }

    private Expr assignment() {
        final var expr = equality();
        if (match(TokenType.EQUAL)) {
            final var equals = previous();
            final var value = assignment();
            if (expr instanceof Expr.Variable) {
                final var name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            //noinspection ThrowableNotThrown
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

//    private Expr orExpr() {
//        if (match(TokenType.OR)) {
//
//        } else {
//            return equality();
//        }
//    }
//
//    private Expr andExpr() {
//        if (match(TokenType.AND)) {
//
//        } else {
//            return equality();
//        }
//    }

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
        if (match(TokenType.IDENTIFIER)) return new Expr.Variable(previous());
        if (match(TokenType.LEFT_PARENS)) {
            final var expr = expression();
            consume(TokenType.RIGHT_PARENS, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression");
    }

//    private Expr.Variable variable() {
//        final var identifierToken = consume(TokenType.IDENTIFIER, "Expected Identifier after.");
//        return new Expr.Variable(identifierToken);
//    }
}
