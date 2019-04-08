package com.springboot.poc.fileupload.utility;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.springboot.poc.fileupload.model.CopyObjects;
import com.springboot.poc.fileupload.repository.CopyObjectRepository;

@Component
public class S3Utility {

	@Value("${cloud.aws.credentials.accessKey}")
	private String accessKey;

	@Value("${cloud.aws.credentials.secretKey}")
	private String secretkey;

	@Value("${app.awsServices.private.bucketName}")
	private String privateBucketName;

	@Value("${app.awsServices.public.bucketName}")
	private String publicBucketName;

	@Value("${cloud.aws.region.static}")
	private String regions;

	// DB Insertion for Copy Objects in Public Bucket
	@Autowired
	private CopyObjectRepository copyObjectRepository;

	/**
	 * Get Basic AWS Credentials
	 * 
	 * @param accesskey
	 * @param secretkey
	 * @return
	 */
	private AWSCredentials getBasicAWSCredentials(String accesskey, String secretkey) {
		return new BasicAWSCredentials(accesskey, secretkey);
	}

	/**
	 * Get AmazonS3
	 * 
	 * @return
	 */
	private AmazonS3 getAmazonS3() {
		return AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(getBasicAWSCredentials(accessKey, secretkey)))
				.withRegion(regions).build();
	}

	/**
	 * Upload On S3
	 * 
	 * @param bucketName
	 * @param fileName
	 * @param file
	 * @return
	 */
	public PutObjectResult uploadOnS3(String fileName, InputStream file) {
		return getAmazonS3().putObject(privateBucketName, fileName, file, null);
	}

	/**
	 * Download Object
	 * 
	 * @param bucketName
	 * @param fileName
	 * @return
	 */
	public S3Object downloadObject(String fileName) {
		return getAmazonS3().getObject(privateBucketName, fileName);
	}

	/**
	 * Get All Objects
	 * 
	 * @return
	 */
	public ObjectListing getAllObjects() {
		return getAmazonS3().listObjects(privateBucketName);
	}

	/**
	 * delete Object
	 * 
	 * @param fileName
	 */
	public void deleteObject(String fileName) {
		getAmazonS3().deleteObject(privateBucketName, fileName);
	}

	private void copyObjectsFromPrivateToPublic(List<String> resources, Long min) {
		writeObjectsInDB(resources, min);
		resources.forEach(fileName -> {
			getAmazonS3().copyObject(privateBucketName, fileName, publicBucketName, fileName);
		});
	}

	public void deleteObjectsFromPublic(List<String> resources) {
		resources.forEach(fileName -> {
			getAmazonS3().deleteObject(publicBucketName, fileName);
		});
	}

	/**
	 * 
	 * @param resources
	 * @param min
	 */
	private void writeObjectsInDB(List<String> resources, Long min) {
		for (S3ObjectSummary os : getAllObjects().getObjectSummaries()) {
			if (resources.contains(os.getKey())) {
				CopyObjects copyObjects = new CopyObjects();
				copyObjects.setFileName(os.getKey());
				copyObjects.setCreatedOn(new java.util.Date());
				copyObjects.setFileSize(os.getSize());
				copyObjects.setIsDeleted(false);
				copyObjects.setValidMin(min);
				copyObjects.setExpirationTime(getExpireDateTime(min));
				copyObjectRepository.save(copyObjects);
			}
		}
	}

	/**
	 * getExpireDateTime
	 * 
	 * @param min
	 * @return
	 */
	public static Date getExpireDateTime(Long min) {
		Calendar now = Calendar.getInstance();
		now.add(Calendar.MINUTE, min.intValue());
		return now.getTime();
	}

	public static Date getExpireDateTime(Date expireTime, Long min) {
		Calendar now = Calendar.getInstance();
		now.setTime(expireTime);
		now.add(Calendar.MINUTE, min.intValue());
		return now.getTime();
	}

	/*
	 * @Async("asyncExecutor") public void
	 * copyObjectsFromS3BucketToBuket(List<String> resources, Long min) throws
	 * InterruptedException { System.out.println("Start");
	 * System.out.println(resources.toString()); //Copy Object From Private Bucket
	 * To Public Bucket copyObjectsFromPrivateToPublic(resources, min);
	 * Thread.sleep(min * 60000); //Delete All Objects From Public
	 * deleteObjectsFromPublic(resources); System.out.println("end");
	 * System.out.println(resources.toString()); }
	 */

	@Async
	public void copyObjectsFromS3BucketToBuket(List<String> resources, Long min) {
		copyObjectsFromPrivateToPublic(resources, min);
	}

	@Scheduled(cron = "0 * * ? * *")
	public void removeFileOnS3() {
		List<CopyObjects> copyObjects = copyObjectRepository.findByCurrentTime(new Date());
		if (!copyObjects.isEmpty()) {
			deleteObjectsFromPublic(copyObjects.stream().map(CopyObjects::getFileName).collect(Collectors.toList()));

			// This will delete data from table
			// copyObjectRepository.deleteAll(copyObjects);

			// This will only maintain the status
			copyObjects.forEach(file -> file.setIsDeleted(true));
			copyObjectRepository.saveAll(copyObjects);
		}
	}
}
