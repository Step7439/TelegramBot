package com.example.telegramBot.repo;

import com.example.telegramBot.model.Users;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface RepoUsers extends CrudRepository<Users, Long> {

}
