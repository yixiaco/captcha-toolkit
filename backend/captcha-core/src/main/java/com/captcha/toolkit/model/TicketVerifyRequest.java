package com.captcha.toolkit.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 业务接口校验票据的请求体：{@code {"ticket": "..."}}。
 */
@Data
public class TicketVerifyRequest {

    /** 验证通过后发放的一次性票据，必填 */
    @NotBlank(message = "缺少票据 ticket")
    private String ticket;
}
