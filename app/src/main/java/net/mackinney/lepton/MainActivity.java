package net.mackinney.lepton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The main activity for Lepton Backgammon, listener for GameHelper, manages all buttons.
 */
public class MainActivity extends AppCompatActivity implements GameHelperListener {
    private final String TAG = "MainActivity";
    GameHelper helper;
    private TextView consoleTextView;
    private EditText commandText;
    private BoardView boardView;
    private TextView oppScore;
    private TextView playerScore;
    private Preferences preferences;
    private ImageView splash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main); // must come BEFORE findViewById commands
        commandText = findViewById(R.id.commandText);
        consoleTextView = findViewById(R.id.console);
        consoleTextView.setMovementMethod(new ScrollingMovementMethod());
        consoleTextView.setTextColor(Color.BLACK);
        String consoleMessage = getString(R.string.client_name) + "\n" + getString(R.string.copyright) + "\n";
        consoleTextView.setText(consoleMessage);
        boardView = findViewById(R.id.boardView);
        oppScore = findViewById(R.id.oppScore);
        playerScore = findViewById(R.id.playerScore);
        helper = new GameHelper(this, this.getApplicationContext());
        boardView.initialize(helper);
        initPreferences();
        splash = findViewById(R.id.splash);
    }

    private void initPreferences() {
        preferences = new Preferences(this.getApplicationContext());
    }

    /**
     * Send a command to GameHelper for processing.
     * @param view The button that was tapped.
     */
    public void sendCommand(View view) {
        helper.addCommand(commandText.getText().toString());
        commandText.setText("");
    }

    /**
     * Toggle visibility of Board and Console.
     * The button name is whichever View is hidden.
     * @param view The button that was tapped.
     */
    public void showHideBoardView(View view) {
        Button btn = (Button) view;
        if (boardView.getVisibility() == View.VISIBLE) {
            splash.setVisibility(View.GONE);
            boardView.setVisibility(View.INVISIBLE);
            consoleTextView.setVisibility(View.VISIBLE);
            oppScore.setVisibility(View.INVISIBLE);
            playerScore.setVisibility(View.INVISIBLE);
            findViewById(R.id.send).setVisibility(View.VISIBLE);
            btn.setText(R.string.button_show);
        } else {
            updateGameBoard(null);
            boardView.setVisibility(View.VISIBLE);
            consoleTextView.setVisibility(View.INVISIBLE);
            oppScore.setVisibility(View.VISIBLE);
            playerScore.setVisibility(View.VISIBLE);
            findViewById(R.id.send).setVisibility(View.GONE);
            btn.setText(R.string.button_hide);
        }
    }

    /**
     * Invite another player to a match.
     * The dialog shows the name of a GammonBot that has recently been reported ready to play,
     * along with a match length value. The match length defaults to the previous value, or 5
     * by default. The player may edit either field.
     * @param view The button that was tapped.
     */
    public void invite(View view) {
        // create an alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.invite_title);
        // set the custom layout and customize it
        final View inviteLayout = getLayoutInflater().inflate(R.layout.invite, null); // use of null is not recommended, but I don't know what else to put here.
        ((EditText) inviteLayout.findViewById(R.id.opponent)).setText(helper.getReady());
        final int invitationLength = preferences.getInvitationLength();
        ((EditText) inviteLayout.findViewById(R.id.match_length)).setText(Integer.toString(invitationLength));
        builder.setView(inviteLayout);
        // add a button
        builder.setPositiveButton(R.string.button_invite, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // send data from the AlertDialog to the Activity
                String opponent = ((EditText) inviteLayout.findViewById(R.id.opponent)).getText().toString();
                String gameLength = ((EditText) inviteLayout.findViewById(R.id.match_length)).getText().toString();
                if (!"".equals(gameLength) && !gameLength.equals(Integer.toString(invitationLength))) { // if different int, save it
                    preferences.setInvitationLength(Integer.parseInt(gameLength));
                    preferences.commit();
                }
                helper.inviteOpponent(opponent, gameLength);
            }
        });
        builder.setNegativeButton(getString(R.string.button_cancel), null);
        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Offer to resign, either Normal, Gammon, or Backgammon.
     * The use of a custom AlertDialog with a list of choices modeled on examples found here:
     * https://medium.com/@suragch/adding-a-list-to-an-android-alertdialog-e13c1df6cf00
     * @param view The button that was tapped.
     */
    public void resign(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Resign the game?");
        String[] choices = {"Normal", "Gammon", "Backgammon"};
        final int[] checkedItem = {0}; // Normal is the default
        builder.setSingleChoiceItems(choices, checkedItem[0], new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkedItem[0] = which;
            }
        });
        // add Resign and Cancel buttons
        builder.setPositiveButton(R.string.button_resign, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handleResignation("Player", checkedItem[0]);
            }
        });
        builder.setNegativeButton(getString(R.string.button_cancel), null);
        // create and show the alert dialog
        builder.create().show();
    }

    /**
     * Toggle the setting of the FIBS autoroll feature and roll the dice.
     * @param view The button that was tapped.
     */
    public void toggleAutoroll(View view) {
        helper.toggleDouble();
    }

    /**
     * Login or Logout
     * If not logged in, the player is prompted to log in with the previous username and password
     * values, if available. Side effects: the splash screen is hidden and the Console is displayed.
     * If logged in, the player is logged out and the telnet session is
     * terminated.
     * @param view The button that was tapped.
     */
    public void loginLogout(View view) {
        Button btn = (Button) view;
        if ("Login".equals(btn.getText().toString())) {
            // create an alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Login Info");
            // Set the custom layout and customize it. Final because the procedure is to be called
            // later; the custom layout is external to the procedure and Java requires free variables
            // in lambdas to be constant.
            final View loginLayout = getLayoutInflater().inflate(R.layout.login, null);
            ((EditText) loginLayout.findViewById(R.id.userName)).setText(preferences.getUser());
            ((EditText) loginLayout.findViewById(R.id.password)).setText(preferences.getPassword());
            builder.setView(loginLayout);
            // add a button
            builder.setPositiveButton(getString(R.string.button_connect), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // send data from the AlertDialog to the Activity
                    EditText userName = loginLayout.findViewById(R.id.userName);
                    String name = userName.getText().toString();
                    EditText password = loginLayout.findViewById(R.id.password);
                    String pw = password.getText().toString();
                    updateLoginData(userName.getText().toString(), password.getText().toString());
                    helper.login(name, pw);
                }
            });
            builder.setNegativeButton(getString(R.string.button_cancel), null);
            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        } else { // btn.getText() == Logout
            helper.logout();
        }
    }

    /**
     * Accept a match challenge.
     * The player may edit the name, and if there is a challenge matching that name,
     * it will be accepted. The match length reflects the original invitation and
     * is not guaranteed to be accurate.
     * @param view The button that was tapped.
     */
    public void join(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.button_join));
        final View joinLayout = getLayoutInflater().inflate(R.layout.join, null);
        String[] challenger = helper.getChallenger();
        ((EditText) joinLayout.findViewById(R.id.opponent)).setText(challenger[0]);
        ((TextView) joinLayout.findViewById(R.id.match_length)).setText(challenger[1]);
        builder.setView(joinLayout);
        builder.setPositiveButton(R.string.button_join, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                EditText opponent = joinLayout.findViewById(R.id.opponent);
                String name = opponent.getText().toString();
                TextView matchLength = joinLayout.findViewById(R.id.match_length);
                String length = matchLength.getText().toString();
                helper.joinMatch(opponent.getText().toString());
            }
        });
        builder.setNegativeButton(getString(R.string.button_cancel), null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void updateLoginButton(final boolean isLoggedIn) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int visibility;
                if (isLoggedIn) {
                    ((Button) findViewById(R.id.loginLogout)).setText(R.string.button_logout);
                    visibility = View.VISIBLE;
                    Button showHide = ((Button) findViewById(R.id.showHide));
                    showHide.setText(R.string.button_hide);
                    showHideBoardView(showHide);
                } else {
                    ((Button) findViewById(R.id.loginLogout)).setText(R.string.button_login);
                    visibility = View.GONE;
                }
                findViewById(R.id.autoroll).setVisibility(visibility);
                findViewById(R.id.resign).setVisibility(visibility);
                findViewById(R.id.invite).setVisibility(visibility);
                findViewById(R.id.join).setVisibility(visibility);
            }
        });
    }

    private void updateLoginData(String username, String password) {
        preferences.setUser(username);
        preferences.setPassword(password);
        preferences.commit();
    }

    @Override
    public void updateGameBoard(final Board board) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.updateGameBoard(board);
            }
        });
    }

    @Override
    public void setScoreBoardMessage(final Board board) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (board != null) {
                    int player = board.getState(Board.SCORE_PLAYER);
                    int opponent = board.getState(Board.SCORE_OPPONENT);
                    int match = board.getState(Board.MATCH_LENGTH);
                    String isFinal = (opponent >= match || player >= match) ? " Final" : "";
                    String oppScoreText = board.getOppName() + "\n" + opponent
                            + "/" + match + isFinal;
                    oppScore.setText(oppScoreText);
                    String playerScoreText = helper.getPlayerName() + "\n" + board.getState(Board.SCORE_PLAYER)
                            + "/" + board.getState(Board.MATCH_LENGTH) + isFinal;
                    playerScore.setText(playerScoreText);
                    oppScore.setVisibility(View.VISIBLE);
                    oppScore.invalidate();
                    playerScore.setVisibility(View.VISIBLE);
                    playerScore.invalidate();
                } else {
                    oppScore.setVisibility(View.GONE);
                    playerScore.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void handleResignation(final String s, final int i) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.handleResignation(s, i);
            }
        });
    }

    @Override
    public void appendConsole(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                consoleTextView.append(s);
            }

        });
    }

    @Override
    public void newMove(final Board b) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.newMove(b);
            }
        });
    }

    @Override
    public void setPendingOffer(final int i) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.setPendingOffer(i);
            }
        });
    }

    @Override
    public void setPendingOffer(final int i, final int j) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.setPendingOffer(i, j);
            }
        });
    }

    @Override
    public void quit() {
        // ref: https://stackoverflow.com/questions/6330200/how-to-quit-android-application-programmatically/27765687#27765687
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.finishAndRemoveTask();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            this.finishAffinity();
        }
    }
}
