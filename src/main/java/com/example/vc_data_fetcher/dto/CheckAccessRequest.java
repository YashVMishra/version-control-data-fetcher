package com.example.vc_data_fetcher.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CheckAccessRequest {
	private String repoUrl;
	private Long userId;
}
