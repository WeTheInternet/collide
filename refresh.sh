#!/bin/sh
if [ -z $V ]; then export V="0.4-SNAPSHOT"; fi
if [ -z $V2 ]; then export V2="0.3"; fi
if [ -z $COLLIDE ]; then export COLLIDE="/opt/collide"; fi

mkdir -p $COLLIDE/deps/xapi-$V2
cd $COLLIDE/deps/xapi-$V2/
rm -f xapi-gwt-$V2.jar
rm -f xapi-dev-$V2.jar

cd /repo/net/wetheinter/ 
cp xapi-gwt/$V/xapi-gwt-$V.jar $COLLIDE/deps/xapi-$V2/
cp xapi-jre/$V/xapi-jre-$V-uber.jar $COLLIDE/deps/xapi-$V2/

cd $COLLIDE/deps/xapi-$V2/
mv xapi-gwt-$V.jar xapi-gwt-$V2.jar
mv xapi-jre-$V-uber.jar xapi-dev-$V2.jar

cd /opt/collide
ant dist

#cd /opt/collide/bin/deploy/lib/
#rm -f xapi-common-0.2.jar
#rm -f xapi-super-0.2.jar
#cd /opt/collide/deps/xapi-0.2
#cp xapi-common-0.2.jar ../../bin/deploy/lib/
#cp xapi-super-0.2.jar ../../bin/deploy/lib/
