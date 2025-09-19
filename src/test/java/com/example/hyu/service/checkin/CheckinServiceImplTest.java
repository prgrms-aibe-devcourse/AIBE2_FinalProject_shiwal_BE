/*
  Testing library/framework note:
  - This test class is written for JUnit 5 (Jupiter) with Mockito (MockitoExtension).
  - It uses AssertJ assertions if available; otherwise falls back to JUnit assertions.
*/
package com.example.hyu.service.checkin;

import com.example.hyu.dto.checkin.CheckinCreateResponse;
import com.example.hyu.dto.checkin.CheckinStatsResponse;
import com.example.hyu.dto.checkin.CheckinTodayResponse;
import com.example.hyu.entity.Checkin;
import com.example.hyu.repository.CheckinRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CheckinServiceImpl focusing on the diffed logic.
 * We mock CheckinRepository and control date-dependent behavior by computing today's date
 * according to the implementation's ZoneId (Asia/Seoul).
 */
@ExtendWith(MockitoExtension.class)
class CheckinServiceImplTest {

    @Mock
    private CheckinRepository checkinRepository;

    private CheckinServiceImpl service;

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private long userId;

    @BeforeEach
    void setUp() {
        service = new CheckinServiceImpl(checkinRepository);
        userId = 42L;
    }

    private LocalDate today() {
        return LocalDate.now(ZONE);
    }

    @Nested
    @DisplayName("getToday")
    class GetToday {

        @Test
        @DisplayName("returns checked=false, shouldPrompt=true, streak=0 when user has not checked in today and no prior streak")
        void notCheckedToday_noStreak() {
            LocalDate t = today();
            when(checkinRepository.existsByUserIdAndDate(userId, t)).thenReturn(false);
            // computeStreak will query today first => false -> streak=0
            CheckinTodayResponse res = service.getToday(userId);

            // Prefer AssertJ if present; also include JUnit assertions for compatibility
            assertNotNull(res);
            assertThat(res.date()).isEqualTo(t);
            assertThat(res.checked()).isFalse();
            assertThat(res.shouldPrompt()).isTrue();
            assertThat(res.streak()).isZero();

            verify(checkinRepository, times(1)).existsByUserIdAndDate(userId, t);
        }

        @Test
        @DisplayName("returns checked=true, shouldPrompt=false, streak counts consecutive past days including today")
        void checkedToday_withStreak() {
            LocalDate t = today();
            // Streak of 3 days: today, t-1, t-2 true; then t-3 false
            when(checkinRepository.existsByUserIdAndDate(userId, t)).thenReturn(true);
            when(checkinRepository.existsByUserIdAndDate(userId, t.minusDays(1))).thenReturn(true);
            when(checkinRepository.existsByUserIdAndDate(userId, t.minusDays(2))).thenReturn(true);
            when(checkinRepository.existsByUserIdAndDate(userId, t.minusDays(3))).thenReturn(false);

            CheckinTodayResponse res = service.getToday(userId);

            assertThat(res.checked()).isTrue();
            assertThat(res.shouldPrompt()).isFalse();
            assertThat(res.date()).isEqualTo(t);
            assertThat(res.streak()).isEqualTo(3);

            // Verify minimal essential interactions
            verify(checkinRepository, times(1)).existsByUserIdAndDate(userId, t);
            verify(checkinRepository, times(1)).existsByUserIdAndDate(userId, t.minusDays(1));
            verify(checkinRepository, times(1)).existsByUserIdAndDate(userId, t.minusDays(2));
            verify(checkinRepository, times(1)).existsByUserIdAndDate(userId, t.minusDays(3));
        }
    }

    @Nested
    @DisplayName("checkinToday")
    class CheckinToday {

        @Test
        @DisplayName("creates a checkin when none exists for today; returns created=true and updated streak")
        void createsWhenMissing() {
            LocalDate t = today();

            when(checkinRepository.existsByUserIdAndDate(userId, t)).thenReturn(false);
            // Streak after saving should be computed as 1 (today only), so stub exists for today as true
            when(checkinRepository.existsByUserIdAndDate(userId, t)).thenReturn(true);
            when(checkinRepository.existsByUserIdAndDate(userId, t.minusDays(1))).thenReturn(false);

            ArgumentCaptor<Checkin> captor = ArgumentCaptor.forClass(Checkin.class);
            // save returns the same entity (common Mockito default), no need to stub return explicitly

            CheckinCreateResponse res = service.checkinToday(userId);

            assertThat(res).isNotNull();
            assertThat(res.created()).isTrue();
            assertThat(res.date()).isEqualTo(t);
            assertThat(res.streak()).isEqualTo(1);

            verify(checkinRepository).save(captor.capture());
            Checkin saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getDate()).isEqualTo(t);
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("does not create duplicate when already checked in; returns created=false and current streak")
        void noDuplicateWhenExists() {
            LocalDate t = today();

            when(checkinRepository.existsByUserIdAndDate(userId, t)).thenReturn(true);
            when(checkinRepository.existsByUserIdAndDate(userId, t.minusDays(1))).thenReturn(true);
            when(checkinRepository.existsByUserIdAndDate(userId, t.minusDays(2))).thenReturn(false);

            CheckinCreateResponse res = service.checkinToday(userId);

            assertThat(res.created()).isFalse();
            assertThat(res.date()).isEqualTo(t);
            assertThat(res.streak()).isEqualTo(2);

            verify(checkinRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getStats")
    class GetStats {

        @Test
        @DisplayName("returns map covering inclusive range with checked days marked true and correct counts")
        void statsHappyPath() {
            LocalDate t = today();
            LocalDate from = t.minusDays(6);
            LocalDate to = t;

            // Mark checkins on: t-6, t-4, t-1, t
            List<Checkin> checkins = Arrays.asList(
                    Checkin.builder().userId(userId).date(from).createdAt(Instant.now()).build(),
                    Checkin.builder().userId(userId).date(from.plusDays(2)).createdAt(Instant.now()).build(),
                    Checkin.builder().userId(userId).date(to.minusDays(1)).createdAt(Instant.now()).build(),
                    Checkin.builder().userId(userId).date(to).createdAt(Instant.now()).build()
            );
            when(checkinRepository.findAllByUserIdAndDateBetweenOrderByDateAsc(userId, from, to)).thenReturn(checkins);

            // For streak: today true, yesterday true, day before false => streak=2
            when(checkinRepository.existsByUserIdAndDate(userId, to)).thenReturn(true);
            when(checkinRepository.existsByUserIdAndDate(userId, to.minusDays(1))).thenReturn(true);
            when(checkinRepository.existsByUserIdAndDate(userId, to.minusDays(2))).thenReturn(false);

            CheckinStatsResponse res = service.getStats(userId, from, to);

            assertThat(res).isNotNull();
            assertThat(res.totalDays()).isEqualTo(7);
            assertThat(res.checkedDays()).isEqualTo(4);
            assertThat(res.streak()).isEqualTo(2);
            Map<LocalDate, Boolean> map = res.calendar();
            assertThat(map).hasSize(7);
            assertThat(map.get(from)).isTrue();
            assertThat(map.get(from.plusDays(1))).isFalse();
            assertThat(map.get(from.plusDays(2))).isTrue();
            assertThat(map.get(to.minusDays(1))).isTrue();
            assertThat(map.get(to)).isTrue();
        }

        @Test
        @DisplayName("handles empty range (from > to) gracefully with zero counts and empty map")
        void emptyRange() {
            LocalDate t = today();
            LocalDate from = t.plusDays(1);
            LocalDate to = t.minusDays(1);

            when(checkinRepository.findAllByUserIdAndDateBetweenOrderByDateAsc(userId, from, to))
                    .thenReturn(Collections.emptyList());
            // Streak still computed based on today (assume no checkin today)
            when(checkinRepository.existsByUserIdAndDate(userId, t)).thenReturn(false);

            CheckinStatsResponse res = service.getStats(userId, from, to);

            assertThat(res.totalDays()).isEqualTo(0);
            assertThat(res.checkedDays()).isEqualTo(0);
            assertThat(res.calendar()).isEmpty();
            assertThat(res.streak()).isEqualTo(0);
        }
    }
}