This is the binary release of Jacl 1.4.1.

Jacl is a Java implementation of a Tcl interpreter.

The following startup shell scripts are provided:

bin/jaclsh
bin/jaclsh.bat
bin/tjc
bin/tjc.bat

The jaclsh shell script will work on UNIX systems
and with the msys shell under Win32.

The jaclsh.bat batch file works on Win32 systems.

Both of the above scripts assume that C:/jacl141
is the Jacl install directory. You will need to
edit the startup scripts if installing into another
location or running under UNIX. Note that paths in
bin/jdk.cfg will also need to be updated.

You will need to edit these scripts to adjust the
JAVA path for your system. The binary release of
Jacl provides just the precompiled .jar files and
startup scripts.

The source release contains full documentation
and additional information.

http://tcljava.sourceforge.net

