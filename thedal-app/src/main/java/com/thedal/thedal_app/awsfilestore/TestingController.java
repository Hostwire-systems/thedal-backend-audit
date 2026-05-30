// package com.thedal.thedal_app.awsfilestore;

// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;

// public class TestingController {


//     private final SqsService sqsService;

//     public TestingController(SqsService sqsService) {
//         this.sqsService = sqsService;
//     }

//     @PostMapping("/create")
//     public String createUser(@RequestBody String userPayload) {
//         sqsService.sendMessage(userPayload);
//         return "User details sent to SQS!";
//     }

// }
