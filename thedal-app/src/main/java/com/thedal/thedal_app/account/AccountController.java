package com.thedal.thedal_app.account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.util.Response;

@RestController
@RequestMapping("/account")
public class AccountController {

	@Autowired
	private AccountService accountService;
	
	@PostMapping("/request")
	public ResponseEntity<Response<Integer>> requestToJoinAccount(@RequestParam Long accountId){
		Response<Integer> res=accountService.requestToJoinAccount(accountId);
		return ResponseEntity.status(HttpStatus.CREATED).body(res);
	}

	@DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<String> deleteAccount(@PathVariable Long accountId) {
        try {
            accountService.deleteAccount(accountId);
            return ResponseEntity.ok("Account deleted successfully");
        } catch (ThedalException ex) {
           
            return ResponseEntity.status(ex.getHttpStatus()).body(ex.getMessage());
        } catch (Exception ex) {
           
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting the account");
        }
    }
}
