package com.leoiacovini.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {

    final String name;
    final private Map<String, LoxFunction> methods;
    final private LoxClass superClass;

    LoxClass(String name, LoxClass superClass, List<LoxFunction> methods) {
        this.name = name;
        this.superClass = superClass;
        final HashMap<String, LoxFunction> indexedMethods = new HashMap<>();
        methods.forEach(m -> {
//            final LoxFunction boundMethod = m.bindSuper(superClass);
            indexedMethods.put(m.name(), m);
        });
        this.methods = indexedMethods;
    }

    public LoxFunction getMethod(String methodName) {
        final LoxFunction localMethod = methods.get(methodName);
        if (localMethod != null) return localMethod;
        if (superClass != null) return superClass.getMethod(methodName);
        return null;
    }

    private LoxFunction getInit() {
        return getMethod("init");
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
