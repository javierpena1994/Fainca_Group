package Objetos;

/**
 * Una linea de un acta: que se llevo y como se ha ido saldando.
 * pendiente (aun en proyecto) = cantidad - devueltoOk - devueltoDanado - perdido - consumido.
 */
public class ActaLineaHerramienta {

    private int id;
    private int herramientaId;
    private String nombre;
    private String tipo;            // "herramienta" | "consumible"
    private int cantidad;
    private String observacion;     // series, numeros de parte...
    private int devueltoOk;
    private int devueltoDanado;
    private int perdido;
    private int consumido;
    private int pendiente;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getHerramientaId() { return herramientaId; }
    public void setHerramientaId(int herramientaId) { this.herramientaId = herramientaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public int getDevueltoOk() { return devueltoOk; }
    public void setDevueltoOk(int devueltoOk) { this.devueltoOk = devueltoOk; }

    public int getDevueltoDanado() { return devueltoDanado; }
    public void setDevueltoDanado(int devueltoDanado) { this.devueltoDanado = devueltoDanado; }

    public int getPerdido() { return perdido; }
    public void setPerdido(int perdido) { this.perdido = perdido; }

    public int getConsumido() { return consumido; }
    public void setConsumido(int consumido) { this.consumido = consumido; }

    public int getPendiente() { return pendiente; }
    public void setPendiente(int pendiente) { this.pendiente = pendiente; }
}
