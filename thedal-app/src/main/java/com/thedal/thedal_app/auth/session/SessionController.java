package com.thedal.thedal_app.auth.session;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.thedal.thedal_app.util.Response;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final UserDeviceSessionService sessionService;

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        if (principal instanceof Long l) return l;
        return null;
    }

    @GetMapping
    public ResponseEntity<Response<List<SessionDto>>> listActive() {
        Long uid = currentUserId();
        Response<List<SessionDto>> resp = new Response<>();
        if (uid == null) {
            resp.setMessage("Unauthorized");
            resp.setSuccess(false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }
        String tmpJti = null;
        try {
            var req = (jakarta.servlet.http.HttpServletRequest) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()
                    .resolveReference(org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST);
            if (req != null) {
                String bearer = req.getHeader("Authorization");
                if (bearer != null && bearer.startsWith("Bearer ")) {
                    String token = bearer.substring(7);
                    tmpJti = com.thedal.thedal_app.JwtService.getJti(token);
                }
            }
        } catch (Exception ignored) {}
        final String currentJti = tmpJti; // effectively final for lambda
        var sessions = sessionService.listActive(uid).stream()
                .map(SessionDto::from)
                .peek(dto -> { /* placeholder: jti not exposed in DTO yet */ })
                .collect(Collectors.toList());
        resp.setData(sessions);
        resp.setMessage("Active sessions fetched");
        resp.setSuccess(true);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Integer>> revoke(@PathVariable Long id) {
        Long uid = currentUserId();
        Response<Integer> resp = new Response<>();
        if (uid == null) {
            resp.setMessage("Unauthorized");
            resp.setSuccess(false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }
        boolean ok = sessionService.revoke(uid, id);
        resp.setSuccess(ok);
        resp.setMessage(ok ? "Session revoked" : "Not found or already revoked");
        resp.setData(0);
        return ResponseEntity.status(ok ? HttpStatus.OK : HttpStatus.NOT_FOUND).body(resp);
    }
}
