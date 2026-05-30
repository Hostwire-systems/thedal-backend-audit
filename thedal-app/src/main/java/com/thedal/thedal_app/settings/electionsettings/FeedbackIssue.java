package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thedal.thedal_app.voter.VoterEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "feedback_issues")
@Getter
@Setter
@ToString(exclude = {"voters"})
@NoArgsConstructor
public class FeedbackIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_name", nullable = false, unique = false)
    private String issueName;
    @JsonIgnore
    @Column(name = "election_id", nullable = false)
    private Long electionId;
    
    @JsonIgnore
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "order_index")
    private Integer orderIndex;

    private LocalDateTime createdAt;
    
    @ManyToMany(mappedBy = "feedbackIssues")
    @JsonIgnore
    private Set<VoterEntity> voters = new HashSet<>();

    // ---- Equality & HashCode ----
    // Use only the primary key for equality to avoid touching lazy collections.
    // This prevents LazyInitializationException when entities are placed inside HashSet / HashMap
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FeedbackIssue)) return false;
        FeedbackIssue that = (FeedbackIssue) o;
        // If either id is null (transient entity), fall back to reference equality
        if (this.id == null || that.id == null) return false;
        return this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}

