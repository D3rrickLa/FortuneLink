package com.laderrco.fortunelink;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:3000") 
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionRepository transactionRepository;

    @PostMapping
    public Transaction create(@RequestBody Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    @GetMapping
    public List<Transaction> getALl(@RequestParam UUID userId) {
       return transactionRepository.findByUserIdOrderByCreationDateTimeDesc(userId); 
    }

    @GetMapping("/heartbeat")
    public String heartBeat() {
        return "hello";
    }
}
