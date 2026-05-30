package com.thedal.thedal_app.voter.duplicate;

import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DuplicateRunProcessor {
    private final VoterRepo voterRepo;
    private final VoterDuplicateRunRepository runRepo;
    private final VoterDuplicateGroupRepository groupRepo;
    private final VoterDuplicateMemberRepository memberRepo;

    @Async
    @Transactional
    public void processElectionRunAsync(Long runId) {
        VoterDuplicateRun run = runRepo.findById(runId).orElse(null);
        if (run == null) return;
        try {
            processElectionRun(run);
            run.setStatus(VoterDuplicateRun.Status.COMPLETED);
        } catch (Exception ex) {
            log.error("Duplicate run {} failed: {}", runId, ex.getMessage(), ex);
            run.setStatus(VoterDuplicateRun.Status.FAILED);
        } finally {
            run.setFinishedAt(LocalDateTime.now());
            runRepo.save(run);
        }
    }

    @Async
    @Transactional
    public void processBatchRunAsync(Long runId, Collection<Long> voterIds) {
        VoterDuplicateRun run = runRepo.findById(runId).orElse(null);
        if (run == null) return;
        try {
            processBatchRun(run, voterIds);
            run.setStatus(VoterDuplicateRun.Status.COMPLETED);
        } catch (Exception ex) {
            log.error("Batch duplicate run {} failed: {}", runId, ex.getMessage(), ex);
            run.setStatus(VoterDuplicateRun.Status.FAILED);
        } finally {
            run.setFinishedAt(LocalDateTime.now());
            runRepo.save(run);
        }
    }

    private void processElectionRun(VoterDuplicateRun run) {
        int page = 0;
        int size = 2000;
        Map<String, List<VoterEntity>> groups = new HashMap<>();
        org.springframework.data.domain.Slice<VoterEntity> p;
        do {
            p = voterRepo.findByAccountIdAndElectionIdOrderByIdAsc(run.getAccountId(), run.getElectionId(), PageRequest.of(page++, size));
            for (VoterEntity v : p.getContent()) {
                String key = buildKey(v);
                if (key == null) continue;
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(v);
            }
        } while (p.hasNext());
        persistGroups(run, groups);
    }

    private void processBatchRun(VoterDuplicateRun run, Collection<Long> voterIds) {
        Map<String, List<VoterEntity>> groups = new HashMap<>();
        List<VoterEntity> voters = voterRepo.findAllById(voterIds);
        for (VoterEntity v : voters) {
            String key = buildKey(v);
            if (key == null) continue;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(v);
        }
        persistGroups(run, groups);
    }

    private void persistGroups(VoterDuplicateRun run, Map<String, List<VoterEntity>> grouped) {
        for (Map.Entry<String, List<VoterEntity>> e : grouped.entrySet()) {
            List<VoterEntity> list = e.getValue();
            if (list.size() < 2) continue; // only duplicates
            VoterDuplicateGroup g = new VoterDuplicateGroup();
            g.setRun(run);
            g.setKeyHash(e.getKey());
            g.setSize(list.size());
            VoterEntity first = list.get(0);
            g.setVoterFnameEnNorm(norm(first.getVoterFnameEn()));
            g.setVoterLnameEnNorm(norm(first.getVoterLnameEn()));
            g.setRlnFnameEnNorm(norm(first.getRlnFnameEn()));
            g.setRlnLnameEnNorm(norm(first.getRlnLnameEn()));
            g = groupRepo.save(g);

            for (VoterEntity v : list) {
                VoterDuplicateMember m = new VoterDuplicateMember();
                m.setGroup(g);
                m.setVoter(v);
                m.setPartNo(v.getPartNo());
                m.setSerialNo(v.getSerialNo());
                memberRepo.save(m);
            }
        }
    }

    private String buildKey(VoterEntity v) {
        String a = norm(v.getVoterFnameEn());
        String b = norm(v.getVoterLnameEn());
        String c = norm(v.getRlnFnameEn());
        String d = norm(v.getRlnLnameEn());
        
        // Require at minimum: voter first name AND relation first name
        // Last names are optional but included if present
        if (a == null || c == null) return null;
        
        // Build key with available fields (use empty string for missing last names)
        String bVal = b != null ? b : "";
        String dVal = d != null ? d : "";
        String raw = String.join("|", a, bVal, c, dVal);
        return sha1(raw);
    }

    private String norm(String s) {
        if (s == null) return null;
        String t = s.trim().toLowerCase();
        return t.isEmpty() ? null : t;
    }

    private String sha1(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(raw.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
