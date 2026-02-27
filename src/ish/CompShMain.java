import java.io.IOException;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class CompShMain {
   public static void main(String[] args) {
      try {
         // create a CharStream that reads from standard input:
         CharStream input = CharStreams.fromFileName(args[0]);
         // create a lexer that feeds off of input CharStream:
         CompShLexer lexer = new CompShLexer(input);
         // create a buffer of tokens pulled from the lexer:
         CommonTokenStream tokens = new CommonTokenStream(lexer);
         // create a parser that feeds off the tokens buffer:
         CompShParser parser = new CompShParser(tokens);
         // replace error listener:
         // parser.removeErrorListeners(); // remove ConsoleErrorListener
         // parser.addErrorListener(new ErrorHandlingListener());
         // begin parsing at program rule:
         ParseTree tree = parser.program();
         if (parser.getNumberOfSyntaxErrors() == 0) {
            CompShSemanticCheck semanticCheck = new CompShSemanticCheck();
            CompShType result = semanticCheck.visit(tree);

            if (result == CompShType.ERROR) {
               System.err.println("Erros semânticos encontrados. Interpretação abortada.");
               System.exit(2);
            }

            // print LISP-style tree:
            // System.out.println(tree.toStringTree(parser));
            ishInterpreter visitor0 = new ishInterpreter();
            visitor0.visit(tree);
         }
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      } catch (RecognitionException e) {
         e.printStackTrace();
         System.exit(1);
      }
   }
}
