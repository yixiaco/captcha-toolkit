package com.captcha.toolkit.model;

/**
 * 业务接口校验票据的请求体：{@code {"ticket": "..."}}。
 */
public class TicketVerifyRequest {

    private String ticket;

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }
}
