/*
 * Behavior.java
 *
 * Simple helper for weighting steering vectors.
 * This is a tiny utility used to scale behavior contributions.
 */
package ai;

import math.Vector;

public class Behavior {

    public static void scale(Vector vector, double weight) {
        if (vector != null) {
            vector.multiply(weight);
        }
    }
}
