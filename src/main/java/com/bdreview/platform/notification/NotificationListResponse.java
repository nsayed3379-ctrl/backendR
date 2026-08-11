package com.bdreview.platform.notification;

import com.bdreview.platform.common.PageResponse;

public record NotificationListResponse(PageResponse<Notification> notifications, long unreadCount) {
}
