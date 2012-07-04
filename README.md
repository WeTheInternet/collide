# Collide
## What is Collide?

**Collide is an open-source "collaborative IDE" demonstration.**

Run Collide on your local file system. Browse to (http://localhost:8080). Profit.

Requires:
* [Java 7](http://www.oracle.com/technetwork/java/javase/downloads/jdk-7u4-downloads-1591156.html)
* [Ant 1.8.4+](http://ant.apache.org/bindownload.cgi)
* All other dependencies are currently bundled in


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
