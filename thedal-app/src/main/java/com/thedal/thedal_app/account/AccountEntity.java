package com.thedal.thedal_app.account;

import java.util.List;

import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.volunteer.VolunteerEntity;
import com.thedal.thedal_app.voter.VoterEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "account")
public class AccountEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	

    @Column(name = "on_board_status")
    private Integer onBoardStatus;


}
