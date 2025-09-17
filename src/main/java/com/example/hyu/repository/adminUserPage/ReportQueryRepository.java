package com.example.hyu.repository.adminUserPage;

import com.example.hyu.dto.adminUserPage.ReportSearchCond;
import com.example.hyu.entity.Report;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReportQueryRepository {
    private final EntityManager em;

    public Page<Report> search(ReportSearchCond cond, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // data
        CriteriaQuery<Report> cq = cb.createQuery(Report.class);
        Root<Report> r = cq.from(Report.class);
        List<Predicate> ps = buildPredicates(cb, r, cond);
        cq.select(r).where(ps.toArray(Predicate[]::new));
        // 기본 정렬: 신고일 최신
        cq.orderBy(cb.desc(r.get("reportedAt")));

        var q = em.createQuery(cq);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        List<Report> content = q.getResultList();

        // count
        CriteriaQuery<Long> cc = cb.createQuery(Long.class);
        Root<Report> r2 = cc.from(Report.class);
        cc.select(cb.count(r2)).where(buildPredicates(cb, r2, cond).toArray(Predicate[]::new));
        Long total = em.createQuery(cc).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Report> r, ReportSearchCond cond) {
        List<Predicate> ps = new ArrayList<>();
        if (cond == null) return ps;

        if (cond.q() != null && !cond.q().isBlank()) {
            ps.add(cb.like(cb.lower(r.get("description")), "%" + cond.q().toLowerCase() + "%"));
        }
        if (cond.status() != null && !cond.status().isBlank()) {
            ps.add(cb.equal(r.get("status"), Enum.valueOf(Report.Status.class, cond.status())));
        }
        if (cond.reason() != null && !cond.reason().isBlank()) {
            ps.add(cb.equal(r.get("reason"), Enum.valueOf(Report.Reason.class, cond.reason())));
        }
        if (cond.targetType() != null && !cond.targetType().isBlank()) {
            ps.add(cb.equal(r.get("targetType"), Enum.valueOf(Report.TargetType.class, cond.targetType())));
        }
        // 날짜 범위
        if (cond.from() != null) {
            Instant from = cond.from().atStartOfDay().toInstant(ZoneOffset.UTC);
            ps.add(cb.greaterThanOrEqualTo(r.get("reportedAt"), from));
        }
        if (cond.to() != null) {
            LocalDate toDate = cond.to().plusDays(1); // inclusive 처리
            Instant to = toDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            ps.add(cb.lessThan(r.get("reportedAt"), to));
        }
        return ps;
    }
}
