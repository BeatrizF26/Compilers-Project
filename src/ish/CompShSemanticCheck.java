import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.Set;
// import TypeSystem;


public class CompShSemanticCheck extends CompShBaseVisitor<CompShType> {
    private final TypeSystem typeSystem = new TypeSystem();

    public TypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public CompShType visitProgram(CompShParser.ProgramContext ctx) {
        for (var stat : ctx.stat()) {
            CompShType t = visit(stat);
            if (t == CompShType.ERROR) return CompShType.ERROR;
        }
        return CompShType.VOID;
    }

    @Override
    public CompShType visitStatDeclaration(CompShParser.StatDeclarationContext ctx) {
        var ids = ctx.declaration().ID();
        CompShType declaredType = CompShType.fromString(ctx.declaration().TYPE().getText());

        for (var idToken : ids) {
            String id = idToken.getText();
            if (typeSystem.symbolExists(id)) {
                System.err.println("Erro semântico: variável '" + id + "' já foi declarada.");
                return CompShType.ERROR;
            }
            typeSystem.addSymbol(id, declaredType);
        }

        return CompShType.VOID;
    }

    @Override
    public CompShType visitExprPipeAssignement(CompShParser.ExprPipeAssignementContext ctx) {
        String id = ctx.ID().getText();
        CompShType declaredType = ctx.TYPE() != null ? CompShType.fromString(ctx.TYPE().getText()) : null;

        if (typeSystem.symbolExists(id)) {
            CompShType existingType = typeSystem.getSymbolType(id);
            if (declaredType != null && declaredType != existingType) {
                System.out.println("tentativa de reatribuição com tipo diferente para '" + id + "'.");
                return CompShType.ERROR;
            }
            return existingType;
        }

        if (declaredType == null) {
            System.out.println("atribuição via 'store in' requer tipo explícito.");
            return CompShType.ERROR;
        }

        typeSystem.addSymbol(id, declaredType);
        return declaredType;
    }


    @Override public CompShType visitExprStr(CompShParser.ExprStrContext ctx)     { return CompShType.TEXT; }
    @Override public CompShType visitExprInt(CompShParser.ExprIntContext ctx)     { return CompShType.INTEGER; }
    @Override public CompShType visitExprFloat(CompShParser.ExprFloatContext ctx) { return CompShType.REAL; }

    @Override
    public CompShType visitExprId(CompShParser.ExprIdContext ctx) {
        String id = ctx.getText();
        if (!typeSystem.symbolExists(id)) {
            System.err.println("Erro semântico: variável '" + id + "' não foi declarada.");
            return CompShType.ERROR;
        }
        return typeSystem.getSymbolType(id);
    }

    @Override
    public CompShType visitExprTypeCast(CompShParser.ExprTypeCastContext ctx) {
        CompShType targetType = CompShType.fromString(ctx.TYPE().getText());
        CompShType exprType = visit(ctx.expr());

        if (!typeSystem.isValidConversion(targetType, exprType)) {
            System.err.printf("Erro semântico: não é possível converter de %s para %s.%n", exprType, targetType);
            return CompShType.ERROR;
        }

        return targetType;
    }
    
    @Override
    public CompShType visitExprExecute(CompShParser.ExprExecuteContext ctx) {
        CompShType base = visit(ctx.expr(0));
        if (base != CompShType.TEXT) {
            System.err.println("Erro semântico: execução (!) requer string base.");
            return CompShType.ERROR;
        }

        if (ctx.expr().size() > 1) {
              for (int i = 1; i < ctx.expr().size(); i++) {
                  CompShType argType = visit(ctx.expr(i));

                  if (argType == CompShType.LIST) {
                      System.err.println("Erro semântico: listas não podem estar dentro de listas.");
                      return CompShType.ERROR;
                  }

                  if (argType == CompShType.ERROR) return CompShType.ERROR;
              }
        }

        return CompShType.PROGRAM;
    }


    @Override
    public CompShType visitExprExecuteIsh(CompShParser.ExprExecuteIshContext ctx) {
        CompShType inner = visit(ctx.expr());

        if (inner != CompShType.TEXT) {
            System.err.println("Erro semântico: execução ISH (!!) requer texto.");
            return CompShType.ERROR;
        }

        return CompShType.PROGRAM;
    }
    
    
    @Override
    public CompShType visitExprPipe(CompShParser.ExprPipeContext ctx) {
        CompShType left = visit(ctx.expr(0));
        CompShType right = visit(ctx.expr(1));

        String canal = ctx.channel() != null ? ctx.channel().getText() : "|";

        boolean rightIsFilter =
            ctx.expr(1) instanceof CompShParser.ExprFilterContext ||
            ctx.expr(1) instanceof CompShParser.ExprPrefixContext ||
            ctx.expr(1) instanceof CompShParser.ExprSuffixContext;

        // Se for um programa com canal, converte o tipo com base no canal
        if (left == CompShType.PROGRAM && canal != null) {
            switch (canal) {
                case "|":
                case "&":
                    left = CompShType.TEXT;
                    break;
                case "?":
                    left = rightIsFilter ? CompShType.TEXT : CompShType.INTEGER;
                    break;
                case "*":
                case "$":
                    left = CompShType.PROGRAM;
                    break;
                case "-":
                    left = CompShType.VOID;
                    break;
            }
        }
        
        if (ctx.expr(1) instanceof CompShParser.ExprPipeAssignementContext assignCtx) {
            CompShType leftType = visit(ctx.expr(0));
            String id = assignCtx.ID().getText();

            CompShType declaredType;
            if (assignCtx.TYPE() != null) {
                declaredType = CompShType.fromString(assignCtx.TYPE().getText());
            } else if (typeSystem.symbolExists(id)) {
                declaredType = typeSystem.getSymbolType(id);
            } else {
                declaredType = null;
            }

            if (declaredType == null) {
                System.err.println("Erro semântico: atribuição via 'store in' requer tipo explícito.");
                return CompShType.ERROR;
            }

            if (!typeSystem.symbolExists(id)) {
                typeSystem.addSymbol(id, declaredType);
            } else {
                CompShType existing = typeSystem.getSymbolType(id);
                if (existing != declaredType) {
                    System.err.printf("tentativa de reatribuição com tipo diferente para '%s'.%n", id);
                    return CompShType.ERROR;
                }
            }

            // Exceção: permitir guardar resultados de um 'program' diretamente em text/integer/real (caso bc ou echo devolva valor simples)

            if (leftType == CompShType.PROGRAM && (declaredType == CompShType.TEXT || declaredType == CompShType.INTEGER || declaredType == CompShType.REAL)) {
                // Aceita implicitamente
            } else if (!typeSystem.isValidConversion(declaredType, leftType)) {
                System.err.printf("Erro semântico: não é possível armazenar %s em variável do tipo %s%n", leftType, declaredType);
                return CompShType.ERROR;
            }

            return declaredType;
        }

        boolean rightAcceptsInput =
            ctx.expr(1) instanceof CompShParser.ExprStdoutContext ||
            ctx.expr(1) instanceof CompShParser.ExprStderrContext ||
            ctx.expr(1) instanceof CompShParser.ExprNLContext ||
            ctx.expr(1) instanceof CompShParser.ExprStdinContext ||
            ctx.expr(1) instanceof CompShParser.ExprTypeCastContext ||
            ctx.expr(1) instanceof CompShParser.ExprFilterContext ||
            ctx.expr(1) instanceof CompShParser.ExprPrefixContext ||
            ctx.expr(1) instanceof CompShParser.ExprSuffixContext ||
            ctx.expr(1) instanceof CompShParser.ExprPipeAssignementContext ||
            ctx.expr(1) instanceof CompShParser.ExprExecuteContext ||
            ctx.expr(1) instanceof CompShParser.ExprExecuteIshContext;

        if (!rightAcceptsInput) {
            System.err.println("Erro semântico: lado direito do pipe não aceita valor de entrada.");
            return CompShType.ERROR;
        }

        if (left == CompShType.VOID || left == CompShType.ERROR) {
            System.err.printf("Erro semântico: tipo inválido no lado esquerdo do pipe: %s%n", left);
            return CompShType.ERROR;
        }

        if (rightIsFilter && left != CompShType.TEXT) {
            System.err.println("Erro semântico: filtros apenas podem ser aplicados a valores do tipo text.");
            return CompShType.ERROR;
        }

        return rightIsFilter ? CompShType.TEXT : right;

    }

    @Override
    public CompShType visitExprPlusMinus(CompShParser.ExprPlusMinusContext ctx) {
        CompShType left = visit(ctx.expr(0));
        CompShType right = visit(ctx.expr(1));
        String op = ctx.op.getText();

        CompShType result = typeSystem.checkBinaryOperation(left, op, right);
        if (result == CompShType.ERROR) {
            System.err.printf("Erro semântico: operação '%s' inválida entre %s e %s.%n", op, left, right);
        }

        return result;
    }

    @Override
    public CompShType visitExprMulDiv(CompShParser.ExprMulDivContext ctx) {
        CompShType left = visit(ctx.expr(0));
        CompShType right = visit(ctx.expr(1));
        String op = ctx.op.getText();

        CompShType result = typeSystem.checkBinaryOperation(left, op, right);
        if (result == CompShType.ERROR) {
            System.err.printf("Erro semântico: operação '%s' inválida entre %s e %s.%n", op, left, right);
        }

        return result;
    }

    @Override public CompShType visitExprUnary(CompShParser.ExprUnaryContext ctx) {
        return visit(ctx.expr());
    }

    @Override public CompShType visitExprStdout(CompShParser.ExprStdoutContext ctx) { return CompShType.TEXT; }
    @Override public CompShType visitExprStderr(CompShParser.ExprStderrContext ctx) { return CompShType.TEXT; }
    @Override public CompShType visitExprStdin(CompShParser.ExprStdinContext ctx)   { return CompShType.TEXT; }
    @Override public CompShType visitExprParens(CompShParser.ExprParensContext ctx) { return visit(ctx.expr()); }
    @Override public CompShType visitExprNL(CompShParser.ExprNLContext ctx)         { return CompShType.TEXT; }

    @Override public CompShType visitExprBoolTrue(CompShParser.ExprBoolTrueContext ctx) { return CompShType.BOOLEAN; }
    @Override public CompShType visitExprBoolFalse(CompShParser.ExprBoolFalseContext ctx) { return CompShType.BOOLEAN; }

    @Override
    public CompShType visitExprRelational(CompShParser.ExprRelationalContext ctx) {
        CompShType left = visit(ctx.expr(0));
        CompShType right = visit(ctx.expr(1));
        String op = ctx.op.getText();

        if (op.equals("=") || op.equals("/=")) {
            if (left != right) {
                System.err.println("Erro semântico: comparação de igualdade requer operandos do mesmo tipo.");
                return CompShType.ERROR;
            }
        } else {
            if (!left.isNumeric() || !right.isNumeric()) {
                System.err.println("Erro semântico: comparações de ordem só são válidas para valores numéricos.");
                return CompShType.ERROR;
            }
        }

        return CompShType.BOOLEAN;
    }


    @Override
    public CompShType visitExprAndOr(CompShParser.ExprAndOrContext ctx) {
        CompShType left = visit(ctx.expr(0));
        CompShType right = visit(ctx.expr(1));

        if (left != CompShType.BOOLEAN || right != CompShType.BOOLEAN) {
            System.err.println("Erro semântico: operadores 'and' e 'or' só aceitam valores booleanos.");
            return CompShType.ERROR;
        }

        return CompShType.BOOLEAN;
    }

    @Override
    public CompShType visitStatIf(CompShParser.StatIfContext ctx) {
        var ifCtx = ctx.if_(); // acede à sub-regra 'if'

        CompShType condType = visit(ifCtx.expr());
        if (condType != CompShType.BOOLEAN) {
            System.err.println("Erro semântico: expressão condicional (if) requer valor booleano.");
            return CompShType.ERROR;
        }

        for (var stat : ifCtx.preStats.stat()) {
            if (visit(stat) == CompShType.ERROR) return CompShType.ERROR;
        }

        if (ifCtx.postStats != null) {
            for (var stat : ifCtx.postStats.stat()) {
                if (visit(stat) == CompShType.ERROR) return CompShType.ERROR;
            }
        }

        return CompShType.VOID;
    }
    
    @Override
    public CompShType visitLoopHead(CompShParser.LoopHeadContext ctx) {
        CompShType cond = visit(ctx.expr());
        if (cond != CompShType.BOOLEAN) {
            System.err.println("Erro semântico: condição do loop 'while' requer valor booleano.");
            return CompShType.ERROR;
        }
        
        if (ctx.stat().isEmpty()) {
            System.err.println("Erro semântico: corpo do loop 'while' está vazio.");
            return CompShType.ERROR;
        }

        for (var stat : ctx.stat()) {
            if (visit(stat) == CompShType.ERROR) return CompShType.ERROR;
        }

        return CompShType.VOID;
    }
    
    @Override
    public CompShType visitLoopTail(CompShParser.LoopTailContext ctx) {
        for (var stat : ctx.stat()) {
            if (visit(stat) == CompShType.ERROR) return CompShType.ERROR;
        }
        
        if (ctx.stat().isEmpty()) {
            System.err.println("Erro semântico: corpo do loop 'until' está vazio.");
            return CompShType.ERROR;
        }


        CompShType cond = visit(ctx.expr());
        if (cond != CompShType.BOOLEAN) {
            System.err.println("Erro semântico: condição do loop 'until' requer valor booleano.");
            return CompShType.ERROR;
        }

        return CompShType.VOID;
    }

    @Override
    public CompShType visitLoopMiddle(CompShParser.LoopMiddleContext ctx) {
        // Verificar se os blocos existem mas estão vazios
        boolean preEmpty = ctx.preStats == null || ctx.preStats.stat().isEmpty();
        boolean postEmpty = ctx.postStats == null || ctx.postStats.stat().isEmpty();

        if (preEmpty || postEmpty) {
            System.err.println("Erro semântico: loop 'while ... do ... end' requer blocos antes e depois da condição.");
            return CompShType.ERROR;
        }

        if (visit(ctx.preStats) == CompShType.ERROR) return CompShType.ERROR;

        CompShType cond = visit(ctx.expr());
        if (cond != CompShType.BOOLEAN) {
            System.err.println("Erro semântico: condição do loop 'while' requer valor booleano.");
            return CompShType.ERROR;
        }

        if (visit(ctx.postStats) == CompShType.ERROR) return CompShType.ERROR;

        return CompShType.VOID;
    }

    @Override
    public CompShType visitExprListLiteral(CompShParser.ExprListLiteralContext ctx) {
        for (var exprCtx : ctx.expr()) {
            CompShType t = visit(exprCtx);
            if (t == CompShType.LIST) {
                System.err.println("Erro semântico: listas não podem conter outras listas.");
                return CompShType.ERROR;
            }
            if (t == CompShType.ERROR) {
                System.err.println("Erro semântico: elemento inválido dentro da lista.");
                return CompShType.ERROR;
            }
        }
        return CompShType.LIST;
    }
    
    @Override
    public CompShType visitExprPrefix(CompShParser.ExprPrefixContext ctx) {
        CompShType argType = visit(ctx.expr());
        if (argType == CompShType.ERROR) return CompShType.ERROR;

        if (argType != CompShType.TEXT) {
            System.err.println("Erro semântico: operador 'prefix' só pode ser aplicado a texto.");
            return CompShType.ERROR;
        }

        return CompShType.TEXT;
    }
    
    @Override
    public CompShType visitExprSuffix(CompShParser.ExprSuffixContext ctx) {
        CompShType argType = visit(ctx.expr());
        if (argType == CompShType.ERROR) return CompShType.ERROR;

        if (argType != CompShType.TEXT) {
            System.err.println("Erro semântico: operador 'suffix' só pode ser aplicado a texto.");
            return CompShType.ERROR;
        }

        return CompShType.TEXT;
    }
    
    @Override
    public CompShType visitExprFilter(CompShParser.ExprFilterContext ctx) {
        CompShType filterExprType = visit(ctx.expr());

        if (filterExprType == CompShType.ERROR) return CompShType.ERROR;

        if (filterExprType != CompShType.TEXT) {
            System.err.println("Erro semântico: expressão de filtro (após '/') tem de ser texto.");
            return CompShType.ERROR;
        }
        return CompShType.TEXT;
    }


    @Override
    public CompShType visitExprRedirection(CompShParser.ExprRedirectionContext ctx) {
        String src = ctx.channel(0).getText();
        String dst = ctx.channel(1).getText();


        Set<String> canais = Set.of("$", "|", "&", "?", "*", "-");

        // Verifica se ambos os lados são canais válidos
        if (!canais.contains(src) || !canais.contains(dst)) {
            System.err.printf("Erro semântico: redireção inválida '%s^%s'. Apenas são permitidos os canais $, |, &, ?, * e -.%n", src, dst);
            return CompShType.ERROR;
        }

        // Redireções semanticamente inválidas
        if (src.equals("-")) {
            System.err.println("Erro semântico: não é possível redirecionar a partir do canal '-'.");
            return CompShType.ERROR;
        }

        if (dst.equals("$") && (src.equals("?") || src.equals("|") || src.equals("&") || src.equals("*") || src.equals("-"))) {
            System.err.println("Erro semântico: não é possível redirecionar para '$'.");
            return CompShType.ERROR;
        }

        if (src.equals("$") && !(dst.equals("?") || dst.equals("-"))) {
            System.err.println("Erro semântico: canal '$' só pode ser redirecionado para '?' ou '-'.");
            return CompShType.ERROR;
        }

        if (src.equals("?") && dst.equals("$")) {
            System.err.println("Erro semântico: não é possível redirecionar '?' para '$'.");
            return CompShType.ERROR;
        }

        if (src.equals("*") && dst.equals("$")) {
            System.err.println("Erro semântico: não é possível redirecionar todos os canais para '$'.");
            return CompShType.ERROR;
        }

        return CompShType.PROGRAM;
    }
}