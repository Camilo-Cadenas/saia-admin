package com.saia.model;

import java.time.LocalDate;

/**
 * Entidad que representa la tabla {@code persona}.
 *
 * Cambios respecto a la BD nueva:
 *  - num_doc es INT (no BIGINT)
 *  - sexo → genero
 *  - password_hash eliminado (ahora está en tabla cuenta)
 */
public class Persona {

    private int    numDoc;          // PK - INT
    private String tipDoc;          // VARCHAR(20)
    private String nombres;         // VARCHAR(50)
    private String pApe;            // VARCHAR(50)
    private String sApe;            // VARCHAR(50)
    private String tel;             // VARCHAR(20)
    private String tipSang;         // VARCHAR(5)
    private String genero;          // VARCHAR(20)  ← antes "sexo"
    private LocalDate fechaNac;     // DATE
    private String email;           // VARCHAR(255)

    public Persona() {}

    // ── Getters y Setters ────────────────────────────────────────────────────

    public int getNumDoc() { return numDoc; }
    public void setNumDoc(int numDoc) { this.numDoc = numDoc; }

    public String getTipDoc() { return tipDoc; }
    public void setTipDoc(String tipDoc) { this.tipDoc = tipDoc; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getPApe() { return pApe; }
    public void setPApe(String pApe) { this.pApe = pApe; }

    public String getSApe() { return sApe; }
    public void setSApe(String sApe) { this.sApe = sApe; }

    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }

    public String getTipSang() { return tipSang; }
    public void setTipSang(String tipSang) { this.tipSang = tipSang; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public LocalDate getFechaNac() { return fechaNac; }
    public void setFechaNac(LocalDate fechaNac) { this.fechaNac = fechaNac; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // ── Compatibilidad con código antiguo que usaba getSexo() ────────────────
    /** @deprecated usar {@link #getGenero()} */
    @Deprecated public String getSexo() { return genero; }
    /** @deprecated usar {@link #setGenero(String)} */
    @Deprecated public void setSexo(String sexo) { this.genero = sexo; }

    // ── Utilidades ────────────────────────────────────────────────────────────

    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder();
        if (nombres != null) sb.append(nombres.trim()).append(" ");
        if (pApe    != null) sb.append(pApe.trim()).append(" ");
        if (sApe    != null) sb.append(sApe.trim());
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return "Persona{numDoc=" + numDoc + ", nombres='" + getNombreCompleto()
                + "', email='" + email + "'}";
    }
}
