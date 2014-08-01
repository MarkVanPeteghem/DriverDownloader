package driverdownloader;

/**
 *
 * @author Mark Van Peteghem
 */
public class Interrupt {
    private static boolean isInterrupted = false;

    synchronized public static boolean get() {
        return isInterrupted;
    }

    synchronized public static void set(boolean b) {
        isInterrupted = b;
    }
}
