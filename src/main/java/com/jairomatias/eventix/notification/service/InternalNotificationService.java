package com.jairomatias.eventix.notification.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.notification.entity.InternalNotification;
import com.jairomatias.eventix.notification.entity.InternalNotificationType;
import com.jairomatias.eventix.notification.repository.InternalNotificationRepository;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class InternalNotificationService {

    private static final int PAGE_SIZE = 20;

    private final InternalNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public InternalNotificationService(
            InternalNotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<InternalNotification> findForUser(String login, int page) {
        User user = findUser(login);
        return notificationRepository.findAllByRecipient_IdOrderByCreatedAtDesc(
                user.getId(),
                PageRequest.of(
                        Math.max(page, 0),
                        PAGE_SIZE,
                        Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public long unreadCount(String login) {
        return notificationRepository.countByRecipient_IdAndReadAtIsNull(
                findUser(login).getId());
    }

    @Transactional
    public String markRead(Long id, String login) {
        User user = findUser(login);
        InternalNotification notification = notificationRepository
                .findByIdAndRecipient_Id(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la notificación."));
        notification.markRead(LocalDateTime.now());
        String targetUrl = notification.getTargetUrl();
        return targetUrl == null || targetUrl.isBlank()
                ? "/notifications"
                : targetUrl;
    }

    @Transactional
    public void notify(
            User recipient,
            InternalNotificationType type,
            String title,
            String message,
            String targetUrl) {
        notificationRepository.save(new InternalNotification(
                recipient,
                type,
                title,
                message,
                targetUrl));
    }

    private User findUser(String login) {
        return userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(login, login)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));
    }
}
