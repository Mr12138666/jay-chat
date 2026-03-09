package com.sunrisejay.jaychat.dto.request;

import lombok.Data;
import java.util.List;

/**
 * 邀请成员请求DTO
 */
@Data
public class InviteMemberRequest {
    /**
     * 被邀请的用户ID列表
     */
    private List<Long> userIds;
}
