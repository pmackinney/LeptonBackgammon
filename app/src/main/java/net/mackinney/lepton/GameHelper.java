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
    static final Integer CLIP_OWN_INFO = 2; // "2 player 1 1 0 0 0 1 1 1 4899 0 1 0 1 1496.40 1 1 0 0 0 America/Los_Angeles"

    //    @VisibleForTesting
//    private static final Integer CLIP_MOTD = 3;
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

    private static final int WHO_OPPONENT_KEY =2;
    private static final int WHO_READY_KEY = 4;
    private static final String WHO_OPPONENT_NONE = "-";
    private static final String WHO_READY_TRUE = "1";


    // Lists we keep manage
    private final List<String> COMMAND_LIST = new ArrayList<>();
    private static final List<String> CHALLENGERS = new ArrayList<>();
    private static final List<String> READY_QUEUE = new ArrayList<>();
    private static final List<Integer> FIBS_IGNORE = new ArrayList<>(); // TODO refactor using Set

    // constants for the challengers list
    private static final char CHALLENGE_SEP = ':';
    private static final int MAX_READY_QUEUE = 5; // How many challengers to keep in queue

    private static final String DEFAULT_PLAYER_NAME = "You"; // FIBS Player 1 field when playing

    // Classes we talk to
    GameHelperListener listener; // MainActivity is the View manager
    private TelnetHandler fibs;
    private Board board;

    // Flag for the Move object
    private boolean moveHasBeenInitialized = false;

    // Commands
    private static final String CMD_LOGOUT = "bye"; // telnet disconnect request

    /* REGEX PATTERNS */

    // Regex pattern for loading the readyQueue list
    private static final String BOT_PATTERN = CLIP_WHO_INFO + " GammonBot";
    
    // allows filtering messages to console by CLIP_ fields set in FIBS_IGNORE
    @VisibleForTesting
    static Pattern consoleSkip; // Can't be made final because compiling always yields a new Object.
    
    // resume
    //    You are now playing with user. Your running match was loaded.
    //    turn: user.
    //    match length: n
    //    points for user1: n
    //    points for user2: n
    @VisibleForTesting
    static final Pattern RESUME = Pattern.compile("([a-zA-Z_]+) has joined you. Your running match was loaded.");
    // | Are you there\?)

    //    Nortally wants to play a 3 point match with you.  (Nortally, 3)
    // Type 'join Nortally' to accept.
    @VisibleForTesting
    static final Pattern CHALLENGE = Pattern.compile("([a-zA-Z_]+) wants to play a (\\d+) point match with you.");
//    private static final Pattern challenge = Pattern.compile("Type 'join ([a-zA-Z_]+)' to accept.");

    // > ** You are now playing a 3 point match with Nortally (3, Nortally)
    @VisibleForTesting
    static final Pattern NEW_MATCH_1 = Pattern.compile("You are now playing a (\\d+) point match with ([a-zA-Z_]+)");
    // ** Player Nortally has joined you for a 3 point match. (Nortally, 3)
    @VisibleForTesting
    static final Pattern NEW_MATCH_2 = Pattern.compile("Player ([a-zA-Z_]+) has joined you for a (\\d+) point match.");

    // Starting a new game with Nortally. (Nortally)
    @VisibleForTesting
    static final Pattern START = Pattern.compile("Starting a new game with ([a-zA-Z_]+)\\.");

    // You rolled 4 6.
    // Nortally rolls 3 and 5.
    // You roll 1 and 3.
    @VisibleForTesting
    static final Pattern PLAYER_ROLL = Pattern.compile("You roll(ed)? (\\d)( and)? (\\d)\\.");

    // Please move 1 piece.
    // Please move 2 pieces.
    static final Pattern MOVE_PROMPT = Pattern.compile("^Please move (\\d) pieces?\\.m$");

    // Nortally moves 1-off .
    // Nortally moves bar-3 12-17 .
    // Nortally moves 7-5 7-5 7-5 .
    // Nortally moves 4-9 9-14 14-19 17-22 .
    @VisibleForTesting
    //private static final Pattern move = Pattern.compile("[a-zA-Z_]+ moves( (bar|\\d{1,2})-(\\d{1,2}|off)){1,4} \\.");
    // TODO can I split into groups for each value? Use string literal for ( [0-9bar]{1,3}-[0-9of]{1,3})
    static final Pattern MOVE = Pattern.compile(
            ".*moves( [0-9bar]{1,3}-[0-9of]{1,3})( [0-9bar]{1,3}-[0-9of]{1,3})?( [0-9bar]{1,3}-[0-9of]{1,3})?( [0-9bar]{1,3}-[0-9of]{1,3})? \\."
    );
    // Nortally doubles. Type 'accept' or 'reject'.
    // You double. Please wait for Nortally to accept or reject.
    @VisibleForTesting
    static final Pattern DOUBLES = Pattern.compile("([a-zA-Z_]+) (double)s?\\..*accept.*reject");

    // Nortally accepts the double. The cube shows 2.
    // You accept the double. The cube shows 2.
    @VisibleForTesting
    static final Pattern DOUBLE_ACCEPTED = Pattern.compile("[a-zA-Z_]+ accepts? the double. The cube shows \\d+\\.");

    // You give up. Nortally wins 3 points.
    @VisibleForTesting
    static final Pattern DOUBLE_REJECTED = Pattern.compile("You give up. ([a-zA-Z_]+) wins (\\d+) points?\\.");

    // Nortally wants to resign. You will win 2 points. Type 'accept' or 'reject'."
    @VisibleForTesting
    static final Pattern RESIGN_OFFER = Pattern.compile("([a-zA-Z_]+) wants to resign\\. You will win (\\d+) points?");

    // You reject. The game continues.
    // Nortally rejects. The game continues.
    static final Pattern RESIGNATION_REJECTED = Pattern.compile("[a-zA-Z_]+ rejects?\\. The game continues\\.");

    // Nortally accepts and win 1 point.
    // Nortally accepts and wins 4 points.
    // You accept and win 1 point.
    // You accetp and win 3 points.
    @VisibleForTesting
    static final Pattern RESIGNATION_ACCEPTED = Pattern.compile("([a-zA-Z_]+) accepts? and wins? (\\d+) points?\\.");

    // You win the 3 point match 4-0 .
    // GammonBot_XIX wins the 3 point match 6-1 .
    @VisibleForTesting
    static final Pattern END_OF_MATCH = Pattern.compile("([a-zA-Z_]+) wins? the (\\d) point match (\\d+)-(\\d+).");

    // You win the game and get 1 point
    // Nortally wins the game and gets 2 points. Sorry.
    static final Pattern END_OF_GAME = Pattern.compile("([a-zA-Z_]+) wins? the game and gets? (\\d+) point");

    // You can't move.
    // Nortally can't move.
    @VisibleForTesting
    static final Pattern BLOCKED = Pattern.compile("([a-zA-Z_]+) can't move.");

    // Type 'join' if you want to play the next game, type 'leave' if you don't.
    static final Pattern JOIN_NEXT = Pattern.compile("Type 'join' if you want to play the next game, type 'leave' if you don't.");

    static final Pattern TIMEOUT = Pattern.compile("Connection timed out.");

//    static final Pattern SAYS = Pattern.compile("^12\\s(.*)");
//    static final Pattern SHOUTS = Pattern.compile("^13\\s(.*)");
//    static final Pattern KIBITZES = Pattern.compile("^14\\s(.*)");
//    static final Pattern WHISPERS = Pattern.compile("^15\\s(.*)");
    static final Pattern MESSAGE_TO_TOAST = Pattern.compile("^1[245]\\s(.*)");

    // Special cases http://www.fibs.com/fibs_interface.html#play_state_spanner where 2 lines are catenated.
    // The pattern needs to break the line at the missing EOL.
    static final Pattern SPANNER_1 = Pattern.compile("^(board\\S*)\\s(.*)");
    static final Pattern SPANNER_2 = Pattern.compile("^(.*shows \\d{1,2}\\.)(.+)");
    static final String FIBS_CODES = "^\\d{1,2}\\s?";
    static final Pattern[] SPANNER = new Pattern[] {SPANNER_1, SPANNER_2};

    private static final String LEPTON = "set lepton_";
    private static final int LEPTON_START = 4;

    // addCommand regex strings
    static final String JOIN = "^join";
    static final String WHO = "^(raw)?who";

    static final String DEFAULT_BOARD = "board:You:Opponent:0:0:0:0:-2:0:0:0:0:5:0:3:0:0:0:-5:5:0:0:0:-3:0:-5:0:0:0:0:2:0:0:0:0:0:0:1:1:1:0:1:-1:0:25:0:0:0:0:2:5:0:0";

    // for Dialogs
    private Context context;

    /**
     * Controller class for the MVC model. Fields parsed FIBS output to direct BoardView, and User
     * inputs to direct TelnetHandler.
     * @param listener MainActivity is the only class implementing this interface
     * @param context The app context, so we can get Resources
     */
    GameHelper(GameHelperListener listener, Context context) {
        this.listener = listener;
        this.context = context;
        fibs = new TelnetHandler(this, context);
        board = new Board();
        //board.setBoard(DEFAULT_BOARD);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        for (Integer item : new Integer[]{CLIP_MOTD_END, CLIP_WHO_INFO, CLIP_WHO_INFO_END, CLIP_LOGIN, CLIP_LOGOUT}) {
            FIBS_IGNORE.add(item);
        }
        consoleSkip = updateConsoleSkip(FIBS_IGNORE);
    }


    // addCommand & readCommand may be called from different threads, must be synchronized
    @Override
    public synchronized void addCommand(String command) {
        if (command.equals("demo")) {
            parse("demo");
        }
        if (command.startsWith(LEPTON)) {
            listener.setLepton(command.substring((LEPTON_START)));
        } else {
            if (command.matches(JOIN)) {
                listener.setPendingOffer(BoardView.NONE);
                CHALLENGERS.clear();
            } else {
                Log.i(TAG, "command: " + command);
                Pattern who = Pattern.compile(WHO);
                Matcher m = who.matcher(command);
                if (m.find()) {
                    enableWhoOutput(); // normally disabled
                    FIBS_IGNORE.remove(CLIP_WHO_INFO);
                    consoleSkip = updateConsoleSkip(FIBS_IGNORE);
                }
            }
            COMMAND_LIST.add(command);
        }
    }

    @Override
    public synchronized String readCommand() {
        try {
            if (!COMMAND_LIST.isEmpty()) {
                return COMMAND_LIST.remove(0);
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, e.toString());
        }
        return ""; // empty list
    }

    @Override
    public void parse(String line) {
        // special case all board status updates
        if (line.equals("demo")) {
            parse("board:You:GammonBot_XII:5:0:2:1:0:0:0:0:0:8:-2:0:0:1:0:0:4:0:0:0:0:0:-13:0:0:1:0:1:-3:-1:2:6:0:0:2:0:1:0:-1:1:25:0:15:15:0:0:2:0:0:0");
        }
        if (line.startsWith("board:")) {
            // handle case #1 http://www.fibs.com/fibs_interface.html#play_state_spanner
            int spaceTest = line.indexOf(" ");
            if (spaceTest > 0) {
                parse(line.substring(0, spaceTest));
                parse(line.substring(spaceTest + 1));
            }
            if (!line.equals(board.getLastLine())) { // skip if board hasn't changed
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

        if (line.startsWith(CLIP_OWN_INFO + " ")) {
            updateSettings(line);
        }
        //User status field meanings (ref: FIBS CLIP doc)
        //  code name opponent watching ready away rating experience idle login hostname client email
        if (line.startsWith(BOT_PATTERN)) {
            String[] words = line.split(" ");
            if (words.length >= WHO_READY_KEY
                    && words[WHO_OPPONENT_KEY].equals(WHO_OPPONENT_NONE)
                    && words[WHO_READY_KEY].equals(WHO_READY_TRUE)) { // player is ready
                READY_QUEUE.add(words[1]);
                if (READY_QUEUE.size() > MAX_READY_QUEUE) { // only keep 5 in the queue
                    READY_QUEUE.remove(0);
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
        line = line.replaceFirst(FIBS_CODES,"");
        if (!line.isEmpty()) {
            appendConsole(line);
        }

        for (Pattern pattern: SPANNER) {
            match.usePattern(pattern);
            if (match.find()) {
                parse(match.group(1));
                parse(match.group(2));
                return;
            }
        }
        match.usePattern(CHALLENGE);
        if (match.find()) {
            addChallenger(match.group(1) + CHALLENGE_SEP + match.group(2));
            return;
        }

        match.usePattern(NEW_MATCH_1);
        if (match.find()) {
            addCommand("board");
            //board.setGameOver(false);
            return;
        }

        match.usePattern(NEW_MATCH_2);
        if (match.find()) {
            addCommand("board");
            //board.setGameOver(false);
            return;
        }

        match.usePattern(RESUME);
        if (match.find()) {
            addCommand("board");
            //board.setGameOver(false);
            return;
        }

        match.usePattern(START);
        if (match.find()) {
            addCommand("board");
            moveHasBeenInitialized = false;
            //board.setGameOver(false);
            return;
        }

        match.usePattern(PLAYER_ROLL);
        if (match.find()) {
            if (!"You".equals(match.group(1))) {
                moveHasBeenInitialized = false;
            }
            return;
        }

        match.usePattern(DOUBLE_ACCEPTED); // = Pattern.compile("[a-zA-Z_]+ accepts the double. The cube shows (\\d{1,2})\\.");
        if (match.find()) {
            listener.setPendingOffer(BoardView.NONE);
            return;
        }

        match.usePattern(DOUBLES);
        if (match.find()) {
            listener.setPendingOffer(BoardView.DOUBLE_OFFER);
            addCommand("board");
            return;
        }

        match.usePattern(DOUBLE_REJECTED);
        if (match.find()) {
            finishGame(match.group(1));
            return;
        }
        
        match.usePattern(RESIGN_OFFER);
        if (match.find() && !"You".equals(match.group(1))) {
            listener.setPendingOffer(BoardView.RESIGN_OFFER, Integer.parseInt(match.group(2))/board.getState(Board.CUBE));
            updateGameBoard(board);
            return;
        }

        match.usePattern(RESIGNATION_REJECTED);
        if (match.find()) {
            listener.setPendingOffer(BoardView.NONE);
            updateGameBoard(board);
        }

        //      Nortally accepts and wins 4 points. winner/points
        match.usePattern(RESIGNATION_ACCEPTED);
        if (match.find()) {
            finishGame(match.group(1));
            return;
        }

        match.usePattern(END_OF_MATCH); // winner, match length, winner score, loser score
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
        match.usePattern(END_OF_GAME);
        if (match.find()) {
            finishGame(match.group(1));
            return;
        }

        match.usePattern(JOIN_NEXT);
        if (match.find()) {
            CHALLENGERS.clear();
            addCommand("join");
            return;
        }

        match.usePattern(BLOCKED);
        if (match.find()) {
            addCommand("board");
            return;
        }

        match.usePattern(MESSAGE_TO_TOAST);
        if (match.find()) {
            listener.toast(match.group(1));
        }

        match.usePattern(TIMEOUT);
        if (match.find()) {
            addCommand("bye");
        }
    }

    // for resignationAccepted and winGame
    private void finishGame(String opponent) {
        listener.setPendingOffer(BoardView.NONE);
        board.setGameOver();
        listener.setScoreBoardMessage(board); // This is where the score can be FINAL
        updateGameBoard(board);
    }

    @Override
    public void appendConsole(String line) {
        if (!line.isEmpty() && !consoleSkip.matcher(line).find()) {
            listener.appendConsole(line + "\n");
        }
    }

    /**
     * Return the list of players accepting invitations.
     */
    String getReady() {
        if (READY_QUEUE.isEmpty()) {
            addCommand("rawwho ready");
            return "Looking for opponent";
        } else {
            int i = READY_QUEUE.size() - 1;
            return READY_QUEUE.remove(i);
        }
    }

    private void updateGameBoard(Board b) {
        listener.updateGameBoard(b);
    }

    Board getBoard() {
        return board;
    }

//    private void challenge(String player, String matchLength) {
//        addChallenger(player);
//    }

    private List getChallengers() {
        return CHALLENGERS;
    }

    String[] getChallenger() {
        int ix = CHALLENGERS.size();
        String[] result = new String[]{"",""};
        if (ix > 0) {
            String challenger = CHALLENGERS.remove(ix - 1);
            int sep = challenger.indexOf(CHALLENGE_SEP);
            result[0] = challenger.substring(0, sep);
            result[1] = challenger.substring(sep + 1);
        }
        return result;
    }

    private void addChallenger(String s) {
        CHALLENGERS.add(s);
    }

    private void clearChallengers() {
        CHALLENGERS.clear();
    }

    String getPlayerName() {
        if (board.getPlayerName().equals(DEFAULT_PLAYER_NAME)) {
            return fibs.getMyUser();
        } else {
            return board.getPlayerName();
        }
    }

    void roll() { // e.g., roll if appropriate
        if (board.isPlayerTurn() && !board.playerHasRolled()) {
            addCommand("roll");
        }
    }

    @Override
    public void updateLoginButton(boolean b) { listener.updateLoginButton(b); }

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

    void toggleDouble() {
        addCommand(context.getString(R.string.fibs_cmd_double));
        addCommand(context.getString(R.string.fibs_cmd_roll));
    }

    void inviteOpponent(String opponent, String gameLength) {
        addCommand(context.getString(R.string.button_invite) + " " + opponent + " " + gameLength);
    }

    void joinMatch(String challenger) {
        addCommand("join " + challenger);
        clearChallengers();
    }

    void logout() { addCommand(CMD_LOGOUT); }

    void login(String name, String pw) { addCommand("connect " + name + " " + pw); }

    void updateSettings(String clip_own_info) {
        for (Object setting : Preferences.getSettings(clip_own_info)) {
            addCommand((String)setting);
        }
    }
}
