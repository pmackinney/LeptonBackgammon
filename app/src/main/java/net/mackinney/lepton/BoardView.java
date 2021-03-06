package net.mackinney.lepton;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
//import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * Keeps the gameboard current, handles touch events.
 */
class BoardView extends AppCompatImageView {

    private final String TAG = "BoardView";
    private GameHelper helper;

    // TODO CAN WE DEFINE ALL BITMAPS IN XML?
    private final Bitmap backgammon = BitmapFactory.decodeResource(getResources(), R.drawable.backgammon);
    private final Bitmap brownChecker = BitmapFactory.decodeResource(getResources(), R.drawable.brownchecker);
    private final Bitmap whiteChecker = BitmapFactory.decodeResource(getResources(), R.drawable.whitechecker);
    private final Bitmap brownStack = BitmapFactory.decodeResource(getResources(), R.drawable.brownstack);
    private final Bitmap whiteStack = BitmapFactory.decodeResource(getResources(), R.drawable.whitestack);
    private final Bitmap[] whiteDie = new Bitmap[]{
            BitmapFactory.decodeResource(getResources(), R.drawable.white_prompt),
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
    private final Bitmap[] brownDie = new Bitmap[]{
            BitmapFactory.decodeResource(getResources(), R.drawable.brown_prompt),
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
    private final Bitmap[] skeletonDie = new Bitmap[] {
            BitmapFactory.decodeResource(getResources(), R.drawable.skeleton0),
            BitmapFactory.decodeResource(getResources(), R.drawable.skeleton1),
            BitmapFactory.decodeResource(getResources(), R.drawable.skeleton2),
            BitmapFactory.decodeResource(getResources(), R.drawable.skeleton3),
            BitmapFactory.decodeResource(getResources(), R.drawable.skeleton4),
            BitmapFactory.decodeResource(getResources(), R.drawable.skeleton5),
            BitmapFactory.decodeResource(getResources(), R.drawable.skeleton6)
    };
    private final int SKELETON_OPP1 = 0;
    private final int SKELETON_OPP2 = 1;
    private final int SKELETON_PLAYER1 = 2;
    private final int SKELETON_PLAYER2 = 3;
    private final Bitmap[] skeletonHistory = new Bitmap[4];
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
    // private static final int RAW_TOP_LEFT_X = 216; // Not needed because board is 16 tile widths exactly
    private static final int RAW_TOP_LEFT_Y = 55;
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

    private final float diceY;
    private final float oppDie1X;
    private final float oppDie2X;
    private final float playerDie1X;
    private final float playerDie2X;

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

    static boolean DISABLE_TEST = true;

    /**
     * The backgammon board
     * @param context The application context.
     * @param attrs View attributes.
     */
    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        topLeftY = scale(RAW_TOP_LEFT_Y);
        tileWidth = scale(RAW_TILE_WIDTH);
        tileHeight = scale(RAW_TILE_HEIGHT);
        cubeX = (1.5F * tileWidth - cube[0].getWidth()) / 2F;
//        shiftWidth = .5F + (tileWidth - brownChecker.getWidth()) / 2F;
        diceY = topLeftY + DICE_ROW * tileHeight;
        oppDie1X = 3.5F * tileWidth;
        oppDie2X = oppDie1X + tileWidth;
        playerDie1X = oppDie1X + 7 * tileWidth;
        playerDie2X = playerDie1X + tileWidth;
        for (int ix = 0; ix < skeletonHistory.length; ix++) {
            skeletonHistory[ix] = skeletonDie[0];
        }
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
                float x = event.getX();
                float y = event.getY();
                this.myLeft = this.getX(); // for scoreboard
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
        board = helper.getBoard();
        if (board.isGameOver()) {
            return;
        }
        int target = getTarget(getColumn(x), getRow(y)); // get the backgammon point, 1-24
        // skip if not playing
        if (target == CENTER_TAP) {
            helper.listener.showHideScoreBoard();
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

    private int getTarget(int x, int y) { // , , , double, roll, move
        if (x == HOME_COLUMN && y == DICE_ROW) {
            return RESIGN;
        } else if (x == CUBE_COLUMN
                && ((y == DICE_ROW && board.playerMayDouble() && board.oppMayDouble())
                || (y == CUBE_PLAYER_ROW && board.playerMayDouble()))) {
            return REQUEST_DOUBLE;
        } else if (x == CUBE_OFFERED_COLUMN && y == DICE_ROW && board.getState(Board.WAS_DOUBLED) == 1) {
            return ACCEPT;
        } else if (x == CUBE_OFFERED_COLUMN && y == DICE_ROW && offerPending == RESIGN_OFFER) {
            return ACCEPT;
        } else if (x == REJECT_COLUMN && y == DICE_ROW && board.getState(Board.WAS_DOUBLED) == 1) {
            return REJECT;
        } else if (x == REJECT_COLUMN && y == DICE_ROW && offerPending == RESIGN_OFFER) {
            return REJECT;
        } else if (y == DICE_ROW && x == DICE_COLUMN1) {
            return Board.DICE_PLAYER1;
        } else if (y == DICE_ROW && x == DICE_COLUMN1 -1) {
            return SEND_MOVE;
        } else if (y == DICE_ROW && x == DICE_COLUMN2 + 1) {
            return CANCEL_MOVE;
        } else if (y == DICE_ROW && x == DICE_COLUMN2) {
            return Board.DICE_PLAYER2;
        } else if (x == BAR_COLUMN) {
            if (y == DICE_ROW) {
                return CENTER_TAP;
            } else {
                return BAR;
            }
        } else if (x == HOME_COLUMN) {
            return Board.HOME;
        } else if (x > CUBE_COLUMN && y != DICE_ROW) { // find the point
            // adjust column value for point correspondence
            if (x > BAR_COLUMN) {
                x--;
            }
            if (y > DICE_ROW) { // bottom of Board
                x = HOME_COLUMN - 1  - x;
            } else { // top of Board
                x = HOME_COLUMN - 2 + x;
            }
            // convert for direction
            if (board.getState(Board.DIRECTION) > 0) {
                x = BAR2 - x;
            }
            return x;
        }
        return NONE;
    }

    private int getColumn(float x) {
        float unit = getWidth() * RAW_TILE_WIDTH / RAW_BOARD_WIDTH;
        if (0.25F * unit <= x && x <= 1.25F * unit) { // left gutter
            return 0;
        } else if (1.5F * unit <= x && x <= 14.5F * unit) { // points & bar
            return (int) ((x - 0.5F * unit) / unit); // 1 - 13
        } else if (14.75F * unit <= x && x <= 15.75F * unit) { // right gutter
            return 14;
        }
        return -1;
    }

    private int getRow(float y) {
        int boardHeight = getHeight();
        float unit = boardHeight * RAW_TILE_HEIGHT / RAW_BOARD_HEIGHT;
        float startY = boardHeight * RAW_TOP_LEFT_Y / RAW_BOARD_HEIGHT;
        float endY = startY + MAX_ROW * unit;
        if (startY <= y && y <= endY) {
            return (int) ((y - startY) / unit);
        }
        return -1;
    }

    @Override
    protected void onDraw(Canvas canvas) { super.onDraw(canvas); }

    private float scale(int x) {
        return (float) x * backgammon.getWidth() / RAW_BOARD_WIDTH;
    }

    private float scale(float x) {
        return x * backgammon.getWidth() / RAW_BOARD_WIDTH;
    }

    /**
     * Update the board state and refresh the View
     * @param b
     */
    void updateGameBoard(Board b) {
        if (b != null) {
            board = b;
        }
        if (board.getState(Board.TURN) == 0) {
            offerPending = NONE;
        } else if (board.wasDoubled()) {
            offerPending = DOUBLE_OFFER;
        }
        Bitmap gb = backgammon.copy(Bitmap.Config.RGB_565, true);
        Canvas canvas = new Canvas(gb);
        // Calculate the horizontal offset (assume board bitmap is centered)
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        if (board.getState(Board.DIRECTION) == -1) {
            canvas.drawBitmap(point_1_bottom, 0, 0, null);
        } else {
            canvas.drawBitmap(point_1_top, 0, 0, null);
        }

        // draw checkers
        int[] bp = board.getBoardPoints();
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
                    drawBoardPoint(canvas, p, Math.abs(bp[p]), board.getState(Board.DIRECTION), checker);
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
        boolean testa = offerPending == NONE;
        boolean testb = board != null;
        boolean testc = !board.isGameOver();
        if (offerPending == NONE && board != null && !board.isGameOver()) { // TEST
            drawDice(canvas);
        }
        drawCube(canvas);
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

    /** drawDice
        playerTurn and dice not rolled -> draw rollDice bitmaps
        playerTurn and dice rolled -> draw playerDice bitmaps
        oppturn and dice rolled -> draw oppDice bitmaps
        @param canvas The current canvas.
    */
    private void drawDice(Canvas canvas) {
        float dice1X = oppDie1X; // opponent position
        float dice2X = oppDie2X;

        Bitmap[] bitmaps = null; // to silence lint
        int d1 = 0;             //  dice will show prompt or blank
        int d2 = 0;             //       "
        if (board.isPlayerTurn()) {
            dice1X = playerDie1X;    // adjust to player position
            dice2X = playerDie2X;
            if (move == null) {
                newMove(board);
            }
            bitmaps = (board.getState(Board.COLOR) == BROWN) ? brownDie : whiteDie;
            d1 = board.getState(Board.DICE_PLAYER1);
            d2 = board.getState(Board.DICE_PLAYER2);
            // save while neither is active
            skeletonHistory[SKELETON_PLAYER1] = skeletonDie[d1];
            skeletonHistory[SKELETON_PLAYER2] = skeletonDie[d2];
            if (move.isStarted(board)) {
                canvas.drawBitmap(reject, dice1X + 2 * tileWidth, diceY, null); // -- TODO reduce duplicated logic for player, opp
            }
            if (move.isReadyToSend(board)) {
                canvas.drawBitmap(accept, dice1X - tileWidth, diceY, null); // -1
            } else  {
                if (d1 > 0) { // if either die is zero, both are -- we don't have to test both
                    if (d1 == move.getActiveDie()) {
                        d1 = getLargeDieIndex(d1);
                    }
                    if (d2 == move.getActiveDie()) {
                        d2 = getLargeDieIndex(d2);
                    }
                }
            }
            canvas.drawBitmap(skeletonHistory[SKELETON_OPP1], oppDie1X, diceY, null);
            canvas.drawBitmap(skeletonHistory[SKELETON_OPP2], oppDie2X, diceY, null);
        } else {
            bitmaps = (board.getState(Board.COLOR) == BROWN) ? whiteDie : brownDie;
            d1 = board.getState(Board.DICE_OPP1);
            d2 = board.getState(Board.DICE_OPP2);
            // save to display when player turn
            skeletonHistory[SKELETON_OPP1] = skeletonDie[d1];
            skeletonHistory[SKELETON_OPP2] = skeletonDie[d2];
            canvas.drawBitmap(skeletonHistory[SKELETON_PLAYER1], playerDie1X, diceY, null);
            canvas.drawBitmap(skeletonHistory[SKELETON_PLAYER2], playerDie2X, diceY, null);
        }
        canvas.drawBitmap(bitmaps[d1], dice1X, diceY, null);
        canvas.drawBitmap(bitmaps[d2], dice2X , diceY, null);
    }

    private int getLargeDieIndex(int die) { return die + 6; }

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

   private Bitmap getPlayerChecker(Board b, boolean isOff) {
        if (board.getState(Board.COLOR) < 0) {
            return isOff ? brownStack : brownChecker;
        } else {
            return isOff ? whiteStack : whiteChecker;
        }
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
}
