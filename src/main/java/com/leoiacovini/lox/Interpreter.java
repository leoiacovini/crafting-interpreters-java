package com.leoiacovini.lox;

import com.leoiacovini.lox.globals.Clock;

import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

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

    static class Return extends RuntimeException {
        final private Object value;

        Return(Object value) {
            super(null, null, false, false);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    final private Environment environment;

    Interpreter(Environment environment) {
        this.environment = environment;
        // we just to that if we are on the most top level Environment
        if (environment.getEnclosing() == null) {
            List.of(new Clock()).forEach(f -> {
                this.environment.define(f.name(), f);
            });
        }
    }

    Interpreter() {
        this(new Environment());
    }

    public Environment getEnvironment() {
        return this.environment;
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (final var stmt : statements) {
                executeStmt(stmt);
            }
        } catch (RuntimeError error) {
            Reporter.runtimeError(error);
        }
    }

    private void executeStmt(Stmt stmt) {
        stmt.accept(this);
    }

    private Object evaluateExpr(Expr expr) {
        return expr.accept(this);
    }

    public void interpretBlock(List<Stmt> block, Environment environment) {
        final var innerInterpreter = new Interpreter(environment);
        innerInterpreter.interpret(block);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        final var innerEnv = new Environment(this.environment);
        interpretBlock(stmt.statements, environment);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluateExpr(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        final var conditionResult = evaluateExpr(stmt.condition);
        if (isTruthy(conditionResult)) {
            executeStmt(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            executeStmt(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        final var loxFunction = new LoxFunction(stmt);
        environment.define(stmt.name.getLexeme(), loxFunction);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        final var evaluatedExpr = evaluateExpr(stmt.expression);
        System.out.println(stringify(evaluatedExpr));
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluateExpr(stmt.condition))) {
            executeStmt(stmt.body);
        }
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluateExpr(stmt.value);
        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        if (stmt.initializer != null) {
            final var initialValue = evaluateExpr(stmt.initializer);
            environment.define(stmt.name.getLexeme(), initialValue);
        } else {
            environment.define(stmt.name.getLexeme(), null);
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        final var value = evaluateExpr(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        final var left = evaluateExpr(expr.left);
        final var right = evaluateExpr(expr.right);
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
        return evaluateExpr(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        final var evaluated = evaluateExpr(expr.right);
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
        final var evaluatedCondition = evaluateExpr(expr.condition);
        if (isTruthy(evaluatedCondition)) {
            return evaluateExpr(expr.left);
        } else {
            return evaluateExpr(expr.right);
        }
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.getVar(expr.name);
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        final var evaluatedLeft = evaluateExpr(expr.left);
        return switch (expr.operator.getType()) {
            case AND -> !isTruthy(evaluatedLeft) ? evaluatedLeft : evaluateExpr(expr.right);
            case OR -> isTruthy(evaluatedLeft) ? evaluatedLeft : evaluateExpr(expr.right);
            default -> null;
        };
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        final var callee = evaluateExpr(expr.callee);
        final var args = expr.args.stream().map(this::evaluateExpr).toList();
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }
        final var calleeFn = (LoxCallable) callee;
        if (calleeFn.arity() != args.size()) {
            throw new RuntimeError(expr.paren, "Expected " + calleeFn.arity() + " arguments but got " + args.size() + ".");
        }
        return calleeFn.call(args, this);
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
