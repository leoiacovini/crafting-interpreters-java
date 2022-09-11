package com.leoiacovini.lox;

import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final private Environment environment = new Environment();

    static class RuntimeError extends RuntimeException {

        final private Token token;

        RuntimeError(Token token, String message) {
            super(message);
            this.token = token;
        }

        public Token getToken() {
            return token;
        }

    }

    public void interpret(List<Stmt> statements) {
        try {
            for (final var stmt : statements) {
                execute(stmt);
            }
        } catch (RuntimeError error) {
            Reporter.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        final var evaluatedExpr = evaluate(stmt.expression);
        System.out.println(stringify(evaluatedExpr));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        if (stmt.initializer != null) {
            final var initialValue = evaluate(stmt.initializer);
            environment.define(stmt.name.getLexeme(), initialValue);
        } else {
            environment.define(stmt.name.getLexeme(), null);
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        final var value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        final var left = evaluate(expr.left);
        final var right = evaluate(expr.right);
        return switch (expr.operator.getType()) {
            case PLUS -> {
                if (left instanceof Double && right instanceof Double) {
                    yield (double) left + (double) right;
                } else if (left instanceof String && right instanceof String) {
                    //noinspection RedundantCast
                    yield (String) left + (String) right;
                } else {
                    throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
                }
            }
            case MINUS -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left - (double) right;
            }
            case STAR -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left * (double) right;
            }
            case SLASH -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left / (double) right;
            }
            case GREATER -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left > (double) right;
            }
            case GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left >= (double) right;
            }
            case LESS -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left < (double) right;
            }
            case LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double) left <= (double) right;
            }
            case EQUAL_EQUAL -> isEqual(left, right);
            case BANG_EQUAL -> !isEqual(left, right);
            default -> null;
        };
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        final var evaluated = evaluate(expr.right);
        return switch (expr.operator.getType()) {
            case MINUS -> {
                checkNumberOperands(expr.operator, evaluated);
                yield -(double) evaluated;
            }
            case BANG -> !isTruthy(evaluated);
            default -> null;
        };
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        final var evaluatedCondition = evaluate(expr.condition);
        if (isTruthy(evaluatedCondition)) {
            return evaluate(expr.left);
        } else {
            return evaluate(expr.right);
        }
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.getVar(expr.name);
    }

    private boolean isTruthy(Object value) {
        if (value instanceof Boolean) {
            return (boolean) value;
        } else return value != null;
    }

    private boolean isEqual(Object first, Object second) {
        if (first == null && second == null) return true;
        if (first == null) return false;
        return first.equals(second);
    }

    private void checkNumberOperands(Token operator, Object... operands) {
        for (final var operand : operands) {
            if (!(operand instanceof Double)) throw new RuntimeError(operator, "Operand must be a number");
        }
    }

    private String stringify(Object obj) {
        if (obj == null) return "nil";
        if (obj instanceof Double) {
            String text = obj.toString();
            if (text.endsWith(".0")) {
                return text.substring(0, text.length() - 2);
            }
            return text;
        }
        return obj.toString();
    }
}
