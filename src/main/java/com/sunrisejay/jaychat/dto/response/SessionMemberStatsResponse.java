package com.sunrisejay.jaychat.dto.response;

import lombok.Data;

@Data
public class SessionMemberStatsResponse {
    private Integer totalMembers;    // 总人数
    private Integer onlineMembers;   // 在线人数
}