package com.sunrisejay.jaychat.dto.request;

import lombok.Data;

/**
 * 更新群信息请求DTO
 */
@Data
public class UpdateGroupInfoRequest {
    /**
     * 群名称
     */
    private String groupName;

    /**
     * 群公告
     */
    private String notice;
}
