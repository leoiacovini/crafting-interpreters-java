package com.leoiacovini;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    static private boolean hadError = false;

    static void report(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] " + "Error " + where + ": " + message
        );
    }

    static void error(int line, String message) {
        report(line, "", message);
        hadError = true;
    }

    private static void run(String sourceCode) {
        final var scanner = new Scanner(sourceCode);
        final var tokens = scanner.scanTokens();
        for (final var token : tokens) {
            System.out.println(token);
        }
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(input);
        while (true) {
            System.out.print(">> ");
            final var line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    private static void runFile(String filePath) throws IOException {
        final var bytes = Files.readAllBytes(Paths.get(filePath));
        final var sourceStr = new String(bytes, StandardCharsets.UTF_8);
        run(sourceStr);
        if (hadError) {
            System.exit(65);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.err.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }
}