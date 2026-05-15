package com.tepinhui.tepinhui_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(JavaMailSender mailSender,
                       @Value("${spring.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendPasswordResetCode(String to, String code, long expireMinutes) {
        log.info("准备发送修改密码验证码邮件: to={}, from={}", to, fromAddress);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("特品汇修改密码验证码");
        message.setText("您正在修改特品汇账号密码，本次验证码为：" + code
                + "，" + expireMinutes + "分钟内有效。若非本人操作，请忽略本邮件。");

        try {
            log.info("正在连接 SMTP 服务器发送修改密码验证码邮件...");
            mailSender.send(message);
            log.info("修改密码验证码邮件发送成功: to={}", to);
        } catch (MailException e) {
            log.error("修改密码验证码邮件发送失败: to={}, error={}", to, e.getMessage(), e);
            throw new IllegalStateException("验证码发送失败，请稍后重试");
        }
    }

    public void sendRegistrationCode(String to, String code, long expireMinutes) {
        log.info("准备发送验证码邮件: to={}, from={}", to, fromAddress);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("特品汇注册验证码");
        message.setText("您正在注册特品汇账号，本次验证码为：" + code
                + "，" + expireMinutes + "分钟内有效。若非本人操作，请忽略本邮件。");

        try {
            log.info("正在连接 SMTP 服务器发送邮件...");
            mailSender.send(message);
            log.info("验证码邮件发送成功: to={}", to);
        } catch (MailException e) {
            log.error("验证码邮件发送失败: to={}, error={}", to, e.getMessage(), e);
            throw new IllegalStateException("验证码发送失败，请稍后重试");
        }
    }
}
