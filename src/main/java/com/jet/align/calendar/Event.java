package com.jet.align.calendar;

import com.jet.align.common.model.BaseEntity;
import com.jet.align.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_events")
@Getter
@Setter
@NoArgsConstructor
public class Event extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")                 String description;
    @Column(name = "start_at", nullable = false)
    LocalDateTime startAt;

    @Column(name = "end_at")
    LocalDateTime endAt;

    @Column
    String location;

    @Column(name = "reminder_minutes_before")
    Integer reminderMinutesBefore;

    @Column(name = "reminder_at")
    LocalDateTime reminderAt;

    @Column(name = "reminder_sent", nullable = false)
    boolean reminderSent;

}
