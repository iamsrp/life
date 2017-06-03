#!/bin/sh

exec \
    java -Xms1024m -Xmx6144m \
        -Djava.library.path=/usr/lib/jni \
        -classpath classes:/usr/share/java/j3dcore.jar:/usr/share/java/j3dutils.jar:/usr/share/java/vecmath.jar \
        $@ -XX:+UseCompressedOops -XX:+DoEscapeAnalysis \
        World
