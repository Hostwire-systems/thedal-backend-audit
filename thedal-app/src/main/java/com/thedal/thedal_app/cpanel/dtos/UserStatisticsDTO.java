package com.thedal.thedal_app.cpanel.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsDTO {
    private Long totalUsers;
    private Long totalActiveUsers;
    private Long totalInactiveUsers;
    private Long totalSuperAdmins;
}
