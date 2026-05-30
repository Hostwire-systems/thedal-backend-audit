package com.thedal.thedal_app.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

@Component
public class CountryCodeUtil {

	private static final String COUNTRY_CODE_FILE = "countries.json";
    private static final Set<String> validCountryCodes = new HashSet<>();
    
    // Regex pattern to extract country code and mobile number
    private static final String PHONE_NUMBER_PATTERN = "^(\\+\\d{1,3})?(\\d{10})$";

    // Static block to load country codes when the class is loaded
    static {
        loadCountryCodes();
    }

    // Loads country codes from a JSON file located in the resources directory
    private static void loadCountryCodes() {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream inputStream = new ClassPathResource(COUNTRY_CODE_FILE).getInputStream()) {
            // Read the country codes from the JSON file and add them to the set
            Set<CountryCode> countryCodes = objectMapper.readValue(inputStream, new TypeReference<Set<CountryCode>>() {});
            for (CountryCode countryCode : countryCodes) {
                validCountryCodes.add(countryCode.getCc());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load country codes from file", e);
        }
    }

    // Validates if the country code is valid
    public boolean isValidCountryCode(String countryCode) {
        return validCountryCodes.contains(countryCode.toUpperCase());
    }
    
    
 // Extracts the country code and mobile number from the full phone number
    public PhoneNumberInfo extractCountryCodeAndMobile(String fullNumber) {
        Pattern pattern = Pattern.compile(PHONE_NUMBER_PATTERN);
        Matcher matcher = pattern.matcher(fullNumber);

        if (matcher.matches()) {
            String countryCode = matcher.group(1) != null ? matcher.group(1) : ""; // Extract country code
            String mobileNumber = matcher.group(2); // Extract mobile number
            return new PhoneNumberInfo(countryCode, mobileNumber);
        } else {
            throw new ThedalException(ThedalError.INVALID_PHONE_NUMBER_FORMAT, HttpStatus.BAD_REQUEST);
        }
    }

    // DTO class to hold country code and mobile number
    public static class PhoneNumberInfo {
        private String countryCode;
        private String mobileNumber;
        private String alternateMobileNumber;

        public PhoneNumberInfo(String countryCode, String mobileNumber) {
            this.countryCode = countryCode;
            this.mobileNumber = mobileNumber;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public String getMobileNumber() {
            return mobileNumber;
        }

        public String getAlternateMobileNumber(){
            return alternateMobileNumber;
        }

    }
    

    // DTO class to map country codes from JSON
    static class CountryCode {
        private String cc; // ISO 3166-1 alpha-2 country code

        public String getCc() {
            return cc;
        }

        public void setCc(String cc) {
            this.cc = cc;
        }
    }
	
}
