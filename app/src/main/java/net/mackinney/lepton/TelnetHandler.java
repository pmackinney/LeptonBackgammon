package net.mackinney.lepton;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.commons.net.telnet.TelnetClient;
import static java.lang.Thread.sleep;

/**
 * Handles all communication with FIBS in a seperate thread
 */
class TelnetHandler implements Runnable {
    private static final String TAG = "TelnetHandler";
    private String myUser = "";
    private TelnetClient client; // org.apache.commons.net.telnet.TelnetClient
    private BufferedReader in;
    private static final int SERVER_CHAR_SIZE = 512; // optimum size?
    private OutputStreamWriter out;
    private static final int MAX_WAIT = 30000; // milliseconds, e.g., 30 seconds
    private static String consoleMessage;
    private static final String EOL = "\r\n"; // End Of Line, specified in FIBS CLIP
    private Context context;
    private SharedPreferences preferences;
    private SharedPreferences.Editor preferencesEditor;
    private TelnetHandlerListener helper; // GameHelper

    TelnetHandler(TelnetHandlerListener helper, Context context) {
        this.helper = helper;
        this.context = context;
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(this);
    }

    /**
     * Lepton Backgammon's main execution loop.
     */
    @Override
    public void run() {
        try {
            StringBuilder serverOutput = new StringBuilder();
            long startTime = System.currentTimeMillis();
            String command;
            String partial = "";
            do {
                // Main loop A: check for commands
                command = helper.readCommand();
                if (command.length() > 0) {
                    if (command.startsWith("connect ")) { // connect user pw
                        String[] args = command.split(" ");
                        if (args.length == 3) {
                            myUser = args[1]; // store player's handle for reference
                            connect(args[1], args[2], serverOutput);
                        }
                    } else if ("bye".equalsIgnoreCase(command)) { // standard telnet session exit.
                        client.disconnect();
                        helper.updateLoginButton(false);
                    } else {
                        if (command.matches("^(raw)?who\\s")) {
                            helper.enableWhoOutput(); // normally disabled
                        }
                        write(out, command);
                    }
                }
                // Main loop B: sending server output to the parser
                if (client != null && client.isConnected()) {
                    serverOutput.append(partial).append(read());                                        // prepend partial to output
                    if (serverOutput.lastIndexOf(EOL) >= 2                                              // if string contains end of line
                            && !serverOutput.substring(serverOutput.length() - 2).equals(EOL)) {  //     and EOL not at end of string
                        partial = serverOutput.substring(serverOutput.lastIndexOf(EOL) + 1);      // save new partial
                    } else {
                        partial = "";
                    }
                    if (serverOutput.length() > 0) {
                        String[] lines = serverOutput.toString().split("[" + EOL + "]+"); // eliminates most empty lines
                        for (String line : lines) {
                            //Log.i(TAG, line);
                            if (line.length() > 0) {
                                helper.parse(line);
                            }
                        }
                        serverOutput.setLength(0);                // clear the buffer
                    } else {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - startTime > MAX_WAIT) {
                            startTime = System.currentTimeMillis(); // reset timer
                        }
                        sleep(MAX_WAIT / 30); // 1 second
                    }
                }
            } while (!"quit".equals(command));
            helper.quit(); // quit Lepton Backgammon
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Create a telnet connection, storing output in the passed-in StringBuilder
     */
    void connect(String user, String password, StringBuilder serverOutput) {
        try {
             if (client == null) {
                 client = new TelnetClient();
             } else if (client.isConnected()) {
                 client.disconnect();
             }
            client.connect(Preferences.getServer(), Preferences.getPort());
            this.in = new BufferedReader(new InputStreamReader((client.getInputStream())));
            this.out = new OutputStreamWriter(client.getOutputStream());
            // Typical CLIP login: response
            // 1 darth 1592540685 c-67-180-159-87.hsd1.ca.comcast.net
            // 2 darth 1 1 0 0 0 0 1 1 51 0 1 0 1 1496.12 0 0 0 0 0 America/Los_Angeles
            //
            // Login failure is indicated by another login: prompt with no following EOL
            if (readUntil("login: ", serverOutput)) {
                String loginCommand = "login" + " " + context.getString(R.string.client_name) + " " + Preferences.getClipVersion() + " " + user + " " + password;
                write(out, loginCommand);
                if (readUntil(1 + " " + user + " ", serverOutput)) {
                    for (String setting : Settings.getSettings()) {
                        write(out, setting + "\n");
                    }
                    helper.updateLoginButton(true);
                } else {
                    helper.updateLoginButton(false);
                    helper.appendConsole("Could not log in with supplied credentials.");
                }
            } else {
                Log.e(TAG, "Timeout waiting for login: prompt");
            }
        } catch (SocketException e) {
            Log.e(TAG, "SocketException " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "IOException " + e.toString());
        }
    }

    private boolean readUntil(String string, StringBuilder stringBuilder) {
        return readUntil(string, stringBuilder, MAX_WAIT);
    }

    /**
     * @return True if the server output contains the desired string, false if timeout is reached
     * before the string appears.
     */
    private boolean readUntil(String string, StringBuilder stringBuilder, long maxwait) {
        long timer = System.currentTimeMillis();
        try {
            do {
                if (in.ready()) {
                    int newChar = in.read();
                    if (newChar > -1) {
                        stringBuilder.append((char) newChar);
                        timer = System.currentTimeMillis();
                    }
                } else if (System.currentTimeMillis() > timer + maxwait) {
                    Log.e(TAG, "readUntil timed out looking for " + string);
                    return false;
                } else {
                    sleep(200); // 0.2 seconds
                }
            } while (stringBuilder.indexOf(string) < 0);
        } catch (IOException e) {
            Log.e(TAG, "IOException " + e.toString());
            return false;
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException " + e.toString());
            return false;
        }
        return true;
    }

    private String read() {
        char[] serverChars = new char[SERVER_CHAR_SIZE];
        StringBuilder buf = new StringBuilder();
        try {
            while (in.ready()) {
                int serverCharsLength = in.read(serverChars, 0, SERVER_CHAR_SIZE);
                if (serverCharsLength > 0) {
                    buf.append(Arrays.copyOfRange(serverChars, 0, serverCharsLength));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException " + e.toString());
        }
        // just in case read() ended on first char of the 2-char EOL sequence [\r\n]
        if (buf.length() > 0) {
            if (buf.charAt(buf.length() - 1) == EOL.charAt(0)) { // complete the EOL at end of buf
                buf.append(EOL.charAt(1));
            }
            if (buf.charAt(0) == EOL.charAt(1)) { // discard partial EOL at start of buf
                buf.deleteCharAt(0);
            }
        }
        return buf.toString();
    }

    private void write(OutputStreamWriter o, String s) {
        if (client != null && client.isConnected()) {
            try {
                o.write(s + "\n");
                o.flush();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    /**
     * FIBS' board state refers to the player as "You", we prefer to display the login handle.
     */
    String getMyUser() {
        return myUser;
    }
}
