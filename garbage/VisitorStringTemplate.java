import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VisitorStringTemplate extends CompShBaseVisitor<ST> {

    private STGroup templates = new STGroupFile("java.stg");
    private boolean needsScanner = false;


    private int varCount = 0;
    private String newVarName() {
        varCount++;
        return "v" + varCount;
    }

    private String pipeVar = null;
    private Map<String, String> declaredVars = new HashMap<>();

    @Override
    public ST visitProgram(CompShParser.ProgramContext ctx) {
        varCount = 0;

        ST res = templates.getInstanceOf("main");
        res.add("name", "Output");

        for (CompShParser.StatContext stat : ctx.stat()) {
            res.add("stat", visit(stat));
        }

        res.add("needsScanner", needsScanner);

        return res;
    }


    @Override
    public ST visitStatDeclaration(CompShParser.StatDeclarationContext ctx) {
        ST res = templates.getInstanceOf("statDeclaration");

        String id = ctx.declaration().ID().getText();
        String type = ctx.declaration().TYPE().getText();
        String mappedType = mapType(type);

        declaredVars.put(id, mappedType);

        res.add("ID", id);
        res.add("TYPE", mappedType);
        return res;
    }


    @Override
    public ST visitStatExpr(CompShParser.StatExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public ST visitExprInt(CompShParser.ExprIntContext ctx) {
        ST res = templates.getInstanceOf("decl");
        String var = newVarName();
        ctx.varName = var;

        res.add("type", "int");
        res.add("var", var);
        res.add("value", ctx.INT().getText());

        return res;
    }

    @Override
    public ST visitExprFloat(CompShParser.ExprFloatContext ctx) {
        ST res = templates.getInstanceOf("decl");
        String var = newVarName();
        ctx.varName = var;

        res.add("type", "double");
        res.add("var", var);
        res.add("value", ctx.FLOAT().getText());

        return res;
    }

    @Override
    public ST visitExprStr(CompShParser.ExprStrContext ctx) {
        ST res = templates.getInstanceOf("decl");
        String var = newVarName();
        ctx.varName = var;

        res.add("type", "String");
        res.add("var", var);
        res.add("value", ctx.STRING().getText());

        return res;
    }

    @Override
    public ST visitExprId(CompShParser.ExprIdContext ctx) {
        ST res = templates.getInstanceOf("exprId");
        String id = ctx.ID().getText();
        ctx.varName = id;
        res.add("id", id);
        return res;
    }

    @Override
    public ST visitExprPlusMinus(CompShParser.ExprPlusMinusContext ctx) {
        ST res = templates.getInstanceOf("exprPlusMinus");

        String var = newVarName();
        ctx.varName = var;

        ST left = visit(ctx.expr(0));
        ST right = visit(ctx.expr(1));

        res.add("stat", left);
        res.add("stat", right);

        String type = "double";
        if (ctx.op.getText().equals("+")) {
            if (ctx.expr(0) instanceof CompShParser.ExprStrContext || ctx.expr(1) instanceof CompShParser.ExprStrContext) {
                type = "String";
            }
        }

        res.add("type", type);
        res.add("var", var);
        res.add("value1", ctx.expr(0).varName);
        res.add("op", ctx.op.getText());
        res.add("value2", ctx.expr(1).varName);

        return res;
    }

    @Override
    public ST visitExprMulDiv(CompShParser.ExprMulDivContext ctx) {
        ST res = templates.getInstanceOf("exprMulDiv");

        String var = newVarName();
        ctx.varName = var;

        ST left = visit(ctx.expr(0));
        ST right = visit(ctx.expr(1));

        res.add("stat", left);
        res.add("stat", right);

        res.add("type", "double");
        res.add("var", var);
        res.add("value1", ctx.expr(0).varName);
        res.add("op", ctx.op.getText());
        res.add("value2", ctx.expr(1).varName);

        return res;
    }

    @Override
    public ST visitExprUnary(CompShParser.ExprUnaryContext ctx) {
        ST res = templates.getInstanceOf("exprUnary");

        String var = newVarName();
        ctx.varName = var;

        ST inner = visit(ctx.expr());

        res.add("stat", inner);
        res.add("type", "double");
        res.add("var", var);
        res.add("value", ctx.sign.getText() + ctx.expr().getText());

        return res;
    }

    @Override
    public ST visitExprPipe(CompShParser.ExprPipeContext ctx) {
        ST left = visit(ctx.expr(0));
        pipeVar = ctx.expr(0).varName;

        ST res;

        if (ctx.channel() != null && ctx.expr(1) == null) {
            String symbol = ctx.channel().getText();

            switch (symbol) {
                case "$":
                    res = templates.getInstanceOf("DollarChannel");
                    break;
                case "|":
                    res = templates.getInstanceOf("StdoutChannel");
                    break;
                case "&":
                    res = templates.getInstanceOf("StderrChannel");
                    break;
                case "?":
                    res = templates.getInstanceOf("ExitChannel");
                    break;
                default:
                    throw new RuntimeException("Canal não suportado: " + symbol);
            }

            res.add("value", pipeVar);
            return res;
        }

        ST right = visit(ctx.expr(1));

        res = templates.getInstanceOf("exprPipeExpr");
        String resultVar = newVarName();
        ctx.varName = resultVar;

        res.add("type", "Program");
        res.add("var", resultVar);
        res.add("expr1", left);
        res.add("pipe", "|");
        res.add("channel", ctx.channel() != null ? ctx.channel().getText() : "");
        res.add("expr2", right);

        return res;
    }


    @Override
    public ST visitExprPipeAssignement(CompShParser.ExprPipeAssignementContext ctx) {
        String id = ctx.ID().getText();
        String type = (ctx.TYPE() != null) ? ctx.TYPE().getText() : null;
        ST res;

        if (type != null) {
            String mappedType = mapType(type);
            declaredVars.put(id, mappedType);
            res = templates.getInstanceOf("pipeAssignDeclare");
            res.add("type", mappedType);
            res.add("id", id);
            res.add("value", pipeVar);
        } else {
            res = templates.getInstanceOf("pipeAssignment");
            res.add("id", id);
            res.add("value", pipeVar);
        }

        return res;
    }


    @Override
    public ST visitExprStdout(CompShParser.ExprStdoutContext ctx) {
        ST res = templates.getInstanceOf("printStdout");
        res.add("value", pipeVar);
        return res;
    }

    @Override
    public ST visitExprStderr(CompShParser.ExprStderrContext ctx) {
        ST res = templates.getInstanceOf("printStderr");
        res.add("value", pipeVar);
        return res;
    }

    @Override
    public ST visitExprStdin(CompShParser.ExprStdinContext ctx) {
        needsScanner = true;

        ST res = templates.getInstanceOf("exprStdin");

        ST promptExpr = visit(ctx.expr());
        String promptVar = ctx.expr().varName;
        res.add("promptDecl", promptExpr);
        res.add("promptVar", promptVar);

        String inputVar = newVarName();
        res.add("inputVar", inputVar);

        // Verifica se há variável de destino e busca o tipo
        String destinationVar = pipeVar; // ou outra lógica dependendo do uso
        String expectedType = "String"; // default

        if (destinationVar != null && declaredVars.containsKey(destinationVar)) {
            expectedType = declaredVars.get(destinationVar);
        }

        res.add("targetType", expectedType);

        String castCode;
        switch (expectedType) {
            case "int":
                castCode = "Integer.parseInt(" + inputVar + ")";
                break;
            case "double":
                castCode = "Double.parseDouble(" + inputVar + ")";
                break;
            case "String":
                castCode = inputVar;
                break;
            default:
                castCode = inputVar;
        }

        res.add("castCode", castCode);

        String varName = newVarName();
        ctx.varName = varName;
        res.add("varName", varName);

        return res;
    }

    @Override
    public ST visitExprTypeCast(CompShParser.ExprTypeCastContext ctx) {
        ST inner = visit(ctx.expr());
        String var = newVarName();
        ctx.varName = var;

        String targetType = ctx.TYPE().getText();
        String javaType = mapType(targetType);

        ST block = templates.getInstanceOf("block");
        block.add("stat", inner);

        if (ctx.expr().varName == null || ctx.expr().varName.isEmpty()) {
            ctx.varName = inner.getAttribute("var") != null ? inner.getAttribute("var").toString() : var;
            return inner;
        }

        ST cast;
        switch (javaType) {
            case "int":
                cast = templates.getInstanceOf("exprCastToInt");
                break;
            case "double":
                cast = templates.getInstanceOf("exprCastToDouble");
                break;
            case "String":
                cast = templates.getInstanceOf("exprCastToString");
                break;
            default:
                return inner;
        }

        cast.add("var", var);
        cast.add("value", ctx.expr().varName);

        block.add("stat", cast);
        return block;
    }

    @Override
    public ST visitExprExecute(CompShParser.ExprExecuteContext ctx) {
        ST res = templates.getInstanceOf("exprExecute");

        ST commandCode = visit(ctx.expr());
        String commandVar = ctx.expr().varName;

        String processVar = newVarName();

        ctx.varName = processVar;

        res.add("commandCode", commandCode);
        res.add("commandVar", commandVar);
        res.add("processVar", processVar);

        return res;
    }

    @Override
    public ST visitExprExecuteIsh(CompShParser.ExprExecuteIshContext ctx) {
        ST expr = visit(ctx.expr());
        String fileName = ctx.expr().getText().replaceAll("^\"|\"$", "");
        
        StringBuilder fileContent = new StringBuilder();
        try {
            for (String line : java.nio.file.Files.readAllLines(java.nio.file.Paths.get(fileName))) {
                fileContent.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler ficheiro .ish: " + fileName, e);
        }

        CompShLexer lexer = new CompShLexer(CharStreams.fromString(fileContent.toString()));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CompShParser parser = new CompShParser(tokens);
        ParseTree tree = parser.program();

        VisitorStringTemplate nestedVisitor = new VisitorStringTemplate();
        ST codeGenerated = nestedVisitor.visit(tree);
        
        this.needsScanner = this.needsScanner || nestedVisitor.needsScanner;

        Object codeStats = codeGenerated.getAttribute("stat");
        ST block = templates.getInstanceOf("block");

        if (codeStats instanceof Iterable<?> iterable) {
            for (Object s : iterable)
                block.add("stat", s);
        } else if (codeStats != null) {
            block.add("stat", codeStats);
        }

        ST res = templates.getInstanceOf("exprExecuteIsh");
        res.add("code", block);
        return res;
    }


    @Override
    public ST visitDollarChannel(CompShParser.DollarChannelContext ctx) {
        ST res = templates.getInstanceOf("DollarChannel");
        res.add("value", pipeVar);
        return res;
    }

    @Override
    public ST visitStdoutChannel(CompShParser.StdoutChannelContext ctx) {
        ST res = templates.getInstanceOf("StdoutChannel");
        res.add("value", pipeVar);
        return res;
    }

    @Override
    public ST visitStderrChannel(CompShParser.StderrChannelContext ctx) {
        ST res = templates.getInstanceOf("StderrChannel");
        res.add("value", pipeVar);
        return res;
    }

    @Override
    public ST visitExitChannel(CompShParser.ExitChannelContext ctx) {
        ST res = templates.getInstanceOf("ExitChannel");
        res.add("value", pipeVar);
        return res;
    }


    private String mapType(String type) {
        return switch (type) {
            case "int", "integer" -> "int";
            case "float", "real" -> "double";
            case "text" -> "String";
            case "program" -> "Program";
            default -> type;
        };
    }
}
