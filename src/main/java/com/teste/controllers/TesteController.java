package com.teste.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.teste.database.core.Structure;

@RestController
public class TesteController {

    private final Map<String, String> response = new HashMap<>();

    @Autowired
    private Structure structs;

    // @Autowired
    // private Metadata metadata;

    @PostMapping("/teste")
    public ResponseEntity<?> post(@RequestBody Map<String, Map<String, Object>> body) {
        try {
            // new ObjectMapper()
            // .configure(SerializationFeature.INDENT_OUTPUT, true)
            // .writeValue(new File("src/main/resources/output.json"),
            // metadata.getMetadata());
            return ResponseEntity.ok(structs.validateConstraints(body, TesteController.class));
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
