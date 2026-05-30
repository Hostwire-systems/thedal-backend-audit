// package com.thedal.thedal_app.util;

// import java.io.IOException;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import org.thymeleaf.TemplateEngine;
// import org.thymeleaf.context.Context;

// import com.sendgrid.Method;
// import com.sendgrid.Request;
// import com.sendgrid.Response;
// import com.sendgrid.SendGrid;
// import com.sendgrid.helpers.mail.Mail;
// import com.sendgrid.helpers.mail.objects.Content;
// import com.sendgrid.helpers.mail.objects.Email;
// import com.twilio.Twilio;
// import com.twilio.rest.api.v2010.account.Message;
// import com.twilio.type.PhoneNumber;

// import lombok.extern.slf4j.Slf4j;

// @Slf4j
// @Service
// public class MessagingService {

//     @Autowired
//     private TemplateEngine templateEngine;

//     @Value("${twilio.account.sid}")
//     private String accountSid;

//     @Value("${twilio.auth.token}")
//     private String authToken;

//     @Value("${twilio.from.phone.number}")
//     private String fromPhoneNumber;

//     @Value("${sendgrid.from.email}")
//     private String fromEmail;

//     @Value("${sendgrid.api.key}")
//     private String sendGridApiKey;

//     public void sendEmailUsingSendgrid(String email, String subject, String templateName, Context context)
//             throws IOException {

//         String htmlContent = templateEngine.process(templateName, context);

//         Email from = new Email(fromEmail);
//         Email to = new Email(email);
//         Content content = new Content("text/html", htmlContent);
//         Mail mail = new Mail(from, subject, to, content);

//         SendGrid sendGrid = new SendGrid(sendGridApiKey);
//         Request request = new Request();
//         try {
//             request.setMethod(Method.POST);
//             request.setEndpoint("mail/send");
//             request.setBody(mail.build());
//             Response response = sendGrid.api(request);

//             if (response.getStatusCode() == 200 || response.getStatusCode() == 202) {
//                 log.info("Success Response recieved from SendGrid. Status code: {}, Body: {}, Headers: {}",
//                         response.getStatusCode(), response.getBody(),
//                         response.getHeaders());
//             } else {
//                 log.error("Failure Response recieved from SendGrid. Response Status code: {}, Body: {}, Headers: {}",
//                         response.getStatusCode(),
//                         response.getBody(), response.getHeaders());
//             }
//         } catch (IOException ex) {
//             throw ex;
//         }
//     }

//     public void sendSms(String mobileNumber, String messageBody) {
//         try {
//             Twilio.init(accountSid, authToken);

//             Message.creator(
//                     new PhoneNumber("+91" + mobileNumber),
//                     new PhoneNumber(fromPhoneNumber),
//                     messageBody)
//                     .create();
//         } catch (Exception e) {
//             log.error("Error while sending SMS: {}", e.getMessage());
//             throw new IllegalArgumentException("Error while sending SMS");
//         }
//     }

// }