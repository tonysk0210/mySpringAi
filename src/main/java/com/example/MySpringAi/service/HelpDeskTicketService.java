package com.example.mySpringAi.service;

import com.example.mySpringAi.entity.HelpDeskTicketEntity;
import com.example.mySpringAi.payload.HelpDeskTicketPayload;
import com.example.mySpringAi.repo.HelpDeskTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HelpDeskTicketService {

    private final HelpDeskTicketRepository helpDeskTicketRepository;

    public HelpDeskTicketEntity createHelpDeskTicket(HelpDeskTicketPayload payload, String username) {
        HelpDeskTicketEntity ticket = HelpDeskTicketEntity.builder()
                .username(username)
                .issue(payload.issue())
                .status("OPEN")
                .createdAt(LocalDateTime.now())
                .eta(LocalDateTime.now().plusDays(7))
                .build();
        return helpDeskTicketRepository.save(ticket);
    }

    public List<HelpDeskTicketEntity> getHelpDeskTicketsByUser(String username) {
        return helpDeskTicketRepository.findByUsername(username);
    }
}
