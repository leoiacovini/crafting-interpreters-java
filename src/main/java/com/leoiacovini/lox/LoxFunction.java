package com.leoiacovini.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {

    public LoxFunction(Stmt.Function declaration, Environment closureEnv) {
        this.declaration = declaration;
        this.closureEnv = closureEnv;
    }

    private final Stmt.Function declaration;
    private final Environment closureEnv;

    public LoxFunction bind(LoxInstance instance) {
        final Environment environment = new Environment(closureEnv);
        environment.define("this", instance);
        return new LoxFunction(this.declaration, environment);
    }

    @Override
    public Object call(List<Object> args, Interpreter interpreter) {
        final var env = new Environment(closureEnv);
        // Prepare environment biding provided arguments to their respective variable name
        defineEnvArguments(args, env);
        try {
            interpreter.interpretBlock(declaration.body, env);
        } catch (Interpreter.Return returnValue) {
            return returnValue.getValue();
        }
        return null;
    }

    private void defineEnvArguments(List<Object> args, Environment env) {
        for (var i = 0; i < declaration.params.size(); i++) {
            final var varName = declaration.params.get(i).getLexeme();
            final var varValue = args.get(i);
            env.define(varName, varValue);
        }
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
