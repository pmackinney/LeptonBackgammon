package net.mackinney.lepton;

interface GameHelperListener {
    /** The MainActivity will act as a listener for the GameHelper */

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
