package com.leoiacovini.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {

    private final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        this(null);
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    public Environment getEnclosing() {
        return enclosing;
    }

    public void define(String varName, Object value) {
        values.put(varName, value);
    }

    public void assign(Token varName, Object value) {
        if (values.containsKey(varName.getLexeme())) {
            values.put(varName.getLexeme(), value);
        } else if (enclosing != null) {
            enclosing.assign(varName, value);
        } else {
            throw new Interpreter.RuntimeError(varName, "Cannot assign undefined variable '" + varName + "'");
        }
    }

    public Object getVar(Token varName) {
        if (values.containsKey(varName.getLexeme())) {
            return values.get(varName.getLexeme());
        } else if (enclosing != null) {
            return enclosing.getVar(varName);
        } else {
            throw new Interpreter.RuntimeError(varName, "Undefined variable '" + varName.getLexeme() + "'.");
        }
    }

    public Environment copy() {
        final var env = new Environment(this.enclosing != null ? this.enclosing.copy() : null);
        this.values.forEach(env::define);
        return env;
    }

    public Environment ancestor(Integer distance) {
        var env = this;
        for (int i = 0; i < distance; i++) {
            env = env.enclosing;
        }
        return env;
    }

    public Object getAt(Integer distance, String name) {
        return ancestor(distance).values.get(name);
    }

    public void assignAt(Integer distance, Token varName, Object value) {
        ancestor(distance).assign(varName, value);
    }

    @Override
    public String toString() {
        return "<Environment " + values.toString() + " >";
    }
}
