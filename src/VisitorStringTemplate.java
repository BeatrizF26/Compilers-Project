import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;



@SuppressWarnings("CheckReturnValue")
public class VisitorStringTemplate extends CompShBaseVisitor<ST> {

   private STGroup allTemplates = new STGroupFile("java.stg");
   private boolean needsScanner = false;
   private boolean needsBang = false;


   private int varCount = 0;
   private String newVarName() {
      varCount++;
      return "v" + varCount;
   }

   private String pipeVar = null;
   private Map<String, String> declaredVars = new HashMap<>();
   private Map<ParserRuleContext, String> types = new HashMap<>();



   @Override public ST visitProgram(CompShParser.ProgramContext ctx) {
      
      varCount = 0;

      ST res = allTemplates.getInstanceOf("main");
      res.add("name", "Output");

      for (CompShParser.StatContext stat : ctx.stat()) {
         res.add("stat", visit(stat));
      }

      res.add("needsScanner", needsScanner);
      res.add("needsBang", needsBang);

      return res;
   }


   @Override public ST visitStatDeclaration(CompShParser.StatDeclarationContext ctx) {
      
      ST res = allTemplates.getInstanceOf("statDeclaration");

      StringBuilder idList = new StringBuilder();

      for (int i = 0; i < ctx.declaration().ID().size(); i++) {
         if (i > 0) {
             idList.append(", ");
         }

         idList.append(ctx.declaration().ID(i).getText());
     }

      String type = ctx.declaration().TYPE().getText();
      String mappedType = mapType(type);

      declaredVars.put(idList.toString(), mappedType);

      res.add("IDList", idList.toString());
      res.add("TYPE", mappedType);
      
      return res;
   }


   @Override public ST visitStatExpr(CompShParser.StatExprContext ctx) {
      return visit(ctx.expr());
   }


   @Override public ST visitStatLoop(CompShParser.StatLoopContext ctx) {
      return visit(ctx.loop());
   }

   @Override public ST visitStatIf(CompShParser.StatIfContext ctx) {
      return visit(ctx.if_());
   }

   @Override
   public ST visitLoopHead(CompShParser.LoopHeadContext ctx) {
      ST res = allTemplates.getInstanceOf("loopHead");
      ST condST = visit(ctx.expr());

      String condVar = ctx.expr().varName;

      List<ST> statsST = new ArrayList<>();
      for (CompShParser.StatContext statCtx : ctx.stat()) {
         ST st = visit(statCtx);
         if (st != null) statsST.add(st);
      }

      res.add("cond", condVar);
      res.add("condStat", condST);
      res.add("stats", statsST);

      return res;
   }

   @Override
   public ST visitLoopTail(CompShParser.LoopTailContext ctx) {
      ST res = allTemplates.getInstanceOf("loopTail");
      ST condST = visit(ctx.expr());

      String condVar = ctx.expr().varName;

      List<ST> initStats = new ArrayList<>();
      ST condDecl = new ST("<type> <name>;");
      condDecl.add("type", "boolean");
      condDecl.add("name", condVar);
      initStats.add(condDecl);

      List<ST> statsST = new ArrayList<>();
      for (CompShParser.StatContext statCtx : ctx.stat()) {
         ST st = visit(statCtx);
         if (st != null) statsST.add(st);
      }

      res.add("initStats", initStats);
      res.add("stats", statsST);
      res.add("condStat", condST);
      res.add("cond", condVar);

      return res;
   }

   @Override
   public ST visitLoopMiddle(CompShParser.LoopMiddleContext ctx) {
      ST res = allTemplates.getInstanceOf("loopMiddle");

      String resultVar = newVarName();
      ctx.expr().varName = resultVar;
      ST condST = visit(ctx.expr());

      List<ST> prestatsST = new ArrayList<>();
      for (CompShParser.StatContext statCtx : ctx.preStats.stat()) {
         ST st = visit(statCtx);
         if (st != null) {
               prestatsST.add(st);
         }
      }

      List<ST> poststatsST = new ArrayList<>();
      for (CompShParser.StatContext statCtx : ctx.postStats.stat()) {
         ST st = visit(statCtx);
         if (st != null) {
               poststatsST.add(st);
         }
      }

      poststatsST.add(condST);

      res.add("resultVar", resultVar);
      res.add("preStats", prestatsST);
      res.add("postStats", poststatsST);

      return res;
   }

   @Override
   public ST visitIf(CompShParser.IfContext ctx) {
      ST res = new ST("<decl>\n<expr>\nif (<condVar>) {\n<preStats; separator=\"\n\">\n} <if(postStats)>else {\n<postStats; separator=\"\n\">\n}<endif>");

      ST exprST = visit(ctx.expr());             
      String condVar = ctx.expr().varName;

      String decl = "boolean " + condVar + ";";

      res.add("decl", decl);
      res.add("expr", exprST);
      res.add("condVar", condVar);

      List<ST> preStats = ctx.preStats.stat().stream()
         .map(this::visit)
         .collect(Collectors.toList());
      res.add("preStats", preStats);

      if (ctx.postStats != null) {
         List<ST> postStats = ctx.postStats.stat().stream()
               .map(this::visit)
               .collect(Collectors.toList());
         res.add("postStats", postStats);
      }

      return res;
   }

   @Override public ST visitStatBlock(CompShParser.StatBlockContext ctx) {
      ST res = null;
      return visitChildren(ctx);
      //return res;
   }

   @Override public ST visitDeclaration(CompShParser.DeclarationContext ctx) {
      ST res = null;
      return visitChildren(ctx);
      //return res;
   }

   @Override public ST visitExprBoolFalse(CompShParser.ExprBoolFalseContext ctx) {
      ST res = null;
      return visitChildren(ctx);
      //return res;
   }

   @Override public ST visitExprExecuteIsh(CompShParser.ExprExecuteIshContext ctx) {
      
      ST expr = visit(ctx.expr());
      String fileName = ctx.expr().getText().replaceAll("^\"|\"$", "");
      Path filePath = Paths.get("../examples", fileName);
      
      StringBuilder fileContent = new StringBuilder();
      try {
         for (String line : java.nio.file.Files.readAllLines(filePath)) {
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
      ST block = allTemplates.getInstanceOf("block");

      if (codeStats instanceof Iterable<?> iterable) {
         for (Object s : iterable)
               block.add("stat", s);
      } else if (codeStats != null) {
         block.add("stat", codeStats);
      }

      ST res = allTemplates.getInstanceOf("exprExecuteIsh");
      res.add("code", block);
      return res;
   }

   @Override public ST visitExprFloat(CompShParser.ExprFloatContext ctx) {
      
      ST res = allTemplates.getInstanceOf("decl");
      String var = newVarName();
      ctx.varName = var;
      types.put(ctx, "float");

      res.add("type", "double");
      res.add("var", var);
      res.add("value", ctx.FLOAT().getText());

      return res;

   }


   @Override
    public ST visitExprStr(CompShParser.ExprStrContext ctx) {
        ST res = allTemplates.getInstanceOf("decl");
        String var = newVarName();
        ctx.varName = var;
        types.put(ctx, "String");

        res.add("type", "String");
        res.add("var", var);
        res.add("value", ctx.STRING().getText());

        return res;
    }



   @Override public ST visitExprStderr(CompShParser.ExprStderrContext ctx) {
      
      ST res = allTemplates.getInstanceOf("printStderr");
      res.add("value", pipeVar);
      
      return res;
   }

   @Override public ST visitExprPipe(CompShParser.ExprPipeContext ctx) {
      
      ST left = visit(ctx.expr(0));
      pipeVar = ctx.expr(0).varName;

      ST res = allTemplates.getInstanceOf("exprPipeExpr");


      if (ctx.channel() != null && ctx.expr(1) == null) {
         String symbol = ctx.channel().getText();

         switch (symbol) {
               case "$":
                  res = allTemplates.getInstanceOf("DollarChannel");
                  break;
               case "|":
                  res = allTemplates.getInstanceOf("StdoutChannel");
                  break;
               case "&":
                  res = allTemplates.getInstanceOf("StderrChannel");
                  break;
               case "?":
                  res = allTemplates.getInstanceOf("ExitChannel");
                  break;
               default:
                  throw new RuntimeException("Canal não suportado: " + symbol);
         }

         res.add("value", pipeVar);
         return res;
      }


      ST right = visit(ctx.expr(1));


      String resultVar;
      if (ctx.expr(1) instanceof CompShParser.ExprPipeAssignementContext) {
         resultVar = ((CompShParser.ExprPipeAssignementContext) ctx.expr(1)).ID().getText();
      } else {
         resultVar = newVarName();
      }
      ctx.varName = resultVar;


      res.add("type", "Program");
      res.add("var", resultVar);
      res.add("expr1", left);
      res.add("channel", ctx.channel() != null ? ctx.channel().getText() : "");
      res.add("expr2", right);

      return res;
   }


   @Override public ST visitExprAndOr(CompShParser.ExprAndOrContext ctx) {
      
      ST res = allTemplates.getInstanceOf("exprAndOr");

      String var = newVarName();
      ctx.varName = var;

      ST left = visit(ctx.expr(0));
      ST right = visit(ctx.expr(1));

      res.add("stat", left);
      res.add("stat", right);

      String op = ctx.getChild(1).getText();

      
      String javaOp;
      if (op.equals("and")) {
         javaOp = "&&";
      } else if (op.equals("or")) {
         javaOp = "||";
      } else {
         javaOp = op; 
      }

      res.add("value1", ctx.expr(0).varName);
      res.add("value2", ctx.expr(1).varName);
      res.add("op", javaOp);
      res.add("resultVar", var);

      return res;
   }


   @Override public ST visitExprMulDiv(CompShParser.ExprMulDivContext ctx) {
      
      ST res = allTemplates.getInstanceOf("exprMulDiv");

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


   @Override public ST visitExprBoolTrue(CompShParser.ExprBoolTrueContext ctx) {
      ST res = null;
      return visitChildren(ctx);
      //return res;
   }

   @Override public ST visitExprPipeAssignement(CompShParser.ExprPipeAssignementContext ctx) {
      
      
      String id = ctx.ID().getText();
      String type = (ctx.TYPE() != null) ? ctx.TYPE().getText() : null;
      ST res;

      if (type != null) {
         String mappedType = mapType(type);
         declaredVars.put(id, mappedType);
         res = allTemplates.getInstanceOf("pipeAssignDeclare");
         res.add("type", mappedType);
         res.add("id", id);
         res.add("value", pipeVar);
      } else {
         res = allTemplates.getInstanceOf("pipeAssignment");
         res.add("id", id);
         res.add("value", pipeVar);
      }

      return res;
   }


   @Override public ST visitExprStdout(CompShParser.ExprStdoutContext ctx) {
      
      ST res = allTemplates.getInstanceOf("printStdout");
      res.add("value", pipeVar);
      
      return res;
   }


   @Override public ST visitExprNL(CompShParser.ExprNLContext ctx) {
      
      ST res = allTemplates.getInstanceOf("ExprNL");
      return res;
   }


   @Override public ST visitExprId(CompShParser.ExprIdContext ctx) {
      
      ST res = allTemplates.getInstanceOf("exprId");
      String id = ctx.ID().getText();
      ctx.varName = id;
      
      res.add("id", id);
      
      return res;
   }


   @Override public ST visitExprPrefix(CompShParser.ExprPrefixContext ctx) {
      ST res = null;
      return visitChildren(ctx);
      //return res;
   }

   @Override


    public ST visitExprParens(CompShParser.ExprParensContext ctx) {

        ST res = allTemplates.getInstanceOf("exprParen");
        ST inner = visit(ctx.expr());

        res.add("expr", inner);
        ctx.varName = ctx.expr().varName;

        return res;
    }


   @Override public ST visitExprInt(CompShParser.ExprIntContext ctx) {
      
      ST res = allTemplates.getInstanceOf("decl");
      String var = newVarName();
      ctx.varName = var;
      types.put(ctx, "int");

      res.add("type", "int");
      res.add("var", var);
      res.add("value", ctx.INT().getText());

      return res;

   }


   @Override public ST visitExprSuffix(CompShParser.ExprSuffixContext ctx) {
      ST res = null;
      return visitChildren(ctx);
      //return res;
   }

   @Override 
   public ST visitExprRelational(CompShParser.ExprRelationalContext ctx) {
      String var;
      if (ctx.varName != null) {
         var = ctx.varName;
      } else {
         var = newVarName();
         ctx.varName = var;
      }

      ST left = visit(ctx.expr(0));
      ST right = visit(ctx.expr(1));

      String op = ctx.getChild(1).getText();

      String typeLeft = types.get(ctx.expr(0));
      String typeRight = types.get(ctx.expr(1));

      boolean isString = "String".equals(typeLeft) || typeLeft == null
                     || "String".equals(typeRight) || typeRight == null;

      if (("=".equals(op) || "/=".equals(op)) && isString) {
         ST res;
         if ("=".equals(op)) {
            res = allTemplates.getInstanceOf("exprRelationalEquals");
         } else {
            res = allTemplates.getInstanceOf("exprRelationalNotEquals");
         }
         res.add("stat", left);
         res.add("stat", right);
         res.add("value1", ctx.expr(0).varName);
         res.add("value2", ctx.expr(1).varName);
         res.add("resultVar", var);
         return res;

      } else {
         ST res = allTemplates.getInstanceOf("exprRelationalDefault");

         String javaOp = switch (op) {
               case "/=" -> "!=";
               case "="  -> "==";
               default   -> op;
         };

         res.add("stat", left);
         res.add("stat", right);
         res.add("value1", ctx.expr(0).varName);
         res.add("value2", ctx.expr(1).varName);
         res.add("op", javaOp);
         res.add("resultVar", var);
         return res;
      }
   }



   @Override
   public ST visitExprListLiteral(CompShParser.ExprListLiteralContext ctx) {
      ST st = allTemplates.getInstanceOf("listLiteral");

      // Percorrer as expressões da lista
      for (CompShParser.ExprContext exprCtx : ctx.expr()) {
         ST exprST = visit(exprCtx);
         st.add("elements", exprST.render()); // adiciona o código da expressão na lista
      }

      return st;
   }



   @Override public ST visitExprUnary(CompShParser.ExprUnaryContext ctx) {
      
      ST res = allTemplates.getInstanceOf("exprUnary");

      String var = newVarName();
      ctx.varName = var;

      ST inner = visit(ctx.expr());
      res.add("stat", inner);

      String signal = ctx.sign.getText();
      String type;
      String valueExpr = ctx.expr().varName;

      if (signal.equals("not")) {
         signal = "!";
         type = "boolean";
      } else {
         type = "double";
      }

      res.add("type", type);
      res.add("var", var);
      res.add("signal", signal);
      res.add("value", valueExpr);

      return res;
   }


   @Override public ST visitExprExecute(CompShParser.ExprExecuteContext ctx) {
      
      needsBang = true;

      ST res = allTemplates.getInstanceOf("exprExecute");
  
      ST commandCode = visit(ctx.expr(0));
      String commandVar = ctx.expr(0).varName;
  
      StringBuilder additionalParamsBuilder = new StringBuilder();
      List<String> paramVars = new ArrayList<>();
  
      for (int i = 1; i < ctx.expr().size(); i++) {
          ST paramCode = visit(ctx.expr(i));
          String paramVar = ctx.expr(i).varName;
          paramVars.add(paramVar);
          res.add("commandCode", paramCode);
      }
  
      String additionalParams = "";
      if (!paramVars.isEmpty()) {
          additionalParams = String.join(" + \" \" + ", paramVars);
      }
  
      String processVar = newVarName();
      ctx.varName = processVar;
  
      res.add("commandCode", commandCode);
      res.add("commandVar", commandVar);
      res.add("processVar", processVar);
      if (!additionalParams.isEmpty())
          res.add("additionalParams", additionalParams);
  
      return res;
   }



   @Override public ST visitExprStdin(CompShParser.ExprStdinContext ctx) {
      
      needsScanner = true;

      ST res = allTemplates.getInstanceOf("exprStdin");

      String promptVar;
      String promptDecl = "";

      if (ctx.prompt != null) {
         if (ctx.prompt.getType() == CompShParser.STRING) {
            promptVar = ctx.prompt.getText();
         } else {
            promptVar = "\"\" + " + ctx.prompt.getText() + " + \"\"";
         }
      } else {
         promptVar = "\">\"";
      }

      res.add("promptDecl", promptDecl);
      res.add("promptVar", promptVar);

      String inputVar = newVarName();
      res.add("inputVar", inputVar);

      String destinationVar = pipeVar;
      String expectedType = "String";

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


   @Override public ST visitExprFilter(CompShParser.ExprFilterContext ctx) {
      ST res = null;
      return visitChildren(ctx);
      //return res;
   }

   @Override public ST visitExprTypeCast(CompShParser.ExprTypeCastContext ctx) {
      
      ST inner = visit(ctx.expr());
      String var = newVarName();
      ctx.varName = var;

      String targetType = ctx.TYPE().getText();
      String javaType = mapType(targetType);

      ST block = allTemplates.getInstanceOf("block");
      block.add("stat", inner);

      if (ctx.expr().varName == null || ctx.expr().varName.isEmpty()) {
         ctx.varName = inner.getAttribute("var") != null ? inner.getAttribute("var").toString() : var;
         return inner;
      }

      ST cast;
      switch (javaType) {
         case "int":
            cast = allTemplates.getInstanceOf("exprCastToInt");
            break;
         case "double":
            cast = allTemplates.getInstanceOf("exprCastToDouble");
            break;
         case "String":
            cast = allTemplates.getInstanceOf("exprCastToString");
            break;
         default:
            return inner;
      }

      cast.add("var", var);
      cast.add("value", ctx.expr().varName);

      block.add("stat", cast);
      return block;
   }


   @Override public ST visitExprPlusMinus(CompShParser.ExprPlusMinusContext ctx) {
      
      ST res = allTemplates.getInstanceOf("exprPlusMinus");

      String var = newVarName();
      ctx.varName = var;

      ST left = visit(ctx.expr(0));
      ST right = visit(ctx.expr(1));

      res.add("stat", left);
      res.add("stat", right);

      String type = "";
      if (ctx.op.getText().equals("+") &&
         (isStringExpression(ctx.expr(0)) || isStringExpression(ctx.expr(1)))) {
         type = "String";
      } else if (isDoubleExpression(ctx.expr(0)) || isDoubleExpression(ctx.expr(1))) {
         type = "double";
      } else {
         type = "int";
      }


      res.add("type", type);
      res.add("var", var);
      res.add("value1", ctx.expr(0).varName);
      res.add("op", ctx.op.getText());
      res.add("value2", ctx.expr(1).varName);

      return res;
   }


   @Override public ST visitDollarChannel(CompShParser.DollarChannelContext ctx) {
      
      ST res = allTemplates.getInstanceOf("DollarChannel");
      res.add("value", pipeVar);

      return res;
   }


   @Override public ST visitStdoutChannel(CompShParser.StdoutChannelContext ctx) {
      
      ST res = allTemplates.getInstanceOf("StdoutChannel");
      res.add("value", pipeVar);

      return res;
   }


   @Override public ST visitStderrChannel(CompShParser.StderrChannelContext ctx) {
      
      ST res = allTemplates.getInstanceOf("StderrChannel");
      res.add("value", pipeVar);

      return res;
   }


   @Override public ST visitExitChannel(CompShParser.ExitChannelContext ctx) {
      
      ST res = allTemplates.getInstanceOf("ExitChannel");
      res.add("value", pipeVar);

      return res;
   }


   @Override public ST visitAllChannels(CompShParser.AllChannelsContext ctx) {
      ST res = null;
      return visitChildren(ctx);
      //return res;
   }


   @Override public ST visitNoChannel(CompShParser.NoChannelContext ctx) {
      ST res = null;
      return visitChildren(ctx);
      //return res;
   }


   private String mapType(String type) {
      return switch (type) {
          case "int", "integer" -> "int";
          case "float", "real" -> "double";
          case "text" -> "String";
          case "program" -> "Program";
          case "list" -> "ListValue";
          default -> type;
      };
  }

   private boolean isStringExpression(ParseTree expr) {
      if (expr instanceof CompShParser.ExprStrContext) {
         return true;
      } else if (expr instanceof CompShParser.ExprPlusMinusContext ctx) {
         return isStringExpression(ctx.expr(0)) || isStringExpression(ctx.expr(1));
      }
      return false;
   }

   private boolean isDoubleExpression(ParseTree expr) {
      if (expr instanceof CompShParser.ExprFloatContext) {
         return true;
      } else if (expr instanceof CompShParser.ExprPlusMinusContext ctx) {
         return isDoubleExpression(ctx.expr(0)) || isDoubleExpression(ctx.expr(1));
      }
      return false;
   }
}