package net.mackinney.lepton;

/** The MainActivity class will act as a listener for the GameHelper */
interface GameHelperListener {

    void appendConsole(String s);

    void updateGameBoard(Board b);

    void handleResignation(String s, int i);

    void newMove(Board b);

    void setPendingOffer(int i);

    void setPendingOffer(int i, int j);

    void updateLoginButton(boolean b);

    void setScoreBoardMessage(Board b);

    void quit();
}
