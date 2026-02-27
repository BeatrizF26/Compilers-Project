import static java.lang.System.*;
import java.util.*;
import java.io.*;

public class RunExternalProgram {

   public static Program exec(String command) {
      if (command == null || command.isEmpty()) {
          System.err.println("No command provided");
          System.exit(1);
      }

      String stdout = "";
      String stderr = "";
      int exitValue = -1;

      try {
          List<String> progAndArgs = Arrays.asList(command.split("\\s+"));
          Process process = new ProcessBuilder(progAndArgs).start();

          BufferedReader brStdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
          BufferedReader brStderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

          int c;
          while ((c = brStdout.read()) != -1) {
              stdout += (char) c;
          }
          while ((c = brStderr.read()) != -1) {
              stderr += (char) c;
          }

          exitValue = process.waitFor();
          return new Program(stdout, stderr, exitValue);

      } catch (IOException e) {
          System.err.println("IOException!");
          System.exit(1);
      } catch (InterruptedException e) {
          System.err.println("InterruptedException!");
          System.exit(1);
      }

      return null;
  }
}