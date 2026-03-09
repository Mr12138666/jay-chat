package com.sunrisejay.jaychat.dto.request;

import lombok.Data;
import java.util.List;

/**
 * 创建群聊请求DTO
 */
@Data
public class CreateGroupRequest {
    /**
     * 群名称
     */
    private String groupName;

    /**
     * 成员ID列表（不包括群主）
     */
    private List<Long> memberIds;
}
