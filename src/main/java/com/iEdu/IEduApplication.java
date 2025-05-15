package com.iEdu;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.HashMap;
import java.util.Map;

@EnableJpaAuditing
@SpringBootApplication
public class IEduApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();
		Map<String, Object> props = new HashMap<>();
		props.put("AWS_ACCESS_KEY_ID", dotenv.get("AWS_ACCESS_KEY_ID"));
		props.put("AWS_SECRET_ACCESS_KEY", dotenv.get("AWS_SECRET_ACCESS_KEY"));
		props.put("JWT_SECRET_KEY", dotenv.get("JWT_SECRET_KEY"));
		props.put("POSTGRES_HOST", dotenv.get("POSTGRES_HOST"));
		props.put("POSTGRES_USERNAME", dotenv.get("POSTGRES_USERNAME"));
		props.put("POSTGRES_PASSWORD", dotenv.get("POSTGRES_PASSWORD"));

		SpringApplication app = new SpringApplication(IEduApplication.class);
		app.setDefaultProperties(props);
		app.run(args);
	}
}
