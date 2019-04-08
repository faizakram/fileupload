package com.springboot.poc.fileupload.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.springboot.poc.fileupload.model.CopyObjects;

public interface CopyObjectRepository extends JpaRepository<CopyObjects, Long> {

	/*@Query("select r from CopyObjects r where r.expirationTime <=:date")
	List<CopyObjects> findByCurrentTime(@Param("date") Date date);*/
	
	@Query("select r from CopyObjects r where r.expirationTime <=:date and r.isDeleted=false")
	List<CopyObjects> findByCurrentTime(@Param("date") Date date);

	@Query("select r from CopyObjects r where r.fileName in (:fileName)")
	List<CopyObjects> findAll(@Param("fileName") List<String> resources);
}