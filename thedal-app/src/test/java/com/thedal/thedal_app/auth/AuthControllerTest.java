// package com.thedal.thedal_app.auth;
//
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.http.ResponseEntity;
// import org.springframework.test.web.servlet.MockMvc;
// import com.thedal.thedal_app.util.Response;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.thedal.thedal_app.auth.dtos.SignupRequestDto;
// import com.thedal.thedal_app.auth.dtos.LoginResponseDto;
// import static org.mockito.ArgumentMatchers.any; 
// import static org.mockito.Mockito.when;      
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import org.springframework.http.MediaType;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// import java.io.IOException;
//
//
// @WebMvcTest(AuthController.class)
// class AuthControllerTest {
//
//     @Autowired
//     MockMvc mockMvc;
//
//     @MockBean
//     AuthService userService;
//
//     private SignupRequestDto validRequest;
//     private SignupRequestDto invalidRequest;

//     @BeforeEach
//     void setUp() {
//    	
//         LoginResponseDto responseDto = new LoginResponseDto();
//         responseDto.setUserId(54L);
//         responseDto.setFullName("anu");
//         responseDto.setEmail("anu@gmail.com");
//         responseDto.setMobileNumber("8300129149");
//         responseDto.setRole("USER");
//         responseDto.setRoleId(2L);
//         responseDto.setIsEmailVerified(false);
//         responseDto.setIsMobileVerified(false);
//         responseDto.setOnBoardStatus(2);
//         responseDto.setJwt("abc");
//         
//         Response<LoginResponseDto> response = new Response<>();
//         response.setMessage("User created successfully");
//         response.setData(responseDto);
//         response.setSuccess(true);
//        
//         // Create a valid signup request
//         validRequest = new SignupRequestDto();
//         validRequest.setName("testuser");
//         validRequest.setPassword("password123");
//         validRequest.setEmail("testuser@example.com");
//
//         // Create an invalid signup request (e.g., missing email)
//         invalidRequest = new SignupRequestDto();
//         invalidRequest.setName("testuser");
//         invalidRequest.setPassword("password123");
//     }
    
//     @Test
//     void signUp_withValidRequest_shouldReturnOk() throws Exception {
//         // Create the LoginResponseDto object with a dummy token
//         LoginResponseDto responseDto = new LoginResponseDto();
//         responseDto.setUserId(54L);
//         responseDto.setFullName("anu");
//         responseDto.setEmail("anu@gmail.com");
//         responseDto.setMobileNumber("8300129149");
//         responseDto.setRole("USER");
//         responseDto.setRoleId(2L);
//         responseDto.setIsEmailVerified(false);
//         responseDto.setIsMobileVerified(false);
//         responseDto.setOnBoardStatus(2);
//         responseDto.setJwt("abc");
//        
//         Response<LoginResponseDto> response = new Response<>();
//         response.setMessage("User created successfully");
//         response.setData(responseDto);
//         response.setSuccess(true);
//         
//         SignupRequestDto signupDto=new SignupRequestDto();
//         signupDto.setEmail("anu@gmail.com");
//         signupDto.setMobile("8300129147");
//         signupDto.setName("anu");
//         signupDto.setPassword("Sivanesh@1996");
//         signupDto.setRoleID(2);
//
//         // Mock the UserService to return a successful response with the dummy token
//         when(userService.signUp(any(SignupRequestDto.class)))
//             .thenReturn(ResponseEntity.ok(response)); // Return the mock response
//
//         // Perform the request and assert the response
//         mockMvc.perform(post("/auth/signup")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(new ObjectMapper().writeValueAsString(signupDto))) // Convert the valid request to JSON
//             .andExpect(status().isOk());
//     }


//     @Test
//     void signUp_withInvalidRequest_shouldReturnBadRequest() throws Exception {
//         // Test for invalid signup request (missing email)
//         mockMvc.perform(post("/signup")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(asJsonString(invalidRequest)))
//                 .andExpect(status().isBadRequest())
//                 .andExpect(jsonPath("$.errors").exists());
//     }

//     @Test
//     void signUp_withServiceException_shouldReturnInternalServerError() throws Exception {
//         // Simulate a service exception (e.g., IOException)
//         when(userService.signUp(any(SignupRequestDto.class)))
//                 .thenThrow(new IOException("Service error"));

//         mockMvc.perform(post("/signup")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(asJsonString(validRequest)))
//                 .andExpect(status().isInternalServerError())
//                 .andExpect(jsonPath("$.message").value("Service error"));
//     }

//     // Helper method to convert an object to JSON string
//     private static String asJsonString(final Object obj) {
//         try {
//             return new ObjectMapper().writeValueAsString(obj);
//         } catch (Exception e) {
//             throw new RuntimeException(e);
//         }
//     }
// }

