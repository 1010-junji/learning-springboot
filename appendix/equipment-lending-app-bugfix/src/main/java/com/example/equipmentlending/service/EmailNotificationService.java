package com.example.equipmentlending.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// メールで送信する（ダミー）通知機能
// BUG: NotificationService の実装が2つになったため、起動時に NoUniqueBeanDefinitionException が発生する
@Service
public class EmailNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    @Override
    public void sendNotification(String message) {
        logger.info("[メール送信] {}", message);
    }
}
