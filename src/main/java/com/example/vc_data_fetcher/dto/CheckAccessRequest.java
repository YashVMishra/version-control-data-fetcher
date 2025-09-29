package com.example.vc_data_fetcher.dto;

public class CheckAccessRequest {
	private String repoUrl;
	private Long userId;

	// Getters and setters
	public String getRepoUrl() { return repoUrl; }
	public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
	public Long getUserId() { return userId; }
	public void setUserId(Long userId) { this.userId = userId; }
}
