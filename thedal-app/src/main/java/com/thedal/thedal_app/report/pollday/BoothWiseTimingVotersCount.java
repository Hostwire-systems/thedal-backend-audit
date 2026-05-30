package com.thedal.thedal_app.report.pollday;


import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booth_wise_timing_voters_count")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class BoothWiseTimingVotersCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long electionId;

    private Long accountId;

    private Long boothNumber;

    private long time07To08;

    private long time08To09;

    private long time09To10;

    private long time10To11;

    private long time11To12;

    private long time12To13;

    private long time13To14;

    private long time14To15;

    private long time15To16;

    private long time16To17;

    private long totalVote;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}