package com.teste.database.services;

import java.util.Map;

import com.teste.database.errors.StructConstraintException;

public class QueryBuilder {

    public String list(Map<String, Map<String, Object>> body) {
        String query = null;
        for (var external : body.entrySet()) {
            query = external.getKey();
        }
        return query;
    }

    // public String searchById(Map<String, Map<String, Object>> body) {
    //     String query = null;
    //     for (var external : body.entrySet()) {
    //         for (var internal : external.getValue().keySet()) {
                
    //         }
    //     }

    // }
}
