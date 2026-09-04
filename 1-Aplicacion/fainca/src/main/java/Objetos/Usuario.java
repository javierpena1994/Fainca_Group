package Objetos;

public class Usuario {

    private int id;
    private String nombre;
    private String usuario;
    private String passwordHash;
    private String rol; // "superadmin", "admin" o "ventas"
    private boolean activo = true;

    public Usuario() {
    }

    public Usuario(int id, String nombre, String usuario, String passwordHash, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.usuario = usuario;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    // Un superadmin puede hacer todo lo que un admin, mas gestionar usuarios.
    public boolean esAdmin() { return "admin".equals(rol) || "superadmin".equals(rol); }

    public boolean esSuperAdmin() { return "superadmin".equals(rol); }
}
