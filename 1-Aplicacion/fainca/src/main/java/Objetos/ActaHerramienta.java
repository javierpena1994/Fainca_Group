package Objetos;

import java.util.List;

/**
 * Un acta de entrega de herramientas: lo que un tecnico se llevo a un proyecto
 * en una sola operacion. Queda ABIERTA mientras haya cosas sin devolver y se
 * cierra sola cuando todo esta saldado (devuelto, danado, perdido o consumido).
 */
public class ActaHerramienta {

    private int id;
    private String numero;          // HER-000001
    private String solicitante;     // tecnico que retira (firma el acta)
    private String proyecto;        // para que se entrega (ej: Proyecto Terrafértil)
    private String destino;         // donde queda fisicamente (ej: Bodega Mapasingue)
    private String observaciones;
    private String estado;          // "abierta" | "cerrada"
    private int usuarioId;
    private String usuario;         // nombre de quien registro la entrega
    private String fecha;           // yyyy-MM-dd HH:mm:ss
    private String fechaCierre;
    private int items;              // cuantas lineas tiene
    private int unidades;           // suma de cantidades entregadas
    private int pendientes;         // unidades aun en proyecto
    private List<ActaLineaHerramienta> lineas;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getSolicitante() { return solicitante; }
    public void setSolicitante(String solicitante) { this.solicitante = solicitante; }

    public String getProyecto() { return proyecto; }
    public void setProyecto(String proyecto) { this.proyecto = proyecto; }

    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(String fechaCierre) { this.fechaCierre = fechaCierre; }

    public int getItems() { return items; }
    public void setItems(int items) { this.items = items; }

    public int getUnidades() { return unidades; }
    public void setUnidades(int unidades) { this.unidades = unidades; }

    public int getPendientes() { return pendientes; }
    public void setPendientes(int pendientes) { this.pendientes = pendientes; }

    public List<ActaLineaHerramienta> getLineas() { return lineas; }
    public void setLineas(List<ActaLineaHerramienta> lineas) { this.lineas = lineas; }
}
