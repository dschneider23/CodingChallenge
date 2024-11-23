package com.example.demo.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.springframework.stereotype.Component;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class PatientDataExtractor {

    private static final Logger logger = Logger.getLogger(PatientDataExtractor.class.getName());

    FhirContext fhirContext = FhirContext.forR4();

    /**
     * Validates the Patient resource to ensure that it has the required fields.
     *
     * @param patient The Patient resource to validate.
     * @return true if the Patient resource is valid, false otherwise.
     */
    public boolean validatePatient(Patient patient) {
        // Use FHIR Validator to validate the resource
        FhirValidator validator = fhirContext.newValidator();
        ValidationResult result = validator.validateWithResult(patient);

        //Log all errors
        if (!result.isSuccessful()) {
            List<SingleValidationMessage> messages = result.getMessages();
            for (SingleValidationMessage message : messages) {
                logger.warning("Validation issue: " + message.getLocationString() + " - " + message.getMessage());
            }
            return false;
        }
        return true;
    }

    /**
     * Extracts the first name from the Patient resource.
     *
     * @param patient The Patient resource.
     * @return The concatenated first names of the patient.
     */
    public String extractFirstName(Patient patient) {
        return patient.getName().get(0).getGiven().stream()
                .map(PrimitiveType::getValue)
                .collect(Collectors.joining(" "));
    }

    /**
     * Extracts the last name from the Patient resource.
     *
     * @param patient The Patient resource.
     * @return The last name of the patient.
     */
    public String extractLastName(Patient patient) {
        return patient.getName().get(0).getFamily();
    }

    /**
     * Extracts and formats the birth date from the Patient resource.
     *
     * @param patient The Patient resource.
     * @return The birth date of the patient in the desired format (DD.MM.YYYY).
     */
    public String extractBirthDate(Patient patient) {
        return patient.getBirthDateElement().getValueAsString();
    }

    /**
     * Converts the birth date from the FHIR format (YYYY-MM-DD) to the desired format (DD.MM.YYYY).
     *
     * @param birthDate The birth date in FHIR format.
     * @return The birth date in DD.MM.YYYY format.
     */
    private String convertDate(String birthDate) {
        try {
            SimpleDateFormat fhirFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = fhirFormat.parse(birthDate);
            SimpleDateFormat desiredFormat = new SimpleDateFormat("dd.MM.yyyy");
            return desiredFormat.format(date);
        } catch (ParseException e) {
            logger.log(Level.SEVERE, "Error parsing the birth date: " + birthDate, e);
            return "01.01.1900"; // Default or invalid date to handle parsing issues
        }
    }
}
