package com.thedal.thedal_app.sqs;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sqs")
public class Controller {

    private final SQSTest sqsTest;

    public Controller(SQSTest sqsTest) {
        this.sqsTest = sqsTest;
    }

    @PostMapping("/send")
    public String sendMessageToSqs(@RequestBody String message) {
        sqsTest.sendMessage(message);
        return "Message sent to SQS successfully!";
    }

}
