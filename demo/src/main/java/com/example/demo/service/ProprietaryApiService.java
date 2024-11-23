package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service // Kennzeichnet diese Klasse als Spring Service-Komponente
public class ProprietaryApiService {

    private static final Logger logger = Logger.getLogger(ProprietaryApiService.class.getName());
    public static final String PERSON_JSON_SCHEME_JSON = "/schema/Person-JSON-Scheme.json";

    // Erzeugt ein RestTemplate-Objekt für HTTP-Anfragen
    private final RestTemplate restTemplate = new RestTemplate();

    // Base URL for the proprietary API, read from application properties for flexibility
    @Value("${proprietary.api.base-url:http://localhost:3001}")
    private String baseUrl;

     /**
     * Sendet Patientendaten an eine proprietäre API.
     * 
     * @param firstName Der Vorname des Patienten
     * @param lastName Der Nachname des Patienten
     * @param birthDate Das Geburtsdatum des Patienten
     * @return true, wenn die API-Anfrage erfolgreich war; false, wenn ein Fehler aufgetreten ist
     */

    public boolean sendPatientData(String firstName, String lastName, String birthDate) {
        try {
            // Convert birth date to required format DD.MM.YYYY
            String formattedBirthDate = convertDateToGermanFormat(birthDate);

            // Create request payload according to JSON schema
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("PersonFirstName", firstName);
            requestBody.put("PersonLastName", lastName);
            requestBody.put("PersonDOB", formattedBirthDate);

            // Validate request body against JSON schema
            if (!validateAgainstSchema(requestBody)) {
                logger.warning("Request body validation against Person JSON schema failed.");
                return false;
            }

            // Configurable URL
            String url = baseUrl + "/fhir/Person";

            // Loggt die URL und den Anfragekörper
            logger.info("Sending request to proprietary API: " + url);
            logger.info("Request body: " + requestBody);

            // Sendet eine POST-Anfrage an die proprietäre API
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);

            // Loggt den Statuscode der Antwort
            logger.info("Response from proprietary API: " + response.getStatusCode());
            HttpStatus statusCode = response.getStatusCode();

            //Handle response
            if (statusCode == HttpStatus.CREATED) {
                return true;
            } else {
                // Loggt einen Fehler, wenn der Statuscode nicht 200 oder 201 ist
                logger.warning("Proprietary API returned an unexpected status: " + response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            // Loggt eine Ausnahme, falls eine auftritt
            logger.log(Level.SEVERE, "Exception occurred while sending patient data", e);
            return false;
        }
    }

    /**
     * Convert the birth date from FHIR format (YYYY-MM-DD) to German format (DD.MM.YYYY).
     *
     * @param birthDate The birth date in FHIR format.
     * @return The birth date in DD.MM.YYYY format.
     */
    private String convertDateToGermanFormat(String birthDate) {
        try {
            SimpleDateFormat fhirFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = fhirFormat.parse(birthDate);
            SimpleDateFormat germanFormat = new SimpleDateFormat("dd.MM.yyyy");
            return germanFormat.format(date);
        } catch (ParseException e) {
            logger.log(Level.SEVERE, "Error parsing the birth date: " + birthDate, e);
            return "01.01.1900"; // Default or invalid date to handle parsing issues
        }
    }

    /**
     * Validate the request body against the Person JSON schema.
     *
     * @param requestBody The request body to validate.
     * @return true if the request body is valid, false otherwise.
     */
    private boolean validateAgainstSchema(Map<String, String> requestBody) {
        try {
            // Load the JSON schema from resources
            InputStream schemaStream = this.getClass().getResourceAsStream(PERSON_JSON_SCHEME_JSON);
            if (schemaStream == null) {
                logger.severe("Could not load Person JSON schema from classpath.");
                return false;
            }

            // Load schema using networknt JSON Schema Validator
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            JsonSchema schema = schemaFactory.getSchema(schemaStream);

            // Convert the request body to a JsonNode
            ObjectMapper mapper = new ObjectMapper();
            JsonNode requestBodyNode = mapper.convertValue(requestBody, JsonNode.class);

            // Validate the request body against the schema
            Set<ValidationMessage> validationMessages = schema.validate(requestBodyNode);
            if (!validationMessages.isEmpty()) {
                validationMessages.forEach(message -> logger.warning("Schema validation error: " + message.getMessage()));
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception occurred while validating request body against schema", e);
            return false;
        }
    }
}