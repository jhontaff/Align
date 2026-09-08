package com.jet.align.habit;

import com.jet.align.common.model.BaseEntity;
import com.jet.align.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "habits")
@Getter
@Setter
@NoArgsConstructor
public class Habit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private LocalTime scheduledTime;


    @Column(name = "last_reminded_on")
    private LocalDate lastRemindedOn;

}
