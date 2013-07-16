package com.vzw.vma.message;

public enum VMAFlags {
	SEEN("\\Seen"),
	SENT("$Sent"),
	DELETED("\\Deleted"),
	THUMBNAIL("$Thumbnail");
	
	  private String text;

	  VMAFlags(String text) {
	    this.text = text;
	  }

	  public String getText() {
	    return this.text;
	  }

	  public static VMAFlags fromString(String text) {
	    if (text != null) {
	      for (VMAFlags b : VMAFlags.values()) {
	        if (text.equalsIgnoreCase(b.text)) {
	          return b;
	        }
	      }
	    }
	    throw new RuntimeException("Invalid flag " + text);
	  }
}
