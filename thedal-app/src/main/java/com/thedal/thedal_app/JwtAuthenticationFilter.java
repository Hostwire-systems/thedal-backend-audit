package com.thedal.thedal_app;

import java.io.IOException;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("!reporting")
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	
	@Autowired
	private UserRepo userRepo;


//    // authentication filter that checks every request for a valid jwt token
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException {
//        try {
//            String jwt = getJwtFromRequest(request);
//
//            if (StringUtils.hasText(jwt) && JwtService.validateToken(jwt)) {
//                Long userId = JwtService.getUserIdFromToken(jwt);
//
//                UserEntity user = userRepo.findById(userId).orElseThrow(() -> 
//                new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//                if (Boolean.FALSE.equals(user.getIsActive())) {
//                    logger.error("Your account is in inactive status. Please contact support.");
//                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                    response.setContentType("application/json");
//                    response.setCharacterEncoding("UTF-8");
//                    response.getWriter().write("{\"message\": \"Your account is in inactive status. Please contact support\", \"success\": false}");
//                    return;
//                }
//                
//                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId,
//                        null, new ArrayList<>());
//                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                SecurityContextHolder.getContext().setAuthentication(authentication);
//                //UserEntity user = userRepo.findById(userId).orElseThrow(()->new RuntimeException("User not found"));
//				log.info("Got account object");
//				RequestContextHolder.currentRequestAttributes().setAttribute("user", user,
//						RequestAttributes.SCOPE_REQUEST);
//            }
//        } catch (ExpiredJwtException ex) {
//            logger.error("Expired JWT token");
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.setContentType("application/json");
//            response.setCharacterEncoding("UTF-8");
//            response.getWriter().write("{\"message\": \"Expired JWT token\", \"success\": false}");
//            return;
//        } catch (Exception ex) {
//            logger.error("Could not set user authentication in security context");
//
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.setContentType("application/json");
//            response.setCharacterEncoding("UTF-8");
//            response.getWriter().write("{\"message\": \"Invalid token\", \"success\": false}");
//            return;
//
//        }
//
//        filterChain.doFilter(request, response);
//    }
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
	        throws ServletException, IOException {
	    try {
	        String jwt = getJwtFromRequest(request);

	        if (StringUtils.hasText(jwt)) {
	            try {
	                // First validate token structure
	                if (!JwtService.validateToken(jwt)) {
	                    sendErrorResponse(response, "Invalid token");
	                    return;
	                }

	                Long userId = JwtService.getUserIdFromToken(jwt);
	                UserEntity user = userRepo.findByIdWithAccountAndRole(userId).orElseThrow(() -> 
	                    new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));

	                // Check password version only for tokens that include it (version > 1)
	                Integer tokenPwdVersion = JwtService.getPasswordVersionFromToken(jwt);
	                if (tokenPwdVersion > 1 && !tokenPwdVersion.equals(user.getPasswordVersion())) {
	                    sendErrorResponse(response, "Session terminated due to password change");
	                    return;
	                }

	                if (Boolean.FALSE.equals(user.getIsActive())) {
	                    sendErrorResponse(response, "Account inactive");
	                    return;
	                }

	                // Create authentication and proceed
	                UsernamePasswordAuthenticationToken authentication = 
	                    new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
	                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
	                SecurityContextHolder.getContext().setAuthentication(authentication);
	                
	                RequestContextHolder.currentRequestAttributes().setAttribute("user", user,
	                    RequestAttributes.SCOPE_REQUEST);

                // Best-effort: update session lastActive if jti present (non-blocking failure)
                try {
                	String jti = JwtService.getJti(jwt);
                	if (jti != null) {
                		var ctx = org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext();
                		if (ctx != null) {
                			var sessionService = ctx.getBean(com.thedal.thedal_app.auth.session.UserDeviceSessionService.class);
                			// throttle handled in service
                			sessionService.touchIfDue(jti);
                		}
                	}
                } catch (Exception ignored) {}

	            } catch (ExpiredJwtException ex) {
	                sendErrorResponse(response, "Token expired");
	                return;
	            } catch (Exception ex) {
	                sendErrorResponse(response, "Invalid token");
	                return;
	            }
	        }
	    } catch (Exception ex) {
	        logger.error("Authentication error", ex);
	        sendErrorResponse(response, "Authentication error");
	        return;
	    }

	    filterChain.doFilter(request, response);
	}

	private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
	    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	    response.setContentType("application/json");
	    response.setCharacterEncoding("UTF-8");
	    response.getWriter().write("{\"message\": \"" + message + "\", \"success\": false}");
	}

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7, bearerToken.length());
        }
        return null;
    }
}
