package test;

import com.intellij.debugger.DebuggerManager;

/**
 * Created by rishajai on 10/12/16.
 */
public class MainRunner {

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main (String[] args) throws InterruptedException {
        int a = 0;
        Thread t = new Thread() {
            @Override
            public void run() {
                Thread t1 = new Thread() {
                    @Override
                    public void run() {
                        int b = 0;
                        while (true) {
                            System.out.println(b);
                            MainRunner.sleep(1000);
                            b++;
                            MainRunner.sleep(2000);
                        }
                    }
                };
                t1.start();
                int a = 0;
                while (true) {
                    a++;
                    System.out.println(a);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t.start();
        t.join();
        System.out.println("DONE");
    }
}
