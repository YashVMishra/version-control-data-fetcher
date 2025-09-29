package com.example.vc_data_fetcher.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Contributor {
	private String authorName;
	private String githubUrl;
	private int contributions;
}