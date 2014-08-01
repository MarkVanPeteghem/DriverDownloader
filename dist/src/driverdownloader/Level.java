package driverdownloader;

/**
 *
 * @author Mark Van Peteghem
 */
public class Level {
    private static int level = 0;

    static public void Enter() {
        ++level;
    }

    static public void Leave() {
        --level;
    }

    static public int Get() {
        return level;
    }
}
