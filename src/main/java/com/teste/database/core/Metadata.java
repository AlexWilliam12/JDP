package com.teste.database.core;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Component
public class Metadata {

    @Autowired
    private Executor executor;

    public Map<String, List<Map<String, Object>>> getMetadata() throws SQLException, ClassNotFoundException {
        return executor.execute(con -> {
            Map<String, List<Map<String, Object>>> result = new HashMap<>();

            DatabaseMetaData metadata = con.getMetaData();

            ResultSet resultTables = metadata.getTables(null, null, "%", new String[] { "TABLE" });

            while (resultTables.next()) {

                String tableName = resultTables.getString("TABLE_NAME");

                PreparedStatement statement = con.prepareStatement("SELECT * FROM " + tableName);

                ResultSet resultSet = statement.executeQuery();
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

                List<Map<String, Object>> columns = new ArrayList<>();

                for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("COLUMN", tableName + "." + resultSetMetaData.getColumnName(i));
                    info.put("ORIGINAL_TYPE", resultSetMetaData.getColumnTypeName(i));
                    info.put("CLASS_TYPE", resultSetMetaData.getColumnClassName(i));
                    info.put("SIZE", resultSetMetaData.getColumnDisplaySize(i));
                    info.put("PRECISION", resultSetMetaData.getPrecision(i));
                    int scale = resultSetMetaData.getScale(i);
                    if (scale > 0) {
                        info.put("SCALE", scale);
                    }
                    info.put("NULLABLE", resultSetMetaData.isNullable(i) > 0);
                    boolean isAutoIncrement = resultSetMetaData.isAutoIncrement(i);
                    if (isAutoIncrement) {
                        info.put("IS_AUTO_INCREMENT", isAutoIncrement);
                    }
                    columns.add(info);
                }

                result.put(tableName, columns);

            }

            for (var tables : result.entrySet()) {
                String tableName = tables.getKey();
                for (var columns : tables.getValue()) {
                    ResultSet resultPrimaryKeys = metadata.getPrimaryKeys(null, null, tableName);

                    while (resultPrimaryKeys.next()) {
                        String key = tableName + "." + resultPrimaryKeys.getString("COLUMN_NAME");
                        boolean condition = columns.get("COLUMN").equals(key);
                        if (condition) {
                            columns.put("PRIMARY_KEY", condition);
                        }
                    }

                    ResultSet resultImportedKeys = metadata.getImportedKeys(null, null, tableName);

                    while (resultImportedKeys.next()) {
                        String key = tableName + "." + resultImportedKeys.getString("FKCOLUMN_NAME");
                        boolean condition = columns.get("COLUMN").equals(key);
                        if (condition) {
                            columns.put("FOREIGN_KEY", condition);
                            columns.put("REFERENCE", resultImportedKeys.getString("PKTABLE_NAME") + "."
                                    + resultImportedKeys.getString("PKCOLUMN_NAME"));
                            columns.put("UPDATE_RULE", getForeignKeyAction(resultImportedKeys.getShort("UPDATE_RULE")));
                            columns.put("DELETE_RULE", getForeignKeyAction(resultImportedKeys.getShort("DELETE_RULE")));
                        }
                    }

                    ResultSet resultIndexesInfo = metadata.getIndexInfo(null, null, tableName, false, true);

                    while (resultIndexesInfo.next()) {
                        String column = resultIndexesInfo.getString("TABLE_NAME") + "."
                                + resultIndexesInfo.getString("COLUMN_NAME");
                        boolean unique = !resultIndexesInfo.getBoolean("NON_UNIQUE");
                        boolean key = columns.get("PRIMARY_KEY") != null ? !(boolean) columns.get("PRIMARY_KEY") : true;
                        if (columns.get("COLUMN").equals(column) && unique && key) {
                            columns.put("UNIQUE", unique);
                        }
                    }
                }
            }
            return result;
        });
    }

    private static String getForeignKeyAction(short action) {
        switch (action) {
            case DatabaseMetaData.importedKeyCascade:
                return "CASCADE";
            case DatabaseMetaData.importedKeySetNull:
                return "SET NULL";
            case DatabaseMetaData.importedKeySetDefault:
                return "SET DEFAULT";
            case DatabaseMetaData.importedKeyRestrict:
                return "RESTRICT";
            case DatabaseMetaData.importedKeyNoAction:
                return "NO ACTION";
            default:
                return "UNKNOWN";
        }
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper()
                    .configure(SerializationFeature.INDENT_OUTPUT, true)
                    .writeValueAsString(getMetadata());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
