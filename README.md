# Collide
## What is Collide?

**Collide is an open-source "collaborative IDE" demonstration.**

Collide allows multiple users to edit the same files in real-time,  
with syntax highlighting, autcomplete, quick-search and a host of other features.

Collide was built and open sourced by google;  
the original repository is online @ [https://code.google.com/p/collide/]  
This fork has updated all dependencies, and added a number of features (detailed below).

## Quick start ##

    git clone git@github.com:WeTheInternet/collide.git
    cd collide
    ant dist
    bin/deploy/collide
    (optional) sudo ln -s bin/deploy/collide ~/bin (or just add bin/deploy/collide to your PATH environment variable)
    Browse to (http://localhost:8080).
    Profit.

Requirements:
* To run: [Java 7](http://www.oracle.com/technetwork/java/javase/downloads/jdk-7u4-downloads-1591156.html)
* To build: [Ant 1.8.4+](http://ant.apache.org/bindownload.cgi)
* All other dependencies are currently bundled in

## About this Fork ##

This fork has been updated to use the released version of Elemental,  
instead of the pre-release version in the original project.

Vert.x has also been updated,  
and the project now uses [XApi](https://github.com/WeTheInternet/xapi) cross platform java services.

All reusable functionality will be distributed in [XApi](https://github.com/WeTheInternet/xapi),
with Collide being the demo frontend for all developer tools in XApi.

This copy also includes an embedded GWT super dev mode recompiler,  
allowing Collide to edit, recompile and hotswap its own frontend.

You may test this feature by browsing to /res/demo.html#/  
(or right-click demo.html in file navigator and select View in Browser).

## Build ##

    `ant dist`


## Running ##

From any folder, run:

    `[collide directory]/bin/deploy/collide`

Point a browser at (http://localhost:8080/).

You are recommended to add the deploy directory to your PATH environment variable.

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
