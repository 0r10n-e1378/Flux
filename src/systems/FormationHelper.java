package systems;

import core.Formation;
import math.Vector;

public class FormationHelper {

    public static Vector getBossFormationPosition(int boidIndex, int totalBoids, Vector leaderPos, double leaderAngle, Formation formation) {
        if (formation == Formation.NORMAL) {
            return null; // Boss formation should never be NORMAL, but guard against it.
        }

        if (formation == Formation.HEX_SHIELD) {
            return getBossHexShieldPosition(boidIndex, totalBoids, leaderPos, leaderAngle);
        } else if (formation == Formation.ARROWHEAD) {
            return getBossArrowheadPosition(boidIndex, totalBoids, leaderPos, leaderAngle);
        } else if (formation == Formation.PHALANX) {
            return getBossPhalanxPosition(boidIndex, totalBoids, leaderPos, leaderAngle);
        }

        return null;
    }

    public static Vector getFormationPosition(int boidIndex, int totalBoids, Vector leaderPos, double leaderAngle, Formation formation) {
        if (formation == Formation.NORMAL) {
            return null; // Normal formation doesn't use target positions
        }

        if (formation == Formation.HEX_SHIELD) {
            return getHexShieldPosition(boidIndex, totalBoids, leaderPos, leaderAngle);
        } else if (formation == Formation.ARROWHEAD) {
            return getArrowheadPosition(boidIndex, totalBoids, leaderPos, leaderAngle);
        } else if (formation == Formation.PHALANX) {
            return getPhalanxPosition(boidIndex, totalBoids, leaderPos, leaderAngle);
        }

        return null;
    }

    private static Vector getHexShieldPosition(int boidIndex, int totalBoids, Vector leaderPos, double leaderAngle) {
        // Create a true hexagonal formation with offset rows
        double spacing = 50;

        // Determine row and column in hexagonal grid
        int row = 0;
        int boidCountBefore = 0;

        // Find which row this boid belongs to
        while (boidCountBefore + (row + 1) * 6 <= boidIndex) {
            boidCountBefore += (row + 1) * 6;
            row++;
        }

        int posInRow = boidIndex - boidCountBefore;
        int boidsInThisRow = (row + 1) * 6;

        // Calculate angle around the hexagon
        double angleStep = Math.PI * 2 / boidsInThisRow;
        double angle = leaderAngle + (angleStep * posInRow);

        // Distance from leader increases with row
        double distance = 70 + (row * 40);

        double x = leaderPos.x + Math.cos(angle) * distance;
        double y = leaderPos.y + Math.sin(angle) * distance;

        return new Vector(x, y);
    }

    private static Vector getArrowheadPosition(int boidIndex, int totalBoids, Vector leaderPos, double leaderAngle) {
        // Create an arrowhead formation where leader is tucked in middle-back
        // Tip extends far ahead, boids branch back in steep lines

        int row = 0;
        int boidCountBefore = 0;

        // Find which row this boid belongs to
        while (boidCountBefore + row + 1 <= boidIndex) {
            boidCountBefore += row + 1;
            row++;
        }

        int posInRow = boidIndex - boidCountBefore;
        int boidsInThisRow = row + 1;

        // Calculate total rows to determine leader's position
        int totalRows = 0;
        int tempCount = 0;
        while (tempCount < totalBoids) {
            tempCount += totalRows + 1;
            totalRows++;
        }
        totalRows = Math.max(1, totalRows - 1);

        // Position leader in middle-back (around 70% from tip)
        int leaderRow = Math.max(0, (int)(totalRows * 0.7));

        // Calculate depths: tip is furthest forward, rows get closer to leader
        double tipDepth = 180; // Tip 180 units ahead
        double rowSpacing = 50; // Spacing between rows
        double boidDepth = tipDepth - (row * rowSpacing);
        double leaderDepth = tipDepth - (leaderRow * rowSpacing);

        // Calculate lateral offset for this boid
        double lateralSpacing = 45;
        double lateralOffset = (posInRow - (boidsInThisRow - 1) / 2.0) * lateralSpacing;

        // Calculate position relative to leader
        double x = leaderPos.x + Math.cos(leaderAngle) * (boidDepth - leaderDepth);
        double y = leaderPos.y + Math.sin(leaderAngle) * (boidDepth - leaderDepth);

        double perpAngle = leaderAngle + Math.PI / 2;
        x += Math.cos(perpAngle) * lateralOffset;
        y += Math.sin(perpAngle) * lateralOffset;

        return new Vector(x, y);
    }

    private static Vector getBossArrowheadPosition(int boidIndex, int totalBoids, Vector leaderPos, double leaderAngle) {
        int row = 0;
        int boidCountBefore = 0;

        while (boidCountBefore + row + 1 <= boidIndex) {
            boidCountBefore += row + 1;
            row++;
        }

        int posInRow = boidIndex - boidCountBefore;
        int boidsInThisRow = row + 1;

        int totalRows = 0;
        int tempCount = 0;
        while (tempCount < totalBoids) {
            tempCount += totalRows + 1;
            totalRows++;
        }
        totalRows = Math.max(1, totalRows - 1);

        int leaderRow = Math.max(0, (int)(totalRows * 0.65));

        double tipDepth = 220;
        double rowSpacing = 60;
        double boidDepth = tipDepth - (row * rowSpacing);
        double leaderDepth = tipDepth - (leaderRow * rowSpacing);

        double lateralSpacing = 55;
        double lateralOffset = (posInRow - (boidsInThisRow - 1) / 2.0) * lateralSpacing;

        double x = leaderPos.x + Math.cos(leaderAngle) * (boidDepth - leaderDepth + 40);
        double y = leaderPos.y + Math.sin(leaderAngle) * (boidDepth - leaderDepth + 40);

        double perpAngle = leaderAngle + Math.PI / 2;
        x += Math.cos(perpAngle) * lateralOffset;
        y += Math.sin(perpAngle) * lateralOffset;

        return new Vector(x, y);
    }

    private static Vector getBossPhalanxPosition(int boidIndex, int totalBoids, Vector leaderPos, double leaderAngle) {
        int cols = (int) Math.ceil(Math.sqrt(totalBoids));
        int rows = (int) Math.ceil((double) totalBoids / cols);

        int row = boidIndex / cols;
        int col = boidIndex % cols;

        double spacing = 45;
        double centerRow = (rows - 1) / 2.0;
        double centerCol = (cols - 1) / 2.0;

        double forwardOffset = (row - centerRow) * spacing + 50;
        double lateralOffset = (col - centerCol) * spacing;

        double x = leaderPos.x + Math.cos(leaderAngle) * forwardOffset - Math.sin(leaderAngle) * lateralOffset;
        double y = leaderPos.y + Math.sin(leaderAngle) * forwardOffset + Math.cos(leaderAngle) * lateralOffset;

        return new Vector(x, y);
    }

    private static Vector getBossHexShieldPosition(int boidIndex, int totalBoids, Vector leaderPos, double leaderAngle) {
        double spacing = 65;

        int row = 0;
        int boidCountBefore = 0;
        while (boidCountBefore + (row + 1) * 6 <= boidIndex) {
            boidCountBefore += (row + 1) * 6;
            row++;
        }

        int posInRow = boidIndex - boidCountBefore;
        int boidsInThisRow = (row + 1) * 6;

        double angleStep = Math.PI * 2 / boidsInThisRow;
        double angle = leaderAngle + (angleStep * posInRow);

        double distance = 90 + (row * 55);

        double x = leaderPos.x + Math.cos(angle) * distance;
        double y = leaderPos.y + Math.sin(angle) * distance;

        return new Vector(x, y);
    }

    private static Vector getPhalanxPosition(int boidIndex, int totalBoids, Vector leaderPos, double leaderAngle) {
        // Create a tight rectangular phalanx formation around the leader
        // Dense grid that rotates to face the leader's heading

        // Calculate optimal grid dimensions (try to make it as square as possible)
        int cols = (int) Math.ceil(Math.sqrt(totalBoids));
        int rows = (int) Math.ceil((double) totalBoids / cols);

        // Calculate which row and column this boid belongs to
        int row = boidIndex / cols;
        int col = boidIndex % cols;

        // Tight spacing for phalanx formation
        double spacing = 35; // Close formation

        // Center the formation around leader
        double centerRow = (rows - 1) / 2.0;
        double centerCol = (cols - 1) / 2.0;

        // Local formation offsets: forward/back and left/right
        double forwardOffset = (row - centerRow) * spacing;
        double lateralOffset = (col - centerCol) * spacing;

        // Rotate the grid so it faces leader movement direction
        double x = leaderPos.x + Math.cos(leaderAngle) * forwardOffset - Math.sin(leaderAngle) * lateralOffset;
        double y = leaderPos.y + Math.sin(leaderAngle) * forwardOffset + Math.cos(leaderAngle) * lateralOffset;

        return new Vector(x, y);
    }
}