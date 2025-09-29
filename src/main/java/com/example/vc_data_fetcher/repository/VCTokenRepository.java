package com.example.vc_data_fetcher.repository;

import com.example.vc_data_fetcher.model.VCToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VCTokenRepository extends JpaRepository<VCToken, Long> {

	// Find token by user_id - using custom query to match exact field name
	@Query("SELECT v FROM VCToken v WHERE v.user_id = :userId")
	Optional<VCToken> findByUserId(@Param("userId") Long userId);

	// Find token by access_token - using custom query to match exact field name
	@Query("SELECT v FROM VCToken v WHERE v.access_token = :accessToken")
	Optional<VCToken> findByAccessToken(@Param("accessToken") String accessToken);

	// Check if user already has a token - using custom query
	@Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VCToken v WHERE v.user_id = :userId")
	boolean existsByUserId(@Param("userId") Long userId);
}