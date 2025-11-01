// Enhanced FileData DTO to capture all patch information
package com.example.vc_data_fetcher.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileData {
	private String fileName;
	private String fullPath;        // NEW: Full file path in repository
	private String extension;
	private String operation;       // added, modified, deleted, renamed
	private String code;            // The actual patch/diff data
	private int additions;          // Number of lines added
	private int deletions;          // Number of lines deleted
	private int changes;            // NEW: Total changes (additions + deletions)
	private boolean binaryFile;     // NEW: Whether this is a binary file
	private String previousFilename; // NEW: Previous filename if renamed

	@Override
	public String toString() {
		return String.format(
				"FileData{fileName='%s', fullPath='%s', operation='%s', +%d -%d, binary=%s}",
				fileName, fullPath, operation, additions, deletions, binaryFile
		);
	}
}