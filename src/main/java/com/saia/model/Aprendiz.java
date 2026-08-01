package com.saia.model;

/**
 * Entidad que representa un Aprendiz en el sistema.
 * 
 * ESTRUCTURA NUEVA (Base de datos 'sena'):
 * - Ya NO existe la tabla 'aprendiz' independiente
 * - Los aprendices son personas con id_rol=1 (Aprendiz) en la tabla cuenta
 * - idAprendiz ahora contiene el id_cuenta del usuario
 * - idFicha ya no está disponible (sin relación directa)
 * 
 * ESTRUCTURA ANTIGUA (Comentada):
 * - tabla aprendiz → id_aprendiz, num_doc, id_ficha
 * - Relación directa con ficha y programa
 */
public class Aprendiz {

    private int    idAprendiz;   // NUEVO: Ahora es id_cuenta (antes era id_aprendiz de tabla aprendiz)
    private int    numDoc;       // FK → persona.num_doc (INT)
    private int    idFicha;      // DEPRECADO: Ya no hay relación directa con ficha

    private Persona persona;
    private Ficha   ficha;       // DEPRECADO: Ya no se carga automáticamente
    private boolean cuentaActiva  = true;
    private String  siglasProg    = "";   // DEPRECADO: No disponible sin relación con ficha
    private String  siglasCentro  = "";   // DEPRECADO: No disponible sin relación con ficha

    public Aprendiz() {}

    public int getIdAprendiz() { return idAprendiz; }
    public void setIdAprendiz(int idAprendiz) { this.idAprendiz = idAprendiz; }

    public int getNumDoc() { return numDoc; }
    public void setNumDoc(int numDoc) { this.numDoc = numDoc; }

    public int getIdFicha() { return idFicha; }
    public void setIdFicha(int idFicha) { this.idFicha = idFicha; }

    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }

    public Ficha getFicha() { return ficha; }
    public void setFicha(Ficha ficha) { this.ficha = ficha; }

    public boolean isCuentaActiva() { return cuentaActiva; }
    public void setCuentaActiva(boolean cuentaActiva) { this.cuentaActiva = cuentaActiva; }

    public String getSiglasProg() { return siglasProg; }
    public void setSiglasProg(String siglasProg) { this.siglasProg = siglasProg != null ? siglasProg : ""; }

    public String getSiglasCentro() { return siglasCentro; }
    public void setSiglasCentro(String siglasCentro) { this.siglasCentro = siglasCentro != null ? siglasCentro : ""; }

    public String getNombreCompleto() {
        return persona != null ? persona.getNombreCompleto() : "";
    }

    @Override
    public String toString() {
        return "Aprendiz{id=" + idAprendiz + ", numDoc=" + numDoc + "}";
    }
}
