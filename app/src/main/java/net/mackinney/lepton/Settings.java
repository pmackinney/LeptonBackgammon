package net.mackinney.lepton;

/**
 * User settings defined and supported by FIBS.
 * This class is not currently used, but will be in future versions.
 */

// TODO run the 'toggle' command and make sure we're not ready for invites unless we want to be
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

class Settings {

    /* container for the settings available through 'toggle' and 'set'

    >toggle
    > The current settings are:
    allowpip        YES  <- set to YES
    autoboard       YES  <- the board will be redrawn after every move
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
    ready           NO
    report          NO
    silent          NO
    telnet          YES    <- set to NO for client
    wrap            NO

    >set
    > Settings of variables:
    boardstyle: 3            <- format intended for client
    linelength: 0
    pagelength: 0
    redoubles:  none
    sortwho:    rrating
    timezone:   UTC
    */

    static final String[] toggles = new String[]{
            "allowpip",
            "autoboard",
            "autodouble",
            "automove",
            "bell",
            "crawford",
            "double",
            "greedy",
            "moreboards",
            "moves",
            "notify",
            "ratings",
            "ready",
            "report",
            "silent",
            "telnet",
            "wrap"
    };

    static boolean getToggle(String t) {
        switch(t) {
            case "allowpip":return true;
            case "autoboard":return true;
            case "crawford":return true;
            case "double":return true;
            case "silent":return true;
            default:return false;
        }
    }
    /*
    With the set commands it is possible to assign a value to a variable
    that is not a toggle. 'set' without argument displays the values of
    all available variables. 'set' with one argument <variable> displays
    the value of <variable>. 'set' with two arguments sets <variable> to
    <value>.
    There are currently 6 variables you can set to a value:
    */
    private String boardstyle = "3"; // required by parser
    // private String linelength: 0 // we'll accept the default
    // private String pagelength: 0 // we'll accept the default
    // private String redoubles:  none // we'll accept the default
    // private String sortwho = "login"; // we'll accept the default

    // can't use modern java call without upping the minimum android version :-(  java.time.ZonedDateTime.now().getZone()
    private static final String timezone = java.util.Calendar.getInstance().getTimeZone().getID();
        // "America/Los_Angeles"; // TODO set to system timezone

    private static final String[] settings = new String[]{
            "set boardstyle 3",
            "set timezone " + timezone
    };

    static String[] getSettings() { return settings; }
}
