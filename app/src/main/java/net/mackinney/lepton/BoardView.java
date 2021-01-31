package net.mackinney.lepton;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
//import android.util.Log;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

import net.mackinney.lepton.R;

/**
 * Keeps the gameboard current, handles touch events.
 */
class BoardView extends AppCompatImageView {

    private final String TAG = "BoardView";
    private GameHelper helper;

    // TODO CAN WE DEFINE ALL BITMAPS IN XML?
    private final Bitmap splash = BitmapFactory.decodeResource(getResources(), R.drawable.splash);
    private final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.backgammon);
    private final Bitmap messageboard = BitmapFactory.decodeResource(getResources(), R.drawable.messageboard);
    private final Bitmap brownChecker = BitmapFactory.decodeResource(getResources(), R.drawable.brownchecker);
    private final Bitmap whiteChecker = BitmapFactory.decodeResource(getResources(), R.drawable.whitechecker);
    private final Bitmap brownStack = BitmapFactory.decodeResource(getResources(), R.drawable.brownstack);
    private final Bitmap whiteStack = BitmapFactory.decodeResource(getResources(), R.drawable.whitestack);
    private final Bitmap[] whitedie = new Bitmap[]{
            BitmapFactory.decodeResource(getResources(), R.drawable.whitedie1),
            BitmapFactory.decodeResource(getResources(), R.drawable.whitedie2),
            BitmapFactory.decodeResource(getResources(), R.drawable.whitedie3),
            BitmapFactory.decodeResource(getResources(), R.drawable.whitedie4),
            BitmapFactory.decodeResource(getResources(), R.drawable.whitedie5),
            BitmapFactory.decodeResource(getResources(), R.drawable.whitedie6),
            BitmapFactory.decodeResource(getResources(), R.drawable.whitedie1b),
            BitmapFactory.decodeResource(getResources(), R.drawable.whitedie2b),
            BitmapFactory.decodeResource(getResources(), R.drawable.whitedie3b),
            BitmapFactory.decodeResource(getResources(), R.drawable.whitedie4b),
            BitmapFactory.decodeResource(getResources(), R.drawable.whitedie5b),
            BitmapFactory.decodeResource(getResources(), R.drawable.whitedie6b)};
    private final Bitmap whitePrompt = BitmapFactory.decodeResource(getResources(), R.drawable.white_prompt);
    private final Bitmap[] browndie = new Bitmap[]{
            BitmapFactory.decodeResource(getResources(), R.drawable.browndie1),
            BitmapFactory.decodeResource(getResources(), R.drawable.browndie2),
            BitmapFactory.decodeResource(getResources(), R.drawable.browndie3),
            BitmapFactory.decodeResource(getResources(), R.drawable.browndie4),
            BitmapFactory.decodeResource(getResources(), R.drawable.browndie5),
            BitmapFactory.decodeResource(getResources(), R.drawable.browndie6),
            BitmapFactory.decodeResource(getResources(), R.drawable.browndie1b),
            BitmapFactory.decodeResource(getResources(), R.drawable.browndie2b),
            BitmapFactory.decodeResource(getResources(), R.drawable.browndie3b),
            BitmapFactory.decodeResource(getResources(), R.drawable.browndie4b),
            BitmapFactory.decodeResource(getResources(), R.drawable.browndie5b),
            BitmapFactory.decodeResource(getResources(), R.drawable.browndie6b)};
    private final Bitmap brownPrompt = BitmapFactory.decodeResource(getResources(), R.drawable.brown_prompt);
    private final Bitmap[] cube = new Bitmap[]{
            BitmapFactory.decodeResource(getResources(), R.drawable.cube1),
            BitmapFactory.decodeResource(getResources(), R.drawable.cube2),
            BitmapFactory.decodeResource(getResources(), R.drawable.cube4),
            BitmapFactory.decodeResource(getResources(), R.drawable.cube8),
            BitmapFactory.decodeResource(getResources(), R.drawable.cube16),
            BitmapFactory.decodeResource(getResources(), R.drawable.cube32)
    };
    private final Bitmap point_1_top = BitmapFactory.decodeResource(getResources(), R.drawable.point_1_top);
    private final Bitmap point_1_bottom = BitmapFactory.decodeResource(getResources(), R.drawable.point_1_bottom);
    private final Bitmap reject = BitmapFactory.decodeResource(getResources(), R.drawable.reject);
    private final Bitmap accept = BitmapFactory.decodeResource(getResources(), R.drawable.accept);
    private final Bitmap[] resign = new Bitmap[]{
            BitmapFactory.decodeResource(getResources(), R.drawable.resign0),
            BitmapFactory.decodeResource(getResources(), R.drawable.resign1),
            BitmapFactory.decodeResource(getResources(), R.drawable.resign2),
            BitmapFactory.decodeResource(getResources(), R.drawable.resign3)
    };

    // properties from the backgammon Board PNG
    private static final int RAW_TILE_WIDTH = 169;
    private static final int RAW_TILE_HEIGHT = 142;
    // private static final int RAW_TOP_LEFT_X = 216; // top-left of 1st tile, BG point 13
    private static final int RAW_TOP_LEFT_Y = 50;
    private static final float RAW_BOARD_WIDTH = 2704F; // float for scaling computations
    private static final float RAW_BOARD_HEIGHT = 1680F; // float for scaling computations
    private static final int BROWN = -1;

    // mutable properties to be scaled
    private float cubeX;
    private float topLeftY;
    private float tileWidth;
    private float tileHeight;

    // properties for game logic
    private static final int BAR1 = 0;
    private static final int BAR2 = 25;
    private static final int BOTTOM_ROW_UPPER_BOARD = 4;
    private static final int CENTER_ROW = 5;
    private static final int DICE_ROW = CENTER_ROW;
    private static final int TOP_ROW_LOWER_BOARD = 6;
    private static final int MAX_ROW = 11;
    private static final int CUBE_COLUMN = 0;
    private static final int CUBE_OFFERED_COLUMN = 3;
    private static final int CUBE_PLAYER_ROW = MAX_ROW - 1;
    private static final int REJECT_COLUMN = CUBE_OFFERED_COLUMN + 1;
    private static final int BAR_COLUMN = 7;
    private static final int HOME_COLUMN = 14;
    private static final int DICE_COLUMN1 = 10;
    private static final int DICE_COLUMN2 = 11;
    private static final int BAR = 47;

    // command constants
    static final int NONE = -1; // used by offerPending when not set to DOUBLE_OFFER or RESIGN_OFFER
    private static final int ACCEPT = 60; // avoid collions with Board indices
    private static final int REJECT = 61;
    private static final int REQUEST_DOUBLE = 65;
    private static final int CENTER_TAP = 66;
    static final int DOUBLE_OFFER = 67;
    static final int RESIGN_OFFER = 68;
    private static final int SEND_MOVE = 69;
    private static final int CANCEL_MOVE = 70;
    private static final int RESIGN = 71;

    // flags
    private int offerPending = NONE;
    private int offerLevel;
    private boolean drawSplash = true;

    // objects
    private Board board;
    private Move move;

    // scoreboard message
    private float myLeft = -1;
    private float myTop = -1;

    /**
     * The Backgammon board
     */
    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        topLeftY = scale(RAW_TOP_LEFT_Y);
        tileWidth = scale(RAW_TILE_WIDTH);
        tileHeight = scale(RAW_TILE_HEIGHT);
        cubeX = (1.5F * tileWidth - cube[0].getWidth()) / 2F;
    }

    void initialize(GameHelper helper) {
        this.helper = helper;
        board = helper.getBoard();
        this.invalidate();
    }

    /**
     * Begins a new move
     * @param board The current Board.
     */
    void newMove(Board board) {
        move = new Move(board);
    }

    /**
     * Flag to check if we should display Cube
     * @param state NONE, RESIGN_OFFER, or DOUBLE_OFFER
     */
    void setPendingOffer(int state) { offerPending = state; }

    /**
     * Flag to check if we should display Resignation level
     * @param state NONE, RESIGN_OFFER, or DOUBLE_OFFER
     * @param level 1, 2, or 3 if resign offer, power of 2 if double offer.
     */
    void setPendingOffer(int state, int level) { offerPending = state; offerLevel = level; }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float xp = event.getXPrecision();
                float yp = event.getYPrecision();
                float x = event.getX();
                float y = event.getY();
                this.myLeft = this.getX();
                this.myTop = this.getY();
                int[] viewLocation = new int[2];
                this.getLocationOnScreen(viewLocation);
                downTouch(x, y);
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_MOVE:
                break;
        }
        // tell the View that we handled the event
        return true;
    }

    private void downTouch(float x, float y) {
        // only responds while playing
        if (board.isGameOver()) {
            return;
        }
        int c = getColumn(x);
        int r = getRow(y);
        int target = getTarget(r, c); // get the backgammon point, 1-24
        board = helper.getBoard();
        if (target == CENTER_TAP) {
            if (board.getState(Board.TURN) == 0) {
                helper.addCommand("join");
            } else {
                helper.addCommand("board");
            }
        } else if (target == ACCEPT) { // accept double or resignation
            helper.addCommand("accept");
        } else if (target == REJECT) { // reject double or resignation
            helper.addCommand("reject");
        } else {
            if (board.isPlayerTurn()) { // player can roll or double
                if (target == REQUEST_DOUBLE) {
                    helper.addCommand("double");
                } else if (target == Board.DICE_PLAYER1 || target == Board.DICE_PLAYER2) {
                    if (!board.playerHasRolled()) { // need to roll
                        helper.addCommand("roll");
                    } else { // dice tap should set active die
                        move.resetActiveDie(board, board.getState(target));
                        updateGameBoard(board);
                    }
                } else if (target == SEND_MOVE && move.isReadyToSend(board)) {
                    helper.addCommand(move.toString());
                } else if (target == CANCEL_MOVE && move.isStarted(board)) { // need to be able to cancel partial move
                    board.setBoard(board.getLastLine(), false);
                    newMove(board);
                    updateGameBoard(board);
                } else if ((1 <= target && target <= 24 || target == BAR) && board.getState(Board.DICE_PLAYER1) > 0) { // Board.BoardPoints[]
                    if (target == BAR) {
                        target = board.getState(Board.DIRECTION) == 1 ? 0 : 25;
                    }
                    if (move.process(target, board)) {
                        updateGameBoard(board);
                    }
                }
            }
        }
    }

    private int getTarget(int r, int c) { // , , , double, roll, move
        if (c == HOME_COLUMN && r == DICE_ROW) {
            return RESIGN;
        } else if (c == CUBE_COLUMN
                && ((r == DICE_ROW && board.playerMayDouble() && board.oppMayDouble())
                || (r == CUBE_PLAYER_ROW && board.playerMayDouble()))) {
            return REQUEST_DOUBLE;
        } else if (c == CUBE_OFFERED_COLUMN && r == DICE_ROW && board.getState(Board.WAS_DOUBLED) == 1) {
            return ACCEPT;
        } else if (c == CUBE_OFFERED_COLUMN && r == DICE_ROW && offerPending == RESIGN_OFFER) {
            return ACCEPT;
        } else if (c == REJECT_COLUMN && r == DICE_ROW && board.getState(Board.WAS_DOUBLED) == 1) {
            return REJECT;
        } else if (c == REJECT_COLUMN && r == DICE_ROW && offerPending == RESIGN_OFFER) {
            return REJECT;
        } else if (r == DICE_ROW && c == DICE_COLUMN1) {
            return Board.DICE_PLAYER1;
        } else if (r == DICE_ROW && c == DICE_COLUMN1 -1) {
            return SEND_MOVE;
        } else if (r == DICE_ROW && c == DICE_COLUMN2 + 1) {
            return CANCEL_MOVE;
        } else if (r == DICE_ROW && c == DICE_COLUMN2) {
            return Board.DICE_PLAYER2;
        } else if (c == BAR_COLUMN) {
            if (r == DICE_ROW) {
                return CENTER_TAP;
            } else {
                return BAR;
            }
        } else if (c == HOME_COLUMN) {
            if (r == DICE_ROW) {
                // TODO routine to accept or reject resignation
                return NONE;
            } else {
                return Board.HOME;
            }
        } else if (c > CUBE_COLUMN && r != DICE_ROW) { // find the point
            // adjust column value for point correspondence
            if (c > BAR_COLUMN) {
                c--;
            }
            if (r > DICE_ROW) { // bottom of Board
                c = HOME_COLUMN - 1  - c;
            } else if (r < DICE_ROW) { // top of Board
                c = HOME_COLUMN - 2 + c;
            }
            // convert for direction
            if (board.getState(Board.DIRECTION) > 0) {
                c = BAR2 - c;
            }
            return c;
        }
        return NONE;
    }

    private int getColumn(float x) {
        float unit = getWidth() * RAW_TILE_WIDTH / RAW_BOARD_WIDTH;
        if (0.25F * unit <= x && x <= 1.25F * unit) { // cube column
            return 0;
        } else if (1.5F * unit <= x && x <= 14.5F * unit) { // points & bar
            return (int) ((x - 0.5F * unit) / unit); // 1 - 13
        } else if (14.75F * unit <= x && x <= 15.75F * unit) {
            return 14;
        }
        return -1;
    }

    private int getRow(float y) {
        float unit = getHeight() * RAW_TILE_HEIGHT / RAW_BOARD_HEIGHT;
        float startY = getHeight() * RAW_TOP_LEFT_Y / RAW_BOARD_HEIGHT;
        float endY = startY + MAX_ROW * unit;
        if (startY <= y && y <= endY) {
            return (int) ((y - startY) / unit);
        }
        return -1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    private float scale(int x) {
        return (float) x * bitmap.getWidth() / RAW_BOARD_WIDTH;
    }

    private float scale(float x) {
        return x * bitmap.getWidth() / RAW_BOARD_WIDTH;
    }

    /**
     * Refresh the board
     */
    void updateGameBoard(Board board) {
        if (board != null) {
            this.board = board;
        }
        if (this.board.getState(Board.TURN) == 0) {
            offerPending = NONE;
        } else if (this.board.wasDoubled()) {
            offerPending = DOUBLE_OFFER;
        }
        Bitmap gb = bitmap.copy(Bitmap.Config.RGB_565, true);
        Canvas canvas = new Canvas(gb);
        // Calculate the horizontal offset (assume board bitmap is centered)
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        if (this.board.getState(Board.DIRECTION) == -1) {
            canvas.drawBitmap(point_1_bottom, 0, 0, null);
        } else {
            canvas.drawBitmap(point_1_top, 0, 0, null);
        }

        // draw checkers
        int[] bp = this.board.getBoardPoints();
        Bitmap checker;
        for (int p = 0; p < bp.length; p++) { // iterate over points and bar
            if (bp[p] != 0) {
                if (bp[p] > 0) {
                    checker = whiteChecker;
                } else {
                    checker = brownChecker;
                }
                if (p == BAR1 || p == BAR2) {
                    drawBarPoint(canvas, p, Math.abs(bp[p]), checker);
                } else {
                    drawBoardPoint(canvas, p, Math.abs(bp[p]), this.board.getState(Board.DIRECTION), checker);
                }
            }
        }

        drawOff(canvas); // draw checkers that have been born off
        if (offerPending == RESIGN || offerPending == RESIGN_OFFER) {
            float x = cubeX + 3 * tileWidth; // column 3
            float y = topLeftY + DICE_ROW * tileHeight;
            canvas.drawBitmap(resign[offerLevel], x, y, null);
            if (offerPending == RESIGN_OFFER) {
                canvas.drawBitmap(reject, x + tileWidth, y, null);
            }
        }
        if (offerPending == NONE) {
            drawDice(canvas);
        }
        drawCube(canvas);
        if (drawSplash) {
            canvas.drawBitmap(splash, 0, 0, null);
            drawSplash = false;
        }
        this.setImageBitmap(gb);
        this.invalidate();
        if (this.getVisibility() == View.VISIBLE) {
            helper.listener.setScoreBoardMessage(board);
        }

    }

    private void drawCube(Canvas c) {
        float x = cubeX; // left gutter
        float y = topLeftY + DICE_ROW * tileHeight; // center row
        int cv = board.getState(Board.CUBE);
        if (offerPending == DOUBLE_OFFER) {
            x += CUBE_OFFERED_COLUMN * tileWidth;
            cv *= 2;
            if (!board.isPlayerTurn()) {
                c.drawBitmap(reject, x + tileWidth, y, null);
            }
        } else if (board.playerMayDouble() && !board.oppMayDouble()) {
            y += (MAX_ROW - 1) / 2F * tileHeight; // bottom row
        } else if (!board.playerMayDouble()) {
            y = topLeftY;                         // top row
        }
        c.drawBitmap(cube[log2(cv)], x, y, null);
    }

    // simple function to convert power of 2 to log, rather than call Math library
    private static int log2(int p) {
        switch (p) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 4:
                return 2;
            case 8:
                return 3;
            case 16:
                return 4;
            case 32:
                return 5;
            default:
                return -1;
        }
    }

    /* drawDice
        playerTurn and dice not rolled -> draw rollDice bitmaps
        playerTurn and dice rolled -> draw playerDice bitmaps
        oppturn and dice rolled -> draw oppDice bitmaps
    */
    private void drawDice(Canvas c) {
        float diceY = topLeftY + DICE_ROW * tileHeight;
        float diceX = 3.5F * tileWidth; // opponent position
        Bitmap[] bitmaps;
        if (board.isPlayerTurn()) {
            diceX += 7F * tileWidth;    // adjust to player position
            if (board.getState(Board.DICE_PLAYER1) == 0) {
                Bitmap d = (board.getState(Board.COLOR) == BROWN) ? brownPrompt : whitePrompt;
                c.drawBitmap(d, diceX, diceY, null);
                c.drawBitmap(d, diceX + tileWidth, diceY, null);
            } else { // player's roll is non-zero
                if (move == null) {
                    newMove(board);
                }
                bitmaps = (board.getState(Board.COLOR) == BROWN) ? browndie : whitedie;
                int d1 = board.getState(Board.DICE_PLAYER1);
                int d2 = board.getState(Board.DICE_PLAYER2);
                if (move.isStarted(board)) {
                    c.drawBitmap(reject, diceX + 2 * tileWidth, diceY, null); // -- TODO drawOppDice() method, then unify
                }
                if (move.isReadyToSend(board)) {
                    c.drawBitmap(accept, diceX - tileWidth, diceY, null); // -1
                } else if (d1 == move.getActiveDie()) {
                    d1 += 6; // add 6 to index to select large bitmap
                } else if (d2 == move.getActiveDie()) {
                    d2 += 6;
                }
                c.drawBitmap(bitmaps[d1 - 1], diceX, diceY, null); // -1
                c.drawBitmap(bitmaps[d2 - 1], diceX + tileWidth, diceY, null); // -- TODO drawOppDice() method, then unify
            }
        } else if (board.getState(Board.DICE_OPP1) != 0) {                // opp turn
            bitmaps = (board.getState(Board.COLOR) == BROWN) ? whitedie : browndie;
            c.drawBitmap(bitmaps[board.getState(Board.DICE_OPP1) - 1], diceX, diceY, null);
            c.drawBitmap(bitmaps[board.getState(Board.DICE_OPP2) - 1], diceX + tileWidth, diceY, null);
        }
    }

    private boolean isLeftOfBar(int point) { // points [7 - 18]
        return BAR_COLUMN <= point && point < BAR_COLUMN + HOME_COLUMN - 2;
    }

    private boolean isTopOfBoard(int point, int direction) { // points [13 .. 24] are on top if direction = -1
        if (direction == -1) {
            return point >= HOME_COLUMN - 1;
        } else { // direction == 1
            return point < HOME_COLUMN - 1;
        }
    }

    private void drawBoardPoint(Canvas c, int point, int count, int direction, Bitmap checker) {
        float x = (0.5F + pointToColumn(point)) * tileWidth;
        float y = topLeftY;
        if (isTopOfBoard(point, direction)) { // top of Board
            for (int ix = 0; ix < count; ix++) {
                if (ix == 0 || ix == 9) {
                    y = topLeftY;
                } else if (ix == 5 || ix == 13) { // stack stacking checkers
                    y = topLeftY + tileHeight / 2F;
                }
                c.drawBitmap(checker, x, y, null);
                y += tileHeight;
            }
        } else { // bottom of Board
            for (int ix = count - 1; ix >= 0; ix--) {
                if (ix == count - 1 || ix == count - 10) {
                    y = topLeftY + (MAX_ROW - 1) * tileHeight;
                } else if (ix == count - 6 || ix == count - 14) {
                    y = topLeftY + (MAX_ROW - 1) * tileHeight - Math.round(tileHeight / 2F);
                }
                c.drawBitmap(checker, x, y, null);
                y -= tileHeight;
            }
        }
    }

    private void drawBarPoint(Canvas c, int point, int count, Bitmap checker) {
        // here x, y are grid units
        float x = (1.5F + (BAR_COLUMN - 1)) * tileWidth;
        float y = topLeftY;
        if (point == BAR1) { // use TopOfBoard stacking but start at top of lower Board
            for (int ix = 0; ix < count; ix++) {
                if (ix == 0 || ix == 9) {
                    y = topLeftY + TOP_ROW_LOWER_BOARD * tileHeight;
                } else if (ix == 5 || ix == 13) {
                    y = topLeftY + (TOP_ROW_LOWER_BOARD + 0.5F) * tileHeight;
                }
                c.drawBitmap(checker, x, y, null);
                y += tileHeight;
            }
        } else if (point == BAR2) { // useBottomOfBoard stacking but start at bottom of upper Board
            for (int ix = count - 1; ix >= 0; ix--) {
                if (ix == count - 1 || ix == count - 10) {
                    y = topLeftY + (BOTTOM_ROW_UPPER_BOARD) * tileHeight;
                } else if (ix == count - 6 || ix == count - 14) {
                    y = topLeftY + (BOTTOM_ROW_UPPER_BOARD - 0.5F) * tileHeight;
                }
                c.drawBitmap(checker, x, y, null);
                y -= tileHeight;
            }
        }
    }

    private void drawOff(Canvas c) {
        Bitmap checker = getPlayerChecker(board, true);
        int checkerHeight = checker.getHeight();
        float x = 14.8F * tileWidth;
        float y = topLeftY + MAX_ROW * tileHeight - board.getState(Board.ON_HOME_PLAYER) * checkerHeight;
        for (int ix = 0; ix < board.getState(Board.ON_HOME_PLAYER); ix++) {
            c.drawBitmap(checker, x, y, null);
            y += checkerHeight;
        }
        y = topLeftY;
        checker = getOppChecker(board, true);
        for (int ix = 0; ix < board.getState(Board.ON_HOME_OPP); ix++) {
            c.drawBitmap(checker, x, y, null);
            y += checkerHeight;
        }
    }

    private int pointToColumn(int p) {
        int column;
        p = (p < HOME_COLUMN - 1) ? p : BAR2 - p; // points in the same column always total 25
        column = HOME_COLUMN - p - 1;             // point 12 is column 1, point 11 is column 2, etc
        if (BAR_COLUMN > p) {
            column += 1;                  // if p is to the right of the bar, shift 1 to the right
        }
        return column;
    }

    private Bitmap getPlayerChecker(Board b) {
        return getPlayerChecker(b, false);
    }

    private Bitmap getPlayerChecker(Board b, boolean isOff) {
        if (board.getState(Board.COLOR) < 0) {
            return isOff ? brownStack : brownChecker;
        } else {
            return isOff ? whiteStack : whiteChecker;
        }
    }

    private Bitmap getOppChecker(Board b) {
        return getOppChecker(b, false);
    }

    private Bitmap getOppChecker(Board b, boolean isOff) {
        if (board.getState(Board.COLOR) < 0) {
            return isOff ? whiteStack : whiteChecker;
        } else {
            return isOff ? brownStack : brownChecker;
        }
    }

    /**
     * Handles resignation initiated by opponent (by parsing FIBS output)
     * or by player (received from Resign button dialog)
     * @param user Player, or opponent.
     * @param level Normal, Gammon, or Backgammon.
     */
    public void handleResignation(String user, int level) {
        offerLevel = level + 1; // adjust icon index  N = 1, G = 2, B = 3
        if ("Player".equals(user)) {
            offerPending = RESIGN;
        } else {
            offerPending = RESIGN_OFFER;
        }
        String levelText = "normal"; // default
        if (offerLevel == 2) {
            levelText = "gammon";
        } else if (offerLevel == 3) {
            levelText = "backgammon";
        }
        helper.addCommand("resign " + levelText);
        updateGameBoard(board);
        offerLevel = 0;
    }

    /**
     * Display the splash screen
     */
    public void setBackgroundSplash() { this.setImageBitmap(splash); }
}