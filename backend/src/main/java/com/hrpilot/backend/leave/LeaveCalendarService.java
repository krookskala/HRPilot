package com.hrpilot.backend.leave;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LeaveCalendarService {

    private static final String DEFAULT_COUNTRY_CODE = "DE";
    private static final String DEFAULT_STATE_CODE = "BY";

    private final CompanySettingsRepository companySettingsRepository;
    private final HolidayCalendarEntryRepository holidayCalendarEntryRepository;

    public int calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        Set<LocalDate> holidays = getHolidayDates(startDate, endDate);
        int workingDays = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (isWorkingDay(date, holidays)) {
                workingDays++;
            }
        }

        return workingDays;
    }

    public Set<LocalDate> getHolidayDates(LocalDate startDate, LocalDate endDate) {
        CompanySettings settings = getCompanySettings();
        ensureCalendarExists(startDate.getYear(), endDate.getYear(), settings);
        return holidayCalendarEntryRepository.findApplicableHolidays(
                startDate,
                endDate,
                settings.getCountryCode(),
                settings.getStateCode()
            ).stream()
            .map(HolidayCalendarEntry::getHolidayDate)
            .collect(java.util.stream.Collectors.toSet());
    }

    private boolean isWorkingDay(LocalDate date, Set<LocalDate> holidays) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
            && date.getDayOfWeek() != DayOfWeek.SUNDAY
            && !holidays.contains(date);
    }

    private CompanySettings getCompanySettings() {
        return companySettingsRepository.findAll().stream()
            .findFirst()
            .orElseGet(() -> companySettingsRepository.save(CompanySettings.builder()
                .companyName("HRPilot Demo Company")
                .countryCode(DEFAULT_COUNTRY_CODE)
                .stateCode(DEFAULT_STATE_CODE)
                .build()));
    }

    @Transactional
    protected void ensureCalendarExists(int startYear, int endYear, CompanySettings settings) {
        for (int year = startYear; year <= endYear; year++) {
            seedFederalHolidaysIfMissing(year, settings.getCountryCode());
            if (settings.getStateCode() != null && !settings.getStateCode().isBlank()) {
                seedStateHolidaysIfMissing(year, settings.getCountryCode(), settings.getStateCode());
            }
        }
    }

    private void seedFederalHolidaysIfMissing(int year, String countryCode) {
        if (holidayCalendarEntryRepository.countByHolidayYearAndCountryCodeAndStateCodeIsNull(year, countryCode) > 0) {
            return;
        }
        holidayCalendarEntryRepository.saveAll(buildFederalHolidays(year, countryCode));
    }

    private void seedStateHolidaysIfMissing(int year, String countryCode, String stateCode) {
        if (holidayCalendarEntryRepository.countByHolidayYearAndCountryCodeAndStateCode(year, countryCode, stateCode) > 0) {
            return;
        }
        holidayCalendarEntryRepository.saveAll(buildStateHolidays(year, countryCode, stateCode));
    }

    private List<HolidayCalendarEntry> buildFederalHolidays(int year, String countryCode) {
        LocalDate easterSunday = easterSunday(year);
        return List.of(
            fixedHoliday(year, countryCode, null, Month.JANUARY, 1, "New Year's Day"),
            datedHoliday(easterSunday.minusDays(2), countryCode, null, "Good Friday"),
            datedHoliday(easterSunday.plusDays(1), countryCode, null, "Easter Monday"),
            fixedHoliday(year, countryCode, null, Month.MAY, 1, "Labour Day"),
            datedHoliday(easterSunday.plusDays(39), countryCode, null, "Ascension Day"),
            datedHoliday(easterSunday.plusDays(50), countryCode, null, "Whit Monday"),
            fixedHoliday(year, countryCode, null, Month.OCTOBER, 3, "German Unity Day"),
            fixedHoliday(year, countryCode, null, Month.DECEMBER, 25, "Christmas Day"),
            fixedHoliday(year, countryCode, null, Month.DECEMBER, 26, "Second Christmas Day")
        );
    }

    private List<HolidayCalendarEntry> buildStateHolidays(int year, String countryCode, String stateCode) {
        LocalDate easterSunday = easterSunday(year);
        List<HolidayCalendarEntry> holidays = new ArrayList<>();

        switch (stateCode.toUpperCase(Locale.ROOT)) {
            case "BW", "BY", "ST" -> holidays.add(fixedHoliday(year, countryCode, stateCode, Month.JANUARY, 6, "Epiphany"));
            default -> {
            }
        }

        switch (stateCode.toUpperCase(Locale.ROOT)) {
            case "BB" -> holidays.add(fixedHoliday(year, countryCode, stateCode, Month.MARCH, 8, "International Women's Day"));
            case "BE" -> holidays.add(fixedHoliday(year, countryCode, stateCode, Month.MARCH, 8, "International Women's Day"));
            default -> {
            }
        }

        switch (stateCode.toUpperCase(Locale.ROOT)) {
            case "BW", "BY", "HE", "NW", "RP", "SL" -> holidays.add(datedHoliday(easterSunday.plusDays(60), countryCode, stateCode, "Corpus Christi"));
            default -> {
            }
        }

        switch (stateCode.toUpperCase(Locale.ROOT)) {
            case "SL" -> holidays.add(fixedHoliday(year, countryCode, stateCode, Month.AUGUST, 15, "Assumption Day"));
            default -> {
            }
        }

        switch (stateCode.toUpperCase(Locale.ROOT)) {
            case "BB", "MV", "SN", "ST", "TH" -> holidays.add(fixedHoliday(year, countryCode, stateCode, Month.OCTOBER, 31, "Reformation Day"));
            default -> {
            }
        }

        switch (stateCode.toUpperCase(Locale.ROOT)) {
            case "BW", "BY", "NW", "RP", "SL" -> holidays.add(fixedHoliday(year, countryCode, stateCode, Month.NOVEMBER, 1, "All Saints' Day"));
            default -> {
            }
        }

        switch (stateCode.toUpperCase(Locale.ROOT)) {
            case "SN" -> holidays.add(fixedHoliday(year, countryCode, stateCode, Month.NOVEMBER, 19, "Day of Prayer and Repentance"));
            default -> {
            }
        }

        return holidays;
    }

    private HolidayCalendarEntry fixedHoliday(int year, String countryCode, String stateCode, Month month, int dayOfMonth, String name) {
        return datedHoliday(LocalDate.of(year, month, dayOfMonth), countryCode, stateCode, name);
    }

    private HolidayCalendarEntry datedHoliday(LocalDate holidayDate, String countryCode, String stateCode, String name) {
        return HolidayCalendarEntry.builder()
            .holidayDate(holidayDate)
            .holidayYear(holidayDate.getYear())
            .countryCode(countryCode)
            .stateCode(stateCode)
            .name(name)
            .build();
    }

    private LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
