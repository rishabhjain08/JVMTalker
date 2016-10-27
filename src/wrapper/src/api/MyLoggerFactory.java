package api;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Created by rishajai on 10/17/16.
 */
class MyLoggerFactory implements Logger.Factory {

    class MyLogger extends Logger {

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void debug(@NonNls String s) {
            if (isDebugEnabled()) {
                System.out.print("DEBUG: ");
                System.out.println(s);
            }
        }

        @Override
        public void debug(@Nullable Throwable throwable) {
            if (isDebugEnabled()) {
                throwable.printStackTrace();
            }
        }

        @Override
        public void debug(@NonNls String s, @Nullable Throwable throwable) {
            if (isDebugEnabled()) {
                System.out.print("DEBUG: ");
                System.out.println(s);
                throwable.printStackTrace();
            }
        }

        @Override
        public void info(@NonNls String s) {
//            (new Exception()).printStackTrace();
            System.out.print("INFO: ");
            System.out.println(s);
        }

        @Override
        public void info(@NonNls String s, @Nullable Throwable throwable) {
  //          (new Exception()).printStackTrace();
            System.out.print("INFO: ");
            System.out.println(s);
            throwable.printStackTrace();
        }

        @Override
        public void warn(@NonNls String s, @Nullable Throwable throwable) {
            System.out.print("WARN: ");
            System.out.println(s);
            throwable.printStackTrace();
        }

        @Override
        public void error(@NonNls String s, @Nullable Throwable throwable, @NonNls @NotNull String... strings) {
            System.out.print("ERROR: ");
            System.out.println(s);
            throwable.printStackTrace();
            strings = strings == null ? new String[0] : strings;
            System.out.println(Arrays.toString(strings));
        }

        @Override
        public void setLevel(Level level) {

        }
    }

    @Override
    public Logger getLoggerInstance(String s) {
        return new MyLogger();
    }
}
