package net.mackinney.lepton;

import android.util.Log;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class GameHelperUnitTest extends TestCase {
    /**
     * Unit tests for GameHelper class
     *
     */
    private static final String TAG = "GameHelperUnitTest";
    Matcher match = Pattern.compile("").matcher("");

    private void runTest(Pattern p, String input, String expected) {
        if (input.indexOf("\n") == 0) {
            input = input + "\n";
        }
        String[] lines = input.split("\n");
        for (int ix = 0; ix < lines.size; ix++) {
            line = lines[ix];
            match.reset(line);
            match.usePattern(p);
            boolean b = match.find();
            assertEquals(b, true);
        }
    }

    private void runTest(Pattern p, String input, String[] expected) {
        match.reset(input);
        match.usePattern(p);
        match.find();
        for (int ix = 0; ix < expected.length; ix++) {
            if (expected[ix].length() > 0) {
                assertEquals(expected[ix], match.group(ix + 1)); // expect starts at 0, groups start at 1
            }
        }
    }

    private void specialTest(Pattern p, String input, String[] expected) {
        match.reset(input);
        match.usePattern(p);
        match.find();
        int q = match.groupCount();
        String s = match.group(1);
        String t = match.group(2);
        String u = match.group(3);
        String v = match.group(4);
        String w = match.group(5);
        for (int ix = 0; ix < expected.length; ix++)
            if (expected[ix].length() > 0) {
                String inspectMe = match.group(ix + 1);
                assertEquals(expected[ix], match.group(ix + 1)); // expect starts at 0, groups start at 1
            }
    }

    @Test
    public void testBotPattern() {
        runTest(GameHelper.consoleSkip,
                "> ** You are now playing a 3 point match with Nortally",
                new String[]{"3", "Nortally"});
        runTest(GameHelper.NEW_MATCH_2,
                "** Player Nortally has joined you for a 3 point match.",
                new String[]{"Nortally", "3"});
    }
/*
    @Test
    public void testStart() {
        runTest(GameHelper.START,
                "Starting a new game with Nortally.",
                "Nortally");
    }

//    @Test
//    public void testRoll() {
//        runTest(GameHelper.roll,
//                "You rolled 4 6.",
//                new String[]{"You", "", "4", "", "6"});
//        runTest(GameHelper.roll,
//                "Nortally rolls 3 and 5.",
//                new String[]{"Nortally", "", "3", "", "5"});
//        runTest(GameHelper.roll,
//                "You roll 1 and 3.",
//                new String[]{"You", "", "1", "", "3"});
//        }

    // Nortally moves 1-off .               group(1)
    // Nortally moves bar-3 12-17 .         group(1, 4)
    // Nortally moves 7-5 7-5 7-5 .         group(1, 4, 7)
    // Nortally moves 4-9 9-14 14-19 17-22 .   group(1, 4, 7, 10)
    // "[a-zA-Z_]+ moves(( bar| \\d{1,2})-(\\d{1,2}|off))(( bar| \\d{1,2})-(\\d{1,2}|off))?(( bar| \\d{1,2})-(\\d{1,2}|off))?(( bar| \\d{1,2})-(\\d{1,2}|off))? ."
    public void testMove() {
        runTest(GameHelper.MOVE,
                "Nortally moves 1-off .",
                " 1-off");
        runTest(GameHelper.MOVE,
                "Nortally moves bar-3 12-17 .",
                new String[]{" bar-3", " 12-17"});
        runTest(GameHelper.MOVE,
                "Nortally moves 7-5 7-5 7-5 .",
                new String[]{" 7-5", " 7-5"," 7-5"});
        runTest(GameHelper.MOVE,
                "Nortally moves 4-9 9-14 14-19 17-22 .",
                new String[]{" 4-9", " 9-14", " 14-19", " 17-22"});
    }

    public void testDoubles() {
        runTest(GameHelper.DOUBLES,
                "Nortally doubles. Type 'accept' or 'reject'.",
                "Nortally");
    }

    public void testDoubleAccepted() {
        runTest(GameHelper.DOUBLE_ACCEPTED,
                "Nortally accepts the double. The cube shows 4.",
                "4");
    }

    public void testDoubleRejected() {
        runTest(GameHelper.DOUBLE_REJECTED,
                "> You give up. Nortally wins 2 points.",
                "Nortally");
    }

    public void testResignOffer() {
        runTest(GameHelper.RESIGN_OFFER,
                "Nortally wants to resign. You will win 2 points. Type 'accept' or 'reject'.",
                new String[]{"Nortally", "2"});
    }

    public void testResignation() {
        runTest(GameHelper.RESIGNATION_ACCEPTED,
                "Nortally accepts and wins 4 points.",
                "Nortally");
    }

    public void testWin() {
        runTest(GameHelper.END_OF_MATCH,
            "You win the 3 point match 4-0 .",
            new String[]{"You", "3", "4", "0"});
    }


    public void testConsoleSkip() {
        String[] input = new String[]{
                "login: ",
                "1 darth 1610093580 c-67-180-159-87.hsd1.ca.comcast.net",
                "2 darth 1 1 0 0 0 0 1 1 305 0 1 0 1 1441.15 0 1 0 0 1 America/Los_Angeles",
                "3",
                "As good as it gets",
                "better than most",
                "!punctuated",
                "4",
                "5 audacity_capstone - - 1 0 2043.71 128256 63 1600758905 li1829-102.members.linode.com bot_1p_matches_only bot_____oysteijo@gmail.com",
                "6",
                "7 Haldeman Haldeman logs in.",
                "8 DeVoss DeVoss drops connection.",
                "9 Nortally 1041253132 I'll log in at 10pm if you want to finish that game.",
                "10 Persephone",
                "11 Hades",
                "12 Bullwinkle Fan mail from some flounder?",
                "13 Rocky",
                "14 Rocky That's what I really call a mesage!",
                "15 Odin Meow",
                "16 darth still testing these codes.",
                "17 Nortally He's not all there.",
                "18 I think I'm figuring it all out.",
                "19 At least, I think I am."};
//        List<Integer> fibsIgnore = new ArrayList<>();
//        for (Integer item : new Integer[]{GameHelper.CLIP_MOTD, GameHelper.CLIP_MOTD_END, GameHelper.CLIP_WHO_INFO, GameHelper.CLIP_WHO_INFO_END, GameHelper.CLIP_LOGIN, GameHelper.CLIP_LOGOUT}) {
//            fibsIgnore.add(item);
//        }
//        Pattern consoleSkip = GameHelper.updateConsoleSkip(fibsIgnore);
//        assertEquals("^(0|3|4|5|6|7|8)\\s?", consoleSkip.pattern());
//        for (int ix = 0; ix < input.length; ix++) {
//            String line = input[ix];
//            String expected = line;
//            Matcher match = consoleSkip.matcher(line);
//            String result = match.find() ? match.group(0) : "no match";
//            System.out.println(ix + " : " + result + "+");
//        }
    }
    */
}

