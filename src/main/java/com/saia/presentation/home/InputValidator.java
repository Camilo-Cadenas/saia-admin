package com.saia.presentation.home;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Clase de utilidad para validación y filtrado de entrada de datos con enfoque en seguridad.
 * 
 * <p><b>Seguridad:</b> Implementa validaciones estrictas para:
 * <ul>
 *   <li>Prevenir desbordamiento de datos (buffer overflow)</li>
 *   <li>Garantizar tipos de datos correctos (números, texto, email)</li>
 *   <li>Limitar longitud de entrada según restricciones de BD</li>
 *   <li>Prevenir inyección de caracteres especiales maliciosos</li>
 * </ul>
 * 
 * <p><b>Restricciones de Base de Datos (tabla persona):</b>
 * <pre>
 * - num_doc     : INT (máximo 2,147,483,647)
 * - tip_doc     : VARCHAR(20)
 * - nombres     : VARCHAR(50)
 * - p_ape       : VARCHAR(50)
 * - s_ape       : VARCHAR(50)
 * - tel         : VARCHAR(20)
 * - tip_sang    : VARCHAR(5)
 * - genero      : VARCHAR(20)
 * - email       : VARCHAR(255)
 * - empresa_seg : VARCHAR(100) [tabla personal_seguridad]
 * </pre>
 * 
 * @author SAIA Security Team
 * @version 2.0
 */
public class InputValidator {

    // ── Límites de Base de Datos ─────────────────────────────────────────────
    
    /** Longitud máxima para num_doc como String (10 dígitos para INT) */
    public static final int MAX_NUM_DOC = 10;
    
    /** Longitud máxima para nombres (VARCHAR 50) */
    public static final int MAX_NOMBRES = 50;
    
    /** Longitud máxima para apellidos (VARCHAR 50) */
    public static final int MAX_APELLIDOS = 50;
    
    /** Longitud máxima para email (VARCHAR 255) */
    public static final int MAX_EMAIL = 255;
    
    /** Longitud máxima para teléfono (VARCHAR 10) */
    public static final int MAX_TELEFONO = 10;
    
    /** Longitud máxima para empresa de seguridad (VARCHAR 100) */
    public static final int MAX_EMPRESA = 100;
    
    /** Valor máximo para INT en base de datos */
    public static final long MAX_INT_VALUE = 2147483647L;
    
    // ── Filtros de Documento para JTextField ──────────────────────────────────

    /**
     * DocumentFilter que permite SOLO dígitos numéricos y limita la longitud.
     * Previene inyección de caracteres no numéricos.
     * 
     * @param maxLength Longitud máxima permitida
     * @return DocumentFilter configurado
     */
    public static DocumentFilter numericFilter(int maxLength) {
        return new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string == null) return;
                
                // Solo permitir dígitos
                if (string.matches("\\d+")) {
                    // Verificar que no exceda la longitud máxima
                    int currentLength = fb.getDocument().getLength();
                    int finalLength = currentLength + string.length();
                    
                    if (finalLength <= maxLength) {
                        // Verificar que el valor final no exceda MAX_INT
                        String currentText = fb.getDocument().getText(0, currentLength);
                        String newText = currentText.substring(0, offset) + string + currentText.substring(offset);
                        
                        try {
                            long value = Long.parseLong(newText);
                            if (value <= MAX_INT_VALUE) {
                                super.insertString(fb, offset, string, attr);
                            }
                        } catch (NumberFormatException e) {
                            // No insertar si no es un número válido
                        }
                    }
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text == null) {
                    super.replace(fb, offset, length, text, attrs);
                    return;
                }
                
                // Solo permitir dígitos
                if (text.matches("\\d*")) {
                    // Calcular longitud resultante
                    int currentLength = fb.getDocument().getLength();
                    int finalLength = currentLength - length + text.length();
                    
                    if (finalLength <= maxLength) {
                        // Verificar que el valor final no exceda MAX_INT
                        String currentText = fb.getDocument().getText(0, currentLength);
                        String newText = currentText.substring(0, offset) + text + 
                                        currentText.substring(offset + length);
                        
                        if (newText.isEmpty()) {
                            super.replace(fb, offset, length, text, attrs);
                            return;
                        }
                        
                        try {
                            long value = Long.parseLong(newText);
                            if (value <= MAX_INT_VALUE) {
                                super.replace(fb, offset, length, text, attrs);
                            }
                        } catch (NumberFormatException e) {
                            // No reemplazar si no es un número válido
                        }
                    }
                }
            }
        };
    }

    /**
     * DocumentFilter que limita la longitud del texto alfanumérico.
     * Permite letras, números, espacios y caracteres comunes (acentos, guiones, etc.).
     * Previene desbordamiento de buffer.
     * 
     * @param maxLength Longitud máxima permitida
     * @return DocumentFilter configurado
     */
    public static DocumentFilter alphanumericFilter(int maxLength) {
        return new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string == null) return;
                
                // Permitir letras, números, espacios, acentos y caracteres comunes
                if (isValidAlphanumericChars(string)) {
                    int currentLength = fb.getDocument().getLength();
                    if (currentLength + string.length() <= maxLength) {
                        super.insertString(fb, offset, string, attr);
                    }
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text == null) {
                    super.replace(fb, offset, length, text, attrs);
                    return;
                }
                
                if (isValidAlphanumericChars(text)) {
                    int currentLength = fb.getDocument().getLength();
                    int finalLength = currentLength - length + text.length();
                    
                    if (finalLength <= maxLength) {
                        super.replace(fb, offset, length, text, attrs);
                    }
                }
            }
        };
    }

    /**
     * DocumentFilter especializado para emails con validación de longitud.
     * Permite caracteres válidos en emails según RFC 5322.
     * 
     * @param maxLength Longitud máxima permitida (255 para email en BD)
     * @return DocumentFilter configurado
     */
    public static DocumentFilter emailFilter(int maxLength) {
        return new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string == null) return;
                
                // Caracteres válidos para email: letras, números, @, ., _, %, +, -
                if (isValidEmailChars(string)) {
                    int currentLength = fb.getDocument().getLength();
                    if (currentLength + string.length() <= maxLength) {
                        super.insertString(fb, offset, string, attr);
                    }
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text == null) {
                    super.replace(fb, offset, length, text, attrs);
                    return;
                }
                
                if (isValidEmailChars(text)) {
                    int currentLength = fb.getDocument().getLength();
                    int finalLength = currentLength - length + text.length();
                    
                    if (finalLength <= maxLength) {
                        super.replace(fb, offset, length, text, attrs);
                    }
                }
            }
        };
    }

    // ── Métodos de Validación ─────────────────────────────────────────────────

    /**
     * Valida si una cadena contiene solo caracteres alfanuméricos válidos.
     * Incluye: letras (con acentos), números, espacios, guiones y apóstrofes.
     * 
     * @param text Texto a validar
     * @return true si es válido, false en caso contrario
     */
    public static boolean isValidAlphanumericChars(String text) {
        // Permitir letras (incluyendo acentuadas), números, espacios, guiones, apóstrofes
        // y puntos (para abreviaturas como S.A.S)
        return text.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9\\s.,'\\-]+");
    }

    /**
     * Valida si una cadena contiene solo caracteres válidos para email.
     * 
     * @param text Texto a validar
     * @return true si es válido, false en caso contrario
     */
    public static boolean isValidEmailChars(String text) {
        // Caracteres válidos según RFC 5322 (simplificado)
        return text.matches("[a-zA-Z0-9@._\\-%+]+");
    }

    /**
     * Valida formato de email completo.
     * 
     * @param email Email a validar
     * @return true si el formato es válido, false en caso contrario
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        if (email.length() > MAX_EMAIL) return false;
        
        // Regex mejorado para email según RFC 5322 (versión simplificada pero robusta)
        return email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * Valida número de documento.
     * 
     * @param numDoc Número de documento como String
     * @return true si es válido (numérico y dentro del rango INT), false en caso contrario
     */
    public static boolean isValidNumDoc(String numDoc) {
        if (numDoc == null || numDoc.trim().isEmpty()) return false;
        if (numDoc.length() > MAX_NUM_DOC) return false;
        
        try {
            long value = Long.parseLong(numDoc);
            return value > 0 && value <= MAX_INT_VALUE;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Valida teléfono.
     * 
     * @param telefono Teléfono como String
     * @return true si es válido (numérico y longitud correcta), false en caso contrario
     */
    public static boolean isValidTelefono(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) return true; // Opcional
        if (telefono.length() > MAX_TELEFONO) return false;
        
        // Solo dígitos, mínimo 7 caracteres (teléfonos fijos), máximo 20
        return telefono.matches("\\d{7,20}");
    }

    /**
     * Valida que una cadena no exceda la longitud máxima y no esté vacía.
     * 
     * @param text Texto a validar
     * @param maxLength Longitud máxima permitida
     * @return true si es válido, false en caso contrario
     */
    public static boolean isValidLength(String text, int maxLength) {
        if (text == null || text.trim().isEmpty()) return false;
        return text.length() <= maxLength;
    }

    /**
     * Sanitiza una cadena de texto eliminando caracteres potencialmente peligrosos.
     * Útil como capa adicional de seguridad antes de guardar en BD.
     * 
     * @param input Texto de entrada
     * @return Texto sanitizado
     */
    public static String sanitize(String input) {
        if (input == null) return null;
        
        // Eliminar caracteres de control y no imprimibles
        String sanitized = input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        
        // Eliminar múltiples espacios consecutivos
        sanitized = sanitized.replaceAll("\\s+", " ");
        
        return sanitized.trim();
    }

    /**
     * Obtiene un mensaje de error descriptivo para longitud excedida.
     * 
     * @param fieldName Nombre del campo
     * @param maxLength Longitud máxima permitida
     * @return Mensaje de error formateado
     */
    public static String getLengthErrorMessage(String fieldName, int maxLength) {
        return String.format("%s no puede exceder %d caracteres.", fieldName, maxLength);
    }

    /**
     * Obtiene un mensaje de error para formato numérico inválido.
     * 
     * @param fieldName Nombre del campo
     * @return Mensaje de error formateado
     */
    public static String getNumericErrorMessage(String fieldName) {
        return String.format("%s debe contener solo números.", fieldName);
    }
}
