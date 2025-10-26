package com.yomirein.sochatclient.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "messages")
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne private Chat chat;
    @ManyToOne private User sender;
    private String content;
    private LocalDateTime timestamp = LocalDateTime.now();
}