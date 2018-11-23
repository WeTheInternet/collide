#!/bin/sh
if [ -z $V ]; then export V="0.5.1-SNAPSHOT"; fi
if [ -z $COLLIDE ]; then export COLLIDE="/opt/collide"; fi

mkdir -p $COLLIDE/deps/xapi
cd $COLLIDE/deps/xapi/
rm -f xapi-gwt.jar
rm -f xapi-dev.jar
if [ -f $COLLIDE/deps/xapi/xapi-gwt.jar ]; then echo $COLLIDE/deps/xapi/xapi-gwt.jar; fi

cd /opt/xapi/repo/net/wetheinter
cp xapi-gwt/$V/xapi-gwt-$V.jar $COLLIDE/deps/xapi/
cp xapi-dev/$V/xapi-dev-$V.jar $COLLIDE/deps/xapi/

cd $COLLIDE/deps/xapi/
mv xapi-gwt-$V.jar xapi-gwt.jar
mv xapi-dev-$V.jar xapi-dev.jar

