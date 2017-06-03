
rem If you don't have a Java install which you can mess with

set JAVA3D_HOME=Z:\winnt\java3d
javaw -Xms256m -Xmx1024m -Djava.library.path=%JAVA3D_HOME%\bin -classpath %JAVA3D_HOME%\lib\ext\vecmath.jar;%JAVA3D_HOME%\lib\ext\j3dcore.jar;%JAVA3D_HOME%\lib\ext\j3dutils.jar;classes World

rem java -Xms256m -Xmx1024m -classpath classes World
