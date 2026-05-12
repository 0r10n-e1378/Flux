package ai;

import math.Vector;

public class Behavior {

    public static void scale(Vector vector, double weight) {
        if (vector != null) {
            vector.multiply(weight);
        }
    }
}
