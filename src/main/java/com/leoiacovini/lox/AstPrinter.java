package com.leoiacovini.lox;

public class AstPrinter implements Expr.Visitor<String> {

    public String print(Expr expr) {
        return expr.accept(this);
    }

    private String parenthesize(String name, Expr... exprs) {
        final var stringBuilder = new StringBuilder();
        stringBuilder.append("(").append(name);
        for (final var expr : exprs) {
            stringBuilder.append(" ").append(print(expr));
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize("=", new Expr.Variable(expr.name), expr.value);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.getLexeme(), expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.getLexeme(), expr.right);
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return parenthesize(expr.operator.getLexeme(), expr.condition, expr.left, expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.getLiteral().toString();
    }
}
