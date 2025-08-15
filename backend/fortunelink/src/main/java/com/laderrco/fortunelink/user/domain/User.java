package com.laderrco.fortunelink.user.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class User {
    private UUID id; 
    private String email;
    private String userName;
    private String firstName;
    private String lastName;   
    private LocalDateTime createdAt;
    
    
    
    public UUID getId() {
        return id;
    }
    public String getEmail() {
        return email;
    }
    public String getUserName() {
        return userName;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    } 

    

}
