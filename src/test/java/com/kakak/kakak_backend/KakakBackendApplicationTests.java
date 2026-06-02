package com.kakak.kakak_backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class KakakBackendApplicationTests {

	@Test
	void applicationClassLoads() {
		assertNotNull(KakakBackendApplication.class);
	}

}
