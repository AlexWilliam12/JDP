package com.teste.database.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.teste.database.errors.StructConstraintException;
import com.teste.database.errors.StructRulesException;

@Component
public class Structure {

    private final Map<String, List<Map<String, Object>>> metadata;

    public Structure(Metadata metadata) throws ClassNotFoundException, SQLException {
        this.metadata = metadata.getMetadata();
    }

    public Map<String, Map<String, Object>> validateConstraints(Map<String, Map<String, Object>> body,
            Class<?> controllerClass)
            throws Exception {
        if (!controllerClass.isAnnotationPresent(RestController.class)) {
            throw new StructRulesException("Controller class is not valid, missing '@RestController' annotation");
        }

        Set<String> entity = Objects.requireNonNull(body.keySet());
        if (entity.size() == 0) {
            throw new StructConstraintException("The entity's body cannot be empty");
        }
        if (entity.size() > 1) {
            throw new StructConstraintException("Only one entity can be passed at a time");
        }
        for (var external : body.entrySet()) {
            for (var internal : external.getValue().keySet()) {
                if (internal == null || external.getKey() == null || internal.isEmpty()
                        || external.getKey().isEmpty()) {
                    throw new StructConstraintException("All key fields must be informed");
                }
            }
        }

        Map<String, Map<String, Object>> standard = new LinkedHashMap<>();
        Map<String, Object> subject = new LinkedHashMap<>();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(mapper.writeValueAsString(metadata));

        for (var external : body.entrySet()) {
            String externalKey = standardize(external.getKey());
            if (root.has(externalKey)) {
                for (var internal : external.getValue().entrySet()) {

                    String key = standardize(internal.getKey());
                    Object value = isUUID(internal.getValue());

                    ArrayNode array = (ArrayNode) root.get(externalKey);
                    for (int i = 0; i < array.size(); i++) {
                        JsonNode node = array.get(i);
                        String column = node.get("COLUMN").asText()
                                .substring(node.get("COLUMN").asText().lastIndexOf(".") + 1);
                        boolean nullable = node.get("NULLABLE").asBoolean();
                        String className = node.get("CLASS_TYPE").asText();
                        if (key.equals(column)) {
                            if (!nullable && (value == null || value.toString().isBlank())) {
                                throw new StructConstraintException(
                                        "A value must be entered for the '%s' field".formatted(key));
                            }
                            if (value != null) {
                                if (node.get("SCALE") != null) {
                                    value = decimal(value, Class.forName(className), node.get("SCALE").asInt());
                                } else if (value instanceof Number) {
                                    value = integer(value, Class.forName(className));
                                }
                                if (!value.getClass().equals(Class.forName(className))) {
                                    throw new StructConstraintException(
                                            "The value of the '%s' key does not have a valid type to persist"
                                                    .formatted(key));
                                }
                                if (node.get("PRIMARY_KEY") != null && !annotationType(controllerClass)
                                        && node.get("FOREIGN_KEY") == null && node.get("DEFAULT") != null) {
                                    throw new StructConstraintException(
                                            "Primary keys can only be passed in PUT methods");
                                }
                            }
                        }
                    }
                    subject.put(key, value);
                }
            } else {
                throw new StructConstraintException("There is no key representing a valid entity");
            }
            standard.put(externalKey, subject);
        }
        return standard;
    }

    private String standardize(String key) {
        if (!key.matches(".*[A-Z].*")) {
            return key;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            if (Character.isUpperCase(key.charAt(i))) {
                builder.append("_" + Character.toLowerCase(key.charAt(i)));
            } else {
                builder.append(key.charAt(i));
            }
        }
        return builder.toString();
    }

    private boolean annotationType(Class<?> controllerClass) {
        for (var method : controllerClass.getMethods()) {
            if (method.isAnnotationPresent(PutMapping.class)) {
                return true;
            }
        }
        return false;
    }

    private Object isUUID(Object obj) {
        try {
            if (obj instanceof String) {
                return UUID.fromString(obj.toString());
            }
            return obj;
        } catch (IllegalArgumentException e) {
            return obj;
        }
    }

    private Object decimal(Object source, Class<?> compare, int scale) {
        if (!(source instanceof Number)) {
            return source;
        } else if (compare.equals(BigDecimal.class)) {
            BigDecimal number = BigDecimal.valueOf(Double.valueOf(source.toString()));
            if (number.scale() > scale) {
                throw new StructConstraintException("The number scale exceeds the set amount");
            }
            return number;
        } else if (compare.equals(Float.class)) {
            Float number = Float.valueOf(source.toString());
            String str = String.valueOf(number);
            int indexOfDecimal = str.indexOf(".");
            if (indexOfDecimal >= 0) {
                int decimal = str.length() - indexOfDecimal - 1;
                if (decimal > scale) {
                    throw new StructConstraintException("The number scale exceeds the set amount");
                }
            }
            return number;
        } else if (compare.equals(Double.class)) {
            Double number = Double.valueOf(source.toString());
            String str = String.valueOf(number);
            int indexOfDecimal = str.indexOf(".");
            if (indexOfDecimal >= 0) {
                int decimal = str.length() - indexOfDecimal - 1;
                if (decimal > scale) {
                    throw new StructConstraintException("The number scale exceeds the set amount");
                }
            }
            return number;
        } else {
            throw new NoSuchElementException();
        }
    }

    private Object integer(Object source, Class<?> compare) {
        if (!(source instanceof Number)) {
            return source;
        } else if (compare.equals(Integer.class)) {
            return (int) Math.round(Double.parseDouble(source.toString()));
        } else if (compare.equals(Long.class)) {
            return (long) Math.round(Double.parseDouble(source.toString()));
        } else if (compare.equals(Short.class)) {
            return (short) Math.round(Double.parseDouble(source.toString()));
        } else if (compare.equals(Byte.class)) {
            return (byte) Math.round(Double.parseDouble(source.toString()));
        } else if (compare.equals(BigInteger.class)) {
            return BigInteger.valueOf((long) Math.round(Double.parseDouble(source.toString())));
        } else {
            throw new NoSuchElementException();
        }
    }
}
