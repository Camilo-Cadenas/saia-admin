# Guía: Relación Fichas de Formación con Aprendices

## Resumen

Sistema para gestionar la asignación de aprendices (rol=1) a fichas de formación, con un límite máximo de **30 aprendices por ficha**.

---

## Estructura de Datos

### Tabla Principal: `ficha_aprendiz`

```sql
CREATE TABLE ficha_aprendiz (
    id_ficha_aprendiz INT AUTO_INCREMENT PRIMARY KEY,
    id_ficha INT NOT NULL,
    id_cuenta INT NOT NULL,
    fecha_asignacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('ACTIVO', 'INACTIVO', 'RETIRADO') DEFAULT 'ACTIVO',
    FOREIGN KEY (id_ficha) REFERENCES ficha(id_ficha),
    FOREIGN KEY (id_cuenta) REFERENCES cuenta(id_cuenta)
);
```

### Relaciones

```
┌─────────┐       ┌──────────────────┐       ┌─────────┐
│  ficha  │──1:N──│ ficha_aprendiz   │──N:1──│ cuenta  │
│         │       │ (máx 30 activos) │       │ (rol=1) │
└─────────┘       └──────────────────┘       └─────────┘
```

---

## Instalación

### Paso 1: Ejecutar Script SQL

```cmd
mysql -u root -p sena < SQL_RELACION_FICHA_APRENDIZ.sql
```

O ejecutarlo manualmente desde MySQL Workbench.

### Paso 2: Verificar Instalación

```sql
USE sena;

-- Verificar que la tabla existe
DESCRIBE ficha_aprendiz;

-- Verificar que la vista existe
DESCRIBE vista_ficha_aprendices;

-- Verificar que los procedimientos existen
SHOW PROCEDURE STATUS WHERE Db = 'sena';
```

---

## Uso desde Java

### 1. Asignar Aprendiz a Ficha

```java
import com.saia.business.FichaAprendizService;
import com.saia.business.FichaAprendizService.AsignacionResult;

FichaAprendizService service = new FichaAprendizService();

int idFicha = 1;   // ID de la ficha
int idCuenta = 5;  // ID de la cuenta del aprendiz

AsignacionResult resultado = service.asignarAprendizAFicha(idFicha, idCuenta);

switch (resultado) {
    case OK:
        System.out.println("Aprendiz asignado exitosamente");
        break;
    case FICHA_LLENA:
        System.out.println("La ficha está llena (30 aprendices)");
        break;
    case YA_ASIGNADO:
        System.out.println("El aprendiz ya está en otra ficha");
        break;
    case NO_ES_APRENDIZ:
        System.out.println("La cuenta no es de un aprendiz");
        break;
    case ERROR_BD:
        System.out.println("Error de base de datos");
        break;
}
```

### 2. Ver Aprendices de una Ficha

```java
import com.saia.model.FichaAprendiz;
import java.util.List;

int idFicha = 1;
List<FichaAprendiz> aprendices = service.obtenerAprendicesDeFicha(idFicha);

System.out.println("Aprendices en la ficha:");
for (FichaAprendiz fa : aprendices) {
    System.out.printf("- %s (%d) - %s%n",
        fa.getNombreCompleto(),
        fa.getNumDoc(),
        fa.getEstado()
    );
}

System.out.println("\nTotal: " + aprendices.size() + "/30");
```

### 3. Ver Aprendices Sin Ficha

```java
import com.saia.data.FichaAprendizDAO.AprendizSinFicha;

List<AprendizSinFicha> sinFicha = service.obtenerAprendicesSinFicha();

System.out.println("Aprendices sin ficha asignada:");
for (AprendizSinFicha asf : sinFicha) {
    System.out.printf("- %s (%d) - %s%n",
        asf.getNombreCompleto(),
        asf.numDoc,
        asf.email
    );
}
```

### 4. Ver Fichas con Cupos Disponibles

```java
import com.saia.data.FichaAprendizDAO.FichaConCupos;

List<FichaConCupos> fichas = service.obtenerFichasConCupos();

System.out.println("Fichas con cupos disponibles:");
for (FichaConCupos fcc : fichas) {
    System.out.printf("- %s: %d/%d aprendices (%d cupos)%n",
        fcc.nomFicha,
        fcc.totalAprendices,
        30,
        fcc.cuposDisponibles
    );
}
```

### 5. Retirar Aprendiz de Ficha

```java
import com.saia.business.FichaAprendizService.RetiroResult;

int idCuenta = 5;
RetiroResult resultado = service.retirarAprendizDeFicha(idCuenta);

if (resultado == RetiroResult.OK) {
    System.out.println("Aprendiz retirado exitosamente");
} else {
    System.out.println("Error: " + resultado);
}
```

### 6. Transferir Aprendiz a Otra Ficha

```java
import com.saia.business.FichaAprendizService.TransferenciaResult;

int idCuenta = 5;
int idFichaNueva = 2;

TransferenciaResult resultado = service.transferirAprendiz(idCuenta, idFichaNueva);

if (resultado == TransferenciaResult.OK) {
    System.out.println("Aprendiz transferido exitosamente");
} else {
    System.out.println("Error: " + resultado);
}
```

### 7. Validar Antes de Asignar

```java
String error = service.validarAsignacion(idFicha, idCuenta);

if (error != null) {
    System.out.println("No se puede asignar: " + error);
} else {
    service.asignarAprendizAFicha(idFicha, idCuenta);
}
```

### 8. Obtener Estadísticas de una Ficha

```java
String stats = service.getEstadisticasFicha(idFicha);
System.out.println(stats);
// Salida: "25/30 aprendices (83% ocupado) - 5 cupos disponibles"
```

---

## Uso desde SQL

### Asignar Aprendiz a Ficha

```sql
CALL sp_asignar_aprendiz_ficha(1, 5);  -- id_ficha=1, id_cuenta=5
```

### Retirar Aprendiz de Ficha

```sql
CALL sp_retirar_aprendiz_ficha(5);  -- id_cuenta=5
```

### Transferir Aprendiz a Otra Ficha

```sql
CALL sp_transferir_aprendiz_ficha(5, 2);  -- id_cuenta=5 a id_ficha=2
```

### Consultar Aprendices de una Ficha

```sql
SELECT * FROM vista_ficha_aprendices WHERE id_ficha = 1;
```

### Ver Fichas con Cupos

```sql
SELECT 
    f.id_ficha,
    f.nom_ficha,
    COUNT(fa.id_cuenta) AS ocupados,
    30 - COUNT(fa.id_cuenta) AS cupos
FROM ficha f
LEFT JOIN ficha_aprendiz fa ON f.id_ficha = fa.id_ficha AND fa.estado = 'ACTIVO'
GROUP BY f.id_ficha, f.nom_ficha
ORDER BY cupos DESC;
```

---

## Validaciones Automáticas

### 1. Límite de 30 Aprendices

El trigger `before_insert_ficha_aprendiz` valida automáticamente:

```sql
-- Esto fallará si la ficha ya tiene 30 aprendices:
INSERT INTO ficha_aprendiz (id_ficha, id_cuenta, estado) 
VALUES (1, 5, 'ACTIVO');

-- Error: La ficha ha alcanzado el límite máximo de 30 aprendices activos
```

### 2. Solo Aprendices (rol=1)

```sql
-- Esto fallará si la cuenta no es de un aprendiz:
CALL sp_asignar_aprendiz_ficha(1, 999);

-- Error: Solo se pueden asignar cuentas de aprendices (rol=1) a fichas
```

### 3. Un Aprendiz en Una Sola Ficha

```sql
-- Esto fallará si el aprendiz ya está en una ficha activa:
CALL sp_asignar_aprendiz_ficha(1, 5);
CALL sp_asignar_aprendiz_ficha(2, 5);  -- FALLA

-- Error: El aprendiz ya está asignado a una ficha activa
```

---

## Escenarios Comunes

### Escenario 1: Registrar Nuevo Aprendiz y Asignar a Ficha

```java
// 1. Registrar aprendiz (ya implementado en AprendizService)
AprendizService aprendizService = new AprendizService();
RegistroResult resultado = aprendizService.registrar(persona, activo);

if (resultado == RegistroResult.OK) {
    // 2. Obtener id_cuenta del aprendiz
    Optional<Aprendiz> aprendizOpt = aprendizService.buscarPorNumDoc(persona.getNumDoc());
    
    if (aprendizOpt.isPresent()) {
        int idCuenta = aprendizOpt.get().getIdAprendiz(); // Es id_cuenta ahora
        
        // 3. Asignar a ficha
        FichaAprendizService fichaService = new FichaAprendizService();
        AsignacionResult asignacion = fichaService.asignarAprendizAFicha(idFicha, idCuenta);
        
        if (asignacion == AsignacionResult.OK) {
            System.out.println("Aprendiz registrado y asignado a ficha");
        }
    }
}
```

### Escenario 2: Mostrar Ficha en Panel de Aprendices

```java
// En AprendicesPanel, agregar columna "Ficha"
FichaAprendizService fichaService = new FichaAprendizService();

for (Aprendiz a : listaAprendices) {
    FichaAprendiz fichaActual = fichaService.obtenerFichaDeAprendiz(a.getIdAprendiz());
    
    String nombreFicha = (fichaActual != null) 
        ? fichaActual.getNomFicha() 
        : "Sin asignar";
    
    tableModel.addRow(new Object[]{
        a.getNumDoc(),
        a.getNombres(),
        a.getApellidos(),
        nombreFicha,  // Nueva columna
        a.getEstado()
    });
}
```

### Escenario 3: Crear Panel de Gestión de Fichas

```java
public class GestionFichasPanel extends JPanel {
    
    private FichaAprendizService service = new FichaAprendizService();
    private JComboBox<FichaConCupos> cmbFichas;
    private JList<AprendizSinFicha> listAprendices;
    
    private void asignarAprendizSeleccionado() {
        FichaConCupos fichaSeleccionada = (FichaConCupos) cmbFichas.getSelectedItem();
        AprendizSinFicha aprendizSeleccionado = listAprendices.getSelectedValue();
        
        if (fichaSeleccionada == null || aprendizSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione ficha y aprendiz");
            return;
        }
        
        // Validar antes de asignar
        String error = service.validarAsignacion(
            fichaSeleccionada.idFicha, 
            aprendizSeleccionado.idCuenta
        );
        
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Asignar
        AsignacionResult resultado = service.asignarAprendizAFicha(
            fichaSeleccionada.idFicha,
            aprendizSeleccionado.idCuenta
        );
        
        if (resultado == AsignacionResult.OK) {
            JOptionPane.showMessageDialog(this, "Aprendiz asignado exitosamente");
            actualizarListas();
        }
    }
}
```

---

## Consultas Útiles para Reportes

### Reporte: Fichas con Ocupación

```sql
SELECT 
    f.nom_ficha,
    pf.nom_prog AS programa,
    COUNT(fa.id_cuenta) AS ocupados,
    30 - COUNT(fa.id_cuenta) AS cupos,
    ROUND((COUNT(fa.id_cuenta) * 100.0 / 30), 2) AS porcentaje_ocupacion
FROM ficha f
LEFT JOIN ficha_aprendiz fa ON f.id_ficha = fa.id_ficha AND fa.estado = 'ACTIVO'
LEFT JOIN programa_formacion pf ON f.id_programa = pf.id_programa
GROUP BY f.id_ficha, f.nom_ficha, pf.nom_prog
ORDER BY porcentaje_ocupacion DESC;
```

### Reporte: Aprendices por Centro

```sql
SELECT 
    cf.nom_centro,
    COUNT(DISTINCT fa.id_cuenta) AS total_aprendices
FROM ficha_aprendiz fa
INNER JOIN ficha f ON fa.id_ficha = f.id_ficha
INNER JOIN programa_formacion pf ON f.id_programa = pf.id_programa
INNER JOIN centro_formacion cf ON pf.id_centro = cf.id_centro
WHERE fa.estado = 'ACTIVO'
GROUP BY cf.id_centro, cf.nom_centro
ORDER BY total_aprendices DESC;
```

### Reporte: Aprendices Sin Ficha

```sql
SELECT COUNT(*) AS total_sin_ficha
FROM cuenta c
WHERE c.id_rol = 1 
  AND c.estado = TRUE
  AND NOT EXISTS (
      SELECT 1 
      FROM ficha_aprendiz fa 
      WHERE fa.id_cuenta = c.id_cuenta 
        AND fa.estado = 'ACTIVO'
  );
```

---

## Mantenimiento

### Limpiar Registros Antiguos

```sql
-- Eliminar relaciones de aprendices que ya no están activos
DELETE fa 
FROM ficha_aprendiz fa
INNER JOIN cuenta c ON fa.id_cuenta = c.id_cuenta
WHERE c.estado = FALSE 
  AND fa.estado = 'ACTIVO';
```

### Ver Historial de un Aprendiz

```sql
SELECT 
    fa.id_ficha_aprendiz,
    f.nom_ficha,
    fa.estado,
    fa.fecha_asignacion
FROM ficha_aprendiz fa
INNER JOIN ficha f ON fa.id_ficha = f.id_ficha
WHERE fa.id_cuenta = 5  -- ID de la cuenta
ORDER BY fa.fecha_asignacion DESC;
```

---

## Troubleshooting

### Error: "La ficha ha alcanzado el límite"

**Causa:** La ficha ya tiene 30 aprendices activos.

**Solución:**
1. Verificar cuántos aprendices tiene:
   ```sql
   SELECT COUNT(*) FROM ficha_aprendiz WHERE id_ficha = 1 AND estado = 'ACTIVO';
   ```
2. Retirar aprendices inactivos o transferirlos a otra ficha

### Error: "El aprendiz ya está asignado"

**Causa:** El aprendiz ya está en una ficha activa.

**Solución:**
1. Ver ficha actual:
   ```sql
   SELECT * FROM vista_ficha_aprendices WHERE id_cuenta = 5 AND estado_en_ficha = 'ACTIVO';
   ```
2. Retirar de ficha actual o transferir directamente

### Error: "Solo se pueden asignar aprendices"

**Causa:** La cuenta no tiene rol=1 (Aprendiz).

**Solución:**
1. Verificar rol:
   ```sql
   SELECT c.id_cuenta, c.num_doc, r.nom_rol 
   FROM cuenta c 
   INNER JOIN rol r ON c.id_rol = r.id_rol 
   WHERE c.id_cuenta = 5;
   ```

---

## Estado de Implementación

1. ✅ Script SQL creado
2. ✅ Clases Java creadas (FichaAprendiz, FichaAprendizDAO, FichaAprendizService)
3. ✅ Columna "Ficha Asignada" agregada en AprendicesPanel
4. 🔲 Ejecutar script SQL en base de datos `sena`

## Gestión

Las asignaciones de fichas se realizan **directamente desde SQL** usando los procedimientos almacenados.
No hay panel UI para gestión de fichas - todo se hace mediante consultas SQL.

Para ver los cambios en la aplicación, usa el botón **"Actualizar"** en el panel de Aprendices.

---

**Desarrollado por:** Equipo SAIA  
**Fecha:** Agosto 2026  
**Versión:** 1.0.0
