//package com.thedal.thedal_app.account;
//
//import com.thedal.thedal_app.user.UserEntity;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToOne;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Entity
//public class AccountToUser {
//
//	@Id
//	@GeneratedValue(strategy = GenerationType.AUTO)
//	private Long id;
//	
//	@ManyToOne
//	@JoinColumn(name = "user_id")
//	private UserEntity userEntity;
//
//	@ManyToOne
//	@JoinColumn(name = "account_id")
//	private Account account;
//	
//	private String uuid;
//
//	@Column(nullable = false)
//	private Integer status; // 1->invited 2->active 4->requested -8->rejected
//	
//}
