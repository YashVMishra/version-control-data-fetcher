package com.example.vc_data_fetcher.api;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RequestMapping("/api/auth")
public interface GithubAPI {

	@GetMapping("/github-login")
	String login(HttpServletResponse response,
			   @RequestParam(value = "user_id", required = false) Long userId) throws IOException;

	@GetMapping("/callback")
	void callback(@RequestParam("code") String code,
				  @RequestParam(value = "state", required = false) String state,
				  HttpServletResponse response) throws IOException;

	@GetMapping("/exchange-token")
	ResponseEntity<?> exchangeToken(@RequestParam("code") String code,
									@RequestParam(value = "user_id", required = false) Long userId);

	@GetMapping("/check-token")
	ResponseEntity<?> checkToken(@RequestParam("user_id") Long userId);

	@GetMapping("/get-token")
	ResponseEntity<?> getToken(@RequestParam("user_id") Long userId);
}
