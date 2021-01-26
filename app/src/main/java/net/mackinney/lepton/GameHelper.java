package net.mackinney.lepton;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

import static java.lang.Thread.sleep;

/**
 * Communications center: passes messages to and from server, passes messages back to MainActivity.
 */
class GameHelper implements TelnetHandlerListener {

    private static final String TAG = "GameHelper";
    // types of FIBS messages http://www.fibs.com/fibs_interface.html#clip
//    private static final Integer NUM_CLIP_FIELDS  = 52;
    @VisibleForTesting
    private static final Integer CLIP_NULL = 0;  // guarrantees FIBS_IGNORE won't be empty
//    private static final Integer CLIP_WELCOME = 1;
//    private static final Integer CLIP_OWN_INFO = 2;
    @VisibleForTesting
    private static final Integer CLIP_MOTD = 3;
    @VisibleForTesting
    private static final Integer CLIP_MOTD_END = 4;
    @VisibleForTesting
    private static final Integer CLIP_WHO_INFO = 5; // 5 GammonBot 0 0 bla bla bla
    @VisibleForTesting
    private static final Integer CLIP_WHO_INFO_END = 6; // 6 end of who command
    @VisibleForTesting
    private static final Integer CLIP_LOGIN = 7;
    @VisibleForTesting
    private static final Integer CLIP_LOGOUT = 8;
//    private static final Integer CLIP_MESSAGE = 9;
//    private static final Integer CLIP_MESSAGE_DELIVERED = 10;
//    private static final Integer CLIP_MESSAGE_SAVED = 11;
//    private static final Integer CLIP_SAYS = 12;
//    private static final Integer CLIP_SHOUTS = 13;
//    private static final Integer CLIP_WHISPERS = 14;
//    private static final Integer CLIP_KIBITZES = 15;
//    private static final Integer CLIP_YOU_SAY = 16;
//    private static final Integer CLIP_YOU_SHOUT = 17;
//    private static final Integer CLIP_YOU_WHISPER = 18;
//    private static final Integer CLIP_YOU_KIBITZ = 19;
    @VisibleForTesting
    private static final String BOT_PATTERN = CLIP_WHO_INFO + " GammonBot";
    @VisibleForTesting
    private static final List<Integer> FIBS_IGNORE = new ArrayList<>(); // TODO refactor using Set
    private final List<String> commandList = new ArrayList<>();
    private TelnetHandler fibs;
    private Board board;
    private boolean moveHasBeenInitialized = false;
    private static final List<String> challengers = new ArrayList<>();
    private static final List<String> readyQueue = new ArrayList<>();
    private static final int MAX_READY_QUEUE = 5; // How many potential opponents to keep in queue
    GameHelperListener listener; // MainActivity is the View manager

    // Regex patterns
    // TODO run the 'toggle' command and make sure we're not ready for invites unless we want to be
    // TODO pending double and red X must be cleared before new game
    // TODO all of these settings need to be added to user-configurable preferences.
   /*
    > toggle
    The current settings are:
    allowpip        YES
    autoboard       YES
    autodouble      NO
    automove        NO
    bell            NO
    crawford        YES
    double          YES
    greedy          NO
    moreboards      YES
    moves           NO
    notify          YES
    ratings         YES
    ready           NO <-- 'toggle ready' flips the status
    report          NO
    silent          NO
    telnet          YES
    wrap            NO
    >
    */

    // allows filtering messages to console by CLIP_ fields set in FIBS_IGNORE
    @VisibleForTesting
    private static Pattern consoleSkip; // Can't be made final because compiling always yields a new Object.

    // resume
    //    You are now playing with user. Your running match was loaded.
    //    turn: user.
    //    match length: n
    //    points for user1: n
    //    points for user2: n
    @VisibleForTesting
    private static final Pattern resume = Pattern.compile("(Your running match was loaded.| Are you there\\?)");

    //    Nortally wants to play a 3 point match with you.  (Nortally, 3)
    // Type 'join Nortally' to accept.
    @VisibleForTesting
    private static final Pattern challenge = Pattern.compile("Type 'join ([a-zA-Z_]+)' to accept.");

    // > ** You are now playing a 3 point match with Nortally (3, Nortally)
    @VisibleForTesting
    private static final Pattern newMatch1 = Pattern.compile("You are now playing a (\\d+) point match with ([a-zA-Z_]+)");
    // ** Player Nortally has joined you for a 3 point match. (Nortally, 3)
    @VisibleForTesting
    private static final Pattern newMatch2 = Pattern.compile("Player ([a-zA-Z_]+) has joined you for a (\\d+) point match.");

    // Starting a new game with Nortally. (Nortally)
    @VisibleForTesting
    private static final Pattern start = Pattern.compile("Starting a new game with ([a-zA-Z_]+)\\.");

    // You rolled 4 6.
    // Nortally rolls 3 and 5.
    // You roll 1 and 3.
    @VisibleForTesting
    private static final Pattern playerRoll = Pattern.compile("You roll(ed)? (\\d)( and)? (\\d)\\.");

    // Please move 1 piece.
    // Please move 2 pieces.
    private static final Pattern movePrompt = Pattern.compile("^Please move (\\d) pieces?\\.m$");

    // Nortally moves 1-off .
    // Nortally moves bar-3 12-17 .
    // Nortally moves 7-5 7-5 7-5 .
    // Nortally moves 4-9 9-14 14-19 17-22 .
    @VisibleForTesting
    //private static final Pattern move = Pattern.compile("[a-zA-Z_]+ moves( (bar|\\d{1,2})-(\\d{1,2}|off)){1,4} \\.");
    // TODO can I split into groups for each value? Use string literal for ( [0-9bar]{1,3}-[0-9of]{1,3})
    private static final Pattern move = Pattern.compile(
            ".*moves( [0-9bar]{1,3}-[0-9of]{1,3})( [0-9bar]{1,3}-[0-9of]{1,3})?( [0-9bar]{1,3}-[0-9of]{1,3})?( [0-9bar]{1,3}-[0-9of]{1,3})? \\."
    );
    // Nortally doubles. Type 'accept' or 'reject'.
    // You double. Please wait for Nortally to accept or reject.
    @VisibleForTesting
    private static final Pattern doubles = Pattern.compile("([a-zA-Z_]+) (double)s?\\..*accept.*reject");

    // Nortally accepts the double. The cube shows 2.
    // You accept the double. The cube shows 2.
    @VisibleForTesting
    private static final Pattern doubleAccepted = Pattern.compile("[a-zA-Z_]+ accepts? the double. The cube shows \\d+\\.");

    // You give up. Nortally wins 3 points.
    @VisibleForTesting
    private static final Pattern doubleRejected = Pattern.compile("You give up. ([a-zA-Z_]+) wins (\\d+) points?\\.");

    // Nortally wants to resign. You will win 2 points. Type 'accept' or 'reject'."
    @VisibleForTesting
    private static final Pattern resignOffer = Pattern.compile("([a-zA-Z_]+) wants to resign\\. You will win (\\d+) points?");

    // You reject. The game continues.
    // Nortally rejects. The game continues.
    private static final Pattern resignationRejected = Pattern.compile("[a-zA-Z_]+ rejects?\\. The game continues\\.");

    // Nortally accepts and win 1 point.
    // Nortally accepts and wins 4 points.
    // You accept and win 1 point.
    // You accetp and win 3 points.
    @VisibleForTesting
    private static final Pattern resignationAccepted = Pattern.compile("([a-zA-Z_]+) accepts? and wins? (\\d+) points?\\.");

    // You win the 3 point match 4-0 .
    // GammonBot_XIX wins the 3 point match 6-1 .
    @VisibleForTesting
    private static final Pattern endOfMatch = Pattern.compile("([a-zA-Z_]+) wins? the (\\d) point match (\\d+)-(\\d+).");

    // You win the game and get 1 point
    // Nortally wins the game and gets 2 points. Sorry.
    private static final Pattern endOfGame = Pattern.compile("([a-zA-Z_]+) wins? the game and gets? (\\d+) point");


    // You can't move.
    // Nortally can't move.
    @VisibleForTesting
    private static final Pattern blocked = Pattern.compile("([a-zA-Z_]+) can't move.");

    // Type 'join' if you want to play the next game, type 'leave' if you don't.
    private static final Pattern joinNext = Pattern.compile("Type 'join' if you want to play the next game, type 'leave' if you don't.");

    private static final Pattern timeout = Pattern.compile("Connection timed out.");

    // Special cases http://www.fibs.com/fibs_interface.html#play_state_spanner where 2 lines are catenated.
    // The pattern needs to break the line at the missing EOL.
    private static final Pattern spanner1 = Pattern.compile("^(board\\S*)\\s(.*)");
    private static final Pattern spanner2 = Pattern.compile("^(.*shows \\d{1,2}\\.)(.+)");
    private static final String fibsCodes = "^\\d{1,2}\\s?";
    private static final Pattern[] spanner = new Pattern[] {spanner1, spanner2};


    private static final String DEFAULT_BOARD = "board:You:Opponent:0:0:0:0:-2:0:0:0:0:5:0:3:0:0:0:-5:5:0:0:0:-3:0:-5:0:0:0:0:2:0:0:0:0:0:0:1:1:1:0:1:-1:0:25:0:0:0:0:2:5:0:0";

    // for Dialogs
    private Context context;

    /**
     * Controller class for the MVC model. Fields parsed FIBS output to direct BoardView, and User
     * inputs to direct TelnetHandler.
     * @param listener
     * @param context
     */
    GameHelper(GameHelperListener listener, Context context) {
        this.listener = listener;
        this.context = context;
        fibs = new TelnetHandler(this, context);
        board = new Board();
        board.setBoard(DEFAULT_BOARD);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        for (Integer item : new Integer[]{CLIP_MOTD_END, CLIP_WHO_INFO, CLIP_WHO_INFO_END, CLIP_LOGIN, CLIP_LOGOUT}) {
            FIBS_IGNORE.add(item);
        }
        consoleSkip = updateConsoleSkip(FIBS_IGNORE);
    }


    // addCommand & readCommand may be called from different threads, must be synchronized
    @Override
    public synchronized void addCommand(String cmd) {
        if ("join".equals(cmd)) {
            listener.setPendingOffer(BoardView.NONE);
            if (!challengers.isEmpty()) {
                cmd += " " + challengers.get(challengers.size() - 1); // For now, most recent challenger. TODO dialog to let user pick
            }
            challengers.clear();
        } else if (cmd.startsWith("who") || cmd.startsWith("rawwho")) {
            FIBS_IGNORE.remove(CLIP_WHO_INFO);
            consoleSkip = updateConsoleSkip(FIBS_IGNORE);
        }
        commandList.add(cmd);
    }

    @Override
    public synchronized String readCommand() {
        try {
            if (!commandList.isEmpty()) {
                return commandList.remove(0);
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, e.toString());
        }
        return ""; // empty list
    }

    @Override
    public void parse(String line) {
        // special case all board status updates
        if (line.startsWith("board:")) {
            // handle case #1 http://www.fibs.com/fibs_interface.html#play_state_spanner
            int spaceTest = line.indexOf(" ");
            if (spaceTest > 0) {
                parse(line.substring(0, spaceTest));
                parse(line.substring(spaceTest + 1));
            }
            if (!board.getLastLine().equals(line)) { // skip if board hasn't changed
                board.setBoard(line);
                if (board.isPlayerTurn()) {             // initialize a new move
                    if (!moveHasBeenInitialized) {
                        listener.newMove(board);
                        moveHasBeenInitialized = true;
                    }
                }
                // TODO if terms.size() > NUM_CLIP_FIELDS
                listener.updateGameBoard(board);
                if (!board.isPlayerTurn() && board.getState(Board.DICE_OPP1) > 0)
                    try {
                        sleep(1200);
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getMessage());
                    }
            }
            return;
        }

        //User status field meanings:
        //  code name opponent watching ready away rating experience idle login hostname client email
        if (line.startsWith(BOT_PATTERN)) {
            String[] words = line.split(" ");
            if (words[3].equals("-") && words[4].equals("1")) { // player is ready
                readyQueue.add(words[1]);
                if (readyQueue.size() > MAX_READY_QUEUE) { // only keep 5 in the queue
                    readyQueue.remove(0);
                }
            }
        }

        // if WHO info is not currently being skipped, start skipping it again
        if (line.equals(CLIP_WHO_INFO_END.toString()) && !FIBS_IGNORE.contains(CLIP_WHO_INFO)) {
            disableWhoOutput();
        }
        // use FIBS_IGNORE to skip lines
        Matcher match = consoleSkip.matcher(line);
        if (match.find()) {
            //Log.i(TAG, "skipping: " + line);
            return;
        }
        // strip FIBS codes if any and print to console
        line = line.replaceFirst(fibsCodes,"");
        if (!line.isEmpty()) {
            appendConsole(line);
        }

        for (Pattern pattern: spanner) {
            match.usePattern(pattern);
            if (match.find()) {
                parse(match.group(1));
                parse(match.group(2));
                return;
            }
        }
        match.usePattern(challenge);
        if (match.find()) {
            addChallenger(match.group(1));
            return;
        }

        match.usePattern(newMatch1);
        if (match.find()) {
            newMatch(match.group(2), match.group(1));
            board.setGameOver(false);
            return;
        }

        match.usePattern(newMatch2);
        if (match.find()) {
            newMatch(match.group(1), match.group(2));
            board.setGameOver(false);
            return;
        }

        match.usePattern(resume);
        if (match.find()) {
            addCommand("board");
            board.setGameOver(false);
            return;
        }

        match.usePattern(start);
        if (match.find()) {
            moveHasBeenInitialized = false;
            board.setGameOver(false);
            return;
        }

        match.usePattern(playerRoll);
        if (match.find()) {
            if (!"You".equals(match.group(1))) {
                moveHasBeenInitialized = false;
            }
            return;
        }

        match.usePattern(doubleAccepted); // = Pattern.compile("[a-zA-Z_]+ accepts the double. The cube shows (\\d{1,2})\\.");
        if (match.find()) {
            listener.setPendingOffer(BoardView.NONE);
            return;
        }

        match.usePattern(doubles);
        if (match.find()) {
            listener.setPendingOffer(BoardView.DOUBLE_OFFER);
            addCommand("board");
            return;
        }

        match.usePattern(doubleRejected);
        if (match.find()) {
            listener.setPendingOffer(BoardView.NONE);
            updateGameBoard(board);
            return;
        }
        
        match.usePattern(resignOffer);
        if (match.find() && !"You".equals(match.group(1))) {
            listener.setPendingOffer(BoardView.RESIGN_OFFER, Integer.parseInt(match.group(2))/board.getState(Board.CUBE));
            updateGameBoard(board);
            return;
        }

        match.usePattern(resignationRejected);
        if (match.find()) {
            listener.setPendingOffer(BoardView.NONE);
            updateGameBoard(board);
        }

        //      Nortally accepts and wins 4 points. winner/points
        match.usePattern(resignationAccepted);
        if (match.find()) {
            finishGame(match.group(1), Integer.parseInt(match.group(2)));
            return;
        }

        match.usePattern(endOfMatch); // winner, match length, winner score, loser score
        if (match.find()) {
            for (int ix = 0; ix <= 4; ix++) {
                try {
                    String value = match.group(ix);
                } catch (Exception e) {
                    Log.e(TAG, ix + e.getMessage());
                }
            }
            if ("You".equals(match.group(1))) {
                board.setState(Board.SCORE_PLAYER, Integer.parseInt(match.group(3)));
                board.setState(Board.SCORE_OPPONENT, Integer.parseInt(match.group(4)));
            } else {
                board.setState(Board.SCORE_PLAYER, Integer.parseInt(match.group(4)));
                board.setState(Board.SCORE_OPPONENT, Integer.parseInt(match.group(3)));
            }
            listener.setScoreBoardMessage(board);
            updateGameBoard(board);
            return;
        }

        // "([a-zA-Z_]+) wins? the game and gets? (\\d+) point"
        match.usePattern(endOfGame);
        if (match.find()) {
            finishGame(match.group(1), Integer.parseInt(match.group(2)));
            return;
        }

        match.usePattern(joinNext);
        if (match.find()) {
            challengers.clear();
            addCommand("join");
            return;
        }

        match.usePattern(blocked);
        if (match.find()) {
            addCommand("board");
            return;
        }

        match.usePattern(timeout);
        if (match.find()) {
            addCommand("bye");
        }
    }

    // for resignationAccepted and winGame
    private void finishGame(String winner, int points) {
        addCommand("oldmoves ");
        listener.setPendingOffer(BoardView.NONE);
        board.setGameOver(true);
        //listener.newMove(board);
        listener.setScoreBoardMessage(board); // This is where the score can be FINAL
    }

    @Override
    public void appendConsole(String line) {
        // LogCat gets everything
        Log.i(TAG, line);
            if (!line.isEmpty() && !consoleSkip.matcher(line).find()) {
                listener.appendConsole(line + "\n");
            }
    }

    /**
     * Return the list of players accepting invitations.
     */
    String getReady() {
        if (readyQueue.isEmpty()) {
            addCommand("rawwho ready");
            return "Looking for opponent";
        } else {
            int i = readyQueue.size() - 1;
            return readyQueue.remove(i);
        }
    }

    private void newMatch(String opp, String matchLength) {
        //board = new Board(MY_USER, opponent, Integer.parseInt(matchLength));
        //opponent = opp;
        // TODO implement dialog-based invitations/responses to invitations
    }

    private void updateGameBoard(Board b) {
        listener.updateGameBoard(b);
    }

    Board getBoard() {
        return board;
    }

    private void challenge(String player, String matchLength) {
        addChallenger(player);
    }

    private List getChallengers() {
        return challengers;
    }

    private void addChallenger(String s) {
        challengers.add(s);
    }

    private void clearChallengers() {
        challengers.clear();
    }

    String getPlayerName() {
        return fibs.getMyUser();
    }

    void roll() { // e.g., roll if appropriate
        if (board.isPlayerTurn() && !board.playerHasRolled()) {
            addCommand("roll");
        }
    }

    @Override
    public void updateLoginButton(boolean b) {
        listener.updateLoginButton(b);
    }

    @Override
    public void quit() {
        listener.quit();
    }

    @VisibleForTesting
    private static Pattern updateConsoleSkip(List<Integer> list) {
        StringBuilder stringBuilder = new StringBuilder("^(" + CLIP_NULL + "|"); // ensure pattern not empty
        for (Integer item : list) {
            stringBuilder.append(item + "|");
        }
        stringBuilder.replace(stringBuilder.lastIndexOf("|"), stringBuilder.length(), ")\\s?");
        return Pattern.compile(stringBuilder.toString());
    }

    /**
     * Allow player status information to be sent to the console.
     */
    @VisibleForTesting
    public void enableWhoOutput() {
        FIBS_IGNORE.remove(CLIP_WHO_INFO);
        consoleSkip = updateConsoleSkip(FIBS_IGNORE);
    }

    /**
     * Prevent player status information from being sent to the console.
     */
    @VisibleForTesting
    public void disableWhoOutput() {
        FIBS_IGNORE.add(CLIP_WHO_INFO);
        consoleSkip = updateConsoleSkip(FIBS_IGNORE);
    }

}
