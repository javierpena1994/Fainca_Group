package Objetos;

public class Movimiento {

    private int id;
    private String productoCodigo;
    private String descripcion; // caracteristica del producto (para mostrar en historial)
    private String marca;
    private String tipo;        // "ingreso", "egreso" o "ajuste"
    // Identificador del documento al que pertenece esta linea: todas las lineas
    // registradas en un mismo envio lo comparten. Null en los movimientos anteriores
    // a esta funcion, que se muestran individualmente.
    private String lote;
    private int cantidad;
    private int stockResultante;
    private String observaciones;
    private String fecha;       // formato "yyyy-MM-dd HH:mm:ss"
    private String usuario;     // nombre de quien registro el movimiento

    // Datos del producto que se copian al armar el detalle de un documento
    // (para el despliegue en el historial y para el PDF exportado).
    private String imagen;
    private String unidadMedida;
    private String ubicacion;

    // Cuantas lineas trae el documento al que pertenece este movimiento (1 si va suelto).
    // Lo calcula la consulta del historial para que el resumen del grupo sea exacto
    // aunque la pagina cargada no contenga todas sus lineas.
    private int itemsLote;

    public Movimiento() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getProductoCodigo() { return productoCodigo; }
    public void setProductoCodigo(String productoCodigo) { this.productoCodigo = productoCodigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public int getStockResultante() { return stockResultante; }
    public void setStockResultante(int stockResultante) { this.stockResultante = stockResultante; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public int getItemsLote() { return itemsLote; }
    public void setItemsLote(int itemsLote) { this.itemsLote = itemsLote; }
}
