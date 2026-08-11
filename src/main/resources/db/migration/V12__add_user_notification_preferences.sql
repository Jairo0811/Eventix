ALTER TABLE dbo.users
ADD reservation_notifications_enabled BIT NOT NULL
        CONSTRAINT DF_users_reservation_notifications DEFAULT 1 WITH VALUES,
    event_reminder_notifications_enabled BIT NOT NULL
        CONSTRAINT DF_users_event_reminder_notifications DEFAULT 1 WITH VALUES;
