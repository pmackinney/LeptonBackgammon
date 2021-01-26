package net.mackinney.lepton;

/** The GameHandler class will act as a listener for the TelnetHandler */
interface TelnetHandlerListener {

    void parse(String s);

    void appendConsole(String s);

    void addCommand(String s);

    String readCommand();

    void updateLoginButton(boolean b);

    void quit();

    void enableWhoOutput();
}
