package Objetos;

/**
 * Una linea del libro de movimientos de la bodega de herramientas.
 * Tipos: ingreso, entrega, devolucion, dano, perdida, reparacion, baja, ajuste.
 *
 * Las lineas registradas en una misma operacion comparten "lote", y el historial
 * las muestra como UNA sola fila con su lista de herramientas (igual que el
 * historial de la bodega de productos).
 */
public class MovimientoHerramienta {

    private int id;
    private String nombre;          // la herramienta (no hay codigo: el nombre la identifica)
    private String tipo;
    private String lote;
    private int cantidad;
    private int disponibleResultante;
    private Integer actaId;
    private String actaNumero;      // HER-000001, si el movimiento pertenece a un acta
    private String observaciones;
    private String usuario;
    private String fecha;
    private int itemsLote;          // cuantas lineas trae el lote al que pertenece esta

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public int getDisponibleResultante() { return disponibleResultante; }
    public void setDisponibleResultante(int disponibleResultante) { this.disponibleResultante = disponibleResultante; }

    public Integer getActaId() { return actaId; }
    public void setActaId(Integer actaId) { this.actaId = actaId; }

    public String getActaNumero() { return actaNumero; }
    public void setActaNumero(String actaNumero) { this.actaNumero = actaNumero; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public int getItemsLote() { return itemsLote; }
    public void setItemsLote(int itemsLote) { this.itemsLote = itemsLote; }
}
