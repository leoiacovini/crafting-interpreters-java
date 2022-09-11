package com.leoiacovini.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ExprGenerator {

    private static List<String> loadDefinitions() throws IOException {
        final var path = Paths.get("src/main/resources/ast_description.txt");
        return Files.readAllLines(path);
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldsString) {

        final var fields = fieldsString.split(", ");

        writer.println("  static class " + className + " extends " + baseName + " {");

        writer.println("    " + className + "(" + fieldsString + ") {");
        for (final var field : fields) {
            final var fieldName = field.split(" ")[1];
            writer.println("    this." + fieldName + " = " + fieldName + ";");
        }
        writer.println("    }");

        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("      return visitor.visit" + className + baseName + "(this);");
        writer.println("    }");

        writer.println();

        for (final var field : fields) {
            writer.println("    final " + field + ";");
        }

        writer.println("  }");
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("  interface Visitor<R> {");
        for (final var type : types) {
            final var typeName = type.split(":")[0].trim();
            writer.println("    R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }
        writer.println("  }");
    }

    private static void defineAst(String outDir, String baseName, List<String> types) throws IOException {
        final var path = outDir + "/" + baseName + ".java";
        final var writer = new PrintWriter(path, StandardCharsets.UTF_8);

        writer.println("package com.leoiacovini.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);

        for (final var type : types) {
            final var className = type.split(":")[0].trim();
            final var fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        writer.println();
        writer.println("  abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <out_dir>");
            System.exit(64);
        }
        final var outDir = args[0];

        final var astDescription = loadDefinitions();

        defineAst(outDir, "Expr", astDescription);
        defineAst(outDir, "Stmt", List.of(
                "Block: List<Stmt> statements",
                "Expression: Expr expression",
                "If: Expr condition, Stmt thenBranch, Stmt elseBranch",
                "Print: Expr expression",
                "While: Expr condition, Stmt body",
                "Var: Token name, Expr initializer"
        ));

    }

}
