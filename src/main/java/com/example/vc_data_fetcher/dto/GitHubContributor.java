package com.example.vc_data_fetcher.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubContributor {
	private String login;
	private String html_url;
	private int contributions;
}