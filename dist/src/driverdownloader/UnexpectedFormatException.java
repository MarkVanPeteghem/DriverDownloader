package driverdownloader;

/**
 *
 * @author Mark Van Peteghem
 */
public class UnexpectedFormatException extends Exception {
    UnexpectedFormatException() {
        super("HTML in page has other format than expected.\nContact developer for adjustments.");
    }
}
