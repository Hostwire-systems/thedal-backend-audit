package com.thedal.thedal_app.voter.duplicate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "voter_duplicate_group", indexes = {
    @Index(name = "idx_dup_group_run", columnList = "run_id"),
    @Index(name = "idx_dup_group_keyhash", columnList = "key_hash"),
    @Index(name = "idx_dup_group_size_desc", columnList = "size DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class VoterDuplicateGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private VoterDuplicateRun run;

    @Column(name = "voter_fname_en_norm")
    private String voterFnameEnNorm;

    @Column(name = "voter_lname_en_norm")
    private String voterLnameEnNorm;

    @Column(name = "rln_fname_en_norm")
    private String rlnFnameEnNorm;

    @Column(name = "rln_lname_en_norm")
    private String rlnLnameEnNorm;

    @Column(name = "key_hash", nullable = false)
    private String keyHash;

    @Column(name = "size", nullable = false)
    private Integer size = 0;
}
