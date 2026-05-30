package com.thedal.thedal_app.voter;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.sql.DataSource;

/**
 * Memory-optimized CSV ZIP export service for handling 1M+ voter records.
 * Uses streaming, batching, and EntityManager clearing to minimize memory footprint.
 */
@Component
public class VoterCsvZipExportService {
	
	private static final Logger log = LoggerFactory.getLogger(VoterCsvZipExportService.class);
	private static final int BATCH_SIZE = 500;
	private static final int PROGRESS_LOG_INTERVAL = 10000;
	
	private final VoterRepo voterRepository;
	private final DataSource dataSource;
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public VoterCsvZipExportService(VoterRepo voterRepository, DataSource dataSource) {
		this.voterRepository = voterRepository;
		this.dataSource = dataSource;
	}
	
	/**
	 * Generate CSV ZIP file with specification-based filtering.
	 * Memory-efficient for large datasets.
	 * Now includes dynamic/custom fields in the export.
	 */
	@Transactional(readOnly = true)
	public File generateCsvZipStreamed(Specification<VoterEntity> spec, Integer limit, Long jobId, List<String> columns, 
			Long accountId, Long electionId) throws IOException {
		log.info("EXPORT_FLOW: Starting generateCsvZipStreamed for jobId: {}", jobId);

		// Fetch unique dynamic field names for this election
		List<String> dynamicFieldNames = voterRepository.findUniqueDynamicFieldNames(accountId, electionId);
		log.info("EXPORT_FLOW: Found {} dynamic fields for export in jobId: {}", 
				dynamicFieldNames != null ? dynamicFieldNames.size() : 0, jobId);

		List<String> selectedColumns = VoterCsvWriter.resolveColumns(columns, dynamicFieldNames);
		Path exportDir = Paths.get(System.getProperty("java.io.tmpdir"), "thedal-exports");
		Files.createDirectories(exportDir);
		File zipFile = exportDir.resolve("voter-export-" + jobId + ".zip").toFile();

		int processed = 0;
		int page = 0;
		
		try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
		     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8))) {

			zos.putNextEntry(new ZipEntry("voter-export-" + jobId + ".csv"));
			writer.write('\ufeff'); // BOM so Excel opens UTF-8 correctly
			writer.write(VoterCsvWriter.header(selectedColumns));
			writer.newLine();

			while (true) {
				Pageable pageable = PageRequest.of(page, BATCH_SIZE);
				Page<VoterEntity> voterPage = voterRepository.findAll(spec, pageable);
				if (voterPage.isEmpty()) {
					break;
				}

				for (VoterEntity voter : voterPage.getContent()) {
					if (voter != null && voter.getVoterId() != null) {
						writer.write(VoterCsvWriter.row(voter, selectedColumns));
						writer.newLine();
						processed++;

						if (limit != null && processed >= limit) {
							log.info("EXPORT_FLOW: Reached CSV limit {} for jobId: {}", limit, jobId);
							writer.flush();
							zos.closeEntry();
							log.info("EXPORT_FLOW: Generated CSV zip with {} records at {} for jobId: {}", 
									processed, zipFile.getAbsolutePath(), jobId);
							return zipFile;
						}
					}
				}

				// Clear EntityManager to release memory after each batch
				if (entityManager != null) {
					entityManager.clear();
				}
				
				// Progress logging
				if (processed > 0 && processed % PROGRESS_LOG_INTERVAL == 0) {
					log.info("EXPORT_FLOW: Progress - exported {} records for jobId: {}", processed, jobId);
				}

				page++;
			}

			writer.flush();
			zos.closeEntry();

			log.info("EXPORT_FLOW: Generated CSV zip with {} records at {} for jobId: {}", 
					processed, zipFile.getAbsolutePath(), jobId);
			return zipFile;
			
		} catch (Exception e) {
			log.error("EXPORT_FLOW: Error generating CSV zip for jobId: {}: {}", jobId, e.getMessage(), e);
			if (zipFile.exists() && !zipFile.delete()) {
				log.warn("EXPORT_FLOW: Failed to delete temp csv zip {} for jobId: {}", zipFile.getAbsolutePath(), jobId);
			}
			throw new IOException("Failed to generate CSV zip: " + e.getMessage(), e);
		}
	}

	/**
	 * Generate CSV ZIP file with optimized eager fetching for "All Part" exports.
	 * Uses LEFT JOIN FETCH to avoid N+1 query problems.
	 * Now includes dynamic/custom fields in the export.
	 */
	@Transactional(readOnly = true)
	public File generateCsvZipOptimized(Long accountId, Long electionId, Integer limit, Long jobId, List<String> columns) throws IOException {
		log.info("EXPORT_FLOW: Starting generateCsvZipOptimized for jobId: {}", jobId);

		// Fetch unique dynamic field names for this election
		List<String> dynamicFieldNames = voterRepository.findUniqueDynamicFieldNames(accountId, electionId);
		log.info("EXPORT_FLOW: Found {} dynamic fields for export in jobId: {}", 
				dynamicFieldNames != null ? dynamicFieldNames.size() : 0, jobId);

		List<String> selectedColumns = VoterCsvWriter.resolveColumns(columns, dynamicFieldNames);
		Path exportDir = Paths.get(System.getProperty("java.io.tmpdir"), "thedal-exports");
		Files.createDirectories(exportDir);
		File zipFile = exportDir.resolve("voter-export-" + jobId + ".zip").toFile();

		int processed = 0;
		int page = 0;
		
		try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
		     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8))) {

			zos.putNextEntry(new ZipEntry("voter-export-" + jobId + ".csv"));
			writer.write('\ufeff'); // BOM so Excel opens UTF-8 correctly
			writer.write(VoterCsvWriter.header(selectedColumns));
			writer.newLine();

			while (true) {
				Pageable pageable = PageRequest.of(page, BATCH_SIZE, 
						Sort.by(Sort.Order.asc("partNo"), Sort.Order.asc("serialNo")));
				Page<VoterEntity> voterPage = voterRepository.findAllForExportWithRelationships(accountId, electionId, pageable);
				
				if (voterPage.isEmpty()) {
					break;
				}

				for (VoterEntity voter : voterPage.getContent()) {
					if (voter != null && voter.getVoterId() != null) {
						writer.write(VoterCsvWriter.row(voter, selectedColumns));
						writer.newLine();
						processed++;

						if (limit != null && processed >= limit) {
							log.info("EXPORT_FLOW: Reached OPTIMIZED CSV limit {} for jobId: {}", limit, jobId);
							writer.flush();
							zos.closeEntry();
							log.info("EXPORT_FLOW: Generated OPTIMIZED CSV zip with {} records at {} for jobId: {}", 
									processed, zipFile.getAbsolutePath(), jobId);
							return zipFile;
						}
					}
				}

				// Clear EntityManager to release memory after each batch
				if (entityManager != null) {
					entityManager.clear();
				}
				
				// Progress logging
				if (processed > 0 && processed % PROGRESS_LOG_INTERVAL == 0) {
					log.info("EXPORT_FLOW: Progress - exported {} records for jobId: {}", processed, jobId);
				}

				page++;
			}

			writer.flush();
			zos.closeEntry();

			log.info("EXPORT_FLOW: Generated OPTIMIZED CSV zip with {} records at {} for jobId: {}", 
					processed, zipFile.getAbsolutePath(), jobId);
			return zipFile;
			
		} catch (Exception e) {
			log.error("EXPORT_FLOW: Error generating OPTIMIZED CSV zip for jobId: {}: {}", jobId, e.getMessage(), e);
			if (zipFile.exists() && !zipFile.delete()) {
				log.warn("EXPORT_FLOW: Failed to delete OPTIMIZED csv zip {} for jobId: {}", zipFile.getAbsolutePath(), jobId);
			}
			throw new IOException("Failed to generate OPTIMIZED CSV zip: " + e.getMessage(), e);
		}
	}

	/**
	 * FASTEST: Generate CSV ZIP using PostgreSQL native COPY command.
	 * Bypasses Hibernate/JPA completely for maximum speed (1M rows in 1-2 minutes).
	 * Uses SQL aggregation for collection fields (languages, benefit schemes, etc.).
	 * Now includes dynamic/custom fields in the export.
	 */
	@Transactional(readOnly = true)
	public File generateCsvZipWithNativeCopy(Long accountId, Long electionId, Integer limit, Long jobId, List<String> columns, List<Integer> boothNumberList) throws IOException {
		log.info("EXPORT_FLOW: Starting NATIVE COPY export for jobId: {}", jobId);
		long startTime = System.currentTimeMillis();

		// Fetch unique dynamic field names for this election
		List<String> dynamicFieldNames = voterRepository.findUniqueDynamicFieldNames(accountId, electionId);
		log.info("EXPORT_FLOW: Found {} dynamic fields for native copy export in jobId: {}", 
				dynamicFieldNames != null ? dynamicFieldNames.size() : 0, jobId);

		List<String> selectedColumns = VoterCsvWriter.resolveColumns(columns, dynamicFieldNames);
		Path exportDir = Paths.get(System.getProperty("java.io.tmpdir"), "thedal-exports");
		Files.createDirectories(exportDir);
		
		File tempCsvFile = exportDir.resolve("voter-export-" + jobId + "-temp.csv").toFile();
		File zipFile = exportDir.resolve("voter-export-" + jobId + ".zip").toFile();

		try (Connection conn = dataSource.getConnection()) {
			BaseConnection pgConn = conn.unwrap(BaseConnection.class);
			CopyManager copyManager = new CopyManager(pgConn);

			// Build the native SQL query with all joins and aggregations
			String copyQuery = buildNativeCopyQuery(accountId, electionId, limit, selectedColumns, boothNumberList, dynamicFieldNames);
			
			log.info("EXPORT_FLOW: Executing PostgreSQL COPY for jobId: {} with {} booth filters", jobId, 
					(boothNumberList == null ? 0 : boothNumberList.size()));
			
			// Execute COPY TO file
			try (FileOutputStream fos = new FileOutputStream(tempCsvFile);
			     BufferedOutputStream bos = new BufferedOutputStream(fos, 65536)) {
				
				// COPY directly to output stream
				long rowCount = copyManager.copyOut(copyQuery, bos);
				log.info("EXPORT_FLOW: COPY exported {} rows in {} ms for jobId: {}", 
						rowCount, System.currentTimeMillis() - startTime, jobId);
			}

			// Add UTF-8 BOM and ZIP the file
			log.info("EXPORT_FLOW: Creating ZIP file for jobId: {}", jobId);
			try (ZipOutputStream zos = new ZipOutputStream(
					new BufferedOutputStream(new FileOutputStream(zipFile), 65536));
			     FileInputStream fis = new FileInputStream(tempCsvFile);
			     BufferedInputStream bis = new BufferedInputStream(fis, 65536)) {

				zos.putNextEntry(new ZipEntry("voter-export-" + jobId + ".csv"));
				
				// Write UTF-8 BOM first
				zos.write(0xEF);
				zos.write(0xBB);
				zos.write(0xBF);
				
				// Copy CSV content to ZIP
				byte[] buffer = new byte[65536];
				int bytesRead;
				while ((bytesRead = bis.read(buffer)) != -1) {
					zos.write(buffer, 0, bytesRead);
				}
				
				zos.closeEntry();
			}

			// Clean up temp file
			if (!tempCsvFile.delete()) {
				log.warn("EXPORT_FLOW: Failed to delete temp CSV file: {}", tempCsvFile.getAbsolutePath());
			}

			long totalTime = System.currentTimeMillis() - startTime;
			log.info("EXPORT_FLOW: NATIVE COPY completed in {} ms ({} seconds) for jobId: {}", 
					totalTime, totalTime / 1000.0, jobId);
			
			// Verify the ZIP file was created successfully
			if (!zipFile.exists()) {
				throw new IOException("ZIP file was not created at: " + zipFile.getAbsolutePath());
			}
			if (zipFile.length() == 0) {
				throw new IOException("ZIP file is empty (0 bytes) at: " + zipFile.getAbsolutePath());
			}
			
			log.info("EXPORT_FLOW: ZIP file verified - path: {}, size: {} bytes", 
					zipFile.getAbsolutePath(), zipFile.length());
			
			return zipFile;

		} catch (SQLException e) {
			log.error("EXPORT_FLOW: SQL error in NATIVE COPY for jobId: {}: {}", jobId, e.getMessage(), e);
			if (tempCsvFile.exists() && !tempCsvFile.delete()) {
				log.warn("EXPORT_FLOW: Failed to delete temp CSV {}", tempCsvFile.getAbsolutePath());
			}
			if (zipFile.exists() && !zipFile.delete()) {
				log.warn("EXPORT_FLOW: Failed to delete ZIP {}", zipFile.getAbsolutePath());
			}
			throw new IOException("Failed to export using native COPY: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("EXPORT_FLOW: Error in NATIVE COPY for jobId: {}: {}", jobId, e.getMessage(), e);
			if (tempCsvFile.exists() && !tempCsvFile.delete()) {
				log.warn("EXPORT_FLOW: Failed to delete temp CSV {}", tempCsvFile.getAbsolutePath());
			}
			if (zipFile.exists() && !zipFile.delete()) {
				log.warn("EXPORT_FLOW: Failed to delete ZIP {}", zipFile.getAbsolutePath());
			}
			throw new IOException("Failed to generate ZIP: " + e.getMessage(), e);
		}
	}

	/**
	 * Build PostgreSQL COPY query with all joins and aggregations.
	 * Follows the same column order as VoterColumnMapper.
	 * Now includes dynamic/custom fields using JSONB aggregation.
	 */
	private String buildNativeCopyQuery(Long accountId, Long electionId, Integer limit, List<String> selectedColumns, 
			List<Integer> boothNumberList, List<String> dynamicFieldNames) {
		// Note: PostgreSQL COPY doesn't support parameters, so we embed values
		// This is safe because accountId/electionId are Long values (not user input)
		
		StringBuilder sql = new StringBuilder("COPY (SELECT ");
		
		// Build column list with aggregations for collections
		sql.append("v.part_no, ");
		sql.append("v.section_no, ");
		sql.append("v.serial_no, ");
		sql.append("v.house_no_en, ");
		sql.append("v.house_no_l1, ");
		sql.append("v.house_no_l2, ");
		sql.append("v.voter_fname_en, ");
		sql.append("v.voter_lname_en, ");
		sql.append("v.voter_fname_l1, ");
		sql.append("v.voter_lname_l1, ");
		sql.append("v.voter_fname_l2, ");
		sql.append("v.voter_lname_l2, ");
		sql.append("v.rln_fname_en, ");
		sql.append("v.rln_lname_en, ");
		sql.append("v.rln_fname_l1, ");
		sql.append("v.rln_lname_l1, ");
		sql.append("v.rln_fname_l2, ");
		sql.append("v.rln_lname_l2, ");
		sql.append("v.rln_type, ");
		sql.append("v.epic_number, ");
		sql.append("v.gender, ");
		sql.append("v.section_name_en, ");
		sql.append("v.section_name_l1, ");
		sql.append("v.section_name_l2, ");
		sql.append("v.full_address, ");
		sql.append("v.part_name_en, ");
		sql.append("v.part_name_l1, ");
		sql.append("v.part_name_l2, ");
		sql.append("v.pincode, ");
		sql.append("v.part_lati, ");
		sql.append("v.part_long, ");
		sql.append("v.age, ");
		sql.append("v.dob, ");
		sql.append("v.mobile_no, ");
		sql.append("v.whatsapp_no, ");
		sql.append("v.e_mail, ");
		sql.append("v.voter_lati, ");
		sql.append("v.voter_longi, ");
		sql.append("v.state_code, ");
		sql.append("v.state_name_en, ");
		sql.append("v.state_name_l1, ");
		sql.append("v.state_name_l2, ");
		sql.append("v.district_code, ");
		sql.append("v.district_name_en, ");
		sql.append("v.district_name_l1, ");
		sql.append("v.district_name_l2, ");
		sql.append("v.pc_no, ");
		sql.append("v.pc_name_en, ");
		sql.append("v.pc_name_l1, ");
		sql.append("v.pc_name_l2, ");
		sql.append("v.ac_no, ");
		sql.append("v.ac_name_en, ");
		sql.append("v.ac_name_l1, ");
		sql.append("v.ac_name_l2, ");
		sql.append("v.urban_no, ");
		sql.append("v.urban_name_en, ");
		sql.append("v.urban_name_l1, ");
		sql.append("v.urban_ward_no, ");
		sql.append("v.rur_district_union_no, ");
		sql.append("v.rur_district_union_name_en, ");
		sql.append("v.rur_district_union_name_l1, ");
		sql.append("v.rur_district_union_name_l2, ");
		sql.append("v.rur_district_union_ward_no, ");
		sql.append("v.pan_union_no, ");
		sql.append("v.pan_union_name_en, ");
		sql.append("v.pan_union_name_l1, ");
		sql.append("v.pan_union_name_l2, ");
		sql.append("v.pan_union_ward_no, ");
		sql.append("v.vill_pan_no, ");
		sql.append("v.vill_pan_name_en, ");
		sql.append("v.vill_pan_name_l1, ");
		sql.append("v.vill_pan_ward_no, ");
		sql.append("r.religion_name, ");
		sql.append("c.caste_name, ");
		sql.append("sc.sub_caste_name, ");
		
		// Aggregated collection: languages
		sql.append("string_agg(DISTINCT l.language_name, ', ' ORDER BY l.language_name) AS languages, ");
		
		// Aggregated collection: benefit schemes name
		sql.append("string_agg(DISTINCT bs.scheme_name, ', ') AS benefit_schemes_name, ");
		
		// Aggregated collection: benefit schemes by (party/govt/central)
		sql.append("string_agg(DISTINCT CAST(bs.scheme_by AS TEXT), ', ') AS benefit_schemes_by, ");
		
		sql.append("v.scheme, ");
		sql.append("a.description AS availability_description, ");
		sql.append("a.category_name AS availability_category_name, ");
		sql.append("p.party_name, ");
		sql.append("p.party_short_name, ");
		sql.append("v.family_id, ");
		sql.append("v.family_count, ");
		
		// Aggregated collection: feedback issues
		sql.append("string_agg(DISTINCT fi.issue_name, ', ') AS feedback_issue_names, ");
		
		// Aggregated collection: voter history
		sql.append("string_agg(DISTINCT vh.voter_history_name, ', ') AS voter_history_names, ");
		
		sql.append("v.star_number, ");
		sql.append("v.aadhaar_number, ");
		sql.append("v.pan_number, ");
		sql.append("v.party_registration_number, ");
		sql.append("v.page_number, ");
		sql.append("v.remarks, ");
		sql.append("cc.caste_category_name, ");
		sql.append("v.aadhaar_verified, ");
		sql.append("v.photo_url, ");
		sql.append("v.friend_count");
		
		// Add dynamic fields with proper aggregation from voter_dynamic_fields table
		if (dynamicFieldNames != null && !dynamicFieldNames.isEmpty()) {
			for (String dynamicField : dynamicFieldNames) {
				sql.append(", ");
				// Use MAX aggregation to get the field value (there should only be one per voter/field combo)
				sql.append("MAX(CASE WHEN vdf.field_name = '").append(dynamicField.replace("'", "''")).append("' THEN vdf.field_value END) AS \"").append(dynamicField.replace("\"", "\"\"")).append("\"");
			}
		}
		sql.append(" ");
		
		sql.append("FROM _voters v ");
		sql.append("LEFT JOIN religion r ON v.religion_id = r.id ");
		sql.append("LEFT JOIN caste c ON v.caste_id = c.id ");
		sql.append("LEFT JOIN sub_caste sc ON v.sub_caste_id = sc.id ");
		sql.append("LEFT JOIN caste_category cc ON v.caste_category_id = cc.id ");
		sql.append("LEFT JOIN availability a ON v.availability_id = a.id ");
		sql.append("LEFT JOIN parties p ON v.party_id = p.id ");
		
		// Join for languages (many-to-many) - table name is singular, use v.id not v.voter_id
		sql.append("LEFT JOIN voter_language vl ON v.id = vl.voter_id ");
		sql.append("LEFT JOIN language l ON vl.language_id = l.id ");
		
		// Join for benefit schemes (one-to-many through voter_benefit_schemes entity), use v.id not v.voter_id
		sql.append("LEFT JOIN voter_benefit_schemes vbs ON v.id = vbs.voter_id ");
		sql.append("LEFT JOIN benefit_schemes bs ON vbs.benefit_scheme_id = bs.id ");
		
		// Join for feedback issues (many-to-many), use v.id not v.voter_id
		sql.append("LEFT JOIN voter_feedback_issues vfi ON v.id = vfi.voter_id ");
		sql.append("LEFT JOIN feedback_issues fi ON vfi.feedback_issue_id = fi.id ");
		
		// Join for voter histories (many-to-many) - table name is singular, use v.id not v.voter_id
		sql.append("LEFT JOIN voter_voter_history vvh ON v.id = vvh.voter_id ");
		sql.append("LEFT JOIN voter_history vh ON vvh.voter_history_id = vh.id ");
		
		// Join for dynamic fields (if any exist)
		if (dynamicFieldNames != null && !dynamicFieldNames.isEmpty()) {
			sql.append("LEFT JOIN voter_dynamic_fields vdf ON v.id = vdf.voter_id ");
		}
		
		sql.append("WHERE v.account_id = ").append(accountId);
		sql.append(" AND v.election_id = ").append(electionId);
		
		// Apply booth number filter if provided
		if (boothNumberList != null && !boothNumberList.isEmpty()) {
			sql.append(" AND v.part_no IN (");
			for (int i = 0; i < boothNumberList.size(); i++) {
				if (i > 0) sql.append(", ");
				sql.append(boothNumberList.get(i));
			}
			sql.append(")");
		}
		
		// Group by all non-aggregated columns
		sql.append(" GROUP BY v.voter_id, v.part_no, v.section_no, v.serial_no, v.house_no_en, v.house_no_l1, v.house_no_l2, ");
		sql.append("v.voter_fname_en, v.voter_lname_en, v.voter_fname_l1, v.voter_lname_l1, v.voter_fname_l2, v.voter_lname_l2, ");
		sql.append("v.rln_fname_en, v.rln_lname_en, v.rln_fname_l1, v.rln_lname_l1, v.rln_fname_l2, v.rln_lname_l2, v.rln_type, ");
		sql.append("v.epic_number, v.gender, v.section_name_en, v.section_name_l1, v.section_name_l2, v.full_address, ");
		sql.append("v.part_name_en, v.part_name_l1, v.part_name_l2, v.pincode, v.part_lati, v.part_long, v.age, v.dob, ");
		sql.append("v.mobile_no, v.whatsapp_no, v.e_mail, v.voter_lati, v.voter_longi, v.state_code, v.state_name_en, ");
		sql.append("v.state_name_l1, v.state_name_l2, v.district_code, v.district_name_en, v.district_name_l1, v.district_name_l2, ");
		sql.append("v.pc_no, v.pc_name_en, v.pc_name_l1, v.pc_name_l2, v.ac_no, v.ac_name_en, v.ac_name_l1, v.ac_name_l2, ");
		sql.append("v.urban_no, v.urban_name_en, v.urban_name_l1, v.urban_ward_no, v.rur_district_union_no, ");
		sql.append("v.rur_district_union_name_en, v.rur_district_union_name_l1, v.rur_district_union_name_l2, ");
		sql.append("v.rur_district_union_ward_no, v.pan_union_no, v.pan_union_name_en, v.pan_union_name_l1, ");
		sql.append("v.pan_union_name_l2, v.pan_union_ward_no, v.vill_pan_no, v.vill_pan_name_en, v.vill_pan_name_l1, ");
		sql.append("v.vill_pan_ward_no, r.religion_name, c.caste_name, sc.sub_caste_name, v.scheme, ");
		sql.append("a.description, a.category_name, p.party_name, p.party_short_name, v.family_id, v.family_count, ");
		sql.append("v.star_number, v.aadhaar_number, v.pan_number, v.party_registration_number, v.page_number, ");
		sql.append("v.remarks, cc.caste_category_name, v.aadhaar_verified, v.photo_url, v.friend_count ");
		
		sql.append("ORDER BY v.part_no, v.serial_no");
		
		if (limit != null && limit > 0) {
			sql.append(" LIMIT ").append(limit);
		}
		
		sql.append(") TO STDOUT WITH (FORMAT CSV, HEADER true, ENCODING 'UTF8')");
		
		return sql.toString();
	}
}
