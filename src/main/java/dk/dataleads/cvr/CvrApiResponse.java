package dk.dataleads.cvr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Rå JSON-form fra cvrapi.dk — bevidst package-private: resten af appen ser
 * kun CvrCompany. 'protected' er reklamebeskyttelse (data-protection.md) og
 * omdøbes fordi det er et Java-keyword. Ukendte felter ignoreres, så vi ikke
 * knækker når den uofficielle kilde tilføjer felter.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record CvrApiResponse(
        Long vat,
        String name,
        String address,
        String zipcode,
        String city,
        @JsonProperty("protected") Boolean protectedStatus,
        Long industrycode,
        String error
) {
}
