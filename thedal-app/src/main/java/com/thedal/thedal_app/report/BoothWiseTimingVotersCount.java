//package com.thedal.thedal_app.report;
//
//import java.time.LocalDateTime;
//
// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Id;
// import jakarta.persistence.PrePersist;
// import lombok.AllArgsConstructor;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.Setter;
//
// @NoArgsConstructor
// @AllArgsConstructor
// @Setter
// @Getter
// @Entity
// public class BoothWiseTimingVotersCount {
//
// 	@Id
// 	@GeneratedValue(strategy = GenerationType.IDENTITY)
// 	private Long id;
//	
// 	@Column(nullable = false)
// 	private Long electionId;
//	
// 	@Column(nullable = false)
// 	private Long boothNumber;
//	
// 	@Column(nullable = false)
// 	private int time07To08;
//	
// 	@Column(nullable = false)
// 	private int time08To09;
//	
// 	@Column(nullable = false)
// 	private int time09To10;
//	
// 	@Column(nullable = false)
// 	private int time10To11;
//	
// 	@Column(nullable = false)
// 	private int time11To12;
//	
// 	@Column(nullable = false)
// 	private int time12To13;
//	
// 	@Column(nullable = false)
// 	private int time13To14;
//	
// 	@Column(nullable = false)
// 	private int time14To15;
//	
// 	@Column(nullable = false)
// 	private int time15To16;
//	
// 	@Column(nullable = false)
// 	private int time16To17;
//	
// 	@Column(nullable = false)
// 	private long totalVote;
//	
// 	@Column(nullable = false)
// 	private LocalDateTime timestamp;
//	
// 	  @PrePersist
// 	    public void onSave() {
// 		  time07To08=0;
// 		  time08To09=0;
// 		  time09To10=0;
// 		  time10To11=0;
// 		  time11To12=0;
// 		  time12To13=0;
// 		  time13To14=0;
// 		  time14To15=0;
// 		  time15To16=0;
// 		  time16To17=0;
// 		  totalVote=0;
// 		  timestamp= LocalDateTime.now();
// 	    }
// }
