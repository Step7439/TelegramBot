package com.example.telegramBot.repo;

import com.example.telegramBot.model.Joke;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface RepoJoke extends CrudRepository<Joke, Integer> {

}
