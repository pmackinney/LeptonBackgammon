package net.mackinney.lepton;

/**
 * An ordered pair defining the move of one checker
 */
class SingleMove {
    private int source;
    private int dest;

    SingleMove(int s, int d) {
        source = s;
        dest = d;
    }

    int getSource() {
        return source;
    }

    int getDest() {
        return dest;
    }
}
