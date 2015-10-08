/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.patientimage.rest.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientimage.rest.resource.PatientImageResource;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.api.RestService;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceController;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + PatientImageController.PIMG_NAMESPACE)
public class PatientImageController extends MainResourceController {
	
	public static final String PIMG_NAMESPACE = "/patientimage";
	
	@Override
	public String getNamespace() {
		return RestConstants.VERSION_1 + PIMG_NAMESPACE;
	}
	
	/**
	 * @param patientid int
	 * @param pageid int
	 * @return ResponseEntity<byte[]> containing image binary data with JPEG
	 * 	image header.
	 * @throws ResponseException
	 * @throws IOException 
	 */
	@RequestMapping(value = "/{patientid}/{pageid}", method = RequestMethod.GET)
	public ResponseEntity<byte[]> retrieve(@PathVariable("patientid") String patientIdStr,
	        @PathVariable("pageid") String pageIdStr, HttpServletRequest request) throws IOException {
		//RequestContext context = RestUtil.getRequestContext(request);
		PatientImageResource r = new PatientImageResource();
		int patientId = Integer.parseInt(patientIdStr);
		int pageId = Integer.parseInt(pageIdStr);
		final HttpHeaders headers = new HttpHeaders();
		byte[] imageData = null;
		HttpStatus status = null;
		try {
			imageData = r.retrieve(patientId, pageId);
			headers.setContentType(MediaType.IMAGE_JPEG);
			status = HttpStatus.OK;
		}
		catch (IOException e) {
			status = HttpStatus.NOT_FOUND;
		}
		return new ResponseEntity<byte[]>(imageData, headers, status);
	}
	
	@RequestMapping(value = "/{patientid}/{pageid}", method = RequestMethod.GET)
	public ResponseEntity<byte[]> retrieveById(@PathVariable("patientid") String patientIdStr,
	        @PathVariable("pageid") String pageIdStr, HttpServletRequest request) throws IOException {
		//RequestContext context = RestUtil.getRequestContext(request);
		PatientImageResource r = new PatientImageResource();
		int patientId = Integer.parseInt(patientIdStr);
		int pageId = Integer.parseInt(pageIdStr);
		final HttpHeaders headers = new HttpHeaders();
		byte[] imageData = null;
		HttpStatus status = null;
		try {
			imageData = r.retrieve(patientId, pageId);
			headers.setContentType(MediaType.IMAGE_JPEG);
			status = HttpStatus.OK;
		}
		catch (IOException e) {
			status = HttpStatus.NOT_FOUND;
		}
		return new ResponseEntity<byte[]>(imageData, headers, status);
	}
	
	/**
	 * 
	 * @param patientid
	 * @param fileCategory
	 * @param file
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "/uploadimage", method = RequestMethod.POST)
	public @ResponseBody
	String handleFileUpload(@RequestParam("patientid") String patientid, @RequestParam("fileCategory") String fileCategory,
	        @RequestParam("file") MultipartFile file) throws IOException {
		String name = file.getName();
		
		if (patientid != null) {
			
			Patient patient = getPatientByIdentifier(patientid);
			
			if (!file.isEmpty()) {
				if (fileCategory.equalsIgnoreCase("dp")) {
					return addDP(file, patient);
				} else {
					
					return addImage(file, fileCategory, patient);
				}
			} else {
				return "You failed to upload " + name + " because the file was empty.";
			}
		} else {
			return "patient id is null";
			
		}
	}
	
	/**
	 * gets patient by his/her identifier
	 * @param identifier
	 * @return Patient
	 */
	private Patient getPatientByIdentifier(String identifier) {
		List<Patient> list = Context.getPatientService().getAllPatients();
		for (Patient patient : list) {
			for (PatientIdentifier p : patient.getIdentifiers()) {
				if (p.getIdentifier().equalsIgnoreCase(identifier)) {
					return patient;
				}
				
			}
		}
		
		return null;
	}
	
	private String addDP(MultipartFile file, Patient patient) {
		String name = "test";
		PatientService patientService = Context.getPatientService();
		try {
			name = file.getOriginalFilename();
			String separator = System.getProperty("file.separator");
			
			File imgDir = new File(OpenmrsUtil.getApplicationDataDirectory(), "patient_images");
			if (!imgDir.exists()) {
				FileUtils.forceMkdir(imgDir);
			}
			
			//adding attribute
			PersonAttribute attribute = patient.getAttribute(Context.getPersonService().getPersonAttributeTypeByName(
			    "Patient Image"));
			if (attribute == null) {
				attribute = new PersonAttribute(Context.getPersonService().getPersonAttributeTypeByName("Patient Image"), "");
			}
			
			byte[] bytes = file.getBytes();
			//String completePath = imgDir.getAbsolutePath()+separator + patient.getPatientIdentifier().getIdentifier() + ".jpg";
			BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(imgDir, patient
			        .getPatientIdentifier().getIdentifier()
			        + ".jpg")));
			stream.write(bytes);
			stream.close();
			
			attribute.setValue(patient.getPatientIdentifier().getIdentifier() + ".jpg");
			patient.addAttribute(attribute);
			patientService.savePatient(patient);
			return "Patient Image is  successfully uploaded !";
		}
		catch (Exception e) {
			e.printStackTrace();
			return "You failed to upload  => " + e.getMessage();
		}
		
	}
	
	private String addImage(MultipartFile file, String fileCategory, Patient patient) {
		String name = "test";
		PatientService patientService = Context.getPatientService();
		try {
			name = file.getOriginalFilename();
			String separator = System.getProperty("file.separator");
			String relativePath = separator + patient.getUuid() + separator + fileCategory;
			File imgDir = new File(OpenmrsUtil.getApplicationDataDirectory() + relativePath);
			
			if (!imgDir.exists()) {
				System.out.println(" directory created :" + imgDir.mkdirs());
			}
			
			//creating attribute 
			createPersonAttributeType(fileCategory);
			
			//adding attribute
			PersonAttribute attribute = patient.getAttribute(Context.getPersonService().getPersonAttributeTypeByName(
			    fileCategory));
			if (attribute == null) {
				attribute = new PersonAttribute(Context.getPersonService().getPersonAttributeTypeByName(fileCategory), "");
			}
			
			byte[] bytes = file.getBytes();
			String completePath = imgDir.getAbsolutePath() + separator + file.getOriginalFilename();
			BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(completePath)));
			stream.write(bytes);
			stream.close();
			
			attribute.setValue(relativePath);
			patient.addAttribute(attribute);
			patientService.savePatient(patient);
			return "You successfully uploaded " + name + " into OpenMRS";
		}
		catch (Exception e) {
			e.printStackTrace();
			return "You failed to upload " + name + " => " + e.getMessage();
		}
		
	}
	
	private void createPersonAttributeType(String fileCategory) {
		PersonService ps = Context.getPersonService();
		if (ps.getPersonAttributeTypeByName(fileCategory) == null) {
			PersonAttributeType pat = new PersonAttributeType();
			pat.setName(fileCategory);
			pat.setFormat("java.lang.String");
			pat.setDescription("Stores the filename for the patient image");
			ps.savePersonAttributeType(pat);
			
			System.out.println("Created New Person Attribute:" + fileCategory);
		} else {
			System.out.println("Person Attribute: " + fileCategory + " already exists");
		}
		
	}
}
