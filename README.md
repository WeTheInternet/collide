# Collide
## What is Collide?

**Collide is an open-source "collaborative IDE" demonstration.**

Run Collide on your local file system. Browse to (http://localhost:8080). Profit.

Requires:
* [Java 7](http://www.oracle.com/technetwork/java/javase/downloads/jdk-7u4-downloads-1591156.html)
* [Ant 1.8.4+](http://ant.apache.org/bindownload.cgi)
* All other dependencies are currently bundled in

Collide was built and open sourced by google;  
the original repository is online @ [https://code.google.com/p/collide/]

## About this Fork ##

This fork has been updated to use the released version of Elemental, instead of the pre-release version in the original project.

Vert.x has also been updated, and the project now uses [XApi](https://github.com/WeTheInternet/xapi) cross platform java services.

This copy also includes an embedded GWT super dev mode recompiler, allowing users to use Collide to edit its own source code, 
and then recompile and hotswap that code, for extremely rapid development iterations.

The recompiler is being refactored to exist outside of collide and merely plugin to the application, to allow lightweight reuse in any GWT project.

You may test this feature by browsing to /res/demo.html#/ (or right-click demo.html in file navigator and select View in Browser).

## Build ##

    `ant dist`


## Running ##

From any folder, run:

    `[collide directory]/bin/deploy/collide`

Point a browser at (http://localhost:8080/).


## Hints for USING COLLIDE ##

`Atl+enter` brings up the "Awesome Box" for fast file switching.

`Ctrl+space` does client side lexical completions and code snippets. 


## Eclipse setup ##

* First, run `ant build test` to make sure all generated files are created.
* Import the existing Eclipse project in the root directory.
* Java 7 must be your default JRE.

## Debug the server ##

  `[collide directory]/bin/deploy/collide -debug`
  
Remote attach to port 8001.  An Eclipse launch config is included.
