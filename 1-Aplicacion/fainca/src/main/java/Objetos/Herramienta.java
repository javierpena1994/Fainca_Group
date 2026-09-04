package Objetos;

/**
 * Un item de la bodega de herramientas. Puede ser:
 *  - "herramienta": se presta y debe volver (taladros, testers, las maletas...)
 *  - "consumible":  se entrega y se gasta (pernos, discos, mascarillas...)
 *
 * No lleva codigo: el NOMBRE es el identificador (se registra "TALADRO" y la
 * marca, el estado o lo que le falte va en la observacion). Por eso el nombre es
 * UNIQUE en la base, con collation que ignora mayusculas y acentos.
 *
 * Contadores: fuera (en proyectos) = total - disponible - danadas.
 * Los consumibles no vuelven, asi que su "fuera" siempre es 0: al entregarse
 * bajan total y disponible a la vez.
 */
public class Herramienta {

    private int id;
    private String nombre;
    private String tipo;            // "herramienta" | "consumible"
    private int total;              // lo que bodega posee
    private int disponible;         // fisicamente en bodega
    private int danadas;            // esperando reparacion o baja
    private Integer stockMinimo;    // consumibles: alerta de reposicion (null = sin alerta)
    private String observaciones;
    private boolean activo = true;
    private int fuera;              // calculado: total - disponible - danadas
    private int perdidas;           // historico de perdidas (del libro de movimientos)

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getDisponible() { return disponible; }
    public void setDisponible(int disponible) { this.disponible = disponible; }

    public int getDanadas() { return danadas; }
    public void setDanadas(int danadas) { this.danadas = danadas; }

    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public int getFuera() { return fuera; }
    public void setFuera(int fuera) { this.fuera = fuera; }

    public int getPerdidas() { return perdidas; }
    public void setPerdidas(int perdidas) { this.perdidas = perdidas; }
}
