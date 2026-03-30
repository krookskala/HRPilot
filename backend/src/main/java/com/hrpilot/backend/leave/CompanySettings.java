package com.hrpilot.backend.leave;

import com.hrpilot.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanySettings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(name = "state_code")
    private String stateCode;
}
