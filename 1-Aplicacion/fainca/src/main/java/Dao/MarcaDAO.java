package Dao;

import Objetos.Marca;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MarcaDAO {

    public List<Marca> listar() {
        String sql = "SELECT id, nombre FROM marcas ORDER BY nombre";
        List<Marca> marcas = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                marcas.add(new Marca(rs.getInt("id"), rs.getString("nombre")));
            }
            return marcas;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando marcas", e);
        }
    }

    /** Nombre de una marca por su id, o null si no existe. */
    public String nombrePorId(int id) {
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT nombre FROM marcas WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando la marca", e);
        }
    }

    /** Crea la marca y devuelve su id generado. */
    public int crear(String nombre) {
        String sql = "INSERT INTO marcas (nombre) VALUES (?)";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creando marca (¿ya existe?)", e);
        }
    }
}
