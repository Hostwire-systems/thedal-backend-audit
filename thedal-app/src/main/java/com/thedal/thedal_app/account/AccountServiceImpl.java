package com.thedal.thedal_app.account;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.amazonaws.services.accessanalyzer.model.ResourceNotFoundException;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.util.Response;
import com.thedal.thedal_app.volunteer.VolunteerEntity;
import com.thedal.thedal_app.volunteer.VolunteerRepository;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AccountServiceImpl implements AccountService {
	
//	@Autowired
//	private AccountToUserRepository accountToUserRepository;
	
	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private VoterRepo voterRepo;
	
	
	@Autowired
	private UserRepo userRepo;

	@Autowired 
	private VolunteerRepository volunteerRepository;

	@Autowired
	private ElectionRepository electionRepository;

	@Override
	public Response<Integer> requestToJoinAccount(Long accountId) {
		Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		UserEntity user = userRepo.findByIdAndIsActive(currentUserId, true).orElse(null);
		if (user == null)
			throw new RuntimeException("User is not present or is not active");
			
		AccountEntity account = accountRepository.findById(accountId).orElseThrow(()-> new RuntimeException("Account does not exist"));
//		AccountToUser existingAccoutToUser= accountToUserRepository.findByAccountAndUserEntity(account,user).orElse(null);
//		if(existingAccoutToUser != null) 
//			throw new RuntimeException("already you are in this account.please contact team");
//		
//		AccountToUser accountToUser=new AccountToUser();
//		accountToUser.setAccount(account);
//		accountToUser.setUserEntity(user);
//		accountToUser.setStatus(AccountStatus.REQUESTED.getValue());		
		
		Response<Integer> response=new Response<>();
		response.setSuccess(true);
		response.setMessage("Join account request is successful");
		response.setData(1);
		return response;
	}
	
	 public AccountOnBoardStatus getCurrentAccountOnBoardStatus() {
	     
	        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
	        AccountEntity account = ((UserEntity) attributes.getAttribute(RequestDetailsService.USER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)).getAccountEntity();
	        
	        if (account != null && account.getOnBoardStatus() != null) {
	           
	            return AccountOnBoardStatus.fromValue(account.getOnBoardStatus());
	        }
	        
	        // Handle case where account or onBoardStatus is null
	        throw new ThedalException(ThedalError.ACCOUNT_OR_ONBOARD_STATUS_NOT_FOUND,HttpStatus.NOT_FOUND);
	    }
	 
	// Common method to retrieve the current account entity from the request attributes
	    public AccountEntity getCurrentAccountFromRequest() {
	    	log.info(" inside getCurrentAccountFromRequest method");
	        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
	        AccountEntity account = ((UserEntity) attributes.getAttribute(RequestDetailsService.USER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)).getAccountEntity();
	        log.info(" inside getCurrentAccountFromRequest method: Got Account Object:{}",account.getId());
	        return account;
	    }
	    
		// Common method to update onBoardStatus of the account
		public boolean updateAccountOnBoardStatus(AccountOnBoardStatus newStatus) {
			try {
				log.info("inside updateAccountOnBoardStatus ");
				// Retrieve the current account from request
				AccountEntity account = getCurrentAccountFromRequest();

				// Log the current and new onBoardStatus
				log.info("Updating onBoardStatus from {} to {}", account.getOnBoardStatus(), newStatus.getValue());

				// Update the account's onBoardStatus
				account.setOnBoardStatus(newStatus.getValue());

				// Save the updated account entity
				accountRepository.save(account);

				log.info("onBoardStatus updated successfully for account with id {}", account.getId());
				return true;
			} catch (Exception e) {
				log.error("Error updating onBoardStatus", e);
				return false; // Indicate failure
			}
		}

		@Transactional
        public void deleteAccount(Long accountId) {
        AccountEntity account = accountRepository.findById(accountId)  
		.orElseThrow(() ->{
			log.warn("account not found with ID: {}", accountId);
			return new ThedalException(ThedalError.ACCOUNT_DELETE_FAILED, HttpStatus.NOT_FOUND);
		});

		List<VolunteerEntity> volunteers = volunteerRepository.findByAccountId(accountId);
        if (!volunteers.isEmpty()) {
        volunteerRepository.deleteAll(volunteers);
        log.info("Deleted {} volunteers for account ID: {}", volunteers.size(), accountId);
    }

	    List<VoterEntity> voters = voterRepo.findByAccountId(accountId);
        if (!voters.isEmpty()) {
        voterRepo.deleteAll(voters);
        log.info("Deleted {} voters for account ID: {}", voters.size(), accountId);
    }

	    List<ElectionEntity> elections = electionRepository.findByAccountId(accountId);
        if (!elections.isEmpty()) {
        electionRepository.deleteAll(elections);
        log.info("Deleted {} elections for account ID: {}", elections.size(), accountId);
    }
    
    // Delete account (cascade will delete related entities)
         accountRepository.delete(account);
		 log.info("Account with ID: {} successfully deleted", accountId);
}

}
