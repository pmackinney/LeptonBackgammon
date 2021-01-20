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
 * Handles all communication with server
 */
class TelnetHandler implements Runnable {

    private static final String TAG = "TelnetHandler";
    // Server & login settings are here for now
    /*
    http://www.fibs.com/help.html#register
    To create an account through Telnet you need to connect to FIBS at port 4321
    (telnet://fibs.com:4321). Login using the login name guest then type name [username] where name
    is the word 'name' and [username] is the login name you want to use. The username may not
    contain blanks ' ' or colons ':'. The server will then ask for your password twice

    telnet fibs.com 4321
     */
//    private String myServer = "fibs.com"; // TODO: remove hard defaults once real prefs module is implemented
//    private int myPort = 4321;
    private String myUser = "";
//    private String myPassword = "";
//    private static final String CLIENT_NAME = "Lepton_a1";
//    private static final String CLIP_VERSION = "1008"; // http://www.fibs.com/fibs_interface.html
    private TelnetHandlerListener helper; // GameHelper
    private TelnetClient client;
    private BufferedReader in;
    private static final int SERVER_CHAR_SIZE = 512; // optimum size?
    private OutputStreamWriter out;
    private static final int MAX_WAIT = 30000; // 30 seconds TODO check ticks per sec
    private static String consoleMessage;
    private static final String EOL = "\r\n"; // End Of Line, specified in FIBS CLIP
    private Context context;
    private SharedPreferences preferences;
    private SharedPreferences.Editor preferencesEditor;


    TelnetHandler(TelnetHandlerListener helper, Context context) {

        this.helper = helper;
        this.context = context;
//        initSettings();
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(this);
    }

    /**
     * Lepton Backgammon's main execution loop.
     */
    @Override
    public void run() {
        // runOnUITread -- use this go from background to foreground.
        try {
//            Log.i(TAG, "TelnetHandler constructor");
            StringBuilder serverOutput = new StringBuilder();
            long startTime = System.currentTimeMillis();
            String command = "";
            String partial = "";
            do {
                // Main loop A: looking for commands
                command = helper.readCommand();
                if (command.length() > 0) {
                    if (command.startsWith("connect ")) { // connect user pw
                        String[] args = command.split(" ");
                        if (args.length == 3) {
                            myUser = args[1];
                            connect(args[1], args[2], serverOutput);
                        }
                    } else if ("bye".equalsIgnoreCase(command)) {
                        client.disconnect();
                        helper.updateLoginButton(false);
                    } else {
                        if (command.matches("^(raw)?who\\s")) {
                            helper.enableWhoOutput();
                        }
                        write(out, command);
//                        Log.i(TAG, "sent: " + command);
                    }
                }
                // Main loop B: sending server output to the parser TODO: There is a bug here. Read breaks in middle of EOL?
                if (client != null && client.isConnected()) {
                    serverOutput.append(partial + read());                                             // prepend partial to output
                    partial = "";                                                                      // clear partial BUGFIX?
                    if (serverOutput.lastIndexOf(EOL) >= 2                                             // if string contains end of line
                            && !serverOutput.substring(serverOutput.length() - 2).equals(EOL)) {  //     and EOL not at end of string
                        partial = serverOutput.substring(serverOutput.lastIndexOf(EOL) + 1);     // save new partial
                    }
                    if (serverOutput.length() > 0) {
                        String[] lines = serverOutput.toString().split("[" + EOL + "]+"); // eliminates most empty lines
                        for (int ix = 0; ix < lines.length; ix++) {
                            if (lines[ix].length() > 0) {
                                helper.parse(lines[ix]);
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
            helper.quit();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Create a telnet connection
     * @param user
     * @param password
     * @param serverOutput
     */
    void connect(String user, String password, StringBuilder serverOutput) {
        try {
             if (client == null) {
                 client = new TelnetClient();
             } else if (client.isConnected()) {
                 client.disconnect();
             }
            client.connect(PreferencesManager.getServer(), PreferencesManager.getPort());
            this.in = new BufferedReader(new InputStreamReader((client.getInputStream())));
            this.out = new OutputStreamWriter(client.getOutputStream());
            // Typical CLIP login: response
            // 1 darth 1592540685 c-67-180-159-87.hsd1.ca.comcast.net
            // 2 darth 1 1 0 0 0 0 1 1 51 0 1 0 1 1496.12 0 0 0 0 0 America/Los_Angeles
            //
            // Typical failure
            // login: 5 giovanni_giorgio - - 1 0 1487.29 14850 1 1602460357 67.167.1.235 BGOnline_v3.3.1 -
            //
            // e.g., login: with no carriage return, followed by a stream of info for some random user.
            if (readUntil("login: ", serverOutput)) {
                String loginCommand = "login" + " " + PreferencesManager.getClientName() + " " + PreferencesManager.getClipVersion() + " " + user + " " + password;
                write(out, loginCommand);
                if (readUntil(1 + " " + user + " ", serverOutput)) {
//                    Log.i(TAG, "Connected as user " + myUser);
                    helper.updateLoginButton(true);
                } else {
//                    Log.i(TAG, "Couldn't connect as " + myUser);
                    helper.updateLoginButton(false);
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

    /*
     waits maxwait for string to appear in server output
     reads into a char[] because the readLine method blocks if no CR
     saves all chars read in the provided buffer
    */
    private boolean readUntil(String string, StringBuilder stringBuilder) {
        return readUntil(string, stringBuilder, MAX_WAIT);
    }

    /**
     * @param string
     * @param maxwait
     * @return True if the server output contains the desired string, false if timeout is reached
     * before the string appears.
     */
    private boolean readUntil(String string, StringBuilder stringBuilder, long maxwait) {
        long timer = System.currentTimeMillis();
        // char[] serverChars = new char[SERVER_CHAR_SIZE];
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
        // just in case we managed to read 1/2 of the 2-char EOL
        if (buf.length() > 0) {
            if (buf.charAt(buf.length() - 1) == EOL.charAt(0)) { // complete the EOL at end of buf
                buf.append(EOL.charAt(1));
            }
            if (buf.charAt(0) == EOL.charAt(1)) { // discard 2nd char of EOL at start of buf
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

    String getMyUser() {
        return myUser;
    }
}
