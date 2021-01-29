package net.mackinney.lepton;

import java.util.ArrayList;
import java.util.List;

// TODO on error (e.g., server says 'You can't ... ') reset move
// TODO if man on bar, active die must be legal to bear on

/**
 * This class is used to stage a move consisting of 1-4 individual checker movements (SingleMoves).
 * It is responsible for validating potential moves to ensure that checker movements are permitted
 * iff they will be accepted by FIBS (e.g., follow the rules of Backgammon).
 *
 */
class Move {
    private static final int NONE = -1;
    private List<SingleMove> moves;
    private int playerBar;
    private int playerHome;
    private int oppBar;
    private List<Integer> dice;
    private int activeDie;

    /**
     * Class constructor
     */
    Move(Board board) {
        dice = new ArrayList<>(4); // Integer
        moves = new ArrayList<>(); // Move.SingleMove
        activeDie = NONE;
        playerBar = board.getState(Board.BAR);
        playerHome = board.getState(Board.HOME);
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

    /**
     * Returns the value of the die being used.
     */
    int getActiveDie() {
        return activeDie;
    }

    /**
     * Designate the active die, if possible.
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
     * Try to move the tapped checker using the activeDie.
     * Side effect: If the proposed move is valid, the local copy of the board is altered to reflect
     * it, and the BoardView is updated.
     *
     * @param p the proposed destination
     * @return true if the proposed move is valid, false otherwise
     */
    boolean process(int p, Board b) {
        if (isReadyToSend(b) || !b.isPlayerPoint(p)) {
            return false;
        }
        boolean result = false;
        int target = p + b.getState(Board.DIRECTION) * activeDie;
        if (b.playerMayBearOff()) {
            if (target == playerHome) {                                               //   die is exactly enough to bear off
                result = true;
            } else if (b.getState(Board.DIRECTION) == 1 && target > playerHome
                       || b.getState(Board.DIRECTION) == -1 && target < playerHome) { //   die is more than enough to bear off
                if (isPointWithCheckersFurthestFromHome(p, b)) {                      //   and all higher points in home field are empty
                    target = playerHome;
                    result = true;
                }
            } else { // normal move
                result = b.pointIsAvailable(target);
            }
        } else if (1 <= target && target <= 4 * BoardView.NUM_POINTS_PER_QUADRANT) {  // target is regular point
            result = b.pointIsAvailable(target);
        } else {                                                                      // target is home but we can't bear off
            result = false;
        }
        if (result) {
            b.getBoardPoints()[p] -= b.getState(Board.COLOR); // remove checker
            // addChecker(target);
            if (target == playerHome) {
                b.setState(Board.ON_HOME_PLAYER, b.getState(Board.ON_HOME_PLAYER) + 1);
            } else {
                int[] boardPoints = b.getBoardPoints();
                if (boardPoints[target] * b.getState(Board.COLOR) == -1) { // if the target has an opponent's checker
                    boardPoints[target] = 0;                               //    clear it
                    boardPoints[oppBar] -= b.getState(Board.COLOR);        //    place on opponent's bar
                }
                boardPoints[target] += b.getState(Board.COLOR); // place on target
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

    /**
     * Test whether the move has started
     */
    boolean isStarted(Board b) {
        return b.isPlayerTurn() && b.getState(Board.DICE_PLAYER1) > 0 // Board.CAN_MOVE is valid
                && moves.size() > 0 && b.getState(Board.CAN_MOVE) > 0; // we have at least 1 move
    }

    /**
     * Test whether the move is complete
     */
    boolean isReadyToSend(Board b) {
        return b.isPlayerTurn() && b.getState(Board.DICE_PLAYER1) > 0 // Board.CAN_MOVE is valid
                && moves.size() > 0 && moves.size() ==  b.getState(Board.CAN_MOVE); // we have enough moves
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("m ");
        for (SingleMove s : moves) {
            String source = (s.getSource() == playerBar) ? "bar" : Integer.toString(s.getSource());
            String dest = (s.getDest() == playerHome) ? "off" : Integer.toString(s.getDest());
            buf.append(source + "-" + dest + " ");
        }
        return buf.toString();
    }

    private boolean isPointWithCheckersFurthestFromHome(int p, Board b) {
        // This method only gets called when we're bearing off
        // depending on Quadrant, check 6 through p+1 or 19 through p-1
        if (playerHome == 0) { // Quadrant I
            for (int ix = BoardView.NUM_POINTS_PER_QUADRANT; ix > p; ix--) {
                if (b.isPlayerPoint(ix)) {
                    return false;
                }
            }
        } else { // Quadrant IV
            for (int ix = 3 * BoardView.NUM_POINTS_PER_QUADRANT + 1; ix < p; ix++) {
                if (b.getBoardPoints()[ix] * b.getState(Board.DIRECTION) > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * An ordered pair defining the move of one checker
     */
    private class SingleMove {
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
}
