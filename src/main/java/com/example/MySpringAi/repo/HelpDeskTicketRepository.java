package com.example.mySpringAi.repo;

import com.example.mySpringAi.entity.HelpDeskTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HelpDeskTicketRepository extends JpaRepository<HelpDeskTicketEntity, Long> {

    //Derived Query
    List<HelpDeskTicketEntity> findByUsername(String username);
}
