package com.thedal.thedal_app.account;

import com.thedal.thedal_app.util.Response;

import jakarta.transaction.Transactional;

public interface AccountService {

	Response<Integer> requestToJoinAccount(Long accountId);

	public AccountOnBoardStatus getCurrentAccountOnBoardStatus();
	
	public AccountEntity getCurrentAccountFromRequest();
	
	public boolean updateAccountOnBoardStatus(AccountOnBoardStatus newStatus);
	
	@Transactional
    void deleteAccount(Long accountId);

}
