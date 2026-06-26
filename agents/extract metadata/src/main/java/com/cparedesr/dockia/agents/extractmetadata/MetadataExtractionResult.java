/*
 * Copyright (c) 2026 cparedes. Todos los derechos reservados.
 */
package com.cparedesr.dockia.agents.extractmetadata;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MetadataExtractionResult {

    private String nombre;
    private String apellido1;
    private String apellido2;
    private String dni;
    private String fechaNacimiento;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = clean(nombre);
    }

    public String getApellido1() {
        return apellido1;
    }

    public void setApellido1(String apellido1) {
        this.apellido1 = clean(apellido1);
    }

    public String getApellido2() {
        return apellido2;
    }

    public void setApellido2(String apellido2) {
        this.apellido2 = clean(apellido2);
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = clean(dni);
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = normalizeDate(clean(fechaNacimiento));
    }

    public Map<String, Object> toAlfrescoProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        putIfPresent(properties, "em:nombre", nombre);
        putIfPresent(properties, "em:apellido1", apellido1);
        putIfPresent(properties, "em:apellido2", apellido2);
        putIfPresent(properties, "em:dni", dni);
        putIfPresent(properties, "em:fechaNacimiento", fechaNacimiento);
        return properties;
    }

    public boolean hasValues() {
        return !toAlfrescoProperties().isEmpty();
    }

    private void putIfPresent(Map<String, Object> properties, String key, String value) {
        if (value != null && !value.isBlank()) {
            properties.put(key, value);
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) || "unknown".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private String normalizeDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value).toString();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
