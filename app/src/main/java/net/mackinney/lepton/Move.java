package net.mackinney.lepton;

import java.util.ArrayList;
import java.util.List;

// TODO on error (e.g., server says 'You can't ... ') reset move
// TODO if man on bar, active die must be legal to bear on

/**
 * Stages the player's move.
 */
class Move {
    private static final int NONE = -1;
    private List<SingleMove> moves;
    private int playerBar;
    private int oppBar;
    private List<Integer> dice;
    private int activeDie;

    Move(Board board) {
        dice = new ArrayList<>(4); // Integer
        moves = new ArrayList<>(); // SingleMove
        activeDie = NONE;
        playerBar = board.getState(Board.BAR);
        oppBar = (playerBar == 0) ? 25 : 0;
        int moveCount = board.getState(Board.CAN_MOVE);
        if (moveCount > 0) {
            activeDie = board.getState(Board.DICE_PLAYER1);                 // assume die1 has moves
            if (board.getState(Board.DICE_PLAYER2) == activeDie) {          // test for doubles
                for (int ix = 0; ix < board.getState(Board.CAN_MOVE); ix++) {
                    dice.add(activeDie);
                }
            } else if (board.hasMovesByDie(activeDie)) {            // confirm die1 has moves
                dice.add(activeDie);
                if (moveCount > 1) {                                // add 2nd move if available
                    dice.add(board.getState(Board.DICE_PLAYER2));
                }
            } else {
                activeDie = board.getState(Board.DICE_PLAYER2);     // die1 can't move first
                dice.add(activeDie);
                if (moveCount > 1) {                                // add 2nd move if available
                    dice.add(board.getState(Board.DICE_PLAYER1));
                }
            }
        }
    }

    int getActiveDie() {
        return activeDie;
    }

    /**
     *
     * @param d - the die to make active, if possible
     */
    void resetActiveDie(Board b, int d) {
        if (b.hasMovesByDie(d)) {
            activeDie = d;
        }
    }

    private void init(Board b) {
        moves.clear();
        dice.clear();
        activeDie = NONE;
        playerBar = b.getState(Board.BAR);
        oppBar = (playerBar == 0) ? 25 : 0;
        int moveCount = b.getState(Board.CAN_MOVE);
        if (moveCount > 0) {
            activeDie = b.getState(Board.DICE_PLAYER1);                 // assume die1 has moves
            if (b.getState(Board.DICE_PLAYER2) == activeDie) {          // test for doubles
                for (int ix = 0; ix < b.getState(Board.CAN_MOVE); ix++) {
                    dice.add(activeDie);
                }
            } else if (b.hasMovesByDie(activeDie)) {                    // confirm die1 has moves
                    dice.add(activeDie);
                if (moveCount > 1) {                                    // add 2nd move if available
                    dice.add(b.getState(Board.DICE_PLAYER2));
                }
            } else {
                activeDie = b.getState(Board.DICE_PLAYER2);             // die2 only has moves
                dice.add(activeDie);
            }
        }
    }

    // Forced moves

    /**
     * Processes the checker tapped
     *
     * @param p the point to become a source or destination
     * @return true if the View should be updated, false otherwise
     */
    boolean process(int p, Board b) {
        if (isReadyToSend(b) || !b.isPlayerPoint(p)) {
            return false;
        }
        boolean result = false;
        int target = p + b.getState(Board.DIRECTION) * activeDie; // TODO create Board.getState(int parameter)
        if (b.playerMayBearOff()
                && (b.getState(Board.DIRECTION) == 1 && target >= b.getState(Board.HOME)
                || b.getState(Board.DIRECTION) == -1 && target <= b.getState(Board.HOME))) {
            target = Board.HOME;
            result = true;
        } else if (target > 0 && b.pointIsAvailable(target)) {
            result = true;
        }
        if (result) {
            b.getBoardPoints()[p] -= b.getState(Board.COLOR); // remove checker
            // addChecker(target);
            if (target == Board.HOME) {
                b.setState(Board.ON_HOME_PLAYER, b.getState(Board.ON_HOME_PLAYER) + 1);
            } else {
                int[] bp = b.getBoardPoints();
                if (bp[target] * b.getState(Board.COLOR) == -1) { // if the target has an opponent's checker
                    bp[target] = 0;                        //    clear it
                    bp[oppBar] -= b.getState(Board.COLOR);        //    place on opponent's bar
                }
                bp[target] += b.getState(Board.COLOR); // place on target
            }
            moves.add(new SingleMove(p, target)); // save the move
            consumeDie(activeDie);
        }
        return result;
    }

    private void consumeDie(int d) {
        if (dice.contains(d)) {
            dice.remove(dice.indexOf(d)); // dice.remove(d) would treat d as an index, not an element.
        }
        if (dice.size() > 0) {
            activeDie = dice.get(0);
        } else {
            activeDie = NONE;
        }
    }

    boolean isStarted(Board b) {
        return b.isPlayerTurn() && b.getState(Board.DICE_PLAYER1) > 0 // Board.CAN_MOVE is valid
                && moves.size() > 0 && b.getState(Board.CAN_MOVE) > 0; // we have at least 1 move
    }

    boolean isReadyToSend(Board b) {
        return b.isPlayerTurn() && b.getState(Board.DICE_PLAYER1) > 0 // Board.CAN_MOVE is valid
                && moves.size() > 0 && moves.size() ==  b.getState(Board.CAN_MOVE); // we have enough moves
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("m ");
        for (SingleMove s : moves) {
            String source = (s.getSource() == playerBar) ? "bar" : Integer.toString(s.getSource());
            String dest = (s.getDest() == Board.HOME) ? "home" : Integer.toString(s.getDest());
            buf.append(source + "-" + dest + " ");
        }
        return buf.toString();
    }

    private List<SingleMove> getMovePoints() {
        return moves;
    }

    private boolean isUsed(int die, int count) {
        for (int ix = 0; ix < moves.size(); ix++) {
            SingleMove s = moves.get(ix);
            if (Math.abs(s.getDest() - s.getSource()) == die) {
                if (count == 1) {
                    return true;
                } else {
                    return count < moves.size();
                }
            }
        }
        return false;
    }

    private void setDice(int a, int b) {
        dice.clear();
        dice.add(a);
        dice.add(b);
        if (a == b) {
            dice.add(a);
            dice.add(a);
        }
    }


    // TODO ensure these have been addressed: Ellen's feedback:

    // mutation: moveInProgress & targets are getting changed
    // onTap has side effects not reflected by name
    // setTargets: ditto

    // List {3, 5} dice values
    // method finds targets with not mutations/side effects

    // BUG not preserving which dice

}
