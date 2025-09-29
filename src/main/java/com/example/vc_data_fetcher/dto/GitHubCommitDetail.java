package com.example.vc_data_fetcher.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubCommitDetail {
	private GitHubCommitInfo commit;
	private List<GitHubFile> files;

	@Getter
	@Setter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class GitHubCommitInfo {
		private String message;
		private GitHubAuthor author;

		@Getter
		@Setter
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class GitHubAuthor {
			private String date;
		}
	}

	@Getter
	@Setter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class GitHubFile {
		private String filename;
		private String status;
		private int additions;
		private int deletions;
		private String patch;
	}
}