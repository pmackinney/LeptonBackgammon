package net.mackinney.lepton;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores state information between launches
 *
 * NOTE: android.preference.PreferenceManager is deprecated, however I have not found a suitable
 *       replacement. This should if the minimum supported version is raised from minSdkVersion 15
 *       (Android 4.1 Jelly Bean).
 */
class Preferences {
    private static final String TAG = "Preferences";
    private Context context;

    // Preferences properties
    private SharedPreferences preferences;
    private SharedPreferences.Editor preferencesEditor;
    private static final String SERVER_KEY = "server";
    private static final String PORT_KEY = "port";
    private static final String USER_KEY = "user";
    private static final String PASSWORD_KEY = "password";
    private static final String INVITATION_LENGTH_KEY = "invitationLength";
    private static final String DEFAULT_SERVER = "fibs.com";
    private static final int DEFAULT_PORT = 4321;
    private static final String CLIP_VERSION = "1008";
    private static final int DEFAULT_INVITATION_LENGTH = 5;
    private static String SERVER;
    private static int PORT;
    private String user;
    private String password;
    private int invitationLength;

    // FIBS Toggles & Settings
    // TODO these should  be configurable in GUI
    // "2 player 1 1 0 0 0 1 1 1 4899 0 1 0 1 1496.40 1 1 0 0 0 America/Los_Angeles"
    //    private static final int CLIP_MESSAGE_KEY = 0;
    //    private static final int PLAYER_KEY = 1;
    //    private static final int ALLOWPIP_KEY = 2;
    //    private static final int AUTOBOARD_KEY = 3;
    //    private static final int AUTODOUBLE_KEY = 4;
    //    private static final int AUTOMOVE_KEY = 5;
    //    private static final int AWAY_KEY = 6;
    //    private static final int BELL_KEY = 7 ;
    //    private static final int CRAWFORD_KEY = 8;
    //    private static final int DOUBLE_KEY = 9;
    //    private static final int EXPERIENCE_KEY = 10;
    //    private static final int GREEDY_KEY = 11;
    private static final int MOREBOARDS_KEY = 12;
    //    private static final int MOVES_KEY = 13;
    //    private static final int NOTIFY_KEY = 14;
    //    private static final int RATING_KEY = 15;
    //    private static final int RATINGS_KEY = 16;
    //    private static final int READY_KEY = 17;
    //    private static final int REPORT_KEY = 18;
    //    private static final int SILENT_KEY = 19;
        private static final int TELNET_KEY = 20;
    //    private static final int TIMEZONE_KEY = 21;

    // required toggles
    private static final String TOGGLE_CMD = "toggle ";
    private static final String MOREBOARDS = "moreboards";
    private static final String MOREBOARDS_REQUIREMENT = "1";
    private static final String TELNET = "telnet";
    private static final String TELNET_REQUIREMENT = "0";

    // required settings
    private static final String BOARDSTYLE_CMD = "set boardstyle 3";
    private static String TIMEZONE_CMD_PREFIX = "set timezone ";

    // lepton settings
    /**
     * Phrase to shout when logging in
     */
    static final String LOGIN_GREETING_KEY = "login_greeting";
    static final String MATCH_HELLO_KEY = "match_hello";
    static final String MATCH_GOODBYE_KEY = "match_goodbye";
    private static String login_greeting;
    private static String match_hello;
    private static String match_goodbye;

    Preferences(Context c) {
        context = c;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferencesEditor = preferences.edit();
        SERVER = preferences.getString(SERVER_KEY, DEFAULT_SERVER);
        PORT = preferences.getInt(PORT_KEY, DEFAULT_PORT);
        user = preferences.getString(USER_KEY, "none");
        password = preferences.getString(PASSWORD_KEY, "none");
        invitationLength = preferences.getInt(INVITATION_LENGTH_KEY, DEFAULT_INVITATION_LENGTH);
        login_greeting = preferences.getString(LOGIN_GREETING_KEY, "none");
        match_hello = preferences.getString(MATCH_HELLO_KEY, "none");
        match_goodbye = preferences.getString(MATCH_GOODBYE_KEY, "none");
    }

    static String getClipVersion() {
        return CLIP_VERSION;
    }
    
    static String getServer() {
        return SERVER;
    }
    
    static int getPort() {
        return PORT;
    }
    
    String getUser() {
        return user;
    }

    void setUser(String user) {
        this.user = user;
    }

    String getPassword() {
        return password;
    }

    void setPassword(String password) {
        this.password = password;
    }

    int getInvitationLength() {
        return invitationLength;
    }

    void setInvitationLength(int invitationLength) {
        this.invitationLength = invitationLength;
    }

    void commit() {
        preferencesEditor.putString(SERVER_KEY, SERVER);
        preferencesEditor.putInt(PORT_KEY, PORT);
        preferencesEditor.putString(USER_KEY, user);
        preferencesEditor.putString(PASSWORD_KEY, password);
        preferencesEditor.putInt(INVITATION_LENGTH_KEY, invitationLength);
        preferencesEditor.apply();
    }

    /**
     * Provides a list of commands to be sent to FIBS
     * @param clip_own_info - current state of FIBS settings and toogles
     * @return - List of FIBS commands to ensure required settings
     */
    static List requiredSettingsCommands(String clip_own_info) {
        List settingsCommands = new ArrayList<String>();
        String[] own_info = clip_own_info.split(" ");
        if (!MOREBOARDS_REQUIREMENT.equals(own_info[MOREBOARDS_KEY])) {
            settingsCommands.add(TOGGLE_CMD + MOREBOARDS);
        }
        if (!TELNET_REQUIREMENT.equals(own_info[TELNET_KEY])) {
            settingsCommands.add(TOGGLE_CMD + TELNET);
        }
        settingsCommands.add(BOARDSTYLE_CMD);
        // can't use modern java call without upping the minimum android version
        // java.time.ZonedDateTime.now().getZone()
        settingsCommands.add(TIMEZONE_CMD_PREFIX +
                            java.util.Calendar.getInstance().getTimeZone().getID());
        return settingsCommands;
    }

    private static final String LEPTON_PREFIX = "set lepton ";
    /**
     * Sets a Lepton custom message
     * Syntax: set lepton login_greeting|match_hello|match_goodbye <message>
     *     login_greeting is shouted at login
     *     match_hello is kibitzed to opponent at start of match
     *     match_goodbye is kibitzed to opponent at end of match
     * @param command - key, value pair separated by a space
     */
    void setLepton(String command) {
        if (command.startsWith(LEPTON_PREFIX)) {
            command = command.substring(LEPTON_PREFIX.length());
            int mark = command.indexOf(' ');
            if (mark > 0 && command.length() > mark) {
                String key = command.substring(0, mark);
                String message = command.substring(mark + 1);
                if (LOGIN_GREETING_KEY.equals(key)
                || MATCH_HELLO_KEY.equals(key)
                ||MATCH_GOODBYE_KEY.equals(key)) {
                    preferencesEditor.putString(key, message);
                    preferencesEditor.apply();
                }
            }
        }
    }

    String getLeptonGreeting() {
        String message = preferences.getString(LOGIN_GREETING_KEY, "");
        if (null != message && message.length() > 0) {
            return message;
        } else {
            return "";
        }
    }

    String getLeptonHello() {
        String message = preferences.getString(MATCH_HELLO_KEY, "");
        if (null != message && message.length() > 0) {
            return message;
        } else {
            return "";
        }
    }

    String getLeptonGoodbye() {
        String message = preferences.getString(MATCH_GOODBYE_KEY, "");
        if (null != message && message.length() > 0) {
            return message;
        } else {
            return "";
        }
    }
}
