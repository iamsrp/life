#!/bin/sh

: ${J3D_HOME:=/usr/share/java}

exec \
    java -Xms1024m -Xmx8192m \
        -Dsun.java2d.xrender=false \
        -classpath classes:${J3D_HOME}/j3dcore.jar:${J3D_HOME}/j3dutils.jar:${J3D_HOME}/vecmath.jar \
        $@ \
        World
