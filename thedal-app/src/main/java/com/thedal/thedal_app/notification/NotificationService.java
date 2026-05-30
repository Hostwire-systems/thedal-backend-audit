package com.thedal.thedal_app.notification;

import java.util.List;

import com.thedal.thedal_app.user.UserEntity;

public interface  NotificationService {
    public void saveUserEntityNotification(boolean sendEmail,UserEntity userCred,NotificationType notificationType);
	 public void saveNotification(boolean sendEmail,NotificationType notificationType);
	 public List<NotificationsEntity> getLast30DaysNotifications(int page, int size);
	 public void deleteOldNotifications();

}
