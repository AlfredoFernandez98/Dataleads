package dk.dataleads.cvr;

/**
 * Vores interne repræsentation af et CVR-opslag — kun de felter Dataleads
 * bruger, afkoblet fra kildens rå JSON (ADR-0002: kilden skal kunne skiftes
 * fra cvrapi.dk til Virk uden at røre resten af koden).
 */
public record CvrCompany(
        String cvr,
        String name,
        String address,
        String zipcode,
        String city,
        String industryCode,
        boolean reklamebeskyttet
) {

    /** Fuld adresse som én linje: "Testvej 1, 8000 Aarhus C" — null-tolerant. */
    public String fullAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null && !address.isBlank()) {
            sb.append(address.trim());
        }
        String zipCity = joinZipCity();
        if (!zipCity.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(zipCity);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String joinZipCity() {
        StringBuilder sb = new StringBuilder();
        if (zipcode != null && !zipcode.isBlank()) {
            sb.append(zipcode.trim());
        }
        if (city != null && !city.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(city.trim());
        }
        return sb.toString();
    }
}
