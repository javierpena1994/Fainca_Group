package Objetos;

public class Producto {

    private String codigo;
    private String marca;        // nombre de la marca (para mostrar)
    private int marcaId;
    private String descripcion;  // caracteristica tecnica del producto
    private String ubicacion;
    private String unidadMedida;
    private int stockActual;
    private boolean activo;
    private String imagen;       // nombre de archivo de la foto de referencia (o null)
    // Nota de texto libre con el contenido/estado de una maleta-kit (ej: "BAN-201=5, BAN-F01=2").
    // Solo se usa en los productos que SON una maleta (BAN-TC..). En el resto queda null.
    private String notaMaletas;

    public Producto() {
    }

    public Producto(String codigo, int marcaId, String descripcion, String ubicacion,
                    String unidadMedida, int stockActual) {
        this.codigo = codigo;
        this.marcaId = marcaId;
        this.descripcion = descripcion;
        this.ubicacion = ubicacion;
        this.unidadMedida = unidadMedida;
        this.stockActual = stockActual;
        this.activo = true;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public int getMarcaId() { return marcaId; }
    public void setMarcaId(int marcaId) { this.marcaId = marcaId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

    public int getStockActual() { return stockActual; }
    public void setStockActual(int stockActual) { this.stockActual = stockActual; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    public String getNotaMaletas() { return notaMaletas; }
    public void setNotaMaletas(String notaMaletas) { this.notaMaletas = notaMaletas; }
}
