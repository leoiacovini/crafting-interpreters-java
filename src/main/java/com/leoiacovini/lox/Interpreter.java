package com.leoiacovini.lox;

import com.leoiacovini.lox.globals.Clock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final Environment environment;
    private final Environment globalEnv;
    private final Map<Expr, Integer> locals;

    Interpreter(Environment environment, Environment globalEnv, Map<Expr, Integer> locals) {
        this.environment = environment;
        this.globalEnv = globalEnv;
        this.locals = locals;
    }

    Interpreter() {
        final var globalEnv = new Environment();
        List.of(new Clock()).forEach(f -> globalEnv.define(f.name(), f));
        this.environment = globalEnv;
        this.globalEnv = globalEnv;
        this.locals = new HashMap<>();
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

    public void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    private Object evaluateExpr(Expr expr) {
        return expr.accept(this);
    }

    private Interpreter fork(Environment environment) {
        return new Interpreter(environment, this.globalEnv, this.locals);
    }

    public void interpretBlock(List<Stmt> block, Environment environment) {
        final var innerInterpreter = fork(environment);
        innerInterpreter.interpret(block);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        interpretBlock(stmt.statements, this.environment.newChild());
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
        final var loxFunction = new LoxFunction(stmt, environment);
        environment.define(stmt.name.getLexeme(), loxFunction);
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.getLexeme(), null);
        final List<LoxFunction> fns = stmt.methods.stream().map(m -> new LoxFunction(m, environment)).toList();
        LoxClass klass = new LoxClass(stmt.name.getLexeme(), fns);
        environment.assign(stmt.name, klass);
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
        Reporter.debug("starting assignExpr for " + expr.name.getLexeme());
        final var value = evaluateExpr(expr.value);
        final var distance = locals.get(expr);
        Reporter.debug("assignExpr: " + expr + " with value " + value + " at distance: " + distance);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globalEnv.assign(expr.name, value);
        }
        return value;
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        final LoxInstance instance = (LoxInstance) evaluateExpr(expr.object);
        final Object value = evaluateExpr(expr.value);
        instance.set(expr.name, value);
        return null;
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

    private Object lookupVariable(Token name, Expr expr) {
        final var distance = locals.get(expr);
        Reporter.debug("lookupVariable `" + name.getLexeme() + "` at distance: " + distance);
        if (distance != null) {
            return environment.getAt(distance, name.getLexeme());
        } else {
            return globalEnv.getVar(name);
        }
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookupVariable(expr.name, expr);
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookupVariable(expr.keyword, expr);
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
        if (!(callee instanceof final LoxCallable calleeFn)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }
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

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        final LoxInstance instance = (LoxInstance) evaluateExpr(expr.object);
        return instance.get(expr.name);
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
