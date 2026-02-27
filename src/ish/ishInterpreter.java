import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import javax.management.RuntimeErrorException;

@SuppressWarnings("CheckReturnValue")
public class ishInterpreter extends CompShBaseVisitor<Value> {

   private final Map<String, Value> memory = new HashMap<>();
   private Value pipedValue = null;

   // @Override public Value visitProgram(CompShParser.ProgramContext ctx) {
   // Value res = null;
   // return visitChildren(ctx);
   // //return res;
   // }

   // @Override public Value
   // visitStatDeclaration(CompShParser.StatDeclarationContext ctx) {
   // Value res = null;
   // return visitChildren(ctx);
   // //return res;
   // }

   // @Override public Value visitStatExpr(CompShParser.StatExprContext ctx) {
   // Value res = null;
   // return visitChildren(ctx);
   // //return res;
   // }

   // @Override
   // public Value visitStatLoop(CompShParser.StatLoopContext ctx) {
   // Value res = null;
   // return visitChildren(ctx);
   // // return res;
   // }

   // @Override
   // public Value visitStatIf(CompShParser.StatIfContext ctx) {
   // Value res = null;
   // return visitChildren(ctx);
   // // return res;
   // }

   @Override
   public Value visitLoopHead(CompShParser.LoopHeadContext ctx) {
      // execute a 'loop' 'while' expr 'do' (stat ';'?)* 'end'

      Value condition = visit(ctx.expr());

      if (!(condition instanceof BooleanValue)) {
         throw new RuntimeException("Loop condition must be of type boolean");
      }

      while (((BooleanValue) condition).value()) {
         for (CompShParser.StatContext statCtx : ctx.stat()) {
            // visit each stat inside the loop
            visit(statCtx);
         }

         // update condition
         condition = visit(ctx.expr());

         if (!(condition instanceof BooleanValue)) {
            throw new RuntimeException("Loop condition must be of type boolean");
         }
      }

      return null;
   }

   @Override
   public Value visitLoopTail(CompShParser.LoopTailContext ctx) {
      // execute a 'loop' (stat ';'?)* 'until' expr 'end'
      Value condition;

      // run at least once
      do {
         for (CompShParser.StatContext statCtx : ctx.stat()) {
            // visit each stat inside the loop
            visit(statCtx);
         }

         // evaluate condition
         condition = visit(ctx.expr());
         if (!(condition instanceof BooleanValue)) {
            throw new RuntimeException("Loop condition must be of type boolean");
         }
      } while (!((BooleanValue) condition).value());

      return null;
   }

   @Override
   public Value visitLoopMiddle(CompShParser.LoopMiddleContext ctx) {
      // execute a 'loop' (stat ';'?)* 'while' expr 'do' (stat ';'?)* 'end'
      Value condition;

      // execute first block of stats before while loop
      for (CompShParser.StatContext statCtx : ctx.preStats.stat()) {
         visit(statCtx);
      }

      // evaluate condition and loop through it
      while (true) {
         condition = visit(ctx.expr());
         System.out.println(condition);
         if (!(condition instanceof BooleanValue)) {
            throw new RuntimeException("Loop condition must be of type boolean");
         }

         BooleanValue boolCond = (BooleanValue) condition;

         // if loop condition is false --break
         if (!boolCond.value()) {
            break;
         }

         // execute stats after do
         for (CompShParser.StatContext statCtx : ctx.postStats.stat()) {
            visit(statCtx);
         }
      }
      return null;
   }

   @Override
   public Value visitIf(CompShParser.IfContext ctx) {
      // execute a 'if' expr 'then' (stat ';'?)* ('else' (stat ';'?)*)? 'end'

      // evaluate if condtion
      Value condition = visit(ctx.expr());
      if (!(condition instanceof BooleanValue)) {
         throw new RuntimeException("If condition must be of type boolean");
      }

      BooleanValue boolCond = (BooleanValue) condition;

      // if the condition is true --execute first block if stats
      if (boolCond.value()) {
         for (CompShParser.StatContext statCtx : ctx.preStats.stat()) {
            visit(statCtx);
         }
      } else {
         if (ctx.postStats != null) {
            // execute second block of stats
            for (CompShParser.StatContext statCtx : ctx.postStats.stat()) {
               visit(statCtx);
            }
         }
      }

      return null;

   }

   @Override
   public Value visitDeclaration(CompShParser.DeclarationContext ctx) {
      // declare (a set of) variables
      String type = ctx.TYPE().getText();

      for (TerminalNode idNode : ctx.ID()) {
         String id = idNode.getText();
         Value val;

         switch (type) {
            case "text":
               val = new TextValue();
               break;
            case "integer":
               val = new IntegerValue();
               break;
            case "real":
               val = new RealValue();
               break;
            case "program":
               val = new Program();
               break;
            default:
               throw new RuntimeException("Unknown type: " + type);
         }

         if (this.memory.containsKey(id)) {
            throw new RuntimeException("Variable '" + id + "' already declared.");
         }

         this.memory.put(id, val);
      }

      return null;
   }

   @Override
   public Value visitExprBoolFalse(CompShParser.ExprBoolFalseContext ctx) {
      // returns a new BooleanValue with value = false
      return new BooleanValue(false);
   }

   // @Override
   // public Value visitExprExecuteIsh(CompShParser.ExprExecuteIshContext ctx) {
   // Value res = null;
   // return visitChildren(ctx);
   // // return res;
   // }

   @Override
   public Value visitExprFloat(CompShParser.ExprFloatContext ctx) {
      // return a new real type
      return new RealValue(Double.parseDouble(ctx.getText()));
   }

   @Override
   public Value visitExprStderr(CompShParser.ExprStderrContext ctx) {
      // print to stderr
      if (this.pipedValue == null) {
         throw new RuntimeException("Don't know what to print!");
      }

      if (this.pipedValue instanceof Program && ((Program) this.pipedValue).stderr() != null) {
         // if program --print stderr
         System.err.println(((Program) this.pipedValue).stderr());
      } else {
         // if not program just print its normal value
         System.err.println(this.pipedValue.value());
      }
      return null;
   }

   // currently only supports execute | execute (more than this does not work!)
   @Override
   public Value visitExprPipe(CompShParser.ExprPipeContext ctx) {
      Value left = visit(ctx.expr(0));
      Value prevPipe = this.pipedValue;

      Value result;

      boolean isLeftExec = ctx.expr(0) instanceof CompShParser.ExprExecuteContext;
      boolean isRightExec = ctx.expr(1) instanceof CompShParser.ExprExecuteContext;

      // both sides are external commands
      if (isLeftExec && isRightExec) {
         Program leftProgram = (Program) left;

         CompShParser.ExprExecuteContext execCtx = (CompShParser.ExprExecuteContext) ctx.expr(1);
         Value progVal = visit(execCtx.expr(0));
         if (!progVal.isText()) {
            throw new RuntimeException("Expected text as command.");
         }
         String prog = ((TextValue) progVal).value();

         List<String> args = new ArrayList<>();
         for (int i = 1; i < execCtx.expr().size(); i++) {
            Value argVal = visit(execCtx.expr(i));
            if (!argVal.isText()) {
               throw new RuntimeException("Expected text as argument.");
            }
            args.add(((TextValue) argVal).value());
         }

         // redirect stdout of left program to stdin of right
         result = runProgramWithStdin(prog, args, leftProgram.stdout());
      }

      // only right is external or a specific channel is selected
      else if (isRightExec || ctx.channel() != null) {
         Program pipe;
         if (left instanceof Program) {
            pipe = (Program) left;
         } else {
            pipe = new Program(left);
         }

         String channel = ctx.channel() != null ? ctx.channel().getText() : "$";
         String pipeInput;

         switch (channel) {
            case "$":
               pipeInput = pipe.value().value().toString();
               break;
            case "|":
               pipeInput = pipe.stdout();
               break;
            case "&":
               pipeInput = pipe.stderr();
               break;
            case "?":
               pipeInput = Integer.toString(pipe.exitValue());
               break;
            default:
               throw new RuntimeException("Unknown channel");
         }

         if (ctx.channel() != null) {
            this.pipedValue = new TextValue(pipeInput);
         } else {
            this.pipedValue = pipe;
         }

         if (isRightExec) {
            // run external program with redirected input
            CompShParser.ExprExecuteContext execCtx = (CompShParser.ExprExecuteContext) ctx.expr(1);
            Value progVal = visit(execCtx.expr(0));
            if (!progVal.isText()) {
               throw new RuntimeException("Expected text as command.");
            }
            String prog = ((TextValue) progVal).value();

            List<String> args = new ArrayList<>();
            for (int i = 1; i < execCtx.expr().size(); i++) {
               Value argVal = visit(execCtx.expr(i));
               if (!argVal.isText()) {
                  throw new RuntimeException("Expected text as argument.");
               }
               args.add(((TextValue) argVal).value());
            }

            result = runProgramWithStdin(prog, args, pipeInput);

         } else {
            // right-hand side is an expression like 'stdout'
            this.pipedValue = new TextValue(pipeInput);
            result = visit(ctx.expr(1));
         }

      } else {
         // normal pipe into an expression
         this.pipedValue = left;
         result = visit(ctx.expr(1));
      }

      this.pipedValue = prevPipe;
      return result;
   }

   private Program runProgramWithStdin(String cmd, List<String> args, String input) {
      // helper function to run an external program with a provided stdin
      List<String> command = new ArrayList<>();
      command.add(cmd);
      command.addAll(args);
      String stdout = "";
      String stderr = "";
      int exitValue = -1;

      try {
         int c;
         ProcessBuilder pb = new ProcessBuilder(command);
         Process process = pb.start();

         // access stdin
         OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
         writer.write(input + "\n");
         writer.flush();
         writer.close();

         // save output
         BufferedReader brStdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
         BufferedReader brStderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
         while ((c = brStdout.read()) != -1) {
            stdout += (char) c;
         }
         while ((c = brStderr.read()) != -1) {
            stderr += (char) c;
         }
         exitValue = process.waitFor();

         return new Program(stdout, stderr, exitValue, new TextValue(cmd));
      } catch (IOException | InterruptedException e) {
         throw new RuntimeException("Failed to run external command with input: " + e.getMessage(), e);
      }
   }

   @Override
   public Value visitExprAndOr(CompShParser.ExprAndOrContext ctx) {
      // perform 'and' or 'or' operation in 2 bool types
      Value op1 = visit(ctx.expr(0));
      String op = ctx.getChild(1).getText();
      Value op2 = visit(ctx.expr(1));

      // check if operands are boolean type
      if (!(op1 instanceof BooleanValue) || !(op2 instanceof BooleanValue)) {
         throw new RuntimeException("Operands must be of type boolean");
      }

      BooleanValue op1Bool = (BooleanValue) op1;
      BooleanValue op2Bool = (BooleanValue) op2;

      Value result;
      switch (op) {
         case "and":
            result = op1Bool.and(op2Bool);
            break;
         case "or":
            result = op1Bool.or(op2Bool);
            break;
         default:
            throw new RuntimeException("Unknown boolean operator: " + op);
      }
      return result;
   }

   @Override
   public Value visitExprMulDiv(CompShParser.ExprMulDivContext ctx) {
      Value op1 = visit(ctx.expr(0));
      Value op2 = visit(ctx.expr(1));
      String op = ctx.op.getText();

      if (op.equals("*")) {
         return op1.multiply(op2);
      } else {
         return op1.divide(op2);
      }
   }

   @Override
   public Value visitExprBoolTrue(CompShParser.ExprBoolTrueContext ctx) {
      // returns a new BooleanValue with value = true
      return new BooleanValue(true);
   }

   @Override
   public Value visitExprPipeAssignement(CompShParser.ExprPipeAssignementContext ctx) {
      String id = ctx.ID().getText();
      Value valueToStore;

      if (this.pipedValue == null) {
         throw new RuntimeException("Don't know where to store " + id);
      }
      if (ctx.TYPE() == null) {
         // store in without declaration of new variable
         if (this.pipedValue instanceof Program) {
            Program prog = (Program) this.pipedValue;
            valueToStore = prog;
         } else {
            valueToStore = this.pipedValue;
         }
      } else {
         // store in with declaration of a new variable
         if (this.pipedValue instanceof Program) {
            Program prog = (Program) this.pipedValue;
            String typeStr = ctx.TYPE().getText();
            Type type = Type.fromString(typeStr);
            valueToStore = prog.convertTo(type);
         } else {
            String typeStr = ctx.TYPE().getText();
            Type type = Type.fromString(typeStr);
            valueToStore = this.pipedValue.convertTo(type);
         }
      }
      memory.put(id, valueToStore);
      return valueToStore;
   }

   @Override
   public Value visitExprStdout(CompShParser.ExprStdoutContext ctx) {
      // print to stdout
      if (this.pipedValue == null) {
         throw new RuntimeException("Don't know what to print!");
      }

      if (this.pipedValue instanceof Program && ((Program) this.pipedValue).stdout() != null) {
         // if program --print stdout
         System.out.println(((Program) this.pipedValue).stdout());
      } else {
         // if not program just print its normal value
         System.out.println(this.pipedValue.value());
      }
      return null;
   }

   @Override
   public Value visitExprNL(CompShParser.ExprNLContext ctx) {
      // create a new text value with /n
      return new TextValue("\n");
   }

   @Override
   public Value visitExprId(CompShParser.ExprIdContext ctx) {
      // return the id's value in memory map
      String id = ctx.getText();

      if (!memory.containsKey(id)) {
         throw new RuntimeException("Undefined variable: " + id);
      }

      return memory.get(id);
   }

   @Override
   public Value visitExprPrefix(CompShParser.ExprPrefixContext ctx) {
      // inject a prefix into a TextValue
      Value input = visit(ctx.expr());

      // make sure input is a text value
      if (!(input instanceof TextValue)) {
         throw new RuntimeException("Prefix can only be applied to Text");
      }

      String prefix = input.value().toString();

      if (this.pipedValue == null) {
         throw new RuntimeException("No piped value to apply a prefix to");
      }

      String[] lines = this.pipedValue.value().toString().split("\n");
      StringBuilder result = new StringBuilder();
      for (String line : lines) {
         result.append(prefix).append(line).append("\n");
      }

      return new TextValue(result.toString().stripTrailing());

   }

   @Override
   public Value visitExprParens(CompShParser.ExprParensContext ctx) {
      return visit(ctx.expr());
   }

   @Override
   public Value visitExprInt(CompShParser.ExprIntContext ctx) {
      // return a new integer type
      return new IntegerValue(Integer.parseInt(ctx.getText()));
   }

   @Override
   public Value visitExprSuffix(CompShParser.ExprSuffixContext ctx) {
      // inject a sufix into a TextValue

      Value input = visit(ctx.expr());

      // make sure input is a text value
      if (!(input instanceof TextValue)) {
         throw new RuntimeException("Prefix can only be applied to Text");
      }

      String sufix = input.value().toString();

      if (this.pipedValue == null) {
         throw new RuntimeException("No piped value to apply a prefix to");
      }

      String[] lines = this.pipedValue.value().toString().split("\n");
      StringBuilder result = new StringBuilder();
      for (String line : lines) {
         result.append(line).append(sufix).append("\n");
      }

      return new TextValue(result.toString().stripTrailing());

   }

   @Override
   public Value visitExprRelational(CompShParser.ExprRelationalContext ctx) {
      // apply relational operations between 2 values
      Value left = visit(ctx.expr(0));
      Value right = visit(ctx.expr(1));

      String op = ctx.op.getText();

      String leftValue = left.value().toString();
      String rightValue = right.value().toString();

      boolean isNumeric = left.value() instanceof Number && right.value() instanceof Number;
      boolean result;

      if (isNumeric) {
         double leftNum = ((Number) left.value()).doubleValue();
         double rightNum = ((Number) right.value()).doubleValue();
         switch (op) {
            case ">":
               result = leftNum > rightNum;
               break;
            case "<":
               result = leftNum < rightNum;
               break;
            case ">=":
               result = leftNum >= rightNum;
               break;
            case "<=":
               result = leftNum <= rightNum;
               break;
            case "=":
               result = leftNum == rightNum;
               break;
            case "/=":
               result = leftNum != rightNum;
               break;
            default:
               throw new RuntimeException("Unknown operator: " + op);
         }
      } else {
         // Compare strings
         switch (op) {
            case "=":
               result = leftValue.equals(rightValue);
               break;
            case "/=":
               result = !leftValue.equals(rightValue);
               break;
            case ">":
               result = leftValue.compareTo(rightValue) > 0;
               break;
            case "<":
               result = leftValue.compareTo(rightValue) < 0;
               break;
            case ">=":
               result = leftValue.compareTo(rightValue) >= 0;
               break;
            case "<=":
               result = leftValue.compareTo(rightValue) <= 0;
               break;
            default:
               throw new RuntimeException("Unknown operator: " + op);
         }
      }

      return new BooleanValue(result);
   }

   @Override
   public Value visitExprStr(CompShParser.ExprStrContext ctx) {
      // return a new text type
      String original = ctx.getText(); // contains ""
      return new TextValue(original.substring(1, original.length() - 1));
   }

   @Override
   public Value visitExprListLiteral(CompShParser.ExprListLiteralContext ctx) {
      // create a new ListValue
      List<Value> values = new ArrayList<>();

      // add first expr
      values.add(visit(ctx.expr(0)));

      // add other exprs separated by commas
      for (int i = 1; i < ctx.expr().size(); i++) {
         values.add(visit(ctx.expr(i)));
      }

      return new ListValue(values);

   }

   // TODO REFACTOR THIS
   @Override
   public Value visitExprRedirection(CompShParser.ExprRedirectionContext ctx) {
      // redirect one (src) channel to another (dst)

      Value left = visit(ctx.expr());

      // make sure left side is a program variable
      Program prog;
      if (left instanceof Program) {
         prog = (Program) left;
      } else {
         String output = left.value().toString();
         prog = new Program(output, "", 0, left);
      }

      String src = ctx.channel(0).getText();
      String dst = ctx.channel(1).getText();

      String stdout = prog.stdout();
      String stderr = prog.stderr();
      int exit = prog.exitValue();

      // fetch channel contents
      String srcVal;
      switch (src) {
         case "|":
            srcVal = stdout;
            break;
         case "&":
            srcVal = stderr;
            break;
         case "?":
            srcVal = Integer.toString(exit);
            break;
         case "$":
            srcVal = prog.value().toString();
            break;
         case "*":
            srcVal = stdout + "\n" + stderr + "\n" + exit;
            break;
         default:
            throw new RuntimeException("Invalid source channel: " + src);
      }

      if (dst.equals("*")) {
         // copy srcVal to all channels
         stdout = srcVal;
         stderr = srcVal;
         exit = Integer.parseInt(srcVal);
      } else if (src.equals("*")) {
         // merge all channels into one destination
         String all = stdout + "\n" + stderr + "\n" + exit;
         switch (dst) {
            case "|":
               stdout = all;
               break;
            case "&":
               stderr = all;
               break;
            case "?":
               exit = Integer.parseInt(all);
               break;
            case "-":
               stdout = "";
               stderr = "";
               exit = 0;
               break;
            default:
               throw new RuntimeException("Invalid destination channel: " + dst);
         }
      } else {
         // simple redirection: srcVal -> dst
         switch (dst) {
            case "|":
               stdout = srcVal;
               break;
            case "&":
               stderr = srcVal;
               break;
            case "?":
               exit = Integer.parseInt(srcVal);
               break;
            case "-":
               // remove src channel only
               switch (src) {
                  case "|":
                     stdout = "";
                     break;
                  case "&":
                     stderr = "";
                     break;
                  case "?":
                     exit = 0;
                     break;
                  default:
                     throw new RuntimeException("Cannot clear channel: " + src);
               }
               break;
            default:
               throw new RuntimeException("Invalid destination channel: " + dst);
         }
      }

      return new Program(stdout.strip(), stderr.strip(), exit, prog);
   }

   @Override
   public Value visitExprUnary(CompShParser.ExprUnaryContext ctx) {
      String sign = ctx.sign.getText();
      Value val = visit(ctx.expr());
      return val.unary(sign);
   }

   @Override
   public Value visitExprExecute(CompShParser.ExprExecuteContext ctx) {
      Value exprVal = visit(ctx.expr(0));

      // if it is a program go fetch the actual value
      if (exprVal.isProgram()) {
         Value programValue = (Value) exprVal.value();
         exprVal = (Value) programValue.value();
      }

      if (!exprVal.isText()) {
         throw new RuntimeException("Expected a text expression to execute as a command.");
      }

      String commandText = ((TextValue) exprVal).value();
      List<String> commandParts = new ArrayList<>();
      commandParts.add(commandText);

      // check if we have list of arguments
      if (ctx.expr().size() > 1) {
         for (int i = 1; i < ctx.expr().size(); i++) {
            Value argVal = visit(ctx.expr(i));

            if (!argVal.isText()) {
               throw new RuntimeException("All command arguments must be text values.");
            }

            commandParts.add(((TextValue) argVal).value());
         }
      }

      try {
         ProcessBuilder pb = new ProcessBuilder(commandParts);
         Process process = pb.start();

         String stdout = new String(process.getInputStream().readAllBytes());
         String stderr = new String(process.getErrorStream().readAllBytes());
         int exitCode = process.waitFor();

         return new Program(stdout, stderr, exitCode, exprVal);
      } catch (IOException | InterruptedException e) {
         throw new RuntimeException("Failed to execute command: " + e.getMessage(), e);
      }
   }

   @Override
   public Value visitExprStdin(CompShParser.ExprStdinContext ctx) {
      // handle user input

      // if prompt --print to screen
      if (ctx.prompt != null) {
         System.out.print(ctx.prompt.getText().substring(1, ctx.prompt.getText().length() - 1));
      }

      // Read input from stdin
      Scanner sc = new Scanner(System.in);
      String line = sc.nextLine();
      return new TextValue(line);
   }

   @Override
   public Value visitExprFilter(CompShParser.ExprFilterContext ctx) {
      // apply grep-like filter to piped value
      Value patternValue = visit(ctx.expr());
      if (!(patternValue instanceof TextValue)) {
         throw new RuntimeException("Grep filter can only be applied to Text");
      }

      String pattern = patternValue.value().toString();

      if (this.pipedValue == null) {
         throw new RuntimeException("No piped value to apply grep filter");
      }

      String[] lines = this.pipedValue.value().toString().split("\n");
      StringBuilder result = new StringBuilder();
      for (String line : lines) {
         if (line.contains(pattern)) {
            result.append(line).append("\n");
         }
      }

      return new TextValue(result.toString().stripTrailing());
   }

   @Override
   public Value visitExprTypeCast(CompShParser.ExprTypeCastContext ctx) {
      String typeStr = ctx.TYPE().getText();
      Type targetType = Type.fromString(typeStr);
      Value val = visit(ctx.expr());
      return val.convertTo(targetType);
   }

   @Override
   public Value visitExprPlusMinus(CompShParser.ExprPlusMinusContext ctx) {
      Value op1 = visit(ctx.expr(0));
      Value op2 = visit(ctx.expr(1));
      String op = ctx.op.getText();

      if (op.equals("+")) {
         return op1.add(op2);
      } else {
         return op1.subtract(op2);
      }
   }

   @Override
   public Value visitDollarChannel(CompShParser.DollarChannelContext ctx) {
      Value res = null;
      return visitChildren(ctx);
      // return res;
   }

   @Override
   public Value visitStdoutChannel(CompShParser.StdoutChannelContext ctx) {
      Value res = null;
      return visitChildren(ctx);
      // return res;
   }

   @Override
   public Value visitStderrChannel(CompShParser.StderrChannelContext ctx) {
      Value res = null;
      return visitChildren(ctx);
      // return res;
   }

   @Override
   public Value visitExitChannel(CompShParser.ExitChannelContext ctx) {
      Value res = null;
      return visitChildren(ctx);
      // return res;
   }

   @Override
   public Value visitAllChannels(CompShParser.AllChannelsContext ctx) {
      Value res = null;
      return visitChildren(ctx);
      // return res;
   }

   @Override
   public Value visitNoChannel(CompShParser.NoChannelContext ctx) {
      Value res = null;
      return visitChildren(ctx);
      // return res;
   }
}
