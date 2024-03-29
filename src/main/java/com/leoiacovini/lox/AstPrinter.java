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

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.getLexeme(), expr.left, expr.right);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return parenthesize(print(expr.callee), expr.args.toArray(Expr[]::new));
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return parenthesize(expr.name.getLexeme(), expr.object);
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        return parenthesize(expr.name.getLexeme(), expr.object, expr.value);
    }

    @Override
    public String visitThisExpr(Expr.This expr) {
        return expr.keyword.getLexeme();
    }

    @Override
    public String visitSuperExpr(Expr.Super expr) {
        return expr.keyword.getLexeme();
    }
}
