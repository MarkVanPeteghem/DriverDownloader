package driverdownloader;

/**
 *
 * @author Mark Van Peteghem
 */
public class GlobalOptions {
    private GlobalOptions() {}

    private static boolean dryRun = false;

    synchronized public static boolean isDryRun() {
        return dryRun;
    }

    synchronized public static void setDryRun(boolean dryRun) {
        GlobalOptions.dryRun = dryRun;
    }

}
