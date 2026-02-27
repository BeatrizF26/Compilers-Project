import java.io.IOException;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.stringtemplate.v4.*;
import java.io.FileWriter;
import java.io.FilenameFilter;

public class CompShMain {
    public static void main(String[] args) {
        CompShSemanticCheck semanticVisitor = null;

        try {
            CharStream input = CharStreams.fromFileName(args[0]);
            CompShLexer lexer = new CompShLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            CompShParser parser = new CompShParser(tokens);

            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                        int line, int charPositionInLine,
                                        String msg, RecognitionException e) {
                    System.err.println("Erro sintático na linha " + line + ":" + charPositionInLine + " - " + msg);
                    System.exit(1);
                }
            });

            ParseTree tree = parser.program();

            if (parser.getNumberOfSyntaxErrors() == 0) {
                // print LISP-style tree:
                System.out.println(tree.toStringTree(parser));

                CompShSemanticCheck visitor0 = new CompShSemanticCheck();
                visitor0.visit(tree);

                // CompShExecutor executor = new CompShExecutor();
                // executor.visit(tree);

                VisitorStringTemplate visitor1 = new VisitorStringTemplate();
                ST program = visitor1.visit(tree);
                FileWriter myWriter = new FileWriter("Output.java");myWriter.write(program.render());
                myWriter.close();
             }

            semanticVisitor = new CompShSemanticCheck();
            Type result = semanticVisitor.visit(tree);

            if (result == Type.ERROR) {
                System.err.println("O programa contém erros semânticos.");
                semanticVisitor.getTypeSystem().dumpSymbolTable();
                System.exit(1);
            }

            System.out.println("Análise semântica concluída com sucesso!");

        } catch (IOException e) {
            System.err.println("Erro de I/O: " + e.getMessage());
            if (semanticVisitor != null)
                semanticVisitor.getTypeSystem().dumpSymbolTable();
            System.exit(1);
        }
    }
}
