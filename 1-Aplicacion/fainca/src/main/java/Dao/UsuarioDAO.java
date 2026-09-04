package Dao;

import Objetos.Usuario;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    /** Devuelve el usuario si las credenciales son correctas; null si no. */
    public Usuario autenticar(String usuario, String password) {
        String sql = "SELECT id, nombre, usuario, password_hash, rol FROM usuarios WHERE usuario = ? AND activo = 1";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String hash = rs.getString("password_hash");
                BCrypt.Result resultado = BCrypt.verifyer().verify(password.toCharArray(), hash);
                if (!resultado.verified) return null;
                return new Usuario(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("usuario"),
                        hash,
                        rs.getString("rol"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando usuario", e);
        }
    }

    /** Cambia la contrasena solo si la actual es correcta. */
    public boolean cambiarPassword(int usuarioId, String passwordActual, String passwordNueva) {
        String sqlSelect = "SELECT password_hash FROM usuarios WHERE id = ?";
        String sqlUpdate = "UPDATE usuarios SET password_hash = ? WHERE id = ?";
        try (Connection con = Db.getConnection();
             PreparedStatement psSelect = con.prepareStatement(sqlSelect)) {
            psSelect.setInt(1, usuarioId);
            try (ResultSet rs = psSelect.executeQuery()) {
                if (!rs.next()) return false;
                BCrypt.Result resultado = BCrypt.verifyer()
                        .verify(passwordActual.toCharArray(), rs.getString("password_hash"));
                if (!resultado.verified) return false;
            }
            String hashNuevo = BCrypt.withDefaults().hashToString(10, passwordNueva.toCharArray());
            try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdate)) {
                psUpdate.setString(1, hashNuevo);
                psUpdate.setInt(2, usuarioId);
                psUpdate.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Error cambiando contrasena", e);
        }
    }

    // ===== Gestion de usuarios (solo superadmin) =====

    /** Lista todos los usuarios (sin el hash de contrasena). */
    public List<Usuario> listar() {
        String sql = "SELECT id, nombre, usuario, rol, activo FROM usuarios ORDER BY rol, usuario";
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Usuario u = new Usuario(rs.getInt("id"), rs.getString("nombre"),
                        rs.getString("usuario"), null, rs.getString("rol"));
                u.setActivo(rs.getBoolean("activo"));
                usuarios.add(u);
            }
            return usuarios;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando usuarios", e);
        }
    }

    /** Crea un usuario nuevo con su contrasena. Devuelve el id generado. */
    public int crear(String nombre, String usuario, String password, String rol) {
        String hash = BCrypt.withDefaults().hashToString(10, password.toCharArray());
        String sql = "INSERT INTO usuarios (nombre, usuario, password_hash, rol) VALUES (?, ?, ?, ?)";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, usuario);
            ps.setString(3, hash);
            ps.setString(4, rol);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creando usuario (¿el nombre de usuario ya existe?)", e);
        }
    }

    /** Edita nombre visible, nombre de usuario, rol y estado (no la contrasena). */
    public void editar(int id, String nombre, String usuario, String rol, boolean activo) {
        String sql = "UPDATE usuarios SET nombre = ?, usuario = ?, rol = ?, activo = ? WHERE id = ?";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, usuario);
            ps.setString(3, rol);
            ps.setBoolean(4, activo);
            ps.setInt(5, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error editando usuario (¿el nombre de usuario ya existe?)", e);
        }
    }

    /** Restablece la contrasena de un usuario sin pedir la anterior (accion de superadmin). */
    public void resetearPassword(int id, String passwordNueva) {
        String hash = BCrypt.withDefaults().hashToString(10, passwordNueva.toCharArray());
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE usuarios SET password_hash = ? WHERE id = ?")) {
            ps.setString(1, hash);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error restableciendo contrasena", e);
        }
    }

    /** Baja logica: el usuario ya no puede entrar, pero se conserva su historial. */
    public void desactivar(int id) {
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE usuarios SET activo = 0 WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error desactivando usuario", e);
        }
    }

    /** Cuenta cuantos superadmin activos quedan (para no quedarse sin ninguno). */
    public int contarSuperadminsActivos() {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE rol = 'superadmin' AND activo = 1";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error contando superadmins", e);
        }
    }

    /** Devuelve el rol actual de un usuario, o null si no existe. */
    public String rolDe(int id) {
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT rol FROM usuarios WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("rol") : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando rol", e);
        }
    }
}
