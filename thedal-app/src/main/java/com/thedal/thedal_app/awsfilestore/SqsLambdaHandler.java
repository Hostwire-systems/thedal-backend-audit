// package com.thedal.thedal_app.awsfilestore;

// import com.amazonaws.services.lambda.runtime.Context;
// import com.amazonaws.services.lambda.runtime.RequestHandler;
// import com.amazonaws.services.sqs.model.Message;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import java.util.*;


// public class SqsLambdaHandler implements RequestHandler<List<Message>, String> {

//     @Autowired
//     private JdbcTemplate jdbcTemplate;

//     @Override
//     public String handleRequest(List<Message> messages, Context context) {
//         for (Message message : messages) {
//             try {
//                 String messageBody = message.getBody();
//                 System.out.println("Received message: " + messageBody);

//                 // Convert JSON message into a User object
//                 ObjectMapper objectMapper = new ObjectMapper();
//                 Testing user = objectMapper.readValue(messageBody, Testing.class);

//                 // Store user data in PostgreSQL
//                 String sql = "INSERT INTO users (name, age, family) VALUES (?, ?, ?)";
//                 jdbcTemplate.update(sql, user.getName(), user.getAge(), user.getFamily());

//                 System.out.println("User data saved to PostgreSQL: " + user);

//             } catch (Exception e) {
//                 e.printStackTrace();
//                 System.err.println("Error processing message: " + e.getMessage());
//             }
//         }
//         return "Processed successfully";
//     }
// }


