// // package com.thedal.thedal_app.voter;

// // import com.fasterxml.jackson.databind.ObjectMapper;
// // import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
// // import com.thedal.thedal_app.election.ElectionEntity;
// // import com.thedal.thedal_app.election.ElectionRepository;
// // import com.thedal.thedal_app.voter.dto.VoterLocationDTO;

// // import org.slf4j.Logger;
// // import org.slf4j.LoggerFactory;
// // import org.springframework.beans.factory.annotation.Autowired;
// // import org.springframework.scheduling.annotation.Scheduled;
// // import org.springframework.stereotype.Service;

// // import java.util.List;
// // import java.util.Map;
// // import java.util.stream.Collectors;

// // @Service
// // public class VoterLocationJsonGeneratorService {

// //     private static final Logger log = LoggerFactory.getLogger(VoterLocationJsonGeneratorService.class);

// //     @Autowired
// //     private ElectionRepository electionRepository;

// //     @Autowired
// //     private VoterRepo voterRepository;

// //     @Autowired
// //     private AwsFileUpload awsFileUpload;

// //     @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
// //     public void generateVoterLocationJson() {
// //         log.info("Starting daily voter location JSON generation.");
// //         List<ElectionEntity> elections = electionRepository.findAll();

// //         for (ElectionEntity election : elections) {
// //             Long electionId = election.getId();
// //             Long accountId = election.getAccountId();

// //             try {
// //                 // Fetch all voters for the election
// //                 List<VoterEntity> voters = voterRepository.findByElectionIdAndAccountId(electionId, accountId);
// //                 if (voters.isEmpty()) {
// //                     log.info("No voters found for electionId: {}, skipping.", electionId);
// //                     continue;
// //                 }

// //                 // Group voters by booth number
// //                 Map<Integer, List<VoterLocationDTO>> boothToVotersMap = voters.stream()
// //                         .collect(Collectors.groupingBy(
// //                                 VoterEntity::getBoothNumber,
// //                                 Collectors.mapping(v -> mapToVoterLocationDTO(v), Collectors.toList())));

// //                 // Convert to JSON
// //                 ObjectMapper mapper = new ObjectMapper();
// //                 String json = mapper.writeValueAsString(boothToVotersMap);

// //                 // Define S3 key and upload
// //                 String key = "voter_locations/election_" + electionId + ".json";
// //                 awsFileUpload.voterlatandlongupload(key, json.getBytes());
// //                 log.info("Successfully generated and uploaded voter locations JSON for electionId: {}", electionId);

// //             } catch (Exception e) {
// //                 log.error("Error generating JSON for electionId: {}: {}", electionId, e.getMessage(), e);
// //             }
// //         }
// //         log.info("Completed daily voter location JSON generation.");
// //     }

// //     private VoterLocationDTO mapToVoterLocationDTO(VoterEntity voter) {
// //         VoterLocationDTO dto = new VoterLocationDTO();
// //         dto.setVoterId(voter.getVoterId());
// //         dto.setVoterLati(voter.getVoterLati());
// //         dto.setVoterLongi(voter.getVoterLongi());
// //         // Set any other properties you need to include
// //         return dto;
// //     }
// // }

// package com.thedal.thedal_app.voter;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
// import com.thedal.thedal_app.election.ElectionEntity;
// import com.thedal.thedal_app.election.ElectionRepository;
// import com.thedal.thedal_app.voter.dto.VoterLocationDTO;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;

// import java.io.File;
// import java.io.FileOutputStream;
// import java.io.IOException;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.TimeUnit;
// import java.util.concurrent.atomic.AtomicInteger;
// import java.util.stream.Collectors;

// @Service
// public class VoterLocationJsonGeneratorService {

//     private static final Logger log = LoggerFactory.getLogger(VoterLocationJsonGeneratorService.class);
//     private static final int PAGE_SIZE = 1000; // Process 1000 voters at a time
//     private static final int MAX_CONCURRENT_ELECTIONS = 2; // Limit concurrent election processing

//     @Value("${voter.location.json.enabled:true}")
//     private boolean generationEnabled;

//     @Autowired
//     private ElectionRepository electionRepository;

//     @Autowired
//     private VoterRepo voterRepository;

//     @Autowired
//     private AwsFileUpload awsFileUpload;

//     @Scheduled(cron = "0 0 */2 * * ?") // Runs every 2 hours
//     public void generateVoterLocationJson() {
//         if (!generationEnabled) {
//             log.info("Voter location JSON generation is disabled. Skipping execution.");
//             return;
//         }

//         log.info("Starting voter location JSON generation task.");
//         long startTime = System.currentTimeMillis();

//         try {
//             List<ElectionEntity> elections = electionRepository.findAll();
//             ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_ELECTIONS);
//             AtomicInteger activeElections = new AtomicInteger(0);

//             for (ElectionEntity election : elections) {
//                 Long electionId = election.getId();
//                 Long accountId = election.getAccountId();

//                 executorService.submit(() -> {
//                     activeElections.incrementAndGet();
//                     try {
//                         processElection(electionId, accountId);
//                     } catch (Exception e) {
//                         log.error("Error processing election {}: {}", electionId, e.getMessage(), e);
//                     } finally {
//                         activeElections.decrementAndGet();
//                     }
//                 });
//             }

//             executorService.shutdown();
//             if (!executorService.awaitTermination(30, TimeUnit.MINUTES)) {
//                 log.warn("Task didn't complete within timeout. Some elections may not have been processed.");
//                 executorService.shutdownNow();
//             }

//             long duration = System.currentTimeMillis() - startTime;
//             log.info("Completed voter location JSON generation in {} ms.", duration);

//         } catch (Exception e) {
//             log.error("Fatal error in voter location JSON generation task: {}", e.getMessage(), e);
//         }
//     }

//     private void processElection(Long electionId, Long accountId) {
//         log.info("Processing election ID: {}", electionId);
//         long startTime = System.currentTimeMillis();

//         try {
//             // Get total count to know how many pages we need to process
//             long voterCount = voterRepository.countByElectionIdAndAccountId(electionId, accountId);

//             if (voterCount == 0) {
//                 log.info("No voters found for electionId: {}, skipping.", electionId);
//                 return;
//             }

//             log.info("Found {} voters for electionId: {}", voterCount, electionId);

//             // Process voters in pages and build booth map
//             Map<Integer, List<VoterLocationDTO>> boothToVotersMap = new HashMap<>();
//             int totalPages = (int) Math.ceil((double) voterCount / PAGE_SIZE);

//             for (int page = 0; page < totalPages; page++) {
//                 Page<VoterEntity> voterPage = voterRepository.findPageByElectionIdAndAccountId(
//                         electionId, accountId, PageRequest.of(page, PAGE_SIZE));

//                 // Group current page of voters by booth
//                 Map<Integer, List<VoterLocationDTO>> pageBoothMap = voterPage.getContent().stream()
//                         .collect(Collectors.groupingBy(
//                                 VoterEntity::getBoothNumber,
//                                 Collectors.mapping(this::mapToVoterLocationDTO, Collectors.toList())));

//                 // Merge with main map
//                 pageBoothMap.forEach((booth, voters) -> {
//                     boothToVotersMap.computeIfAbsent(booth, k -> collectors.toList()).addAll(voters);
//                 });

//                 log.debug("Processed page {}/{} for election {}", page + 1, totalPages, electionId);

//                 // Give the system a small breathing room between large pages
//                 if (page % 10 == 9 && page < totalPages - 1) {
//                     Thread.sleep(100);
//                 }
//             }

//             // Convert to JSON and upload
//             ObjectMapper mapper = new ObjectMapper();
//             String key = "voter_locations/election_" + electionId + ".json";

//             // Create temp file and write JSON in chunks to avoid OOM
//             File tempFile = File.createTempFile("voter_json_" + electionId + "_", ".json");
//             try (FileOutputStream fos = new FileOutputStream(tempFile)) {
//                 // Write opening brace
//                 fos.write("{".getBytes());

//                 boolean first = true;
//                 for (Map.Entry<Integer, List<VoterLocationDTO>> entry : boothToVotersMap.entrySet()) {
//                     if (!first) {
//                         fos.write(",".getBytes());
//                     }
//                     first = false;

//                     // Write booth number and opening bracket for voters array
//                     fos.write(("\"" + entry.getKey() + "\":").getBytes());
//                     fos.write(mapper.writeValueAsBytes(entry.getValue()));
//                 }

//                 // Write closing brace
//                 fos.write("}".getBytes());
//             }

//             // Upload the file
//             awsFileUpload.voterlatandlongupload(key, tempFile);

//             // Clean up
//             if (!tempFile.delete()) {
//                 log.warn("Could not delete temporary file: {}", tempFile.getAbsolutePath());
//             }

//             long duration = System.currentTimeMillis() - startTime;
//             log.info("Successfully processed election {} with {} voters in {} ms", electionId, voterCount, duration);

//         } catch (Exception e) {
//             log.error("Error generating JSON for electionId {}: {}", electionId, e.getMessage(), e);
//         }
//     }

//     private VoterLocationDTO mapToVoterLocationDTO(VoterEntity voter) {
//         VoterLocationDTO dto = new VoterLocationDTO();
//         dto.setVoterId(voter.getVoterId());
//         dto.setVoterLati(voter.getVoterLati());
//         dto.setVoterLongi(voter.getVoterLongi());
//         // Set any other properties you need to include
//         return dto;
//     }
// }

package com.thedal.thedal_app.voter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.voter.dto.VoterLocationDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class VoterLocationJsonGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(VoterLocationJsonGeneratorService.class);
    private static final int PAGE_SIZE = 1000; // Process 1000 voters at a time
    private static final int MAX_CONCURRENT_ELECTIONS = 2; // Limit concurrent election processing

    @Value("${voter.location.json.enabled:true}")
    private boolean generationEnabled;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private VoterRepo voterRepository;

    @Autowired
    private AwsFileUpload awsFileUpload;

    @Scheduled(cron = "0 0 */2 * * ?") // Runs every 2 hours
    public void generateVoterLocationJson() {
        if (!generationEnabled) {
            log.info("Voter location JSON generation is disabled. Skipping execution.");
            return;
        }

        log.info("Starting voter location JSON generation task.");
        long startTime = System.currentTimeMillis();

        try {
            List<ElectionEntity> elections = electionRepository.findAll();
            ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_ELECTIONS);
            AtomicInteger activeElections = new AtomicInteger(0);

            for (ElectionEntity election : elections) {
                Long electionId = election.getId();
                Long accountId = election.getAccountId();

                executorService.submit(() -> {
                    activeElections.incrementAndGet();
                    try {
                        processElection(electionId, accountId);
                    } catch (Exception e) {
                        log.error("Error processing election {}: {}", electionId, e.getMessage(), e);
                    } finally {
                        activeElections.decrementAndGet();
                    }
                });
            }

            executorService.shutdown();
            if (!executorService.awaitTermination(30, TimeUnit.MINUTES)) {
                log.warn("Task didn't complete within timeout. Some elections may not have been processed.");
                executorService.shutdownNow();
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Completed voter location JSON generation in {} ms.", duration);

        } catch (Exception e) {
            log.error("Fatal error in voter location JSON generation task: {}", e.getMessage(), e);
        }
    }

    // private void processElection(Long electionId, Long accountId) {
    // log.info("Processing election ID: {}", electionId);
    // long startTime = System.currentTimeMillis();

    // try {
    // // Get total count to know how many pages we need to process
    // long voterCount = voterRepository.countByElectionIdAndAccountId(electionId,
    // accountId);

    // if (voterCount == 0) {
    // log.info("No voters found for electionId: {}, skipping.", electionId);
    // return;
    // }

    // log.info("Found {} voters for electionId: {}", voterCount, electionId);

    // // Process voters in pages and build booth map
    // Map<Integer, List<VoterLocationDTO>> boothToVotersMap = new HashMap<>();
    // int totalPages = (int) Math.ceil((double) voterCount / PAGE_SIZE);

    // for (int page = 0; page < totalPages; page++) {
    // Page<VoterEntity> voterPage =
    // voterRepository.findPageByElectionIdAndAccountId(
    // electionId, accountId, PageRequest.of(page, PAGE_SIZE));

    // // Group current page of voters by booth
    // Map<Integer, List<VoterLocationDTO>> pageBoothMap =
    // voterPage.getContent().stream()
    // .collect(Collectors.groupingBy(
    // VoterEntity::getBoothNumber,
    // Collectors.mapping(this::mapToVoterLocationDTO, Collectors.toList())));

    // // Merge with main map
    // pageBoothMap.forEach((booth, voters) -> {
    // boothToVotersMap.computeIfAbsent(booth, k -> new
    // ArrayList<>()).addAll(voters);
    // });

    // log.debug("Processed page {}/{} for election {}", page + 1, totalPages,
    // electionId);

    // try {
    // if (page % 10 == 9 && page < totalPages - 1) {
    // Thread.sleep(100);
    // }
    // } catch (InterruptedException e) {
    // log.warn("Thread interrupted while processing election {}", electionId);
    // Thread.currentThread().interrupt(); // Preserve the interrupt status
    // return; // Exit the method or handle it as needed
    // }
    // }

    // // Convert to JSON and upload
    // ObjectMapper mapper = new ObjectMapper();
    // String key = "voter_locations/election_" + electionId + ".json";

    // // Create temp file and write JSON in chunks to avoid OOM
    // File tempFile = File.createTempFile("voter_json_" + electionId + "_",
    // ".json");
    // try (FileOutputStream fos = new FileOutputStream(tempFile)) {
    // // Write opening brace
    // fos.write("{".getBytes());

    // boolean first = true;
    // for (Map.Entry<Integer, List<VoterLocationDTO>> entry :
    // boothToVotersMap.entrySet()) {
    // if (!first) {
    // fos.write(",".getBytes());
    // }
    // first = false;

    // // Write booth number and opening bracket for voters array
    // fos.write(("\"" + entry.getKey() + "\":").getBytes());
    // fos.write(mapper.writeValueAsBytes(entry.getValue()));
    // }

    // // Write closing brace
    // fos.write("}".getBytes());
    // }

    // // Upload the file
    // awsFileUpload.voterlatandlongupload(key, tempFile);

    // // Clean up
    // if (!tempFile.delete()) {
    // log.warn("Could not delete temporary file: {}", tempFile.getAbsolutePath());
    // }

    // long duration = System.currentTimeMillis() - startTime;
    // log.info("Successfully processed election {} with {} voters in {} ms",
    // electionId, voterCount, duration);

    // } catch (Exception e) {
    // log.error("Error generating JSON for electionId {}: {}", electionId,
    // e.getMessage(), e);
    // }
    // }

    // Update the entire processElection method to handle interruptions properly

    private void processElection(Long electionId, Long accountId) throws InterruptedException {
        log.info("Processing election ID: {}", electionId);
        long startTime = System.currentTimeMillis();
        File tempFile = null;

        try {
            // Check if thread has been interrupted before we start
            if (Thread.currentThread().isInterrupted()) {
                log.info("Thread was interrupted before starting election {}, skipping", electionId);
                return;
            }

            // Get total count to know how many pages we need to process
            long voterCount = voterRepository.countByElectionIdAndAccountId(electionId, accountId);

            if (voterCount == 0) {
                log.info("No voters found for electionId: {}, skipping.", electionId);
                return;
            }

            log.info("Found {} voters for electionId: {}", voterCount, electionId);

            // Process voters in pages and build booth map
            Map<Integer, List<VoterLocationDTO>> boothToVotersMap = new HashMap<>();
            int totalPages = (int) Math.ceil((double) voterCount / PAGE_SIZE);

            for (int page = 0; page < totalPages; page++) {
                // Check for interruption at the start of each page processing
                if (Thread.currentThread().isInterrupted()) {
                    log.info("Thread interrupted during processing of election {}, stopping gracefully", electionId);
                    return;
                }

                Page<VoterEntity> voterPage = voterRepository.findPageByElectionIdAndAccountId(
                        electionId, accountId, PageRequest.of(page, PAGE_SIZE));

                // Group current page of voters by booth
                Map<Integer, List<VoterLocationDTO>> pageBoothMap = voterPage.getContent().stream()
                        .collect(Collectors.groupingBy(
                                VoterEntity::getBoothNumber,
                                Collectors.mapping(this::mapToVoterLocationDTO, Collectors.toList())));

                // Merge with main map
                pageBoothMap.forEach((booth, voters) -> {
                    boothToVotersMap.computeIfAbsent(booth, k -> new ArrayList<>()).addAll(voters);
                });

                log.debug("Processed page {}/{} for election {}", page + 1, totalPages, electionId);

                // Short pause to give the system a breathing room between large pages
                if (page % 10 == 9 && page < totalPages - 1) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // Log and restore interrupted status, but continue processing current election
                        log.debug("Thread pause interrupted, continuing processing for election {}", electionId);
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // Check again for interruption before file operations
            if (Thread.currentThread().isInterrupted()) {
                log.info("Thread interrupted before file creation for election {}, skipping file operations",
                        electionId);
                return;
            }

            // Convert to JSON and upload
            ObjectMapper mapper = new ObjectMapper();
            String key = "voter_locations/election_" + electionId + ".json";

            // Create temp file and write JSON in chunks to avoid OOM
            tempFile = File.createTempFile("voter_json_" + electionId + "_", ".json");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                // Write opening brace
                fos.write("{".getBytes());

                boolean first = true;
                for (Map.Entry<Integer, List<VoterLocationDTO>> entry : boothToVotersMap.entrySet()) {
                    // Check for interruption during file writing
                    if (Thread.currentThread().isInterrupted()) {
                        log.info("Thread interrupted during file writing for election {}, aborting", electionId);
                        return;
                    }

                    if (!first) {
                        fos.write(",".getBytes());
                    }
                    first = false;

                    // Write booth number and opening bracket for voters array
                    fos.write(("\"" + entry.getKey() + "\":").getBytes());
                    fos.write(mapper.writeValueAsBytes(entry.getValue()));
                }

                // Write closing brace
                fos.write("}".getBytes());
            }

            // Final interruption check before upload
            if (Thread.currentThread().isInterrupted()) {
                log.info("Thread interrupted before upload for election {}, skipping upload", electionId);
                return;
            }

            // Upload the file
            awsFileUpload.voterlatandlongupload(key, tempFile);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully processed election {} with {} voters in {} ms", electionId, voterCount, duration);

        } catch (Exception e) {
            log.error("Error generating JSON for electionId {}: {}", electionId, e.getMessage(), e);
        } finally {
            // Clean up temp file if it exists
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    log.warn("Could not delete temporary file: {}", tempFile.getAbsolutePath());
                    // Mark for deletion on JVM exit as a fallback
                    tempFile.deleteOnExit();
                }
            }
        }
    }

    private VoterLocationDTO mapToVoterLocationDTO(VoterEntity voter) {
        VoterLocationDTO dto = new VoterLocationDTO();
        dto.setVoterId(voter.getVoterId());
        dto.setVoterLati(voter.getVoterLati());
        dto.setVoterLongi(voter.getVoterLongi());
        // Set any other properties you need to include
        return dto;
    }
}