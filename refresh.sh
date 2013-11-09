#!/bin/bash

source sync.sh

cd $COLLIDE
mvn install
#ant dist

#cd /opt/collide/bin/deploy/lib/
#rm -f xapi-common-0.2.jar
#rm -f xapi-super-0.2.jar
#cd /opt/collide/deps/xapi-0.2
#cp xapi-common-0.2.jar ../../bin/deploy/lib/
#cp xapi-super-0.2.jar ../../bin/deploy/lib/
