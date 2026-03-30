package com.example.equipmentlending.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleNotificationService.class);

    @Override
    public void sendNotification(String message) {
        logger.info("[NOTIFICATION] {}", message);
    }
}
