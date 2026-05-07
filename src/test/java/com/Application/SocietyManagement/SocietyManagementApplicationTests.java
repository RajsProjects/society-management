package com.Application.SocietyManagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.data.mongodb.uri=mongodb://localhost:27017/test",
		"jwt.secret=TestSecretKeyForCIThatIsAtLeast32BytesLong=",
		"jwt.expiration=86400000",
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
})
class SocietyManagementApplicationTests {

	@Test
	void contextLoads() {
	}

}
