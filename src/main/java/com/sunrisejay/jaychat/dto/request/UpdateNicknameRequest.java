package com.sunrisejay.jaychat.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @description:
 * @author: Sunrise_Jay
 * @email: sunrise_jay@yeah.net
 * @date: 2026/3/5 15:34
 */
@Data
public class UpdateNicknameRequest {

    @NotBlank(message = "昵称不能为空")
    private String nickname;


}
