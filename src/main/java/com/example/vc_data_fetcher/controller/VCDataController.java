//package com.example.vc_data_fetcher.controller;
//
//import com.example.vc_data_fetcher.dto.CommitsResponse;
//import com.example.vc_data_fetcher.dto.ContributorsResponse;
//import com.example.vc_data_fetcher.dto.RepoAccessResponse;
//import com.example.vc_data_fetcher.service.VCDataService;
//import lombok.Getter;
//import lombok.Setter;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/vc")
//@CrossOrigin(origins = "*")
//public class VCDataController {
//
//	@Autowired
//	private VCDataService vcDataService;
//
//	/**
//	 * Check if user has access to read the repository
//	 *
//	 * @param repoUrl The GitHub repository URL (e.g., https://github.com/owner/repo)
//	 * @param userId The user ID to fetch the access token from database
//	 * @return RepoAccessResponse indicating whether user has access and relevant details
//	 */
//	@GetMapping("/check-access")
//	public ResponseEntity<?> checkRepoAccess(
//			@RequestParam("repo_url") String repoUrl,
//			@RequestParam("user_id") Long userId) {
//		try {
//			RepoAccessResponse response = vcDataService.checkRepoAccess(repoUrl, userId);
//			return ResponseEntity.ok(response);
//		} catch (IllegalArgumentException e) {
//			return ResponseEntity.badRequest()
//					.body(new ErrorResponse("Invalid repository URL: " + e.getMessage()));
//		} catch (RuntimeException e) {
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//					.body(new ErrorResponse("Error checking repository access: " + e.getMessage()));
//		} catch (Exception e) {
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//					.body(new ErrorResponse("Unexpected error: " + e.getMessage()));
//		}
//	}
//
//	/**
//	 * Get all contributors for a repository
//	 *
//	 * @param repoUrl The GitHub repository URL (e.g., https://github.com/owner/repo)
//	 * @param userId The user ID to fetch the access token from database
//	 * @return ContributorsResponse containing list of contributors
//	 */
//	@GetMapping("/contributors")
//	public ResponseEntity<?> getContributors(
//			@RequestParam("repo_url") String repoUrl,
//			@RequestParam("user_id") Long userId) {
//		try {
//			ContributorsResponse response = vcDataService.getContributors(repoUrl, userId);
//			return ResponseEntity.ok(response);
//		} catch (IllegalArgumentException e) {
//			return ResponseEntity.badRequest()
//					.body(new ErrorResponse("Invalid repository URL: " + e.getMessage()));
//		} catch (RuntimeException e) {
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//					.body(new ErrorResponse("Error fetching contributors: " + e.getMessage()));
//		} catch (Exception e) {
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//					.body(new ErrorResponse("Unexpected error: " + e.getMessage()));
//		}
//	}
//
//	/**
//	 * Get all commits by a specific author for a repository
//	 *
//	 * @param repoUrl The GitHub repository URL (e.g., https://github.com/owner/repo)
//	 * @param authorName The GitHub username of the author
//	 * @param userId The user ID to fetch the access token from database
//	 * @return CommitsResponse containing list of commits with file details
//	 */
//	@GetMapping("/commits")
//	public ResponseEntity<?> getCommitsByAuthor(
//			@RequestParam("repo_url") String repoUrl,
//			@RequestParam("author_name") String authorName,
//			@RequestParam("user_id") Long userId) {
//		try {
//			CommitsResponse response = vcDataService.getCommitsByAuthor(repoUrl, authorName, userId);
//			return ResponseEntity.ok(response);
//		} catch (IllegalArgumentException e) {
//			return ResponseEntity.badRequest()
//					.body(new ErrorResponse("Invalid repository URL: " + e.getMessage()));
//		} catch (RuntimeException e) {
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//					.body(new ErrorResponse("Error fetching commits: " + e.getMessage()));
//		} catch (Exception e) {
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//					.body(new ErrorResponse("Unexpected error: " + e.getMessage()));
//		}
//	}
//
//	/**
//	 * Error response class for consistent error handling
//	 */
//	@Setter
//	@Getter
//	public static class ErrorResponse {
//		private String error;
//		private long timestamp;
//
//		public ErrorResponse(String error) {
//			this.error = error;
//			this.timestamp = System.currentTimeMillis();
//		}
//
//	}
//}

package com.example.vc_data_fetcher.controller;

import com.example.vc_data_fetcher.dto.CheckAccessRequest;
import com.example.vc_data_fetcher.dto.ContributorWithCommits;
import com.example.vc_data_fetcher.dto.RepoAccessResponse;
import com.example.vc_data_fetcher.service.VCDataService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Controller
@RestController
public class VCDataController {

	private final VCDataService vcDataService;

	public VCDataController(VCDataService vcDataService) {
		this.vcDataService = vcDataService;
	}

	/**
	 * Check if user has access to a repository
	 */
	@QueryMapping
	public ResponseEntity checkAccess(@Argument String repoUrl, @Argument Long userId) {
		try {
			return vcDataService.checkRepoAccess(repoUrl, userId);
		} catch (IllegalArgumentException e) {
			throw new GraphQLException("Invalid repository URL: " + e.getMessage());
		} catch (RuntimeException e) {
			throw new GraphQLException("Error checking repository access: " + e.getMessage());
		} catch (Exception e) {
			throw new GraphQLException("Unexpected error: " + e.getMessage());
		}
	}

	@PostMapping("/api/repo-validate")
	public ResponseEntity checkAccess(@RequestBody CheckAccessRequest request) {
		try {
			return vcDataService.checkRepoAccess(request.getRepoUrl(), request.getUserId());
		} catch (IllegalArgumentException e) {
			throw new GraphQLException("Invalid repository URL: " + e.getMessage());
		} catch (RuntimeException e) {
			throw new GraphQLException("Error checking repository access: " + e.getMessage());
		} catch (Exception e) {
			throw new GraphQLException("Unexpected error: " + e.getMessage());
		}
	}

	/**
	 * Get contributors along with their commits in one aggregated query
	 */
	@QueryMapping
	public List<ContributorWithCommits> repoData(@Argument String repoUrl, @Argument Long userId) {
		try {
			return vcDataService.getContributorsWithCommits(repoUrl, userId);
		} catch (IllegalArgumentException e) {
			throw new GraphQLException("Invalid repository URL: " + e.getMessage());
		} catch (RuntimeException e) {
			throw new GraphQLException("Error fetching repository data: " + e.getMessage());
		} catch (Exception e) {
			throw new GraphQLException("Unexpected error: " + e.getMessage());
		}
	}

	/**
	 * Custom exception for GraphQL error responses
	 */
	@Setter
	@Getter
	public static class GraphQLException extends RuntimeException {
		private final String error;
		private final long timestamp;

		public GraphQLException(String error) {
			super(error);
			this.error = error;
			this.timestamp = System.currentTimeMillis();
		}
	}
}
