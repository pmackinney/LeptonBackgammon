package net.mackinney.lepton;

interface TelnetHandlerListener {
    /** The GameHandler will act as a listener for the TelnetHandler */
    void parse(String s);

    void appendConsole(String s);

    void addCommand(String s);

    String readCommand();

    void updateLoginButton(boolean b);

    void quit();

    void enableWhoOutput();
}
