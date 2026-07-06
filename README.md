# SAIA Admin — Panel de Administración

Sistema de Autogestión de Aprendices SENA  
Pantalla de login administrativa con Java Swing.

## Credenciales de demo

| Campo    | Valor                |
|----------|----------------------|
| Correo   | admin@sena.edu.co    |
| Password | Admin2025            |

## Ejecutar

### Con Maven (recomendado)
```bash
mvn exec:java
```

### Sin Maven (directamente con javac/java)
```powershell
# Compilar
javac -encoding UTF-8 -d target/classes (Get-ChildItem -Recurse src/main/java -Filter "*.java" | % FullName)

# Ejecutar
java -cp target/classes com.saia.Main
```

## Estructura del proyecto

```
saia-admin/
├── src/main/java/com/saia/
│   ├── Main.java                          ← Punto de entrada
│   ├── presentation/
│   │   ├── login/LoginFrame.java          ← Pantalla de login
│   │   └── components/
│   │       ├── RoundedButton.java         ← Botón con bordes redondeados
│   │       ├── RoundedTextField.java      ← Campo texto con bordes redondeados
│   │       └── RoundedPasswordField.java  ← Campo contraseña + toggle ojo
│   ├── business/
│   │   └── AuthService.java              ← Lógica de autenticación
│   ├── data/
│   │   └── UserDAO.java                  ← Acceso a datos (en memoria)
│   ├── model/
│   │   └── User.java                     ← Entidad usuario
│   └── util/
│       ├── PasswordUtil.java             ← Hash SHA-256 de contraseñas
│       └── SessionManager.java          ← Sesión en memoria (Singleton)
└── pom.xml
```

## Tecnologías

- Java 25 + Swing + Java 2D
- Maven 3.x
- Sin dependencias externas
