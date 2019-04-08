package com.springboot.poc.fileupload.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import com.springboot.poc.fileupload.model.CopyObjects;
import com.springboot.poc.fileupload.model.S3ObjectDTO;
import com.springboot.poc.fileupload.repository.CopyObjectRepository;
import com.springboot.poc.fileupload.utility.S3Utility;

@RestController
@RequestMapping("/aws")
public class MasterDocumentDownloadController {

	@Autowired
	private S3Utility s3Utility;

	@Autowired
	private CopyObjectRepository copyObjectRepository;

	@PostMapping("upload")
	public Map<String, String> singleFileUpload(@RequestParam("file") MultipartFile file) {
		HashMap<String, String> map = new HashMap<>();
		if (file.isEmpty()) {
			map.put("message", "Please select a file to upload");
			map.put("uploadurl", "");
			map.put("uploadflag", "E");
			return map;
		}
		try {
			// String path = "Uploads/Documents/";
			String filename = UUID.randomUUID() + file.getOriginalFilename();
			s3Utility.uploadOnS3(filename, file.getInputStream());
			map.put("message", "You successfully uploaded ");
			map.put("filename", filename);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	@GetMapping("/download/{fileName}")
	public StreamingResponseBody singleFileDownload(@PathVariable("fileName") String fileName,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		S3Object s3object = s3Utility.downloadObject(fileName);
		httpResponse.setContentType("application/octet-stream");
		httpResponse.setHeader("Content-Disposition", String.format("inline; filename=\"%s\"", fileName));
		return new StreamingResponseBody() {
			@Override
			public void writeTo(OutputStream outputStream) throws IOException {
				outputStream.write(IOUtils.toByteArray(s3object.getObjectContent()));
				outputStream.flush();
			}
		};
	}

	@GetMapping("/list")
	public Map<String, Object> getAllObjectsOnS3() {
		HashMap<String, Object> map = new HashMap<>();
		List<S3ObjectDTO> file = new ArrayList<>();
		ObjectListing objectListing = s3Utility.getAllObjects();
		for (S3ObjectSummary os : objectListing.getObjectSummaries()) {
			S3ObjectDTO s3ObjectDTO = new S3ObjectDTO();
			s3ObjectDTO.setFileSize(os.getSize());
			s3ObjectDTO.setFileName(os.getKey());
			s3ObjectDTO.setFileModifiedDate(os.getLastModified());
			s3ObjectDTO.setFileTag(os.getETag());
			file.add(s3ObjectDTO);
		}
		map.put("files", file);
		return map;
	}

	@DeleteMapping("deleteObject")
	public Map<String, Object> deleteObject(@RequestParam String fileName) {
		HashMap<String, Object> map = new HashMap<>();
		s3Utility.deleteObject(fileName);
		map.put("fileName", fileName);
		return map;
	}

	@GetMapping(value = "/copyObjects")
	public Map<String, Object> moveObjects(@RequestParam List<String> resources,
			@RequestParam(required = false, defaultValue = "1") Long min) throws InterruptedException {
		s3Utility.copyObjectsFromS3BucketToBuket(resources, min);
		HashMap<String, Object> map = new HashMap<>();
		map.put("files", resources);
		map.put("min", min);
		return map;
	}

	@GetMapping(value = "/increaseTime")
	public Map<String, Object> increaseTimeForObjects(@RequestParam List<String> resources,
			@RequestParam(required = false, defaultValue = "1") Long increaseby) throws InterruptedException {
		HashMap<String, Object> map = new HashMap<>();
		List<CopyObjects> fileList = copyObjectRepository.findAll(resources);
		fileList.forEach(copyObject -> {
			copyObject.setValidMin(copyObject.getValidMin()+ increaseby);
			copyObject.setExpirationTime(S3Utility.getExpireDateTime(copyObject.getExpirationTime(), increaseby));
		});
		if (!fileList.isEmpty()) {
			copyObjectRepository.saveAll(fileList);
			map.put("files", resources);
			map.put("increaseby", increaseby);
		}
		else {
			map.put("files", "Not Found");
		}
		return map;
	}
}