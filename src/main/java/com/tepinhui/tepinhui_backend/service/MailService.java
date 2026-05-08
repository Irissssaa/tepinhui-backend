package com.tepinhui.tepinhui_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(JavaMailSender mailSender,
                       @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendRegistrationCode(String to, String code, long expireMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("特品汇注册验证码");
        message.setText("您正在注册特品汇账号，本次验证码为：" + code
                + "，" + expireMinutes + "分钟内有效。若非本人操作，请忽略本邮件。");

        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new IllegalStateException("验证码发送失败，请稍后重试");
        }
    }
}
