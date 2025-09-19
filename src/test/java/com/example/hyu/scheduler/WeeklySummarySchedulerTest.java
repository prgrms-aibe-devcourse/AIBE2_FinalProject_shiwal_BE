package com.example.hyu.scheduler;

import com.example.hyu.entity.Profile;
import com.example.hyu.entity.WeeklySummary;
import com.example.hyu.notification.NotificationSender;
import com.example.hyu.repository.CheckinRepository;
import com.example.hyu.repository.ProfileRepository;
import com.example.hyu.repository.WeeklySummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Note: Tests use JUnit 5 (Jupiter) and Mockito, consistent with common Spring Boot testing setups.
 * Plain unit tests (no Spring context). External dependencies are mocked.
 */
@ExtendWith(MockitoExtension.class)
class WeeklySummarySchedulerTest {

    private ProfileRepository profileRepo;
    private CheckinRepository checkinRepo;
    private WeeklySummaryRepository weeklyRepo;
    private NotificationSender notifier;

    private WeeklySummaryScheduler scheduler;

    @BeforeEach
    void setUp() {
        profileRepo = mock(ProfileRepository.class);
        checkinRepo = mock(CheckinRepository.class);
        weeklyRepo = mock(WeeklySummaryRepository.class);
        notifier = mock(NotificationSender.class);

        scheduler = new WeeklySummaryScheduler(profileRepo, checkinRepo, weeklyRepo, notifier);
    }

    private static LocalDate todayKST() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    private static LocalDate expectedWeekStart(LocalDate today) {
        return today.minusWeeks(1).with(DayOfWeek.MONDAY);
    }

    private static LocalDate expectedWeekEnd(LocalDate weekStart) {
        return weekStart.plusDays(6);
    }

    @Test
    @DisplayName("run: sends weekly summary for opted-in profile when not already sent and persists summary")
    void run_sendsSummaryForOptedInProfile_whenNotAlreadySent() {
        LocalDate today = todayKST();
        LocalDate weekStart = expectedWeekStart(today);
        LocalDate weekEnd = expectedWeekEnd(weekStart);

        Profile p1 = mock(Profile.class);
        when(p1.isWeeklySummary()).thenReturn(true);
        when(p1.getUserId()).thenReturn(1L);

        when(profileRepo.findAll()).thenReturn(List.of(p1));
        when(weeklyRepo.existsByUserIdAndWeekStart(1L, weekStart)).thenReturn(false);

        @SuppressWarnings({"rawtypes","unchecked"})
        List fiveCheckins = Arrays.asList(new Object[5]); // size = 5 is all we need
        when(checkinRepo.findAllByUserIdAndDateBetweenOrderByDateAsc(1L, weekStart, weekEnd))
                .thenReturn((List) fiveCheckins);

        // Allow save to succeed
        when(weeklyRepo.save(any(WeeklySummary.class))).thenAnswer(inv -> inv.getArgument(0));

        String expectedContent = String.format("지난주 출석: %d/7일. 계속 화이팅이에요\! (%s ~ %s)", 5, weekStart, weekEnd);

        scheduler.run();

        verify(weeklyRepo).existsByUserIdAndWeekStart(1L, weekStart);
        verify(checkinRepo).findAllByUserIdAndDateBetweenOrderByDateAsc(1L, weekStart, weekEnd);
        verify(weeklyRepo).save(any(WeeklySummary.class));
        verify(notifier).sendInApp(1L, "주간 요약", expectedContent);

        verifyNoMoreInteractions(notifier);
    }

    @Test
    @DisplayName("run: skips processing when a weekly summary already exists for the user")
    void run_skipsWhenAlreadySent() {
        LocalDate today = todayKST();
        LocalDate weekStart = expectedWeekStart(today);

        Profile p1 = mock(Profile.class);
        when(p1.isWeeklySummary()).thenReturn(true);
        when(p1.getUserId()).thenReturn(1L);

        when(profileRepo.findAll()).thenReturn(List.of(p1));
        when(weeklyRepo.existsByUserIdAndWeekStart(1L, weekStart)).thenReturn(true);

        scheduler.run();

        verify(weeklyRepo).existsByUserIdAndWeekStart(1L, weekStart);
        verify(checkinRepo, never()).findAllByUserIdAndDateBetweenOrderByDateAsc(anyLong(), any(), any());
        verify(weeklyRepo, never()).save(any());
        verify(notifier, never()).sendInApp(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("run: continues processing other users when one user's processing throws an exception")
    void run_continuesOnExceptionAndProcessesNextUser() {
        LocalDate today = todayKST();
        LocalDate weekStart = expectedWeekStart(today);
        LocalDate weekEnd = expectedWeekEnd(weekStart);

        Profile p1 = mock(Profile.class);
        when(p1.isWeeklySummary()).thenReturn(true);
        when(p1.getUserId()).thenReturn(1L);

        Profile p2 = mock(Profile.class);
        when(p2.isWeeklySummary()).thenReturn(true);
        when(p2.getUserId()).thenReturn(2L);

        when(profileRepo.findAll()).thenReturn(List.of(p1, p2));

        when(weeklyRepo.existsByUserIdAndWeekStart(1L, weekStart)).thenReturn(false);
        when(weeklyRepo.existsByUserIdAndWeekStart(2L, weekStart)).thenReturn(false);

        @SuppressWarnings({"rawtypes","unchecked"})
        List threeCheckins = Arrays.asList(new Object[3]);
        @SuppressWarnings({"rawtypes","unchecked"})
        List sevenCheckins = Arrays.asList(new Object[7]);

        when(checkinRepo.findAllByUserIdAndDateBetweenOrderByDateAsc(1L, weekStart, weekEnd))
                .thenReturn((List) threeCheckins);
        when(checkinRepo.findAllByUserIdAndDateBetweenOrderByDateAsc(2L, weekStart, weekEnd))
                .thenReturn((List) sevenCheckins);

        // First save throws, second save succeeds
        when(weeklyRepo.save(any(WeeklySummary.class))).thenAnswer(new Answer<WeeklySummary>() {
            int count = 0;
            @Override public WeeklySummary answer(InvocationOnMock invocation) {
                count++;
                if (count == 1) {
                    throw new RuntimeException("fail first user");
                }
                return invocation.getArgument(0);
            }
        });

        String expectedContentP2 = String.format("지난주 출석: %d/7일. 계속 화이팅이에요\! (%s ~ %s)", 7, weekStart, weekEnd);

        scheduler.run();

        // First user failed before notification
        verify(notifier, never()).sendInApp(eq(1L), anyString(), anyString());
        // Second user processed successfully
        verify(notifier).sendInApp(2L, "주간 요약", expectedContentP2);
    }

    @Test
    @DisplayName("run: filters out profiles with weekly summary disabled")
    void run_filtersOutProfilesWithWeeklySummaryDisabled() {
        LocalDate today = todayKST();
        LocalDate weekStart = expectedWeekStart(today);
        LocalDate weekEnd = expectedWeekEnd(weekStart);

        Profile p1 = mock(Profile.class);
        when(p1.isWeeklySummary()).thenReturn(true);
        when(p1.getUserId()).thenReturn(1L);

        Profile p2 = mock(Profile.class);
        when(p2.isWeeklySummary()).thenReturn(false);
        when(p2.getUserId()).thenReturn(2L);

        when(profileRepo.findAll()).thenReturn(List.of(p1, p2));
        when(weeklyRepo.existsByUserIdAndWeekStart(1L, weekStart)).thenReturn(false);

        @SuppressWarnings({"rawtypes","unchecked"})
        List fiveCheckins = Arrays.asList(new Object[5]);
        when(checkinRepo.findAllByUserIdAndDateBetweenOrderByDateAsc(1L, weekStart, weekEnd))
                .thenReturn((List) fiveCheckins);

        when(weeklyRepo.save(any(WeeklySummary.class))).thenAnswer(inv -> inv.getArgument(0));

        scheduler.run();

        verify(weeklyRepo).existsByUserIdAndWeekStart(1L, weekStart);
        verify(weeklyRepo, never()).existsByUserIdAndWeekStart(eq(2L), any());
        verify(notifier).sendInApp(eq(1L), eq("주간 요약"), anyString());
        verify(notifier, never()).sendInApp(eq(2L), anyString(), anyString());
    }

    @Test
    @DisplayName("run: builds content correctly when there are zero check-ins")
    void run_buildsContentWithZeroCheckedDays() {
        LocalDate today = todayKST();
        LocalDate weekStart = expectedWeekStart(today);
        LocalDate weekEnd = expectedWeekEnd(weekStart);

        Profile p1 = mock(Profile.class);
        when(p1.isWeeklySummary()).thenReturn(true);
        when(p1.getUserId()).thenReturn(1L);

        when(profileRepo.findAll()).thenReturn(List.of(p1));
        when(weeklyRepo.existsByUserIdAndWeekStart(1L, weekStart)).thenReturn(false);

        @SuppressWarnings({"rawtypes","unchecked"})
        List empty = Collections.emptyList();
        when(checkinRepo.findAllByUserIdAndDateBetweenOrderByDateAsc(1L, weekStart, weekEnd))
                .thenReturn((List) empty);

        when(weeklyRepo.save(any(WeeklySummary.class))).thenAnswer(inv -> inv.getArgument(0));

        scheduler.run();

        String expectedContent = String.format("지난주 출석: %d/7일. 계속 화이팅이에요\! (%s ~ %s)", 0, weekStart, weekEnd);
        verify(notifier).sendInApp(1L, "주간 요약", expectedContent);
    }
}