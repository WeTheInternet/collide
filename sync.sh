#!/bin/sh
if [ -z $V ]; then export V="0.4-SNAPSHOT"; fi
if [ -z $V2 ]; then export V2="0.3"; fi
if [ -z $COLLIDE ]; then export COLLIDE="/opt/collide"; fi

mkdir -p $COLLIDE/deps/xapi-$V2
cd $COLLIDE/deps/xapi-$V2/
rm -f xapi-gwt-$V2.jar
rm -f xapi-dev-$V2.jar
if [ -f $COLLIDE/deps/xapi-$V2/xapi-gwt-$V2.jar ]; then echo $COLLIDE/deps/xapi-$V2/xapi-gwt-$V2.jar; fi

cd /repo/net/wetheinter/ 
cp xapi-gwt/$V/xapi-gwt-$V.jar $COLLIDE/deps/xapi-$V2/
cp xapi-jre/$V/xapi-jre-$V.jar $COLLIDE/deps/xapi-$V2/

cd $COLLIDE/deps/xapi-$V2/
mv xapi-gwt-$V.jar xapi-gwt-$V2.jar
mv xapi-jre-$V.jar xapi-dev-$V2.jar

