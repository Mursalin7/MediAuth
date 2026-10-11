package com.mediauth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    // Username for auth — same as email
    @Column(nullable = false)
    private String password;  // stored hashed

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
}
