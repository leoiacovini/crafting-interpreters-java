package com.leoiacovini.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {

    private final Map<String, Object> values = new HashMap<>();

    public void define(String varName, Object value) {
        values.put(varName, value);
    }

    public void assign(Token varName, Object value) {
        if (values.containsKey(varName.getLexeme())) {
            values.put(varName.getLexeme(), value);
        } else {
            throw new Interpreter.RuntimeError(varName, "Cannot assign undefined variable '" + varName + "'");
        }
    }

    public Object getVar(Token varName) {
        if (values.containsKey(varName.getLexeme())) {
            return values.get(varName.getLexeme());
        } else {
            throw new Interpreter.RuntimeError(varName, "Undefined variable '" + varName.getLexeme() + "'.");
        }
    }

}
