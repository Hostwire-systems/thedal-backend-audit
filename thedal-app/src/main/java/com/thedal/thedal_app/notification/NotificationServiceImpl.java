package com.thedal.thedal_app.notification;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.sendgrid.DynamicTemplatePersonalization;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;

import lombok.extern.slf4j.Slf4j;



@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER= LogManager.getLogger(NotificationServiceImpl.class);
    @Autowired
    private NotificationsLogRepository notificationRepository;

    @Autowired
    private JavaMailSender javaMailSender;
     
    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private NotificationTemplateService notificationTemplateService;
    
    @Autowired
    private SendGrid sendGrid;
    
    @Autowired
    private DynamicTemplatePersonalization personalization;
    @Autowired
    private VoterRepo voterRepository;

    @Value("${notification.email.recipient}")
    private String defaultEmailRecipient;
    
    @Value("${sendgrid.email}")
    private String defaultEmailSender;
    @Autowired
	private RequestDetailsService requestDetails;
 
    public void saveNotification(boolean sendEmail,NotificationType notificationType) {
        LOGGER.info("inside saveNotification:notificationtype: {}", notificationType);
    
        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		UserEntity user = userRepo.findById(currentUserId).orElseThrow(()->new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        
	    
    	try {
    	LOGGER.info("Notification Message is {}",notificationType.getMessage());
    	NotificationsEntity notification = new NotificationsEntity();
        notification.setMessage(notificationType.getMessage());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUserId(user);
        notification.setUrl(notificationType.getUrl());
        notificationRepository.save(notification);
        LOGGER.info("app notification successful");
    	}catch(Exception e) {
    		  LOGGER.info("app notification failed:",e);
    	}

        // Send email if required
//        if (isSendEmail(sendEmail)) {
//        	try {
//				sendEmailNotification(javaMailSender,notificationType,userCred);
//			} catch (MessagingException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//        }
        if(sendEmail)
        	sendEmail(notificationType, user);
    }

    public void saveUserEntityNotification(boolean sendEmail,UserEntity userCred,NotificationType notificationType){
		try {
    	NotificationsEntity notification = new NotificationsEntity();
        notification.setMessage(notificationType.getMessage());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUserId(userCred);
        notification.setUrl(notificationType.getUrl());
        notificationRepository.save(notification);
        LOGGER.info("user created app notification successful");
		}catch(Exception e) {
			LOGGER.info("user created app notification failed:",e);
		}
        // Send email if required
//        if (isSendEmail(sendEmail)) {
//            try {
//				sendEmailNotification(javaMailSender,notificationType,userCred);
//			} catch (MessagingException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//        	
//        }
        
        if(sendEmail)
        	sendEmail(notificationType, userCred);
        
        LOGGER.info("user created notification success");
    }
    
    public List<NotificationsEntity> getLast30DaysNotifications(int page, int size) {
        Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        UserEntity user = userRepo.findById(currentUserId)
            .orElseThrow(() -> new ThedalException(ThedalError.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LOGGER.info("Thirty days ago: {}", thirtyDaysAgo);
    
        return notificationRepository.findByCreatedAtAfterAndUserIdOrderByCreatedAtDesc(thirtyDaysAgo, user, PageRequest.of(page, size));
    }
    public void deleteOldNotifications() {
        Date thirtyDaysAgo = new Date(System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L));
        notificationRepository.deleteByCreatedAtBefore(thirtyDaysAgo);
    }
    
//    public void sendEmailNotification(JavaMailSender javaMailSender,NotificationType notificationType,UserCred userCred) throws MessagingException {
//
//    	MimeMessage message=javaMailSender.createMimeMessage();
//    	MimeMessageHelper helper=new MimeMessageHelper(message,true,"UTF-8");
//        helper.setTo(userCred.getEmail());
//        helper.setSubject(notificationType.getSubject());
//        helper.setText(notificationType.getBody(),true);
//
//        javaMailSender.send(message);
//    }

//    public boolean isSendEmail(boolean sendEmail) {
//    	
//        return sendEmail;
//    }
    
    public void sendEmail(NotificationType notificationType,UserEntity userCred) 
	{
		//Mail mail = new Mail(new Email(defaultEmailSender), notificationType.getSubject(), new Email(userCred.getEmail()),new Content("text/plain", notificationType.getBody()));
		Mail mail=new Mail();
		
		personalization.addTo(new Email(userCred.getEmail()));
		mail.setFrom(new Email(defaultEmailSender));
    	mail.setReplyTo(new Email(defaultEmailSender));
    	mail.setSubject("sample subject");
    	personalization.addDynamicTemplateData("subject", notificationType.getSubject());
    	personalization.addDynamicTemplateData("body", notificationType.getBody());
    	mail.addPersonalization(personalization);
    	mail.setTemplateId("d-3ac220367cc54501bde2db144fb00093");
		Request request = new Request();

		Response response = null;

		try {

			request.setMethod(Method.POST);

			request.setEndpoint("mail/send");

			request.setBody(mail.build());

			response=this.sendGrid.api(request);
			
			if(response.getStatusCode()==200||response.getStatusCode()==202) {
				LOGGER.info("Email notification send successfully to:{},by sendEmail method",userCred.getEmail());
				LOGGER.info("SendGrid email body:",response.getBody());
			}else
				LOGGER.info("Inside else :Failed to send email with status code{}",response.getStatusCode());
			
		} catch (IOException ex) {

			LOGGER.info("Inside sendEmail method catch block: Email notification failed:",ex.getMessage());

		}
		
	}
    
    public List<BirthdayVoterDTO> getVotersWithBirthdayToday(Long electionId, int page, int size) {
        
    	Long accountId = requestDetails.getCurrentAccountId();
    	if (accountId == null) {
    		log.error("Account ID not found, unauthorized access.");
    		throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    	}
        
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        int todayMonth = today.getMonthValue();
        int todayDay = today.getDayOfMonth();
        
        LOGGER.info("Fetching voters with birthday today: accountId={}, electionId={}, month={}, day={}", 
            accountId, electionId, todayMonth, todayDay);
        
        List<VoterEntity> voters = voterRepository.findVotersByBirthdayToday(
            accountId, electionId, todayMonth, todayDay, PageRequest.of(page, size));
        
        LOGGER.info("Found {} voters with birthday today for electionId={}", voters.size(), electionId);
        
        List<BirthdayVoterDTO> voterDTOs = voters.stream()
            .map(voter -> {
                BirthdayVoterDTO dto = new BirthdayVoterDTO();
                dto.setEpicNumber(voter.getEpicNumber());
                dto.setMobileNo(voter.getMobileNo());
                dto.setVoterFnameEn(voter.getVoterFnameEn());
                dto.setVoterLnameEn(voter.getVoterLnameEn());
                dto.setDateLabel("TODAY");
                return dto;
            })
            .collect(Collectors.toList());
        
        LOGGER.debug("Voter DTOs for electionId={}: {}", electionId, voterDTOs);
        return voterDTOs;
    }
    

}
