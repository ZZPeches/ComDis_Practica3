La compilación y ejecución se realiza desde los paquetes Servidor y Cliente.

Servidor:
javac -cp "lib/sqlite-jdbc-3.50.3.0.jar" -d bin controlador/*.java
java -cp "bin:lib/sqlite-jdbc-3.50.3.0.jar" controlador.ServidorCB

Cliente:
javac --module-path "lib/" --add-modules javafx.controls -d bin controlador/*.java gui/*.java 
java --module-path "lib/" --add-modules javafx.controls -cp "bin" controlador.ClienteCBGUI
