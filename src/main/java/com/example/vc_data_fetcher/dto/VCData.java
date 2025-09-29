package com.example.vc_data_fetcher.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class VCData {
	List<Contributor> contributorList;
	List<Commit> commitList;
}
