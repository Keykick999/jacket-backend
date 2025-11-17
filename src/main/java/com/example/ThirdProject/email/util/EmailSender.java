package com.example.ThirdProject.email.util;

public interface EmailSender {
    void send(String to, String subject, String body);
}