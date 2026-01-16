package com.planotech.plano.helper;

import com.planotech.plano.exception.MailServerException;
import com.planotech.plano.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Component
public class EmailSender {

    @Autowired
    JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;


    @Async
    public CompletableFuture<Boolean> sendVerificationEmail(User user, String subject, String redirectUrl) {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        } catch (MessagingException e) {
            throw new MailServerException("Couldn't send the mail", e);
        }
        try {
            helper.setFrom(fromEmail, "QuantumShare");
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            String htmlBody = readHtmlTemplate("password_reset_request.html");
            htmlBody = htmlBody.replace("{{RESET_PASSWORD_LINK}}", redirectUrl);
            htmlBody = htmlBody.replace("{{USERNAME}}", user.getName());
            helper.setText(htmlBody, true);
            mailSender.send(message);
            return CompletableFuture.completedFuture(true);
        } catch (UnsupportedEncodingException | MessagingException e) {
            throw new MailServerException("Couldn't send the mail", e);
        }
    }

    private String readHtmlTemplate(String templateName) {
        try (InputStream inputStream =
                     new ClassPathResource("templates/" + templateName).getInputStream()) {

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MailServerException("Failed to load email template", e);
        }
    }
}
