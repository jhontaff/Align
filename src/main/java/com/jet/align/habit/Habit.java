package com.jet.align.habit;

import com.jet.align.common.model.BaseEntity;
import com.jet.align.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    // Hora del día a la que el usuario planea hacer el hábito. Solo informativa:
    // el agente la usa para ubicar el hábito en su hora en la vista unificada.
    // Nullable -- un hábito sin hora sigue siendo válido (se hace en cualquier momento).
    private LocalTime scheduledTime;

}
