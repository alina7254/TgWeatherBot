package com.telegrambot.repository;

import com.telegrambot.model.UserAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeatherRepository extends JpaRepository<UserAction, Long> {
    List<UserAction> findByUserId(Long userId);
}
