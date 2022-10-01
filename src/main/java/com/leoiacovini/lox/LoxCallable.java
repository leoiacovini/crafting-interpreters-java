package com.leoiacovini.lox;

import java.util.List;

public interface LoxCallable {
    Object call(List<Object> args, Interpreter interpreter);

    int arity();

    String name();
}
