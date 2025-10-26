package com.yomirein.sochatclient.repo;


import com.yomirein.sochatclient.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepo extends JpaRepository<Chat, Long> { }