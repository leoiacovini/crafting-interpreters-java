package com.leoiacovini.lox.globals;

import com.leoiacovini.lox.Interpreter;
import com.leoiacovini.lox.LoxCallable;

import java.util.List;

public class Clock implements LoxCallable {
    @Override
    public Object call(List<Object> args, Interpreter interpreter) {
        return (double) System.currentTimeMillis() / 1000.0;
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public String name() {
        return "clock";
    }

    @Override
    public String toString() {
        return "<native fn: clock>";
    }
}
