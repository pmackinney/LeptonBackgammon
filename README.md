*** OBSOLETED ***
July 2, 2024 - This student project was successful in what it achieved, but there will be no
further work on it for the following reasons:

1. The project was proof-of-concept. The code was functional but has significant design flaws.

2. FIBS, the First Internet Backgammon Server, has apparently shut down. I'm grateful to its
 author, Andreas Schneider, and its longtime maintainer, Patti Beadles. They had no connection
 with Lepton Backgammon but I enjoyed playing on the server for many years, and the Andreas'
 clear instructions for writing clients made my project possible.

Lepton Backgammon
=================

A FIBS backgammon client written for Android
--------------------------------------------

The First Internet Backgammon Server (FIBS) is a telnet-based server. It can be played in an telnet 
terminal, and various clients with a Graphical User Interface (GUI) have been developed for it over 
the years. Lepton Backgammon was written with the intention of providing a robust client for Android.

See https://mackinney.net/lepton_info.html for the latest details.

Project Status
--------------

Lepton Backgammon is at the early beta stage. It has robust features that allow the player to log 
in, start a match, and play. 

Other FIBS features are supported, but less robustly.


Security
--------

Security should be considered when using any product that stores login information or has network 
capability. Lepton Backgammon has two features to consider in this regard:
1. Lepton Backgammon saves the username and password used to log in to FIBS and does not use or 
communicate this information for any other purpose. This data may be purged from your device by 
uninstalling Lepton Backgammon. Note that the login dialog displays your password in plain text.
2. Lepton Backgammon uses the Apache Commons Telnet module to create a telnet session with FIBS. 
Only one connection is made at a time, only to FIBS, and only when initiated by the user.

License
-------

Lepton Backgammon is released under the Apache Commons License. 

Copyright 2020 Paul Mackinney
