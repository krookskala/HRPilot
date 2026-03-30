package com.hrpilot.backend.leave;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HolidayCalendarEntryRepository extends JpaRepository<HolidayCalendarEntry, Long> {

    long countByHolidayYearAndCountryCodeAndStateCodeIsNull(int holidayYear, String countryCode);

    long countByHolidayYearAndCountryCodeAndStateCode(int holidayYear, String countryCode, String stateCode);

    @Query("""
        select h from HolidayCalendarEntry h
        where h.holidayDate between :startDate and :endDate
          and h.countryCode = :countryCode
          and (h.stateCode is null or h.stateCode = :stateCode)
        """)
    List<HolidayCalendarEntry> findApplicableHolidays(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("countryCode") String countryCode,
        @Param("stateCode") String stateCode
    );
}
