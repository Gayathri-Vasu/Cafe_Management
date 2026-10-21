package com.cafe.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Cafe Management System backend.
 *
 * <p>Boots the embedded Tomcat server, the Spring context, Spring Security,
 * Spring Data JPA and connects to MySQL using the datasource configured in
 * application.properties.</p>
 */
@SpringBootApplication
public class CafeManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(CafeManagementApplication.class, args);
	}

}
