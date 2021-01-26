package net.mackinney.lepton;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

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
    private SharedPreferences preferences;
    private SharedPreferences.Editor preferencesEditor;
    private static final String SERVER_KEY = "server";
    private static final String PORT_KEY = "port";
    private static final String USER_KEY = "user";
    private static final String PASSWORD_KEY = "password";
    private static final String INVITATION_LENGTH_KEY = "invitationLength";
    private static final String DEFAULT_SERVER = "fibs.com";
    private static final int DEFAULT_PORT = 4321;
    private static final String CLIENT_NAME = "Lepton_b4";
    private static final String CLIP_VERSION = "1008";
    private static final int DEFAULT_INVITATION_LENGTH = 5;
    private static String SERVER;
    private static int PORT;
    private String user;
    private String password;
    private int invitationLength;

    Preferences(Context c) {
        context = c;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferencesEditor = preferences.edit();
        SERVER = preferences.getString(SERVER_KEY, DEFAULT_SERVER);
        PORT = preferences.getInt(PORT_KEY, DEFAULT_PORT);
        user = preferences.getString(USER_KEY, "none");
        password = preferences.getString(PASSWORD_KEY, "none");
        invitationLength = preferences.getInt(INVITATION_LENGTH_KEY, DEFAULT_INVITATION_LENGTH);
    }

    static String getClientName() {
        return CLIENT_NAME;
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
}
