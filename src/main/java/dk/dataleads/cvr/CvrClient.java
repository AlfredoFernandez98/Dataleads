package dk.dataleads.cvr;

import java.util.Optional;

/**
 * Abstraktion over CVR-datakilden (ADR-0002): cvrapi.dk i MVP'en
 * (CvrApiClient), officiel Virk Datadistribution senere (VirkCvrClient).
 * Resten af applikationen kender KUN dette interface og CvrCompany.
 */
public interface CvrClient {

    /**
     * Slår et firma op på CVR-nummer.
     *
     * @param cvr 8-cifret dansk CVR-nummer (valideret af kalderen)
     * @return firmaet, eller empty hvis CVR-nummeret ikke findes i registret
     * @throws org.springframework.web.server.ResponseStatusException 502 hvis
     *         kilden fejler vedvarende (efter retries)
     */
    Optional<CvrCompany> lookup(String cvr);
}
