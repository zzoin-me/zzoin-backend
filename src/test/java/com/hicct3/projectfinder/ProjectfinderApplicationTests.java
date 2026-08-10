package com.hicct3.projectfinder;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:projectfinder;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=validate",
		"spring.flyway.enabled=true",
		"spring.flyway.locations=classpath:db/migration",
		"spring.quartz.job-store-type=jdbc",
		"spring.quartz.jdbc.initialize-schema=never",
		"spring.quartz.auto-startup=false",
		"app.deadline.recovery.enabled=false"
})
class ProjectfinderApplicationTests {

	@Test
	void contextLoads() {
	}

}
