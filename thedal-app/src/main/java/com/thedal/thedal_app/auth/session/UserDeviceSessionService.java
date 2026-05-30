package com.thedal.thedal_app.auth.session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDeviceSessionService {

    private final UserDeviceSessionRepository repository;

    // simple in-memory throttle to reduce DB writes for last_active updates
    private final ConcurrentHashMap<String, LocalDateTime> lastTouchCache = new ConcurrentHashMap<>();
    private static final long TOUCH_INTERVAL_SECONDS = 60; // update at most once per minute per jti

    public String generateDeviceId() {
        // Short unique id (first 10 chars of UUID without dashes)
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    public String generateJti() {
        return UUID.randomUUID().toString();
    }

    @Transactional
    public UserDeviceSession createSession(Long userId, String deviceId, String jti, String userAgent, String ip,
            String deviceName, String platform, String browser, Integer pwdVersion) {
        UserDeviceSession session = UserDeviceSession.builder()
                .userId(userId)
                .deviceId(deviceId)
                .jti(jti)
                .userAgent(userAgent)
                .deviceName(deviceName)
                .platform(platform)
                .browser(browser)
                .ipAddress(ip)
                .passwordVersionAtIssue(pwdVersion)
                .build();
        return repository.save(session);
    }

    public void touchIfDue(String jti) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last = lastTouchCache.get(jti);
        if (last != null && last.plusSeconds(TOUCH_INTERVAL_SECONDS).isAfter(now)) return;
        repository.findByJti(jti).ifPresent(s -> {
            s.setLastActiveAt(now);
            repository.save(s); // simple save; could optimize with @Modifying query
            lastTouchCache.put(jti, now);
        });
    }

    public List<UserDeviceSession> listActive(Long userId) {
        return repository.findByUserIdAndRevokedAtIsNullOrderByLastActiveAtDesc(userId);
    }

    @Transactional
    public boolean revoke(Long userId, Long sessionId) {
        Optional<UserDeviceSession> opt = repository.findById(sessionId);
        if (opt.isPresent() && opt.get().getUserId().equals(userId) && opt.get().getRevokedAt() == null) {
            UserDeviceSession s = opt.get();
            s.setRevokedAt(LocalDateTime.now());
            repository.save(s);
            return true;
        }
        return false;
    }

    @Transactional
    public int revokeOthers(Long userId, Long keepSessionId) {
        return repository.revokeAllOtherActive(userId, keepSessionId);
    }

    @Transactional
    public int revokeAll(Long userId) {
        return repository.revokeAllActive(userId);
    }
}
