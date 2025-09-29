package com.example.vc_data_fetcher.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepoInfo {
	private String name;
	private String full_name;
	private boolean private_repo;
	private GitHubOwner owner;
	private String default_branch;
	private String description;

	@Getter
	@Setter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class GitHubOwner {
		private String login;
		private String html_url;
	}
}