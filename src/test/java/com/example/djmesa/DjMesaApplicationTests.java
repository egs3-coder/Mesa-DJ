package com.example.djmesa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DjMesaApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	void contextLoadsWithWebServer() {
		assert port > 0;
	}

}
