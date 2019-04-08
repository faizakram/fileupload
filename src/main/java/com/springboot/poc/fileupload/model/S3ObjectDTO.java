package com.springboot.poc.fileupload.model;

import java.util.Date;

public class S3ObjectDTO {
	private Long fileSize;
	private String fileName;
	private Date fileModifiedDate;
	private String fileTag;
	
	public long getFileSize() {
		return fileSize;
	}
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public Date getFileModifiedDate() {
		return fileModifiedDate;
	}
	public void setFileModifiedDate(Date fileModifiedDate) {
		this.fileModifiedDate = fileModifiedDate;
	}
	public String getFileTag() {
		return fileTag;
	}
	public void setFileTag(String fileTag) {
		this.fileTag = fileTag;
	}
}
