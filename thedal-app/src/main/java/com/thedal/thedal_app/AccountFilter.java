//package com.thedal.thedal_app;
//
//import java.io.IOException;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.context.request.RequestAttributes;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.support.SpringBeanAutowiringSupport;
//
//import com.thedal.thedal_app.account.Account;
//import com.thedal.thedal_app.account.AccountRepository;
//import com.thedal.thedal_app.auth.SecurityUserDetails;
//import com.thedal.thedal_app.user.UserRepo;
//import com.thedal.thedal_app.util.JwtUtil;
//
//import jakarta.servlet.Filter;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.FilterConfig;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.ServletRequest;
//import jakarta.servlet.ServletResponse;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//public class AccountFilter implements Filter {
//
//	@Autowired
//	private UserRepo userRepo;
//
//	@Autowired
//	private AccountRepository accountRepository;
//	
//	@Autowired
//	private JwtUtil jwtUtil;
//
//	@Override
//	public void init(FilterConfig filterconfig) throws ServletException {
//		SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, filterconfig.getServletContext());
//	}
//
//	@Override
//	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//			throws IOException, ServletException {
//
//		log.info("Inside doFilter");
//		String requestURI = ((HttpServletRequest) request).getRequestURI();
//		
//		// Exclude public and Swagger-related endpoints
//	    if (requestURI.startsWith("/swagger-ui") ||
//	        requestURI.startsWith("/v3/api-docs") ||
//	        requestURI.startsWith("/configuration/ui") ||
//	        requestURI.startsWith("/swagger-resources") ||
//	        requestURI.startsWith("/configuration/security") ||
//	        requestURI.startsWith("/swagger-ui.html") ||
//	        requestURI.startsWith("/webjars") ||
//	        requestURI.equals("/") || 
//	        requestURI.startsWith("/login") ||
//	        requestURI.startsWith("/oauth2") ||
//	        requestURI.startsWith("/auth/two-factor/otp") ||
//	        requestURI.startsWith("/auth/ping") ||
//	        requestURI.startsWith("/auth/signup") ||
//	        requestURI.startsWith("/auth/login") ||
//	        requestURI.startsWith("/auth/two-factor/email-verify") ||
//	        requestURI.startsWith("/auth/signup/oauth-complete") ||
//	        requestURI.startsWith("/oauth/complete-signup")) {
//	        
//	        log.info("Skipping authentication for public endpoints: {}", requestURI);
//	        chain.doFilter(request, response);
//	        return;
//	    }
//
//		if (!requestURI.startsWith("/auth/*")) {
//			log.info("Inside doFilter-other than sign-in links");
//
//			 Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
//  	   	   log.info("authentication is not null:{}",authentication.getPrincipal() instanceof String jwtPrincipal);
//			   if(authentication != null && authentication.getPrincipal() instanceof String jwtPrincipal) {
//				   log.info("Inside doFilter- if condition- authentication");
//				   //SecurityUserDetails userDetails=(SecurityUserDetails)authentication.getPrincipal();
//						Long userId=Long.parseLong(jwtPrincipal);
//						log.info("User ID:{}",userId);
//
//				Account account = accountRepository.findByUserEntity_Id(userId).orElseThrow(()->new RuntimeException("Account not found"));
//				log.info("Got account object");
//				RequestContextHolder.currentRequestAttributes().setAttribute("account", account,
//						RequestAttributes.SCOPE_REQUEST);
//			
//			      }	else {
//		  	   		HttpServletResponse httpServletResponse=(HttpServletResponse)response;
//		  	   		httpServletResponse.setStatus(org.springframework.http.HttpStatus.UNAUTHORIZED.value());
//		  	   		httpServletResponse.getWriter().write("Please login");
//		  	   		return;
//		  	   	}
//		}
//		chain.doFilter(request, response);
//
//	}
//
//
//	@Override
//	public void destroy() {
//		log.info("Filter is being destroyed");
//	}
//
//}
