//package com.example.vc_data_fetcher.service;
//
//import com.example.vc_data_fetcher.dto.*;
//import com.example.vc_data_fetcher.model.VCToken;
//import com.example.vc_data_fetcher.repository.VCTokenRepository;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//@Service
//public class VCDataService {
//
//	@Autowired
//	private VCTokenRepository vcTokenRepository;
//
//	private final RestTemplate restTemplate = new RestTemplate();
//	private final ObjectMapper objectMapper = new ObjectMapper();
//
//	// Extract owner and repo from GitHub URL
//	private String[] extractOwnerAndRepo(String repoUrl) {
//		// Pattern to match GitHub URLs like https://github.com/owner/repo
//		Pattern pattern = Pattern.compile("https://github\\.com/([^/]+)/([^/]+)(?:\\.git)?/?$");
//		Matcher matcher = pattern.matcher(repoUrl.trim());
//
//		if (matcher.matches()) {
//			return new String[]{matcher.group(1), matcher.group(2)};
//		}
//		throw new IllegalArgumentException("Invalid GitHub repository URL: " + repoUrl);
//	}
//
//	// Get access token for user
//	private String getAccessToken(Long userId) {
//		Optional<VCToken> tokenOpt = vcTokenRepository.findByUserId(userId);
//		if (tokenOpt.isEmpty()) {
//			throw new RuntimeException("Access token not found for user: " + userId);
//		}
//		return tokenOpt.get().getAccess_token();
//	}
//
//	// Create HTTP headers with authorization
//	private HttpHeaders createHeaders(String accessToken) {
//		HttpHeaders headers = new HttpHeaders();
//		headers.set("Authorization", "Bearer " + accessToken);
//		headers.set("Accept", "application/vnd.github+json");
//		headers.set("X-GitHub-Api-Version", "2022-11-28");
//		return headers;
//	}
//
//	/**
//	 * Check if user has access to read the repository
//	 */
//	public RepoAccessResponse checkRepoAccess(String repoUrl, Long userId) {
//		try {
//			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
//			String owner = ownerRepo[0];
//			String repo = ownerRepo[1];
//			String accessToken = getAccessToken(userId);
//
//			// Step 1: Try to get basic repo info
//			String repoInfoUrl = String.format("https://api.github.com/repos/%s/%s", owner, repo);
//
//			HttpHeaders headers = createHeaders(accessToken);
//			HttpEntity<?> entity = new HttpEntity<>(headers);
//
//			try {
//				ResponseEntity<String> response = restTemplate.exchange(repoInfoUrl, HttpMethod.GET, entity, String.class);
//
//				if (response.getStatusCode() == HttpStatus.OK) {
//					// User has access, get repo details
//					GitHubRepoInfo repoInfo = objectMapper.readValue(response.getBody(), GitHubRepoInfo.class);
//
//					// Step 2: Try to access commits to verify read access
//					String commitsUrl = String.format("https://api.github.com/repos/%s/%s/commits?per_page=1", owner, repo);
//
//					try {
//						ResponseEntity<String> commitsResponse = restTemplate.exchange(commitsUrl, HttpMethod.GET, entity, String.class);
//
//						if (commitsResponse.getStatusCode() == HttpStatus.OK) {
//							return new RepoAccessResponse(
//									true,
//									"User has read access to the repository",
//									repoInfo.getName(),
//									owner,
//									repoInfo.isPrivate_repo(),
//									"read"
//							);
//						}
//					} catch (HttpClientErrorException e) {
//						// If commits access fails but repo info succeeded, still has some access
//						return new RepoAccessResponse(
//								true,
//								"User has limited access to the repository",
//								repoInfo.getName(),
//								owner,
//								repoInfo.isPrivate_repo(),
//								"limited"
//						);
//					}
//				}
//			} catch (HttpClientErrorException e) {
//				if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
//					return new RepoAccessResponse(
//							false,
//							"User does not have access to this repository or repository does not exist",
//							repo,
//							owner,
//							true, // Assume private since access denied
//							"none"
//					);
//				} else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
//					return new RepoAccessResponse(
//							false,
//							"Access forbidden - insufficient permissions",
//							repo,
//							owner,
//							true,
//							"none"
//					);
//				}
//			}
//
//			return new RepoAccessResponse(
//					false,
//					"Unable to determine repository access",
//					repo,
//					owner,
//					true,
//					"unknown"
//			);
//
//		} catch (Exception e) {
//			throw new RuntimeException("Error checking repository access: " + e.getMessage(), e);
//		}
//	}
//
//	public ContributorsResponse getContributors(String repoUrl, Long userId) {
//		try {
//			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
//			String owner = ownerRepo[0];
//			String repo = ownerRepo[1];
//			String accessToken = getAccessToken(userId);
//
//			String url = String.format("https://api.github.com/repos/%s/%s/contributors", owner, repo);
//
//			HttpHeaders headers = createHeaders(accessToken);
//			HttpEntity<?> entity = new HttpEntity<>(headers);
//
//			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
//
//			if (response.getStatusCode() == HttpStatus.OK) {
//				List<GitHubContributor> githubContributors = objectMapper.readValue(
//						response.getBody(),
//						new TypeReference<List<GitHubContributor>>() {}
//				);
//
//				List<Contributor> contributors = new ArrayList<>();
//				for (GitHubContributor gc : githubContributors) {
//					contributors.add(new Contributor(gc.getLogin(), gc.getHtml_url(), gc.getContributions()));
//				}
//
//				return new ContributorsResponse(contributors);
//			} else {
//				throw new RuntimeException("Failed to fetch contributors: " + response.getStatusCode());
//			}
//		} catch (Exception e) {
//			throw new RuntimeException("Error fetching contributors: " + e.getMessage(), e);
//		}
//	}
//
//	public CommitsResponse getCommitsByAuthor(String repoUrl, String authorName, Long userId) {
//		try {
//			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
//			String owner = ownerRepo[0];
//			String repo = ownerRepo[1];
//			String accessToken = getAccessToken(userId);
//
//			List<Commit> allCommits = new ArrayList<>();
//			int page = 1;
//			int perPage = 30; // GitHub default
//
//			HttpHeaders headers = createHeaders(accessToken);
//
//			while (true) {
//				String commitsUrl = String.format(
//						"https://api.github.com/repos/%s/%s/commits?author=%s&page=%d&per_page=%d",
//						owner, repo, authorName, page, perPage
//				);
//
//				HttpEntity<?> entity = new HttpEntity<>(headers);
//				ResponseEntity<String> response = restTemplate.exchange(commitsUrl, HttpMethod.GET, entity, String.class);
//
//				if (response.getStatusCode() != HttpStatus.OK) {
//					throw new RuntimeException("Failed to fetch commits: " + response.getStatusCode());
//				}
//
//				List<GitHubCommit> commits = objectMapper.readValue(
//						response.getBody(),
//						new TypeReference<List<GitHubCommit>>() {}
//				);
//
//				if (commits.isEmpty()) {
//					break; // No more commits
//				}
//
//				// Process each commit to get detailed information
//				for (GitHubCommit ghCommit : commits) {
//					Commit commit = getCommitDetails(owner, repo, ghCommit.getSha(), accessToken, headers);
//					if (commit != null) {
//						allCommits.add(commit);
//					}
//				}
//
//				page++;
//
//				// Break if we got fewer results than requested (last page)
//				if (commits.size() < perPage) {
//					break;
//				}
//			}
//
//			return new CommitsResponse(allCommits, authorName, allCommits.size());
//		} catch (Exception e) {
//			throw new RuntimeException("Error fetching commits: " + e.getMessage(), e);
//		}
//	}
//
//	private Commit getCommitDetails(String owner, String repo, String sha, String accessToken, HttpHeaders headers) {
//		try {
//			String commitUrl = String.format("https://api.github.com/repos/%s/%s/commits/%s", owner, repo, sha);
//
//			HttpEntity<?> entity = new HttpEntity<>(headers);
//			ResponseEntity<String> response = restTemplate.exchange(commitUrl, HttpMethod.GET, entity, String.class);
//
//			if (response.getStatusCode() == HttpStatus.OK) {
//				GitHubCommitDetail commitDetail = objectMapper.readValue(response.getBody(), GitHubCommitDetail.class);
//
//				List<FileData> fileDataList = new ArrayList<>();
//				if (commitDetail.getFiles() != null) {
//					for (GitHubCommitDetail.GitHubFile file : commitDetail.getFiles()) {
//						FileData fileData = mapToFileData(file);
//						fileDataList.add(fileData);
//					}
//				}
//
//				Commit commit = new Commit();
//				commit.setMsg(commitDetail.getCommit().getMessage());
//				commit.setSha(sha);
//				commit.setDate(commitDetail.getCommit().getAuthor().getDate());
//				commit.setFiles(fileDataList);
//
//				return commit;
//			}
//		} catch (Exception e) {
//			System.err.println("Error fetching commit details for SHA " + sha + ": " + e.getMessage());
//		}
//		return null;
//	}
//
//	private FileData mapToFileData(GitHubCommitDetail.GitHubFile githubFile) {
//		FileData fileData = new FileData();
//
//		// Extract just the filename from the full path
//		String fullPath = githubFile.getFilename();
//		String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
//		fileData.setFileName(fileName);
//
//		// Extract file extension
//		int dotIndex = fileName.lastIndexOf('.');
//		String extension = dotIndex > 0 ? fileName.substring(dotIndex + 1) : "";
//		fileData.setExtension(extension);
//
//		// Map status to operation
//		fileData.setOperation(githubFile.getStatus()); // added, modified, removed, renamed
//
//		// Set patch as code (this contains the diff)
//		fileData.setCode(githubFile.getPatch() != null ? githubFile.getPatch() : "");
//
//		// Set additions and deletions
//		fileData.setAdditions(githubFile.getAdditions());
//		fileData.setDeletions(githubFile.getDeletions());
//
//		return fileData;
//	}
//
//	public List<ContributorWithCommits> getContributorsWithCommits(String repoUrl, Long userId) {
//		try {
//			// Step 1: Get contributors
//			ContributorsResponse contributorsResponse = getContributors(repoUrl, userId);
//			List<Contributor> contributors = contributorsResponse.getContributorList();
//
//			List<ContributorWithCommits> result = new ArrayList<>();
//
//			// Step 2: For each contributor, get commits and combine
//			for (Contributor contributor : contributors) {
//				try {
//					CommitsResponse commitsResponse = getCommitsByAuthor(repoUrl, contributor.getAuthorName(), userId);
//					List<Commit> commits = commitsResponse.getCommitList();
//
//					ContributorWithCommits cwc = new ContributorWithCommits(
//							contributor.getAuthorName(),
//							contributor.getGithubUrl(),
//							contributor.getContributions(),
//							commits
//					);
//
//					result.add(cwc);
//				} catch (RuntimeException e) {
//					// If commits fail for one contributor, log and continue with empty commits list
//					System.err.println("Error fetching commits for contributor " + contributor.getAuthorName() + ": " + e.getMessage());
//					ContributorWithCommits cwc = new ContributorWithCommits(
//							contributor.getAuthorName(),
//							contributor.getGithubUrl(),
//							contributor.getContributions(),
//							new ArrayList<>() // empty commits
//					);
//					result.add(cwc);
//				}
//			}
//
//			return result;
//		} catch (IllegalArgumentException e) {
//			throw new RuntimeException("Invalid repository URL: " + e.getMessage(), e);
//		} catch (RuntimeException e) {
//			throw new RuntimeException("Error aggregating contributors with commits: " + e.getMessage(), e);
//		} catch (Exception e) {
//			throw new RuntimeException("Unexpected error in getContributorsWithCommits: " + e.getMessage(), e);
//		}
//	}
//
//}

//package com.example.vc_data_fetcher.service;
//
//import com.example.vc_data_fetcher.dto.*;
//import com.example.vc_data_fetcher.model.VCToken;
//import com.example.vc_data_fetcher.repository.VCTokenRepository;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//@Service
//public class VCDataService {
//
//	@Autowired
//	private VCTokenRepository vcTokenRepository;
//
//	private final RestTemplate restTemplate = new RestTemplate();
//	private final ObjectMapper objectMapper = new ObjectMapper();
//	private static final String GITHUB_GRAPHQL_URL = "https://api.github.com/graphql";
//
//	// GraphQL query to get repository info for access check
//	private static final String REPO_ACCESS_QUERY = """
//        query($owner: String!, $name: String!) {
//          repository(owner: $owner, name: $name) {
//            name
//            isPrivate
//            owner {
//              login
//            }
//          }
//        }
//        """;
//
//	// GraphQL query to get all contributors with their commits
//	private static final String CONTRIBUTORS_WITH_COMMITS_QUERY = """
//        query($owner: String!, $name: String!, $after: String) {
//          repository(owner: $owner, name: $name) {
//            collaborators(first: 100, after: $after) {
//              pageInfo {
//                hasNextPage
//                endCursor
//              }
//              nodes {
//                login
//                url
//                repositoriesContributedTo(first: 1, includeUserRepositories: false) {
//                  totalCount
//                }
//              }
//            }
//            defaultBranchRef {
//              target {
//                ... on Commit {
//                  history(first: 100) {
//                    pageInfo {
//                      hasNextPage
//                      endCursor
//                    }
//                    nodes {
//                      oid
//                      message
//                      committedDate
//                      author {
//                        user {
//                          login
//                        }
//                      }
//                      additions
//                      deletions
//                      changedFilesIfAvailable
//                    }
//                  }
//                }
//              }
//            }
//          }
//        }
//        """;
//
//	// GraphQL query to get commits by specific author
//	private static final String AUTHOR_COMMITS_QUERY = """
//        query($owner: String!, $name: String!, $author: String!, $after: String) {
//          repository(owner: $owner, name: $name) {
//            defaultBranchRef {
//              target {
//                ... on Commit {
//                  history(first: 100, after: $after, author: {id: $author}) {
//                    pageInfo {
//                      hasNextPage
//                      endCursor
//                    }
//                    nodes {
//                      oid
//                      message
//                      committedDate
//                      author {
//                        user {
//                          login
//                        }
//                      }
//                      additions
//                      deletions
//                      changedFilesIfAvailable
//                    }
//                  }
//                }
//              }
//            }
//          }
//        }
//        """;
//
//	// GraphQL query to get commit details including files
//	private static final String COMMIT_DETAILS_QUERY = """
//        query($owner: String!, $name: String!, $oid: String!) {
//          repository(owner: $owner, name: $name) {
//            object(oid: $oid) {
//              ... on Commit {
//                oid
//                message
//                committedDate
//                author {
//                  user {
//                    login
//                  }
//                }
//                additions
//                deletions
//                changedFilesIfAvailable
//              }
//            }
//          }
//        }
//        """;
//
//	// Extract owner and repo from GitHub URL
//	private String[] extractOwnerAndRepo(String repoUrl) {
//		Pattern pattern = Pattern.compile("https://github\\.com/([^/]+)/([^/]+)(?:\\.git)?/?$");
//		Matcher matcher = pattern.matcher(repoUrl.trim());
//
//		if (matcher.matches()) {
//			return new String[]{matcher.group(1), matcher.group(2)};
//		}
//		throw new IllegalArgumentException("Invalid GitHub repository URL: " + repoUrl);
//	}
//
//	// Get access token for user
//	private String getAccessToken(Long userId) {
//		Optional<VCToken> tokenOpt = vcTokenRepository.findByUserId(userId);
//		if (tokenOpt.isEmpty()) {
//			throw new RuntimeException("Access token not found for user: " + userId);
//		}
//		return tokenOpt.get().getAccess_token();
//	}
//
//	// Create HTTP headers for GraphQL requests
//	private HttpHeaders createGraphQLHeaders(String accessToken) {
//		HttpHeaders headers = new HttpHeaders();
//		headers.set("Authorization", "Bearer " + accessToken);
//		headers.set("Content-Type", "application/json");
//		headers.set("Accept", "application/json");
//		return headers;
//	}
//
//	// Execute GraphQL query
//	private JsonNode executeGraphQLQuery(String query, Map<String, Object> variables, String accessToken) {
//		try {
//			Map<String, Object> requestBody = new HashMap<>();
//			requestBody.put("query", query);
//			requestBody.put("variables", variables);
//
//			HttpHeaders headers = createGraphQLHeaders(accessToken);
//			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//			ResponseEntity<String> response = restTemplate.exchange(
//					GITHUB_GRAPHQL_URL, HttpMethod.POST, entity, String.class
//			);
//
//			if (response.getStatusCode() == HttpStatus.OK) {
//				JsonNode responseNode = objectMapper.readTree(response.getBody());
//
//				// Check for GraphQL errors
//				if (responseNode.has("errors")) {
//					throw new RuntimeException("GraphQL errors: " + responseNode.get("errors").toString());
//				}
//
//				return responseNode.get("data");
//			} else {
//				throw new RuntimeException("Failed to execute GraphQL query: " + response.getStatusCode());
//			}
//		} catch (Exception e) {
//			throw new RuntimeException("Error executing GraphQL query: " + e.getMessage(), e);
//		}
//	}
//
//	/**
//	 * Check if user has access to read the repository using GraphQL
//	 */
//	public boolean checkRepoAccess(String repoUrl, Long userId) {
//		try {
//			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
//			String owner = ownerRepo[0];
//			String repo = ownerRepo[1];
//			String accessToken = getAccessToken(userId);
//
//			Map<String, Object> variables = new HashMap<>();
//			variables.put("owner", owner);
//			variables.put("name", repo);
//
//			try {
//				JsonNode data = executeGraphQLQuery(REPO_ACCESS_QUERY, variables, accessToken);
//				return data.has("repository") && !data.get("repository").isNull();
//			} catch (RuntimeException e) {
//				if (e.getMessage().contains("Could not resolve")) {
//					return false;
//				}
//				throw e;
//			}
//		} catch (Exception e) {
//			throw new RuntimeException("Error checking repository access: " + e.getMessage(), e);
//		}
//	}
//
//	/**
//	 * Get all contributors with all their commits using GraphQL
//	 */
//	public List<ContributorWithCommits> getContributorsWithCommits(String repoUrl, Long userId) {
//		try {
//			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
//			String owner = ownerRepo[0];
//			String repo = ownerRepo[1];
//			String accessToken = getAccessToken(userId);
//
//			// Step 1: Get all contributors using REST API (as GraphQL doesn't have direct contributors endpoint)
//			List<Contributor> contributors = getContributorsFromRestAPI(owner, repo, accessToken);
//
//			// Step 2: For each contributor, get all their commits using GraphQL
//			List<ContributorWithCommits> result = new ArrayList<>();
//
//			for (Contributor contributor : contributors) {
//				try {
//					List<Commit> commits = getCommitsByAuthorGraphQL(owner, repo, contributor.getAuthorName(), accessToken);
//
//					ContributorWithCommits cwc = new ContributorWithCommits(
//							contributor.getAuthorName(),
//							contributor.getGithubUrl(),
//							contributor.getContributions(),
//							commits
//					);
//					result.add(cwc);
//
//				} catch (Exception e) {
//					System.err.println("Error fetching commits for contributor " + contributor.getAuthorName() + ": " + e.getMessage());
//					// Add contributor with empty commits list if commits fetch fails
//					ContributorWithCommits cwc = new ContributorWithCommits(
//							contributor.getAuthorName(),
//							contributor.getGithubUrl(),
//							contributor.getContributions(),
//							new ArrayList<>()
//					);
//					result.add(cwc);
//				}
//			}
//
//			return result;
//		} catch (Exception e) {
//			throw new RuntimeException("Error fetching contributors with commits: " + e.getMessage(), e);
//		}
//	}
//
//	/**
//	 * Get contributors using REST API (fallback as GraphQL doesn't have direct contributors endpoint)
//	 */
//	private List<Contributor> getContributorsFromRestAPI(String owner, String repo, String accessToken) {
//		try {
//			String url = String.format("https://api.github.com/repos/%s/%s/contributors", owner, repo);
//
//			HttpHeaders headers = new HttpHeaders();
//			headers.set("Authorization", "Bearer " + accessToken);
//			headers.set("Accept", "application/vnd.github+json");
//			headers.set("X-GitHub-Api-Version", "2022-11-28");
//
//			HttpEntity<?> entity = new HttpEntity<>(headers);
//			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
//
//			if (response.getStatusCode() == HttpStatus.OK) {
//				JsonNode contributorsArray = objectMapper.readTree(response.getBody());
//				List<Contributor> contributors = new ArrayList<>();
//
//				for (JsonNode contributorNode : contributorsArray) {
//					String login = contributorNode.get("login").asText();
//					String htmlUrl = contributorNode.get("html_url").asText();
//					int contributions = contributorNode.get("contributions").asInt();
//
//					contributors.add(new Contributor(login, htmlUrl, contributions));
//				}
//
//				return contributors;
//			} else {
//				throw new RuntimeException("Failed to fetch contributors: " + response.getStatusCode());
//			}
//		} catch (Exception e) {
//			throw new RuntimeException("Error fetching contributors: " + e.getMessage(), e);
//		}
//	}
//
//	/**
//	 * Get commits by author using GraphQL with pagination
//	 */
//	private List<Commit> getCommitsByAuthorGraphQL(String owner, String repo, String authorName, String accessToken) {
//		try {
//			List<Commit> allCommits = new ArrayList<>();
//			String cursor = null;
//			boolean hasNextPage = true;
//
//			while (hasNextPage) {
//				Map<String, Object> variables = new HashMap<>();
//				variables.put("owner", owner);
//				variables.put("name", repo);
//				variables.put("author", authorName);
//				if (cursor != null) {
//					variables.put("after", cursor);
//				}
//
//				// Use REST API to get commits by author (more reliable for author filtering)
//				String commitsUrl = String.format(
//						"https://api.github.com/repos/%s/%s/commits?author=%s&per_page=100",
//						owner, repo, authorName
//				);
//
//				if (cursor != null) {
//					// For pagination, we'll use page numbers instead of cursor for REST API
//					// This is a simplified approach - in production, you might want more sophisticated pagination
//				}
//
//				HttpHeaders headers = new HttpHeaders();
//				headers.set("Authorization", "Bearer " + accessToken);
//				headers.set("Accept", "application/vnd.github+json");
//
//				HttpEntity<?> entity = new HttpEntity<>(headers);
//				ResponseEntity<String> response = restTemplate.exchange(commitsUrl, HttpMethod.GET, entity, String.class);
//
//				if (response.getStatusCode() == HttpStatus.OK) {
//					JsonNode commitsArray = objectMapper.readTree(response.getBody());
//
//					if (commitsArray.size() == 0) {
//						break;
//					}
//
//					for (JsonNode commitNode : commitsArray) {
//						String sha = commitNode.get("sha").asText();
//						String message = commitNode.get("commit").get("message").asText();
//						String date = commitNode.get("commit").get("author").get("date").asText();
//
//						// Get detailed commit info including files
//						List<FileData> files = getCommitFilesDetails(owner, repo, sha, accessToken);
//
//						Commit commit = new Commit();
//						commit.setSha(sha);
//						commit.setMsg(message);
//						commit.setDate(date);
//						commit.setFiles(files);
//
//						allCommits.add(commit);
//					}
//
//					// For REST API, we break after first batch to avoid too many requests
//					// In production, you might implement proper pagination
//					break;
//				}
//			}
//
//			return allCommits;
//		} catch (Exception e) {
//			throw new RuntimeException("Error fetching commits for author " + authorName + ": " + e.getMessage(), e);
//		}
//	}
//
//	/**
//	 * Get detailed file information for a specific commit
//	 */
//	private List<FileData> getCommitFilesDetails(String owner, String repo, String sha, String accessToken) {
//		try {
//			String commitUrl = String.format("https://api.github.com/repos/%s/%s/commits/%s", owner, repo, sha);
//
//			HttpHeaders headers = new HttpHeaders();
//			headers.set("Authorization", "Bearer " + accessToken);
//			headers.set("Accept", "application/vnd.github+json");
//
//			HttpEntity<?> entity = new HttpEntity<>(headers);
//			ResponseEntity<String> response = restTemplate.exchange(commitUrl, HttpMethod.GET, entity, String.class);
//
//			if (response.getStatusCode() == HttpStatus.OK) {
//				JsonNode commitDetail = objectMapper.readTree(response.getBody());
//				JsonNode filesArray = commitDetail.get("files");
//
//				List<FileData> fileDataList = new ArrayList<>();
//
//				if (filesArray != null && filesArray.isArray()) {
//					for (JsonNode fileNode : filesArray) {
//						FileData fileData = new FileData();
//
//						String filename = fileNode.get("filename").asText();
//						String fileName = filename.substring(filename.lastIndexOf('/') + 1);
//						fileData.setFileName(fileName);
//
//						int dotIndex = fileName.lastIndexOf('.');
//						String extension = dotIndex > 0 ? fileName.substring(dotIndex + 1) : "";
//						fileData.setExtension(extension);
//
//						fileData.setOperation(fileNode.get("status").asText());
//						fileData.setCode(fileNode.has("patch") ? fileNode.get("patch").asText() : "");
//						fileData.setAdditions(fileNode.get("additions").asInt());
//						fileData.setDeletions(fileNode.get("deletions").asInt());
//
//						fileDataList.add(fileData);
//					}
//				}
//
//				return fileDataList;
//			}
//
//			return new ArrayList<>();
//		} catch (Exception e) {
//			System.err.println("Error fetching file details for commit " + sha + ": " + e.getMessage());
//			return new ArrayList<>();
//		}
//	}
//
//	// Legacy methods for backward compatibility
//	public ContributorsResponse getContributors(String repoUrl, Long userId) {
//		try {
//			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
//			String owner = ownerRepo[0];
//			String repo = ownerRepo[1];
//			String accessToken = getAccessToken(userId);
//
//			List<Contributor> contributors = getContributorsFromRestAPI(owner, repo, accessToken);
//			return new ContributorsResponse(contributors);
//		} catch (Exception e) {
//			throw new RuntimeException("Error fetching contributors: " + e.getMessage(), e);
//		}
//	}
//
//	public CommitsResponse getCommitsByAuthor(String repoUrl, String authorName, Long userId) {
//		try {
//			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
//			String owner = ownerRepo[0];
//			String repo = ownerRepo[1];
//			String accessToken = getAccessToken(userId);
//
//			List<Commit> commits = getCommitsByAuthorGraphQL(owner, repo, authorName, accessToken);
//			return new CommitsResponse(commits, authorName, commits.size());
//		} catch (Exception e) {
//			throw new RuntimeException("Error fetching commits: " + e.getMessage(), e);
//		}
//	}
//}

//package com.example.vc_data_fetcher.service;
//
//import com.example.vc_data_fetcher.dto.*;
//import com.example.vc_data_fetcher.model.VCToken;
//import com.example.vc_data_fetcher.repository.VCTokenRepository;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//@Service
//public class VCDataService {
//
//	@Autowired
//	private VCTokenRepository vcTokenRepository;
//
//	private final RestTemplate restTemplate = new RestTemplate();
//	private final ObjectMapper objectMapper = new ObjectMapper();
//	private static final String GITHUB_GRAPHQL_URL = "https://api.github.com/graphql";
//
//	// GraphQL query to check repository access
//	private static final String REPO_ACCESS_QUERY = """
//        query($owner: String!, $name: String!) {
//          repository(owner: $owner, name: $name) {
//            name
//            isPrivate
//            owner {
//              login
//            }
//          }
//        }
//        """;
//
//	// GraphQL query to get repository commits (we'll extract contributors from commits)
//	private static final String REPOSITORY_COMMITS_QUERY = """
//        query($owner: String!, $name: String!, $after: String) {
//          repository(owner: $owner, name: $name) {
//            defaultBranchRef {
//              target {
//                ... on Commit {
//                  history(first: 100, after: $after) {
//                    pageInfo {
//                      hasNextPage
//                      endCursor
//                    }
//                    nodes {
//                      oid
//                      message
//                      committedDate
//                      author {
//                        user {
//                          login
//                          url
//                        }
//                        name
//                        email
//                      }
//                      additions
//                      deletions
//                      changedFilesIfAvailable
//                    }
//                  }
//                }
//              }
//            }
//          }
//        }
//        """;
//
//	// Remove unused queries
//
//	// GraphQL query to get repository commits (we'll extract contributors from commits)
//	private static final String COMMIT_COMPARISON_QUERY = """
//        query($owner: String!, $name: String!, $oid: String!) {
//          repository(owner: $owner, name: $name) {
//            object(oid: $oid) {
//              ... on Commit {
//                oid
//                message
//                committedDate
//                author {
//                  user {
//                    login
//                  }
//                  name
//                  email
//                }
//                additions
//                deletions
//                changedFilesIfAvailable
//                tree {
//                  oid
//                }
//                parents(first: 1) {
//                  nodes {
//                    oid
//                    tree {
//                      oid
//                    }
//                  }
//                }
//              }
//            }
//          }
//        }
//        """;
//
//
//
//	/**
//	 * Extract owner and repo from GitHub URL
//	 */
//	private String[] extractOwnerAndRepo(String repoUrl) {
//		Pattern pattern = Pattern.compile("https://github\\.com/([^/]+)/([^/]+)(?:\\.git)?/?$");
//		Matcher matcher = pattern.matcher(repoUrl.trim());
//
//		if (matcher.matches()) {
//			return new String[]{matcher.group(1), matcher.group(2)};
//		}
//		throw new IllegalArgumentException("Invalid GitHub repository URL: " + repoUrl);
//	}
//
//	/**
//	 * Get access token for user
//	 */
//	private String getAccessToken(Long userId) {
//		Optional<VCToken> tokenOpt = vcTokenRepository.findByUserId(userId);
//		if (tokenOpt.isEmpty()) {
//			throw new RuntimeException("Access token not found for user: " + userId);
//		}
//		return tokenOpt.get().getAccess_token();
//	}
//
//	/**
//	 * Create HTTP headers for GraphQL requests
//	 */
//	private HttpHeaders createGraphQLHeaders(String accessToken) {
//		HttpHeaders headers = new HttpHeaders();
//		headers.set("Authorization", "Bearer " + accessToken);
//		headers.set("Content-Type", "application/json");
//		headers.set("Accept", "application/json");
//		return headers;
//	}
//
//	/**
//	 * Execute GraphQL query
//	 */
//	private JsonNode executeGraphQLQuery(String query, Map<String, Object> variables, String accessToken) {
//		try {
//			Map<String, Object> requestBody = new HashMap<>();
//			requestBody.put("query", query);
//			requestBody.put("variables", variables);
//
//			HttpHeaders headers = createGraphQLHeaders(accessToken);
//			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//			ResponseEntity<String> response = restTemplate.exchange(
//					GITHUB_GRAPHQL_URL, HttpMethod.POST, entity, String.class
//			);
//
//			if (response.getStatusCode() == HttpStatus.OK) {
//				JsonNode responseNode = objectMapper.readTree(response.getBody());
//
//				if (responseNode.has("errors")) {
//					throw new RuntimeException("GraphQL errors: " + responseNode.get("errors").toString());
//				}
//
//				return responseNode.get("data");
//			} else {
//				throw new RuntimeException("Failed to execute GraphQL query: " + response.getStatusCode());
//			}
//		} catch (Exception e) {
//			throw new RuntimeException("Error executing GraphQL query: " + e.getMessage(), e);
//		}
//	}
//
//	/**
//	 * Check if user has access to read the repository
//	 */
//	public boolean checkRepoAccess(String repoUrl, Long userId) {
//		try {
//			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
//			String owner = ownerRepo[0];
//			String repo = ownerRepo[1];
//			String accessToken = getAccessToken(userId);
//
//			Map<String, Object> variables = new HashMap<>();
//			variables.put("owner", owner);
//			variables.put("name", repo);
//
//			try {
//				JsonNode data = executeGraphQLQuery(REPO_ACCESS_QUERY, variables, accessToken);
//				return data.has("repository") && !data.get("repository").isNull();
//			} catch (RuntimeException e) {
//				if (e.getMessage().contains("Could not resolve")) {
//					return false;
//				}
//				throw e;
//			}
//		} catch (Exception e) {
//			throw new RuntimeException("Error checking repository access: " + e.getMessage(), e);
//		}
//	}
//
//	/**
//	 * Get all contributors with their commits using GraphQL
//	 */
//	public List<ContributorWithCommits> getContributorsWithCommits(String repoUrl, Long userId) {
//		try {
//			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
//			String owner = ownerRepo[0];
//			String repo = ownerRepo[1];
//			String accessToken = getAccessToken(userId);
//
//			// Get contributors
//			List<Contributor> contributors = getContributorsGraphQL(owner, repo, accessToken);
//
//			// Get commits for each contributor
//			List<ContributorWithCommits> result = new ArrayList<>();
//
//			for (Contributor contributor : contributors) {
//				try {
//					List<Commit> commits = getCommitsByAuthorGraphQL(owner, repo, contributor.getAuthorName(), accessToken);
//
//					ContributorWithCommits cwc = new ContributorWithCommits(
//							contributor.getAuthorName(),
//							contributor.getGithubUrl(),
//							contributor.getContributions(),
//							commits
//					);
//					result.add(cwc);
//
//				} catch (Exception e) {
//					System.err.println("Error fetching commits for contributor " + contributor.getAuthorName() + ": " + e.getMessage());
//					// Add contributor with empty commits list if commits fetch fails
//					ContributorWithCommits cwc = new ContributorWithCommits(
//							contributor.getAuthorName(),
//							contributor.getGithubUrl(),
//							contributor.getContributions(),
//							new ArrayList<>()
//					);
//					result.add(cwc);
//				}
//			}
//
//			return result;
//		} catch (Exception e) {
//			throw new RuntimeException("Error fetching contributors with commits: " + e.getMessage(), e);
//		}
//	}
//
//	/**
//	 * Get contributors using GraphQL by extracting unique authors from commits
//	 */
//	private List<Contributor> getContributorsGraphQL(String owner, String repo, String accessToken) {
//		try {
//			Map<String, Contributor> contributorsMap = new HashMap<>();
//			String cursor = null;
//			boolean hasNextPage = true;
//
//			while (hasNextPage) {
//				Map<String, Object> variables = new HashMap<>();
//				variables.put("owner", owner);
//				variables.put("name", repo);
//				if (cursor != null) {
//					variables.put("after", cursor);
//				}
//
//				JsonNode data = executeGraphQLQuery(REPOSITORY_COMMITS_QUERY, variables, accessToken);
//				JsonNode repository = data.get("repository");
//
//				if (repository != null && !repository.isNull()) {
//					JsonNode defaultBranch = repository.get("defaultBranchRef");
//					if (defaultBranch != null) {
//						JsonNode target = defaultBranch.get("target");
//						JsonNode history = target.get("history");
//						JsonNode nodes = history.get("nodes");
//						JsonNode pageInfo = history.get("pageInfo");
//
//						// Extract contributors from commits
//						for (JsonNode commitNode : nodes) {
//							JsonNode author = commitNode.get("author");
//							if (author != null) {
//								JsonNode user = author.get("user");
//								String login = null;
//								String url = null;
//
//								if (user != null && !user.isNull()) {
//									login = user.get("login").asText();
//									url = user.get("url").asText();
//								} else {
//									// Fallback to author name if user is null
//									login = author.get("name") != null ? author.get("name").asText() : "Unknown";
//									url = "https://github.com/" + login;
//								}
//
//								if (login != null) {
//									contributorsMap.putIfAbsent(login, new Contributor(login, url, 0));
//									// Increment contribution count
//									Contributor contributor = contributorsMap.get(login);
//									contributorsMap.put(login, new Contributor(login, url, contributor.getContributions() + 1));
//								}
//							}
//						}
//
//						hasNextPage = pageInfo.get("hasNextPage").asBoolean();
//						if (hasNextPage) {
//							cursor = pageInfo.get("endCursor").asText();
//						}
//					} else {
//						break;
//					}
//				} else {
//					break;
//				}
//			}
//
//			return new ArrayList<>(contributorsMap.values());
//		} catch (Exception e) {
//			throw new RuntimeException("Error fetching contributors: " + e.getMessage(), e);
//		}
//	}
//
//	/**
//	 * Get commits by author using GraphQL
//	 */
//	private List<Commit> getCommitsByAuthorGraphQL(String owner, String repo, String authorLogin, String accessToken) {
//		try {
//			List<Commit> allCommits = new ArrayList<>();
//			String cursor = null;
//			boolean hasNextPage = true;
//
//			while (hasNextPage) {
//				Map<String, Object> variables = new HashMap<>();
//				variables.put("owner", owner);
//				variables.put("name", repo);
//				if (cursor != null) {
//					variables.put("after", cursor);
//				}
//
//				JsonNode data = executeGraphQLQuery(REPOSITORY_COMMITS_QUERY, variables, accessToken);
//				JsonNode repository = data.get("repository");
//
//				if (repository != null && !repository.isNull()) {
//					JsonNode defaultBranch = repository.get("defaultBranchRef");
//					if (defaultBranch != null) {
//						JsonNode target = defaultBranch.get("target");
//						JsonNode history = target.get("history");
//						JsonNode nodes = history.get("nodes");
//						JsonNode pageInfo = history.get("pageInfo");
//
//						for (JsonNode commitNode : nodes) {
//							JsonNode author = commitNode.get("author");
//							if (author != null) {
//								String commitAuthor = null;
//								JsonNode user = author.get("user");
//
//								if (user != null && !user.isNull()) {
//									commitAuthor = user.get("login").asText();
//								} else {
//									commitAuthor = author.get("name") != null ? author.get("name").asText() : "Unknown";
//								}
//
//								// Filter commits by author
//								if (authorLogin.equals(commitAuthor)) {
//									String sha = commitNode.get("oid").asText();
//									String message = commitNode.get("message").asText();
//									String date = commitNode.get("committedDate").asText();
//
//									// Get file changes for this commit
//									List<FileData> files = getCommitFilesGraphQL(owner, repo, commitNode, accessToken);
//
//									Commit commit = new Commit();
//									commit.setSha(sha);
//									commit.setMsg(message);
//									commit.setDate(date);
//									commit.setFiles(files);
//
//									allCommits.add(commit);
//								}
//							}
//						}
//
//						hasNextPage = pageInfo.get("hasNextPage").asBoolean();
//						if (hasNextPage) {
//							cursor = pageInfo.get("endCursor").asText();
//						}
//					} else {
//						break;
//					}
//				} else {
//					break;
//				}
//			}
//
//			return allCommits;
//		} catch (Exception e) {
//			throw new RuntimeException("Error fetching commits for author " + authorLogin + ": " + e.getMessage(), e);
//		}
//	}
//
//	/**
//	 * Get file details for a commit using REST API (fallback since GraphQL comparison is complex)
//	 */
//	private List<FileData> getCommitFilesGraphQL(String owner, String repo, JsonNode commitNode, String accessToken) {
//		try {
//			String sha = commitNode.get("oid").asText();
//
//			// Use REST API for file details as it's more reliable and simpler
//			String commitUrl = String.format("https://api.github.com/repos/%s/%s/commits/%s", owner, repo, sha);
//
//			HttpHeaders headers = new HttpHeaders();
//			headers.set("Authorization", "Bearer " + accessToken);
//			headers.set("Accept", "application/vnd.github+json");
//
//			HttpEntity<?> entity = new HttpEntity<>(headers);
//			ResponseEntity<String> response = restTemplate.exchange(commitUrl, HttpMethod.GET, entity, String.class);
//
//			if (response.getStatusCode() == HttpStatus.OK) {
//				JsonNode commitDetail = objectMapper.readTree(response.getBody());
//				JsonNode filesArray = commitDetail.get("files");
//
//				List<FileData> fileDataList = new ArrayList<>();
//
//				if (filesArray != null && filesArray.isArray()) {
//					for (JsonNode fileNode : filesArray) {
//						FileData fileData = new FileData();
//
//						String filename = fileNode.get("filename").asText();
//						String fileName = filename.substring(filename.lastIndexOf('/') + 1);
//						fileData.setFileName(fileName);
//
//						int dotIndex = fileName.lastIndexOf('.');
//						String extension = dotIndex > 0 ? fileName.substring(dotIndex + 1) : "";
//						fileData.setExtension(extension);
//
//						fileData.setOperation(fileNode.get("status").asText());
//						fileData.setCode(fileNode.has("patch") ? fileNode.get("patch").asText() : "");
//						fileData.setAdditions(fileNode.get("additions").asInt());
//						fileData.setDeletions(fileNode.get("deletions").asInt());
//
//						fileDataList.add(fileData);
//					}
//				}
//
//				return fileDataList;
//			}
//
//			return new ArrayList<>();
//		} catch (Exception e) {
//			System.err.println("Error fetching file details for commit: " + e.getMessage());
//			return new ArrayList<>();
//		}
//	}
//
//	// Legacy methods for backward compatibility
//	public ContributorsResponse getContributors(String repoUrl, Long userId) {
//		try {
//			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
//			String owner = ownerRepo[0];
//			String repo = ownerRepo[1];
//			String accessToken = getAccessToken(userId);
//
//			List<Contributor> contributors = getContributorsGraphQL(owner, repo, accessToken);
//			return new ContributorsResponse(contributors);
//		} catch (Exception e) {
//			throw new RuntimeException("Error fetching contributors: " + e.getMessage(), e);
//		}
//	}
//
//	public CommitsResponse getCommitsByAuthor(String repoUrl, String authorName, Long userId) {
//		try {
//			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
//			String owner = ownerRepo[0];
//			String repo = ownerRepo[1];
//			String accessToken = getAccessToken(userId);
//
//			List<Commit> commits = getCommitsByAuthorGraphQL(owner, repo, authorName, accessToken);
//			return new CommitsResponse(commits, authorName, commits.size());
//		} catch (Exception e) {
//			throw new RuntimeException("Error fetching commits: " + e.getMessage(), e);
//		}
//	}
//}

package com.example.vc_data_fetcher.service;

import com.example.vc_data_fetcher.dto.*;
import com.example.vc_data_fetcher.model.VCToken;
import com.example.vc_data_fetcher.repository.VCTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VCDataService {

	@Autowired
	private VCTokenRepository vcTokenRepository;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();
	private static final String GITHUB_GRAPHQL_URL = "https://api.github.com/graphql";

	// GraphQL query to check repository access
	private static final String REPO_ACCESS_QUERY = """
        query($owner: String!, $name: String!) {
          repository(owner: $owner, name: $name) {
            name
            isPrivate
            owner {
              login
            }
          }
        }
        """;

	// GraphQL query to get repository commits (we'll extract contributors from commits)
	private static final String REPOSITORY_COMMITS_QUERY = """
        query($owner: String!, $name: String!, $after: String) {
          repository(owner: $owner, name: $name) {
            defaultBranchRef {
              target {
                ... on Commit {
                  history(first: 100, after: $after) {
                    pageInfo {
                      hasNextPage
                      endCursor
                    }
                    nodes {
                      oid
                      message
                      committedDate
                      author {
                        user {
                          login
                          url
                        }
                        name
                        email
                      }
                      additions
                      deletions
                      changedFilesIfAvailable
                    }
                  }
                }
              }
            }
          }
        }
        """;

	/**
	 * Extract owner and repo from GitHub URL
	 */
	private String[] extractOwnerAndRepo(String repoUrl) {
		Pattern pattern = Pattern.compile("https://github\\.com/([^/]+)/([^/]+)(?:\\.git)?/?$");
		Matcher matcher = pattern.matcher(repoUrl.trim());

		if (matcher.matches()) {
			return new String[]{matcher.group(1), matcher.group(2)};
		}
		throw new IllegalArgumentException("Invalid GitHub repository URL: " + repoUrl);
	}

	/**
	 * Get access token for user
	 */
	private String getAccessToken(Long userId) {
		Optional<VCToken> tokenOpt = vcTokenRepository.findByUserId(userId);
		if (tokenOpt.isEmpty()) {
			throw new RuntimeException("Access token not found for user: " + userId);
		}
		return tokenOpt.get().getAccess_token();
	}

	/**
	 * Create HTTP headers for GraphQL requests
	 */
	private HttpHeaders createGraphQLHeaders(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + accessToken);  // ✅ Important
		headers.set("Accept", "application/vnd.github+json");
		return headers;
	}

	/**
	 * Execute GraphQL query
	 */
	private JsonNode executeGraphQLQuery(String query, Map<String, Object> variables, String accessToken) {
		try {
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("query", query);
			requestBody.put("variables", variables);

			HttpHeaders headers = createGraphQLHeaders(accessToken);
			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

			ResponseEntity<String> response = restTemplate.exchange(
					GITHUB_GRAPHQL_URL, HttpMethod.POST, entity, String.class
			);

			if (response.getStatusCode() == HttpStatus.OK) {
				JsonNode responseNode = objectMapper.readTree(response.getBody());

				if (responseNode.has("errors")) {
					throw new RuntimeException("GraphQL errors: " + responseNode.get("errors").toString());
				}

				return responseNode.get("data");
			} else {
				throw new RuntimeException("Failed to execute GraphQL query: " + response.getStatusCode());
			}
		} catch (Exception e) {
			throw new RuntimeException("Error executing GraphQL query: " + e.getMessage(), e);
		}
	}

	/**
	 * Check if user has access to read the repository
	 */
	public ResponseEntity checkRepoAccess(String repoUrl, Long userId) {
		try {
			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
			String owner = ownerRepo[0];
			String repo = ownerRepo[1];
			String accessToken = getAccessToken(userId);

			Map<String, Object> variables = new HashMap<>();
			variables.put("owner", owner);
			variables.put("name", repo);

			try {
				JsonNode data = executeGraphQLQuery(REPO_ACCESS_QUERY, variables, accessToken);
				return ResponseEntity.status(HttpStatus.OK).build();
			} catch (RuntimeException e) {
				if (e.getMessage().contains("Could not resolve")) {
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
				}
				throw e;
			}
		} catch (Exception e) {
			throw new RuntimeException("Error checking repository access: " + e.getMessage(), e);
		}
	}

	/**
	 * Get all contributors with their commits using GraphQL
	 */
	public List<ContributorWithCommits> getContributorsWithCommits(String repoUrl, Long userId) {
		try {
			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
			String owner = ownerRepo[0];
			String repo = ownerRepo[1];
			String accessToken = getAccessToken(userId);

			// Get contributors
			List<Contributor> contributors = getContributorsGraphQL(owner, repo, accessToken);

			// Get commits for each contributor
			List<ContributorWithCommits> result = new ArrayList<>();

			for (Contributor contributor : contributors) {
				try {
					List<Commit> commits = getCommitsByAuthorGraphQL(owner, repo, contributor.getAuthorName(), accessToken);

					ContributorWithCommits cwc = new ContributorWithCommits(
							contributor.getAuthorName(),
							contributor.getGithubUrl(),
							contributor.getContributions(),
							commits
					);
					result.add(cwc);

				} catch (Exception e) {
					System.err.println("Error fetching commits for contributor " + contributor.getAuthorName() + ": " + e.getMessage());
					// Add contributor with empty commits list if commits fetch fails
					ContributorWithCommits cwc = new ContributorWithCommits(
							contributor.getAuthorName(),
							contributor.getGithubUrl(),
							contributor.getContributions(),
							new ArrayList<>()
					);
					result.add(cwc);
				}
			}

			return result;
		} catch (Exception e) {
			throw new RuntimeException("Error fetching contributors with commits: " + e.getMessage(), e);
		}
	}

	/**
	 * Get contributors using GraphQL by extracting unique authors from commits
	 */
	private List<Contributor> getContributorsGraphQL(String owner, String repo, String accessToken) {
		try {
			Map<String, Contributor> contributorsMap = new HashMap<>();
			String cursor = null;
			boolean hasNextPage = true;

			while (hasNextPage) {
				Map<String, Object> variables = new HashMap<>();
				variables.put("owner", owner);
				variables.put("name", repo);
				if (cursor != null) {
					variables.put("after", cursor);
				}

				JsonNode data = executeGraphQLQuery(REPOSITORY_COMMITS_QUERY, variables, accessToken);
				JsonNode repository = data.get("repository");

				if (repository != null && !repository.isNull()) {
					JsonNode defaultBranch = repository.get("defaultBranchRef");
					if (defaultBranch != null) {
						JsonNode target = defaultBranch.get("target");
						JsonNode history = target.get("history");
						JsonNode nodes = history.get("nodes");
						JsonNode pageInfo = history.get("pageInfo");

						// Extract contributors from commits
						for (JsonNode commitNode : nodes) {
							JsonNode author = commitNode.get("author");
							if (author != null) {
								JsonNode user = author.get("user");
								String login = null;
								String url = null;

								if (user != null && !user.isNull()) {
									login = user.get("login").asText();
									url = user.get("url").asText();
								} else {
									// Fallback to author name if user is null
									login = author.get("name") != null ? author.get("name").asText() : "Unknown";
									url = "https://github.com/" + login;
								}

								if (login != null) {
									contributorsMap.putIfAbsent(login, new Contributor(login, url, 0));
									// Increment contribution count
									Contributor contributor = contributorsMap.get(login);
									contributorsMap.put(login, new Contributor(login, url, contributor.getContributions() + 1));
								}
							}
						}

						hasNextPage = pageInfo.get("hasNextPage").asBoolean();
						if (hasNextPage) {
							cursor = pageInfo.get("endCursor").asText();
						}
					} else {
						break;
					}
				} else {
					break;
				}
			}

			return new ArrayList<>(contributorsMap.values());
		} catch (Exception e) {
			throw new RuntimeException("Error fetching contributors: " + e.getMessage(), e);
		}
	}

	/**
	 * Get commits by author using GraphQL
	 */
	private List<Commit> getCommitsByAuthorGraphQL(String owner, String repo, String authorLogin, String accessToken) {
		try {
			List<Commit> allCommits = new ArrayList<>();
			String cursor = null;
			boolean hasNextPage = true;

			while (hasNextPage) {
				Map<String, Object> variables = new HashMap<>();
				variables.put("owner", owner);
				variables.put("name", repo);
				if (cursor != null) {
					variables.put("after", cursor);
				}

				JsonNode data = executeGraphQLQuery(REPOSITORY_COMMITS_QUERY, variables, accessToken);
				JsonNode repository = data.get("repository");

				if (repository != null && !repository.isNull()) {
					JsonNode defaultBranch = repository.get("defaultBranchRef");
					if (defaultBranch != null) {
						JsonNode target = defaultBranch.get("target");
						JsonNode history = target.get("history");
						JsonNode nodes = history.get("nodes");
						JsonNode pageInfo = history.get("pageInfo");

						for (JsonNode commitNode : nodes) {
							JsonNode author = commitNode.get("author");
							if (author != null) {
								String commitAuthor = null;
								JsonNode user = author.get("user");

								if (user != null && !user.isNull()) {
									commitAuthor = user.get("login").asText();
								} else {
									commitAuthor = author.get("name") != null ? author.get("name").asText() : "Unknown";
								}

								// Filter commits by author
								if (authorLogin.equals(commitAuthor)) {
									String sha = commitNode.get("oid").asText();
									String message = commitNode.get("message").asText();
									String date = commitNode.get("committedDate").asText();

									// Get comprehensive file changes for this commit
									List<FileData> files = getCommitFilesWithPatchData(owner, repo, sha, accessToken);

									Commit commit = new Commit();
									commit.setSha(sha);
									commit.setMsg(message);
									commit.setDate(date);
									commit.setFiles(files);

									allCommits.add(commit);
								}
							}
						}

						hasNextPage = pageInfo.get("hasNextPage").asBoolean();
						if (hasNextPage) {
							cursor = pageInfo.get("endCursor").asText();
						}
					} else {
						break;
					}
				} else {
					break;
				}
			}

			return allCommits;
		} catch (Exception e) {
			throw new RuntimeException("Error fetching commits for author " + authorLogin + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Get comprehensive file details with all patch data for a commit using REST API
	 */
	private List<FileData> getCommitFilesWithPatchData(String owner, String repo, String sha, String accessToken) {
		try {
			String commitUrl = String.format("https://api.github.com/repos/%s/%s/commits/%s", owner, repo, sha);

			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", "Bearer " + accessToken);
			headers.set("Accept", "application/vnd.github+json");
			headers.set("X-GitHub-Api-Version", "2022-11-28");

			HttpEntity<?> entity = new HttpEntity<>(headers);
			ResponseEntity<String> response = restTemplate.exchange(commitUrl, HttpMethod.GET, entity, String.class);

			if (response.getStatusCode() == HttpStatus.OK) {
				JsonNode commitDetail = objectMapper.readTree(response.getBody());
				JsonNode filesArray = commitDetail.get("files");

				List<FileData> fileDataList = new ArrayList<>();

				if (filesArray != null && filesArray.isArray()) {
					for (JsonNode fileNode : filesArray) {
						FileData fileData = new FileData();

						// Get full filename path
						String fullFilename = fileNode.get("filename").asText();
						String fileName = fullFilename.substring(fullFilename.lastIndexOf('/') + 1);
						fileData.setFileName(fileName);
						fileData.setFullPath(fullFilename); // Store full path

						// Extract file extension
						int dotIndex = fileName.lastIndexOf('.');
						String extension = dotIndex > 0 ? fileName.substring(dotIndex + 1) : "";
						fileData.setExtension(extension);

						// Get file operation status (added, modified, deleted, renamed)
						String status = fileNode.get("status").asText();
						fileData.setOperation(status);

						// Get the patch/code changes - this is the key data
						String patchData = "";
						if (fileNode.has("patch") && !fileNode.get("patch").isNull()) {
							patchData = fileNode.get("patch").asText();
						} else {
							// Handle special cases
							switch (status) {
								case "added":
									patchData = "// New file added";
									break;
								case "removed":
									patchData = "// File deleted";
									break;
								case "renamed":
									String previousFilename = fileNode.has("previous_filename") ?
											fileNode.get("previous_filename").asText() : "unknown";
									patchData = "// File renamed from: " + previousFilename + " to: " + fullFilename;
									break;
								default:
									patchData = "// No patch data available";
							}
						}
						fileData.setCode(patchData);

						// Get comprehensive file statistics
						fileData.setAdditions(fileNode.has("additions") ? fileNode.get("additions").asInt() : 0);
						fileData.setDeletions(fileNode.has("deletions") ? fileNode.get("deletions").asInt() : 0);
						fileData.setChanges(fileNode.has("changes") ? fileNode.get("changes").asInt() :
								(fileData.getAdditions() + fileData.getDeletions()));

						// Additional metadata
						fileData.setBinaryFile(fileNode.has("binary") ? fileNode.get("binary").asBoolean() : false);
						if (fileNode.has("previous_filename")) {
							fileData.setPreviousFilename(fileNode.get("previous_filename").asText());
						}

						fileDataList.add(fileData);

						// Debug logging
						System.out.println(String.format(
								"File: %s, Status: %s, Patch length: %d chars, +%d -%d",
								fileName, status, patchData.length(),
								fileData.getAdditions(), fileData.getDeletions()
						));
					}
				}

				return fileDataList;
			} else {
				System.err.println("Failed to fetch commit details. Status: " + response.getStatusCode());
				return new ArrayList<>();
			}

		} catch (Exception e) {
			System.err.println("Error fetching file details for commit " + sha + ": " + e.getMessage());
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/**
	 * Legacy method - kept for backward compatibility
	 */
	private List<FileData> getCommitFilesGraphQL(String owner, String repo, JsonNode commitNode, String accessToken) {
		String sha = commitNode.get("oid").asText();
		return getCommitFilesWithPatchData(owner, repo, sha, accessToken);
	}

	// Legacy methods for backward compatibility
	public ContributorsResponse getContributors(String repoUrl, Long userId) {
		try {
			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
			String owner = ownerRepo[0];
			String repo = ownerRepo[1];
			String accessToken = getAccessToken(userId);

			List<Contributor> contributors = getContributorsGraphQL(owner, repo, accessToken);
			return new ContributorsResponse(contributors);
		} catch (Exception e) {
			throw new RuntimeException("Error fetching contributors: " + e.getMessage(), e);
		}
	}

	public CommitsResponse getCommitsByAuthor(String repoUrl, String authorName, Long userId) {
		try {
			String[] ownerRepo = extractOwnerAndRepo(repoUrl);
			String owner = ownerRepo[0];
			String repo = ownerRepo[1];
			String accessToken = getAccessToken(userId);

			List<Commit> commits = getCommitsByAuthorGraphQL(owner, repo, authorName, accessToken);
			return new CommitsResponse(commits, authorName, commits.size());
		} catch (Exception e) {
			throw new RuntimeException("Error fetching commits: " + e.getMessage(), e);
		}
	}
}

