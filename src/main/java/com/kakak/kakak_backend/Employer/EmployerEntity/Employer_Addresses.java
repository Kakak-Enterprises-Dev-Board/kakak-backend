package com.kakak.kakak_backend.Employer.EmployerEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
@Entity
@Data
@Table(name = "employer_addresses")
@AllArgsConstructor
@NoArgsConstructor
public class Employer_Addresses {
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @ManyToOne
        @JoinColumn(nullable = false)
        private Employer employer_id;

        @Column(nullable = false)
        private String address_line_1;

        @Column(nullable = false)
        private String address_line_2;

        @Column(nullable = false)
        private String city;

        @Column(nullable = false)
        private String state;

        @Column(nullable = false)
        private String country;

        @Column(nullable = false)
        private String pincode;

        @Column(nullable = false)
        private Double latitude;

        @Column(nullable = false)
        private Double longitude;

}
