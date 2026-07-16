package dk.dataleads.dto;

/**
 * DTO (Data Transfer Object) — det objekt vi sender UD til klienten.
 * Vi holder DTO adskilt fra vores entities, så det vi eksponerer i API'et
 * ikke er bundet til hvordan data ligger i databasen.
 *
 * En Java 'record' er en kort, uforanderlig databærer: felterne (status, app)
 * bliver automatisk til getters + constructor + equals/hashCode/toString.
 */
public record HealthResponse(String status, String app) {
}
