package com.leoiacovini.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    enum TokenBindingStatus {
        /// Token is declared but not ready to be used
        DECLARED,
        /// Token is defined and ready to usage
        DEFINED,
    }

    private final Interpreter interpreter;
    private final Stack<Map<String, TokenBindingStatus>> scopes = new Stack<>();

    public Stack<Map<String, TokenBindingStatus>> getScopes() {
        return scopes;
    }

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    public void resolve(Expr expr) {
        expr.accept(this);
    }

    public void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    public void resolve(List<Stmt> stmtList) {
        for (final var stmt : stmtList) {
            resolve(stmt);
        }
    }

    private void beginScope() {
        Reporter.debug("BEGIN SCOPE");
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        Reporter.debug("END SCOPE");
        scopes.pop();
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitTernaryExpr(Expr.Ternary expr) {
        resolve(expr.condition);
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    private void resolveLocal(Expr expr, Token name) {
        final int size = scopes.size() - 1;
        Reporter.debug("Starting resolveLocal for " + name.getLexeme() + " with scopes: " + scopes);
        for (int i = size; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.getLexeme())) {
                Reporter.debug("resolveLocal: " + name.getLexeme() + " at scope index: " + i);
                Reporter.debug("Current Scope: " + scopes.get(i));
                interpreter.resolve(expr, size - i);
                return;
            }
        }
    }

    private boolean variableIsDeclared(Expr.Variable expr) {
        final String lexeme = expr.name.getLexeme();
        return !scopes.isEmpty() && scopes.peek().get(lexeme) == TokenBindingStatus.DECLARED;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (variableIsDeclared(expr)) {
            Reporter.error(expr.name, "Can't read local variable in it's own initializer.");
        }
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        expr.args.forEach(this::resolve);
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    enum FunctionType {
        NONE,
        FUNCTION,
        METHOD,
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        declare(stmt.name);
        define(stmt.name);
        beginScope();
        scopes.peek().put("this", TokenBindingStatus.DEFINED);
        for (Stmt.Function method : stmt.methods) {
            final FunctionType declaration = FunctionType.METHOD;
            resolveFunction(method, declaration);
        }
        endScope();
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    private void resolveFunction(Stmt.Function function, FunctionType functionType) {
        beginScope();
        for (final var param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (stmt.value != null) resolve(stmt.value);
        return null;
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;
        Reporter.debug("Declaring new token: " + name.getLexeme());
        scopes.peek().put(name.getLexeme(), TokenBindingStatus.DECLARED);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        Reporter.debug("Defining new token: " + name.getLexeme());
        scopes.peek().put(name.getLexeme(), TokenBindingStatus.DEFINED);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

}