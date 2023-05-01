package com.leoiacovini.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {

    final String name;
    final private Map<String, LoxFunction> methods;

    public LoxFunction getMethod(String methodName) {
        return methods.get(methodName);
    }

    private LoxFunction getInit() {
        return getMethod("init");
    }

    LoxClass(String name, List<LoxFunction> methods) {
        this.name = name;
        final HashMap<String, LoxFunction> indexedMethods = new HashMap<>();
        methods.forEach(m -> {
            indexedMethods.put(m.name(), m);
        });
        this.methods = indexedMethods;
    }

    @Override
    public String toString() {
        return "Class<" + this.name + ">";
    }

    @Override
    public Object call(List<Object> args, Interpreter interpreter) {
        final LoxInstance instance = new LoxInstance(this);
        final LoxFunction init = getInit();
        if (init != null) {
            init.bind(instance).call(args, interpreter);
        }
        return instance;
    }

    @Override
    public int arity() {
        LoxFunction init = getInit();
        if (init != null) return init.arity();
        return 0;
    }

    @Override
    public String name() {
        return name;
    }
}
