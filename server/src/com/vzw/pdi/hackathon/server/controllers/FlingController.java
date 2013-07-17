/**
 * 
 */
package com.vzw.pdi.hackathon.server.controllers;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import net.sf.serfj.RestController;
import net.sf.serfj.annotations.DoNotRenderPage;
import net.sf.serfj.annotations.GET;
import net.sf.serfj.annotations.POST;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import com.vzw.pdi.hackathon.server.Fling;
import com.vzw.pdi.hackathon.server.FlingManager;

/**
 * @author fred
 *
 */
public class FlingController extends RestController {
	
	private static final Logger	logger = Logger.getLogger(FlingController.class);


	private static final String FILE_PATH = "/tmp/flings/";
	
	
	@GET
	@DoNotRenderPage
	public void show() {
		
		int id = Integer.parseInt(this.getId());
		
		Fling fling = FlingManager.getInstance().get(id);
		
	    // Set the file location
	    this.getResponseHelper().setFile(new File(fling.getFilePath()));
	    this.getResponseHelper().setContentType(fling.getContentType());
	    // SerfJ will download the file
	}
	
	
	@POST
	public void create(){
		int id = -1;
		
		HttpServletRequest request = this.getResponseHelper().getRequest();

		// Check that we have a file upload request
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);

		if (isMultipart) {
			// Create a factory for disk-based file items
			DiskFileItemFactory factory = new DiskFileItemFactory();

			// Configure a repository (to ensure a secure temp location is used)
			File repository = new File(FILE_PATH);
			factory.setRepository(repository);

			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(factory);

			// Parse the request
			try {
				List<FileItem> items = upload.parseRequest(request);
				
				// Process the uploaded items
				Iterator<FileItem> iter = items.iterator();
				while (iter.hasNext()) {
				    FileItem item = iter.next();

				    if (item.isFormField()) {
				        processFormField(item);
				    } else {
				        id = processUploadedFile(item);
				    }
				}
				
				this.putParam("id", id);

			} catch (FileUploadException e) {
				logger.error("Error uploading file", e);
			}
			
		}
		
	}
	
	
	protected void processFormField(FileItem item){
		
	}
	
	protected int processUploadedFile(FileItem item){
		int id = -1;
		// Process a file upload
		if (!item.isFormField()) {
		    String fieldName = item.getFieldName();
		    String fileName = item.getName();
		    String contentType = item.getContentType();
		    boolean isInMemory = item.isInMemory();
		    long sizeInBytes = item.getSize();
		    
		    try {
			    id = FlingManager.getInstance().getId();
			    
			    File uploadedFile = new File(FILE_PATH + id + "_" + fileName);
			    
				item.write(uploadedFile);
				
				// save to db
				FlingManager.getInstance().save(id, contentType, uploadedFile.getAbsolutePath());
			} catch (Exception e) {
				logger.error("Error writing upload to disk", e);
			}
		}
		
		return id;
	}
	
	
}
