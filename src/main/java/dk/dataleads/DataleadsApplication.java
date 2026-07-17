package dk.dataleads;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
// Aktiverer typed config-records (fx dk.dataleads.cvr.CvrProperties, ADR-0002).
@ConfigurationPropertiesScan
public class DataleadsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataleadsApplication.class, args);
	}

}
