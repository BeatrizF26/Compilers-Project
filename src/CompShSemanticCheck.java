import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.Set;

public class CompShSemanticCheck extends CompShBaseVisitor<Type> {
    private final TypeSystem typeSystem = new TypeSystem();

    public TypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Type visitProgram(CompShParser.ProgramContext ctx) {
        for (var stat : ctx.stat()) {
            Type t = visit(stat);
            if (t == Type.ERROR) return Type.ERROR;
        }
        return Type.VOID;
    }

    @Override
    public Type visitStatDeclaration(CompShParser.StatDeclarationContext ctx) {
        var ids = ctx.declaration().ID();
        Type declaredType = Type.fromString(ctx.declaration().TYPE().getText());

        for (var idToken : ids) {
            String id = idToken.getText();
            if (typeSystem.symbolExists(id)) {
                System.err.println("Erro semântico: variável '" + id + "' já foi declarada.");
                return Type.ERROR;
            }
            typeSystem.addSymbol(id, declaredType);
        }

        return Type.VOID;
    }

    @Override
    public Type visitExprPipeAssignement(CompShParser.ExprPipeAssignementContext ctx) {
        String id = ctx.ID().getText();
        Type declaredType = ctx.TYPE() != null ? Type.fromString(ctx.TYPE().getText()) : null;

        if (typeSystem.symbolExists(id)) {
            Type existingType = typeSystem.getSymbolType(id);
            if (declaredType != null && declaredType != existingType) {
                System.out.println("tentativa de reatribuição com tipo diferente para '" + id + "'.");
                return Type.ERROR;
            }
            return existingType;
        }

        if (declaredType == null) {
            System.out.println("atribuição via 'store in' requer tipo explícito.");
            return Type.ERROR;
        }

        typeSystem.addSymbol(id, declaredType);
        return declaredType;
    }


    @Override public Type visitExprStr(CompShParser.ExprStrContext ctx)     { return Type.TEXT; }
    @Override public Type visitExprInt(CompShParser.ExprIntContext ctx)     { return Type.INTEGER; }
    @Override public Type visitExprFloat(CompShParser.ExprFloatContext ctx) { return Type.REAL; }

    @Override
    public Type visitExprId(CompShParser.ExprIdContext ctx) {
        String id = ctx.getText();
        if (!typeSystem.symbolExists(id)) {
            System.err.println("Erro semântico: variável '" + id + "' não foi declarada.");
            return Type.ERROR;
        }
        return typeSystem.getSymbolType(id);
    }

    @Override
    public Type visitExprTypeCast(CompShParser.ExprTypeCastContext ctx) {
        Type targetType = Type.fromString(ctx.TYPE().getText());
        Type exprType = visit(ctx.expr());

        if (!typeSystem.isValidConversion(targetType, exprType)) {
            System.err.printf("Erro semântico: não é possível converter de %s para %s.%n", exprType, targetType);
            return Type.ERROR;
        }

        return targetType;
    }
    
    @Override
    public Type visitExprExecute(CompShParser.ExprExecuteContext ctx) {
        Type base = visit(ctx.expr(0));
        if (base != Type.TEXT) {
            System.err.println("Erro semântico: execução (!) requer string base.");
            return Type.ERROR;
        }

        if (ctx.expr().size() > 1) {
              for (int i = 1; i < ctx.expr().size(); i++) {
                  Type argType = visit(ctx.expr(i));

                  if (argType == Type.LIST) {
                      System.err.println("Erro semântico: listas não podem estar dentro de listas.");
                      return Type.ERROR;
                  }

                  if (argType == Type.ERROR) return Type.ERROR;
              }
        }

        return Type.PROGRAM;
    }


    @Override
    public Type visitExprExecuteIsh(CompShParser.ExprExecuteIshContext ctx) {
        Type inner = visit(ctx.expr());

        if (inner != Type.TEXT) {
            System.err.println("Erro semântico: execução ISH (!!) requer texto.");
            return Type.ERROR;
        }

        return Type.PROGRAM;
    }
    
    
    @Override
    public Type visitExprPipe(CompShParser.ExprPipeContext ctx) {
        Type left = visit(ctx.expr(0));
        Type right = visit(ctx.expr(1));

        String canal = ctx.channel() != null ? ctx.channel().getText() : "|";

        boolean rightIsFilter =
            ctx.expr(1) instanceof CompShParser.ExprFilterContext ||
            ctx.expr(1) instanceof CompShParser.ExprPrefixContext ||
            ctx.expr(1) instanceof CompShParser.ExprSuffixContext;

        // Se for um programa com canal, converte o tipo com base no canal
        if (left == Type.PROGRAM && canal != null) {
            switch (canal) {
                case "|":
                case "&":
                    left = Type.TEXT;
                    break;
                case "?":
                    left = rightIsFilter ? Type.TEXT : Type.INTEGER;
                    break;
                case "*":
                case "$":
                    left = Type.PROGRAM;
                    break;
                case "-":
                    left = Type.VOID;
                    break;
            }
        }
        
        if (ctx.expr(1) instanceof CompShParser.ExprPipeAssignementContext assignCtx) {
            Type leftType = visit(ctx.expr(0));
            String id = assignCtx.ID().getText();

            Type declaredType;
            if (assignCtx.TYPE() != null) {
                declaredType = Type.fromString(assignCtx.TYPE().getText());
            } else if (typeSystem.symbolExists(id)) {
                declaredType = typeSystem.getSymbolType(id);
            } else {
                declaredType = null;
            }

            if (declaredType == null) {
                System.err.println("Erro semântico: atribuição via 'store in' requer tipo explícito.");
                return Type.ERROR;
            }

            if (!typeSystem.symbolExists(id)) {
                typeSystem.addSymbol(id, declaredType);
            } else {
                Type existing = typeSystem.getSymbolType(id);
                if (existing != declaredType) {
                    System.err.printf("tentativa de reatribuição com tipo diferente para '%s'.%n", id);
                    return Type.ERROR;
                }
            }

            // Exceção: permitir guardar resultados de um 'program' diretamente em text/integer/real (caso bc ou echo devolva valor simples)

            if (leftType == Type.PROGRAM && (declaredType == Type.TEXT || declaredType == Type.INTEGER || declaredType == Type.REAL)) {
                // Aceita implicitamente
            } else if (!typeSystem.isValidConversion(declaredType, leftType)) {
                System.err.printf("Erro semântico: não é possível armazenar %s em variável do tipo %s%n", leftType, declaredType);
                return Type.ERROR;
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
            return Type.ERROR;
        }

        if (left == Type.VOID || left == Type.ERROR) {
            System.err.printf("Erro semântico: tipo inválido no lado esquerdo do pipe: %s%n", left);
            return Type.ERROR;
        }

        if (rightIsFilter && left != Type.TEXT) {
            System.err.println("Erro semântico: filtros apenas podem ser aplicados a valores do tipo text.");
            return Type.ERROR;
        }
        
        return rightIsFilter ? Type.TEXT : right;

    }

    @Override
    public Type visitExprPlusMinus(CompShParser.ExprPlusMinusContext ctx) {
        Type left = visit(ctx.expr(0));
        Type right = visit(ctx.expr(1));
        String op = ctx.op.getText();

        Type result = typeSystem.checkBinaryOperation(left, op, right);
        if (result == Type.ERROR) {
            System.err.printf("Erro semântico: operação '%s' inválida entre %s e %s.%n", op, left, right);
        }

        return result;
    }

    @Override
    public Type visitExprMulDiv(CompShParser.ExprMulDivContext ctx) {
        Type left = visit(ctx.expr(0));
        Type right = visit(ctx.expr(1));
        String op = ctx.op.getText();

        Type result = typeSystem.checkBinaryOperation(left, op, right);
        if (result == Type.ERROR) {
            System.err.printf("Erro semântico: operação '%s' inválida entre %s e %s.%n", op, left, right);
        }

        return result;
    }

    @Override public Type visitExprUnary(CompShParser.ExprUnaryContext ctx) {
        return visit(ctx.expr());
    }

    @Override public Type visitExprStdout(CompShParser.ExprStdoutContext ctx) { return Type.TEXT; }
    @Override public Type visitExprStderr(CompShParser.ExprStderrContext ctx) { return Type.TEXT; }
    @Override public Type visitExprStdin(CompShParser.ExprStdinContext ctx)   { return Type.TEXT; }
    @Override public Type visitExprParens(CompShParser.ExprParensContext ctx) { return visit(ctx.expr()); }
    @Override public Type visitExprNL(CompShParser.ExprNLContext ctx)         { return Type.TEXT; }

    @Override public Type visitExprBoolTrue(CompShParser.ExprBoolTrueContext ctx) { return Type.BOOLEAN; }
    @Override public Type visitExprBoolFalse(CompShParser.ExprBoolFalseContext ctx) { return Type.BOOLEAN; }

    @Override
    public Type visitExprRelational(CompShParser.ExprRelationalContext ctx) {
        Type left = visit(ctx.expr(0));
        Type right = visit(ctx.expr(1));
        String op = ctx.op.getText();

        if (op.equals("=") || op.equals("/=")) {
            if (left != right) {
                System.err.println("Erro semântico: comparação de igualdade requer operandos do mesmo tipo.");
                return Type.ERROR;
            }
        } else {
            if (!left.isNumeric() || !right.isNumeric()) {
                System.err.println("Erro semântico: comparações de ordem só são válidas para valores numéricos.");
                return Type.ERROR;
            }
        }

        return Type.BOOLEAN;
    }


    @Override
    public Type visitExprAndOr(CompShParser.ExprAndOrContext ctx) {
        Type left = visit(ctx.expr(0));
        Type right = visit(ctx.expr(1));

        if (left != Type.BOOLEAN || right != Type.BOOLEAN) {
            System.err.println("Erro semântico: operadores 'and' e 'or' só aceitam valores booleanos.");
            return Type.ERROR;
        }

        return Type.BOOLEAN;
    }

    @Override
    public Type visitStatIf(CompShParser.StatIfContext ctx) {
        var ifCtx = ctx.if_(); // acede à sub-regra 'if'

        Type condType = visit(ifCtx.expr());
        if (condType != Type.BOOLEAN) {
            System.err.println("Erro semântico: expressão condicional (if) requer valor booleano.");
            return Type.ERROR;
        }

        for (var stat : ifCtx.preStats.stat()) {
            if (visit(stat) == Type.ERROR) return Type.ERROR;
        }

        if (ifCtx.postStats != null) {
            for (var stat : ifCtx.postStats.stat()) {
                if (visit(stat) == Type.ERROR) return Type.ERROR;
            }
        }

        return Type.VOID;
    }   
    
    @Override
    public Type visitLoopHead(CompShParser.LoopHeadContext ctx) {
        Type cond = visit(ctx.expr());
        if (cond != Type.BOOLEAN) {
            System.err.println("Erro semântico: condição do loop 'while' requer valor booleano.");
            return Type.ERROR;
        }
        
        if (ctx.stat().isEmpty()) {
            System.err.println("Erro semântico: corpo do loop 'while' está vazio.");
            return Type.ERROR;
        }

        for (var stat : ctx.stat()) {
            if (visit(stat) == Type.ERROR) return Type.ERROR;
        }

        return Type.VOID;
    }
    
    @Override
    public Type visitLoopTail(CompShParser.LoopTailContext ctx) {
        for (var stat : ctx.stat()) {
            if (visit(stat) == Type.ERROR) return Type.ERROR;
        }
        
        if (ctx.stat().isEmpty()) {
            System.err.println("Erro semântico: corpo do loop 'until' está vazio.");
            return Type.ERROR;
        }


        Type cond = visit(ctx.expr());
        if (cond != Type.BOOLEAN) {
            System.err.println("Erro semântico: condição do loop 'until' requer valor booleano.");
            return Type.ERROR;
        }

        return Type.VOID;
    }

    @Override
    public Type visitLoopMiddle(CompShParser.LoopMiddleContext ctx) {
        // Verificar se os blocos existem mas estão vazios
        boolean preEmpty = ctx.preStats == null || ctx.preStats.stat().isEmpty();
        boolean postEmpty = ctx.postStats == null || ctx.postStats.stat().isEmpty();

        if (preEmpty || postEmpty) {
            System.err.println("Erro semântico: loop 'while ... do ... end' requer blocos antes e depois da condição.");
            return Type.ERROR;
        }

        if (visit(ctx.preStats) == Type.ERROR) return Type.ERROR;

        Type cond = visit(ctx.expr());
        if (cond != Type.BOOLEAN) {
            System.err.println("Erro semântico: condição do loop 'while' requer valor booleano.");
            return Type.ERROR;
        }

        if (visit(ctx.postStats) == Type.ERROR) return Type.ERROR;

        return Type.VOID;
    }

    @Override
    public Type visitExprListLiteral(CompShParser.ExprListLiteralContext ctx) {
        for (var exprCtx : ctx.expr()) {
            Type t = visit(exprCtx);
            if (t == Type.LIST) {
                System.err.println("Erro semântico: listas não podem conter outras listas.");
                return Type.ERROR;
            }
            if (t == Type.ERROR) {
                System.err.println("Erro semântico: elemento inválido dentro da lista.");
                return Type.ERROR;
            }
        }
        return Type.LIST;
    }
    
    @Override
    public Type visitExprPrefix(CompShParser.ExprPrefixContext ctx) {
        Type argType = visit(ctx.expr());
        if (argType == Type.ERROR) return Type.ERROR;

        if (argType != Type.TEXT) {
            System.err.println("Erro semântico: operador 'prefix' só pode ser aplicado a texto.");
            return Type.ERROR;
        }

        return Type.TEXT;
    }
    
    @Override
    public Type visitExprSuffix(CompShParser.ExprSuffixContext ctx) {
        Type argType = visit(ctx.expr());
        if (argType == Type.ERROR) return Type.ERROR;

        if (argType != Type.TEXT) {
            System.err.println("Erro semântico: operador 'suffix' só pode ser aplicado a texto.");
            return Type.ERROR;
        }

        return Type.TEXT;
    }
    
    @Override
    public Type visitExprFilter(CompShParser.ExprFilterContext ctx) {
        Type filterExprType = visit(ctx.expr());

        if (filterExprType == Type.ERROR) return Type.ERROR;

        if (filterExprType != Type.TEXT) {
            System.err.println("Erro semântico: expressão de filtro (após '/') tem de ser texto.");
            return Type.ERROR;
        }
        return Type.TEXT;
    }


    @Override
    public Type visitExprRedirection(CompShParser.ExprRedirectionContext ctx) {
        String src = ctx.channel(0).getText();
        String dst = ctx.channel(1).getText();


        Set<String> canais = Set.of("$", "|", "&", "?", "*", "-");

        // Verifica se ambos os lados são canais válidos
        if (!canais.contains(src) || !canais.contains(dst)) {
            System.err.printf("Erro semântico: redireção inválida '%s^%s'. Apenas são permitidos os canais $, |, &, ?, * e -.%n", src, dst);
            return Type.ERROR;
        }

        // Redireções semanticamente inválidas
        if (src.equals("-")) {
            System.err.println("Erro semântico: não é possível redirecionar a partir do canal '-'.");
            return Type.ERROR;
        }

        if (dst.equals("$") && (src.equals("?") || src.equals("|") || src.equals("&") || src.equals("*") || src.equals("-"))) {
            System.err.println("Erro semântico: não é possível redirecionar para '$'.");
            return Type.ERROR;
        }

        if (src.equals("$") && !(dst.equals("?") || dst.equals("-"))) {
            System.err.println("Erro semântico: canal '$' só pode ser redirecionado para '?' ou '-'.");
            return Type.ERROR;
        }

        if (src.equals("?") && dst.equals("$")) {
            System.err.println("Erro semântico: não é possível redirecionar '?' para '$'.");
            return Type.ERROR;
        }

        if (src.equals("*") && dst.equals("$")) {
            System.err.println("Erro semântico: não é possível redirecionar todos os canais para '$'.");
            return Type.ERROR;
        }

        return Type.PROGRAM;
    }
}