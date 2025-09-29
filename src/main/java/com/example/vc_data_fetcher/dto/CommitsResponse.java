package com.example.vc_data_fetcher.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommitsResponse {
	private List<Commit> commitList;
	private String authorName;
	private int totalCommits;
}