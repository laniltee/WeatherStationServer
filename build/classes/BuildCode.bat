javac *.java
echo Compiled All JAVA Files
rmic ServerInterface
echo Stub Compiled
copy ServerInterface_Stub.class ..\..\WeatherMonitor\src /Y
start rmiregistry
echo RMI Reigstry Started
java ServerCLI
