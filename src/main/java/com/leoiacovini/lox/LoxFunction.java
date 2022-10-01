package com.leoiacovini.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {

    public LoxFunction(Stmt.Function declaration) {
        this.declaration = declaration;
    }

    private final Stmt.Function declaration;

    @Override
    public Object call(List<Object> args, Interpreter interpreter) {
        final var env = new Environment(interpreter.getEnvironment());
        // Prepare environment biding provided arguments to their respective variable name
        for (var i = 0; i < declaration.params.size(); i++) {
            final var varName = declaration.params.get(i).getLexeme();
            final var varValue = args.get(i);
//            System.out.println("Defining '" + varName + "' with value '" + varValue + "'");
            env.define(varName, varValue);
        }
        interpreter.interpretBlock(declaration.body, env);
        return null;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String name() {
        return declaration.name.getLexeme();
    }

    @Override
    public String toString() {
        return "<fn " + name() + ">";
    }
}
