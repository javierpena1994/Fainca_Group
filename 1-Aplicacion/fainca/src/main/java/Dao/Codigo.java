package Dao;

/**
 * Reglas para comparar codigos de producto.
 *
 * En los catalogos de los proveedores el mismo producto aparece escrito a veces con
 * guion y a veces con espacio ("BAN-P01" y "BAN P01"), y en bodega se teclea de las
 * dos formas. WERMA ademas usa puntos en sus codigos ("241.340.50"). Para el sistema
 * son el MISMO codigo: antes de comparar, se le quitan guiones, espacios y puntos a
 * los dos lados.
 *
 * Ojo: esto NO cambia como se guarda el codigo. El producto conserva su codigo tal
 * cual se registro; la normalizacion se usa solo para buscarlo y reconocerlo.
 */
final class Codigo {

    private Codigo() {
    }

    /**
     * Fragmento SQL que normaliza una columna de codigo dentro de una consulta.
     * Se resuelve en el servidor de base de datos para poder comparar contra lo guardado.
     */
    static String sqlNormalizado(String columna) {
        return "REPLACE(REPLACE(REPLACE(" + columna + ", '-', ''), ' ', ''), '.', '')";
    }

    /** Misma normalizacion, pero del lado de Java (para el valor que se busca). */
    static String normalizar(String codigo) {
        return codigo == null ? null : codigo.replace("-", "").replace(" ", "").replace(".", "");
    }
}
