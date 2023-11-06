package com.example.telegramBot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;


@Entity
@Data
public class Joke {
    @Column(length = 1000)
    String body;
    @Id
    Integer id;
}
