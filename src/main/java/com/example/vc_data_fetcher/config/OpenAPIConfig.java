package com.example.vc_data_fetcher.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {
	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("VC Data Fetcher API")
						.version("1.0.0")
						.description("API documentation for GitHub OAuth and Repository Data endpoints"));
	}
}
