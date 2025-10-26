package com.yomirein.sochatclient.repo;


import com.yomirein.sochatclient.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User, Long> { }
