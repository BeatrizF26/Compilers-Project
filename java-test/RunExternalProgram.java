import static java.lang.System.*;
import java.util.*;
import java.io.*;

public class RunExternalProgram {

   public static final String RED="\033[0;31m";
   public static final String GREEN="\033[0;32m";
   public static final String YELLOW="\033[0;33m";
   public static final String BLUE="\033[0;34m";
   public static final String BOLD="\033[1;38m";
   public static final String RESET="\033[0m";

   public static void main(String[] args) {
      if (args.length == 0) {
         err.println("Usage: java RunExternalProgram <prog> [<arg> ...]");
         exit(1);
      }
      String stdout = "";
      String stderr = "";
      int exitValue = -1;
      try {
         List<String> progAndArgs = Arrays.asList(args);
         Process process = new ProcessBuilder(progAndArgs).start();
         BufferedReader brStdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
         BufferedReader brStderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
         int c;
         while ((c = brStdout.read()) != -1) {
            stdout += (char)c;
         }
         while ((c = brStderr.read()) != -1) {
            stderr += (char)c;
         }
         exitValue = process.waitFor();
      }
      catch(IOException e) {
         err.println("IOException!");
         exit(1);
      }
      catch(InterruptedException e) {
         err.println("InterruptedException!");
         exit(1);
      }
      System.out.println("stdout:");
      System.out.println(stdout);
      System.out.println("stderr:");
      System.out.println(RED+stderr+RESET);
      System.out.println("exit: "+BLUE+exitValue+RESET);
   }
}

