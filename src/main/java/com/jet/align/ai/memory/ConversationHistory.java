package com.jet.align.ai.memory;

import com.jet.align.common.model.BaseEntity;
import com.jet.align.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversation_histories")
@Getter
@Setter
@NoArgsConstructor
public class ConversationHistory extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "history_json", nullable = false, columnDefinition = "TEXT" )
    private String historyJson;

}
