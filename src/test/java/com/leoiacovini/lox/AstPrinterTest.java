package com.leoiacovini.lox;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AstPrinterTest {

    @Test
    void astPrinterTest() {
        final var astPrinter = new AstPrinter();
        final var expr = new Expr.Binary(
                new Expr.Literal("hello"),
                new Token("+", null, 1, Token.TokenType.PLUS),
                new Expr.Literal("world"));
        Assertions.assertEquals("(+ hello world)", astPrinter.print(expr));
    }

}
