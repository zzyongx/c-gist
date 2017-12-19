package commons.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

public class ProcessHelper {
  private static final Logger logger = LoggerFactory.getLogger(ProcessHelper.class);

  private static String[] initCmdArray(String command, String ... args) {
    String cmdarray[] = new String[1 + args.length];
    cmdarray[0] = command;
    for (int i = 0; i < args.length; ++i) cmdarray[i+1] = args[i];
    return cmdarray;
  }

  public static void logError(String command, int code, Process p) throws IOException {
    if (code == 0) {
      logger.info("exec {} exit = {}", command, code);
    } else {
      logger.error("exec {} exit = {} STDERR {}", command, code,
                   StreamUtils.copyToString(p.getErrorStream(), StandardCharsets.UTF_8));
    }
  }

  // WARNING: IF stderr too large (LINUX 65536), exec may block

  public static <T> List<T> execBlind(Function<String, T> f, String file, String ... args) {
    return exec(true, f, file, args);
  }

  public static <T> List<T> exec(Function<String, T> f, String file, String ... args) {
    return exec(false, f, file, args);
  }

  private static <T> List<T> exec(boolean ignoreExitValue, Function<String, T> f, String file, String ... args) {
    String cmdarray[] = initCmdArray(file, args);
    String command = String.join(" ", cmdarray);

    int code = Integer.MIN_VALUE;
    try {
      Runtime r = Runtime.getRuntime();
      Process p = r.exec(cmdarray);

      List<T> objs = new ArrayList<>();
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String textLine;
      while ((textLine = reader.readLine()) != null) {
        objs.add(f.apply(textLine));
      }

      p.waitFor();
      code = p.exitValue();

      logError(command, code, p);

      if (code == 0 || ignoreExitValue) {
        return code != 0 && objs.isEmpty() ? null : objs;
      }
    } catch (Exception e) {
      logger.error("exec {} exit == {} EXCEPTION {}", command, code, e);
    }

    return null;
  }

  public static String execBlind(String file, String ... args) {
    return exec(true, file, args);
  }

  public static String exec(String file, String ... args) {
    return exec(false, file, args);
  }

  private static String exec(boolean ignoreExitValue, String file, String ... args) {
    String cmdarray[] = initCmdArray(file, args);
    String command = String.join(" ", cmdarray);

    int code = Integer.MIN_VALUE;
    try {
      Runtime r = Runtime.getRuntime();
      Process p = r.exec(cmdarray);
      String output = StreamUtils.copyToString(p.getInputStream(), StandardCharsets.UTF_8);

      p.waitFor();
      code = p.exitValue();
      logError(command, code, p);

      if (code == 0 || ignoreExitValue) {
        return code != 0 && output.isEmpty() ? null : output;
      }
    } catch (Exception e) {
      logger.error("exec {} exit = {} EXCEPTION {}", command, code, e);
    }
    return null;
  }
}
