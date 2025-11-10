#!/bin/bash

if [ "$1" = "servidor" ]; then
    cd Servidor
    javac -cp "sqlite-jdbc-3.50.3.0.jar" -d . controlador/*.java
    java --enable-native-access=ALL-UNNAMED -cp ".:sqlite-jdbc-3.50.3.0.jar" controlador.ServidorCB
elif [ "$1" = "cliente" ]; then
    cd Cliente
    javac --module-path "lib" --add-modules javafx.controls -d bin controlador/*.java gui/*.java
    java --module-path "lib" --add-modules javafx.controls --enable-native-access=javafx.graphics -cp "bin" controlador.ClienteCBGUI
else
    echo "Uso: ./run.sh [servidor|cliente]"
fi
