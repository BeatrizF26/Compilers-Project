import static java.lang.System.*;
import java.util.*;
import java.io.*;

public class Pipe {

   public static void main(String[] args) {
      if (args.length == 0) {
         err.println("Usage: java Pipe <prog> [<arg> ...]");
         exit(1);
      }
      String stdin = "";
      String stdout = "";
      String stderr = "";
      int exitValue = -1;
      try {
         int c;
         List<String> progAndArgs = Arrays.asList(args);
         Process process = new ProcessBuilder(progAndArgs).start();
         OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
         BufferedReader brStdin = new BufferedReader(new InputStreamReader(System.in));
         while ((c = brStdin.read()) != -1) {
            stdin += (char)c;
         }
         writer.write(stdin); // redirect a string to process stdin!
         //writer.flush();
         writer.close();
         BufferedReader brStdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
         BufferedReader brStderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
         while ((c = brStdout.read()) != -1) {
            stdout += (char)c;
         }
         while ((c = brStderr.read()) != -1) {
            stderr += (char)c;
         }
         exitValue = process.waitFor();
      }
      catch(IOException e) {
      }
      catch(InterruptedException e) {
      }
      System.out.println("stdout:");
      System.out.println(stdout);
      System.out.println("stderr:");
      System.out.println(stderr);
      System.out.println("exit: "+exitValue);
   }
}
