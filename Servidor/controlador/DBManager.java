package controlador;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class DBManager {

    private Connection conn;

    public DBManager(String dbFile) {
        try {
            // conectar o crear fichero sqlite
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            inicializarTablas();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void inicializarTablas() throws SQLException {
        Statement st = conn.createStatement();

        // tabla de usuarios
        st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS usuarios (" +
                        "nombre TEXT PRIMARY KEY, " +
                        "clave TEXT NOT NULL, " + 
                        "salt TEXT NOT NULL)");

        // tabla de amigos (amistad bidireccional, se añaden dos filas por amistad)
        st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS amigos (" +
                        "usuario1 TEXT, " +
                        "usuario2 TEXT)");

        // tabla de solicitudes de amistad pendientes
        st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS solicitudes (" +
                        "remitente TEXT, " +
                        "destino TEXT)");

        st.close();
    }

    public void rechazarSolicitud(String remitente, String destino) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM solicitudes WHERE remitente=? AND destino=?");
            ps.setString(1, remitente);
            ps.setString(2, destino);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    // registrar usuario nuevo
    public boolean registrarUsuario(String nombre, String clave) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO usuarios(nombre, clave, salt) VALUES(?, ?, ?)");
            
            byte[] salt = GestorContraseñas.generateSalt();
            String securePasswd = GestorContraseñas.hashPassword(clave, salt);
            ps.setString(1, nombre);
            ps.setString(2, securePasswd);
            ps.setString(3, Base64.getEncoder().encodeToString(salt));
            ps.executeUpdate();
            ps.close();
            System.out.println("Exito al registrar");
            return true;
        } catch (SQLException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            // si ya existe usuario
            e.printStackTrace();
            return false;
        }
    }

    // validar login
    public boolean validarLogin(String nombre, String clave) {
    try {
        // Traer hash y salt del usuario
        PreparedStatement ps = conn.prepareStatement(
                "SELECT clave, salt FROM usuarios WHERE nombre=?");
        ps.setString(1, nombre);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            rs.close();
            ps.close();
            return false; // usuario no encontrado
        }

        String storedHash = rs.getString("clave");
        String storedSaltBase64 = rs.getString("salt");
        rs.close();
        ps.close();

        // Convertir salt de Base64 a byte[]
        byte[] salt = Base64.getDecoder().decode(storedSaltBase64);

        // Generar hash de la contraseña ingresada con el mismo salt
        String hashIngresado = GestorContraseñas.hashPassword(clave, salt);

        // Comparar hashes
        return storedHash.equals(hashIngresado);

    } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
        e.printStackTrace();
        return false;
    }
}



    // validar login
    public boolean validarUsuarioExistente(String nombre) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM usuarios WHERE nombre=?")) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                boolean ok = rs.next();
                return ok;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    // obtener amigos de un usuario
    public List<String> obtenerAmigos(String nombre) {
        List<String> amigos = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT usuario2 FROM amigos WHERE usuario1=?");
            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                amigos.add(rs.getString("usuario2"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return amigos;
    }

    // añadir amistad bidireccional
    public void agregarAmigos(String usuario1, String usuario2) {
        try {
            PreparedStatement ps1 = conn.prepareStatement(
                    "INSERT INTO amigos(usuario1, usuario2) VALUES(?, ?)");
            ps1.setString(1, usuario1);
            ps1.setString(2, usuario2);
            ps1.executeUpdate();
            ps1.close();

            PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO amigos(usuario1, usuario2) VALUES(?, ?)");
            ps2.setString(1, usuario2);
            ps2.setString(2, usuario1);
            ps2.executeUpdate();
            ps2.close();

            // eliminar la solicitud si existía
            PreparedStatement psDel = conn.prepareStatement(
                    "DELETE FROM solicitudes WHERE (remitente=? AND destino=?) OR (remitente=? AND destino=?)");
            psDel.setString(1, usuario1);
            psDel.setString(2, usuario2);
            psDel.setString(3, usuario2);
            psDel.setString(4, usuario1);
            psDel.executeUpdate();
            psDel.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // añadir solicitud pendiente
    public boolean agregarSolicitud(String remitente, String destino) {
        try {
            // comprobar si el usuario destino existe
            if (!validarUsuarioExistente(destino)) {
                return false; // usuario destino no existe
            }

            // comprobar si ya son amigos
            PreparedStatement psAmigos = conn.prepareStatement(
                    "SELECT 1 FROM amigos WHERE usuario1=? AND usuario2=?");
            psAmigos.setString(1, remitente);
            psAmigos.setString(2, destino);
            ResultSet rsAmigos = psAmigos.executeQuery();
            if (rsAmigos.next()) {
                rsAmigos.close();
                psAmigos.close();
                return false;
            }
            rsAmigos.close();
            psAmigos.close();

            // comprobar si ya hay una solicitud pendiente
            PreparedStatement psSolicitud = conn.prepareStatement(
                    "SELECT 1 FROM solicitudes WHERE remitente=? AND destino=?");
            psSolicitud.setString(1, remitente);
            psSolicitud.setString(2, destino);
            ResultSet rsSolicitud = psSolicitud.executeQuery();
            if (rsSolicitud.next()) {
                rsSolicitud.close();
                psSolicitud.close();
                return false;
            }
            rsSolicitud.close();
            psSolicitud.close();

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO solicitudes(remitente, destino) VALUES(?, ?)");
            ps.setString(1, remitente);
            ps.setString(2, destino);
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    // obtener solicitudes pendientes para un usuario
    public List<String> obtenerSolicitudes(String usuario) {
        List<String> solicitudes = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT remitente FROM solicitudes WHERE destino=?");
            ps.setString(1, usuario);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                solicitudes.add(rs.getString("remitente"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return solicitudes;
    }

    // cerrar conexión
    public void cerrar() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}