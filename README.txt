La compilación y ejecución se realiza desde los paquetes Servidor y Cliente.

Servidor:
javac -cp "lib/sqlite-jdbc-3.50.3.0.jar" -d bin controlador/*.java
java --enable-native-access=ALL-UNNAMED -cp "bin:sqlite-jdbc-3.50.3.0.jar" controlador.ServidorCB
#    -- para evitar o warn --

Cliente:
javac --module-path "lib/" --add-modules javafx.controls -d bin controlador/*.java gui/*.java 
java --module-path "lib/" --add-modules javafx.controls --enable-native-access=javafx.graphics -cp "bin" controlador.ClienteCBGUI
