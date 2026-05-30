package com.thedal.thedal_app.voter.duplicate;

import com.thedal.thedal_app.voter.VoterEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "voter_duplicate_member", indexes = {
    @Index(name = "idx_dup_member_group", columnList = "group_id"),
    @Index(name = "idx_dup_member_voter", columnList = "voter_id")
})
@Getter
@Setter
@NoArgsConstructor
public class VoterDuplicateMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private VoterDuplicateGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private VoterEntity voter;

    @Column(name = "part_no")
    private Integer partNo;

    @Column(name = "serial_no")
    private Long serialNo;
}
