/**
 * 
 */
package com.vzw.pdi.hackathon.server;

/**
 * @author fred
 *
 */
public class Fling {
	
	int id;
	String contentType;
	String filePath;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	@Override
	public String toString() {
		return "Fling [id=" + id + ", contentType=" + contentType
				+ ", filePath=" + filePath + "]";
	}

}
