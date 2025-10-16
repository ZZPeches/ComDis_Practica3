import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
                        "clave TEXT NOT NULL)");

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

    public void mostrarTodosLosUsuarios() {
        String sql = "SELECT nombre, clave FROM usuarios";
        try (Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql)) {

            System.out.println("Lista de usuarios registrados:");
            while (rs.next()) {
                String nombre = rs.getString("nombre");
                String clave = rs.getString("clave");
                System.out.println("- Nombre: " + nombre + ", Clave: " + clave);
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener usuarios: " + e.getMessage());
            e.printStackTrace();
        }
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
                    "INSERT INTO usuarios(nombre, clave) VALUES(?, ?)");
            ps.setString(1, nombre);
            ps.setString(2, clave);
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (SQLException e) {
            // si ya existe usuario
            return false;
        }
    }

    // validar login
    public boolean validarLogin(String nombre, String clave) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM usuarios WHERE nombre=? AND clave=?");
            ps.setString(1, nombre);
            ps.setString(2, clave);
            ResultSet rs = ps.executeQuery();
            boolean ok = rs.next();
            rs.close();
            ps.close();
            return ok;
        } catch (SQLException e) {
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
    public void agregarSolicitud(String remitente, String destino) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO solicitudes(remitente, destino) VALUES(?, ?)");
            ps.setString(1, remitente);
            ps.setString(2, destino);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
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
