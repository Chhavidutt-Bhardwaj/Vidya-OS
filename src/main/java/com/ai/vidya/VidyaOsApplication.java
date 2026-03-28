package com.ai.vidya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
@EnableCaching
@EnableAsync

public class VidyaOsApplication {

	public static void main(String[] args) {
		SpringApplication.run(VidyaOsApplication.class, args);
	}

	/**
	 * Supplies the current user's identity to the JPA auditing infrastructure
	 * so {@code createdBy} and {@code updatedBy} are populated automatically.
	 */
	@Bean
	public AuditorAware<String> auditorProvider() {
		return () -> {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth == null || !auth.isAuthenticated()) {
				return Optional.of("system");
			}
			return Optional.of(auth.getName());
		};
	}
}
