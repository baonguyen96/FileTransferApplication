package utils;

/***
 * utils.Resynchronizable interface
 *
 * Implemented by FakeServer and FakeClient
 *
 * When it is set to true:
 *      Make only 1 message out-of-sync (wrong message sequence)
 *      Then the rest will get back in sync
 * When it is set to false:
 *      Make every message out-of-sync,
 *      Thus break the communication due to "intrusion"
 */

public interface Resynchronizable {
    boolean IS_RESYNCHRONIZABLE = true;
}
