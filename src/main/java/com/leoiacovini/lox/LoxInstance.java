package com.leoiacovini.lox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {

    final LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    public LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    public Object get(Token property) {
        final String propertyName = property.getLexeme();
        if (fields.containsKey(propertyName)) {
            return fields.get(propertyName);
        }
        final LoxFunction klassMethod = klass.getMethod(propertyName);
        if (klassMethod != null) {
            return klassMethod.bind(this);
        }
        throw new Interpreter.RuntimeError(property, "Undefined property '" + propertyName + "'.");
    }

    public void set(Token property, Object value) {
        final String propertyName = property.getLexeme();
        fields.put(propertyName, value);
    }

    @Override
    public String toString() {
        return "Instance of <" + this.klass.name() + ">";
    }
}
