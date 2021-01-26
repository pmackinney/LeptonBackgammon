package net.mackinney.lepton;

//import android.util.Log;
import java.util.Arrays;

/**
 * Maintains the board state
 */
class Board {
     static final String TAG = "Board";
    /* sample FIBS CLIP output to be parsed; http://www.fibs.com/fibs_interface.html
        board:You:Nortally:5:0:0:0:-3:0:0:2:0:3:0:3:0:-1:0:-4:4:0:1:0:-2:0:-2:-3:1:0:0:1:0:-1:0:0:0:0:2:0:1:0:1:-1:0:25:0:0:0:0:2:0:0:0
    */

    private String lastLine;
    private static final int NUM_CLIP_FIELDS = 52;
    private static final int FIRST_NUMERIC_CLIP_FIELD = 3;
    private int[] state = new int[NUM_CLIP_FIELDS];
    private static final int NUM_POINTS = 26;
    static final int PLAYER = 1;         // the player's name (always 'You', we display the login name on the scoreboard)
    static final int OPPONENT = 2;       // the opponent's name
    static final int MATCH_LENGTH = 3;   // match length or 9999 for unlimited matches
    static final int SCORE_PLAYER = 4;   // player's points in the match so far
    static final int SCORE_OPPONENT = 5; // opponent's points in the match so far
    static final int BOARD_START = 6;    // 26 numbers giving the board. Positions 0 and 25 represent the bars for the players (see below). Positive numbers represent O's pieces negative numbers represent X's pieces
    static final int BOARD_END = 31;     //
    static final int TURN = 32;          // -1 if it's X's turn, +1 if it's O's turn 0 if the game is over
    static final int DICE_PLAYER1 = 33;  // 2 numbers giving the player's dice. If it's the players turn and she or he hasn't rolled, yet both numbers are 0
    static final int DICE_PLAYER2 = 34;  //
    static final int DICE_OPP1 = 35;     // the opponent's dice (2 numbers)
    static final int DICE_OPP2 = 36;     //
    static final int CUBE = 37;          // the number on the doubling cube
    static final int MAY_DOUBLE_PLAYER = 38;  // 1 if player is allowed to double, 0 otherwise
    static final int MAY_DOUBLE_OPP = 39;// the same for the opponent
    static final int WAS_DOUBLED = 40;   // 1 if your opponent has just doubled, 0 otherwise
    static final int COLOR = 41;         // -1 if you are X, +1 if you are O
    static final int DIRECTION = 42;     // -1 if you play from position 24 to position 1
    static final int HOME = 43;          // 0 or 25 depending on direction (obsolete but included anyway)
    static final int BAR = 44;           // 25 or 0 (see home)
    static final int ON_HOME_PLAYER = 45;// number of pieces already removed from the board by player
    static final int ON_HOME_OPP = 46;   // same for opponent
    static final int ON_BAR_PLAYER = 47; // number of player's pieces on the bar
    static final int ON_BAR_OPP = 48;    // same for opponent
    static final int CAN_MOVE = 49;      // a number between 0 and 4. This is the number of pieces you can move. This token is valid if it's your turn and you have already rolled.
    static final int FORCED_MOVE = 50;   // don't use this token
    static final int DID_CRAWFORD = 51;  // don't use this token
    static final int REDOUBLES = 52;     // maximum number of instant redoubles in unlimited matches

    private static final int QUADRANT_I_START = 1;
    private static final int QUADRANT_II_START = QUADRANT_I_START + BoardView.NUM_POINTS_PER_QUADRANT;
    private static final int NONE = -1;
    private static final int PLAYER_TURN = 1;
    private static final int BAR1 = 0;
    private static final int BAR2 = 25;
    private String playerName;                   // Board says "You", we display the login name
    private String oppName = "";
    private int[] boardPoints = new int[NUM_POINTS]; // 24 points + home & bar
    private boolean gameOver = true;


    int getState(int param) {
        return state[param];
    }

    void setState(int param, int value) {
        state[param] = value;
    }

    void setBoard(String line) {
        setBoard(line, true);
    }

    void setBoard(String line, Boolean saveLine) {
        String[] terms = line.split(":");
        if (saveLine) {
            lastLine = line;
        }
        oppName = terms[OPPONENT];
        for (int ix = FIRST_NUMERIC_CLIP_FIELD; ix < NUM_CLIP_FIELDS; ix++) {
            state[ix] = Integer.parseInt(terms[ix]);
        }
        // +1 at end limit because last arg of copyOfRange(int[] original, from, to) is EXCLUSIVE!
        boardPoints = Arrays.copyOfRange(state, BOARD_START, BOARD_END + 1);
    }

    String getLastLine() {
        return lastLine;
    }

    String getOppName() {
        return oppName;
    }

    int[] getBoardPoints() {
        return boardPoints;
    }

    boolean isPlayerTurn() {
        return state[TURN] == state[COLOR];
    }

    boolean isPlayerPoint(int p) {
        return boardPoints[p] * state[COLOR] > 0;
    }

    public boolean pointIsAvailable(int p) {
        if (p < 1) { // home is never blockaded, bar is always blockaded
            return state[DIRECTION] == -1;
        } else if (p > 24) {
            return state[DIRECTION] == 1;
        } else if (Math.abs(boardPoints[p]) < 2) { // less that 2 checkers?
            return true;
        } else if (boardPoints[p] * state[COLOR] > 0) { // is point our color?
            return true;
        }
        return false;
    }

    // player may bear off if not checkers outside home (points 1-6 or 19-24)
    public boolean playerMayBearOff() {
        if (state[ON_BAR_PLAYER] > 0) {
            return false;
        } else {
            int start = (state[DIRECTION] == 1) ? QUADRANT_I_START : QUADRANT_II_START;
            int end = start + BoardView.NUM_POINTS_PER_QUADRANT + 1;
            for (int ix = start; ix <= end; ix++) {
                if (isPlayerPoint(ix)) {
                    return false;
                }
            }
            return true;
        }
    }

    // NOTE: This value is only valid on the first board after the double offer
    boolean wasDoubled() {
        return state[WAS_DOUBLED] == 1;
    }

    boolean playerHasRolled() {
        return isPlayerTurn() && state[DICE_PLAYER1] != 0;
    }

    boolean playerMayDouble() {
        return state[MAY_DOUBLE_PLAYER] == 1;
    }

    boolean oppMayDouble() {
        return state[MAY_DOUBLE_OPP] == 1;
    }

    boolean hasMovesByDie(int d) {      // we expect d in [1, 2, ... 6]
        if (boardPoints[state[BAR]] != 0) {            // if on bar, must move from bar
            return pointIsAvailable(state[BAR] + state[DIRECTION] * d);
        } else {                                // other test all points except home and bar
            for (int ix = 1; ix <= NUM_POINTS - 2; ix++) {
                if (isPlayerPoint(ix)) {
                    int target = ix + state[DIRECTION] * d;
                    if (pointIsAvailable(target)) {
                        if ((state[HOME] == 0 && target <= state[HOME]) // if target is home
                                || (state[HOME] == NUM_POINTS - 1 && target >= state[HOME])) {
                            if (playerMayBearOff()) {                   // valid if bearing off
                                return true;
                            } else {
                                continue;                               // keep looking
                            }
                        } else {
                            return true;               // otherwise valid
                        }
                    }
                }
            }
        }
        return false;
    }

    boolean isGameOver() {
        return gameOver;
    }

    void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }
}
