package com.example.vc_data_fetcher.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RepoAccessResponse {
	private boolean hasAccess;
	private String message;
	private String repoName;
	private String owner;
	private boolean isPrivate;
}