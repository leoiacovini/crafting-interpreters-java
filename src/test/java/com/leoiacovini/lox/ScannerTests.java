package com.leoiacovini.lox;

import com.leoiacovini.lox.Scanner;
import org.junit.jupiter.api.Test;

public class ScannerTests {

    @Test
    void scanTokensTest() {
        final var source = "< > != + - * 200.00 \"hello world\"\n\"bye bye\"\n 10.23\n ola_mundo class for if";
        final var scanner = new Scanner(source);
        final var tokens = scanner.scanTokens();
        System.out.println(tokens);
    }

}
