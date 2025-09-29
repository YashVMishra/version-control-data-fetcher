package com.example.vc_data_fetcher.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubCommit {
	private String sha;
	private GitHubCommitDetails commit;

	@Getter
	@Setter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class GitHubCommitDetails {
		private String message;
		private GitHubAuthor author;

		@Getter
		@Setter
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class GitHubAuthor {
			private String date;
		}
	}
}