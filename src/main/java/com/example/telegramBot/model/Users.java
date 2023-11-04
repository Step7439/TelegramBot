package com.example.telegramBot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.repository.CrudRepository;

import java.sql.Timestamp;

@Entity
@Data
public class Users {

    @Id
    private Long id;
    private String username;
    private Timestamp timestamp;
}
