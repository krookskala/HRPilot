package com.hrpilot.backend.leave;

import com.hrpilot.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "holiday_calendar_entries",
    uniqueConstraints = @UniqueConstraint(columnNames = {"holiday_date", "country_code", "state_code"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayCalendarEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "holiday_year", nullable = false)
    private int holidayYear;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(name = "state_code")
    private String stateCode;

    @Column(nullable = false)
    private String name;
}
