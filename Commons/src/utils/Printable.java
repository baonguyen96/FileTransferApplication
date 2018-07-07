package utils;

/***
 * utils.Printable interface
 *
 * When it is set to true:
 *      Display every messages sent between client and server
 *      Including all behind-the-scene commands
 * When it is set to false:
 *      Does not display the behind-the-scene commands
 *      Only display client's command and server's response that the user needs to see
 */

public interface Printable {
    boolean IS_PRINTABLE = false;
}
