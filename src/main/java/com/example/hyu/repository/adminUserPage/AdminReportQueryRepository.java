package com.example.hyu.repository.adminUserPage;

import com.example.hyu.dto.adminUserPage.ReportSearchCond;
import com.example.hyu.entity.Report;
import jakarta.persistence.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AdminReportQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /** 신고 리스트 검색(필터/페이징/정렬) — 엔티티(Report) 필드에 맞춤 */
    public Page<Report> search(ReportSearchCond cond, Pageable pageable) {
        StringBuilder jpql = new StringBuilder("select r from Report r where 1=1");

        if (hasText(cond.status()))     jpql.append(" and r.status = :status");
        if (hasText(cond.reason()))     jpql.append(" and r.reason = :reason");
        if (hasText(cond.targetType())) jpql.append(" and r.targetType = :targetType");
        if (cond.targetId() != null)    jpql.append(" and r.targetId = :targetId");
        if (cond.from() != null)        jpql.append(" and r.reportedAt >= :fromTs");
        if (cond.to() != null)          jpql.append(" and r.reportedAt < :toTs");
        if (hasText(cond.q()))          jpql.append(" and lower(r.description) like :kw");

        // 정렬 화이트리스트
        jpql.append(buildOrderBy(pageable));

        TypedQuery<Report> query = em.createQuery(jpql.toString(), Report.class);
        bindParams(query, cond);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<Report> content = query.getResultList();

        // count 쿼리 (정렬 제거)
        StringBuilder countJpql = new StringBuilder("select count(r) from Report r where 1=1");
        if (hasText(cond.status()))     countJpql.append(" and r.status = :status");
        if (hasText(cond.reason()))     countJpql.append(" and r.reason = :reason");
        if (hasText(cond.targetType())) countJpql.append(" and r.targetType = :targetType");
        if (cond.targetId() != null)    countJpql.append(" and r.targetId = :targetId");
        if (cond.from() != null)        countJpql.append(" and r.reportedAt >= :fromTs");
        if (cond.to() != null)          countJpql.append(" and r.reportedAt < :toTs");
        if (hasText(cond.q()))          countJpql.append(" and lower(r.description) like :kw");

        TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);
        bindParams(countQuery, cond);
        long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private void bindParams(TypedQuery<?> q, ReportSearchCond cond) {
        if (hasText(cond.status())){
            String s = cond.status().trim().toUpperCase();
            if(!List.of("PENDING", "REVIEWED", "ACTION_TOKEN").contains(s)) {
                throw new IllegalArgumentException("INVALID_STATUS");
            }
            q.setParameter("status", Report.Status.valueOf(s));
        }
        // reason 대소문자 정규화 + enum 검증
        if (hasText(cond.reason())) {
            String r = cond.reason().trim().toUpperCase();
            try {
                q.setParameter("reason", Report.Reason.valueOf(r));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("INVALID_REASON");
            }
        }
        // targetType 대소문자 정규화 + enum 검색
        if (hasText(cond.targetType())) {
            String t = cond.targetType().trim().toUpperCase();
            try {
                q.setParameter("targetType", Report.TargetType.valueOf(t));
            } catch (IllegalArgumentException e){
                throw new IllegalArgumentException("INVALID_TARGET_TYPE");
            }
        }
        if (cond.targetId() != null)    q.setParameter("targetId", cond.targetId());
        if (cond.from() != null)        q.setParameter("fromTs", startOfDay(cond.from()));
        if (cond.to() != null)          q.setParameter("toTs", startOfNextDay(cond.to()));
        if (hasText(cond.q()))          q.setParameter("kw", "%"+cond.q().trim().toLowerCase()+"%");
    }

    /** 정렬 허용 컬럼만 매핑 */
    private String buildOrderBy(Pageable pageable) {
        if (pageable == null || !pageable.getSort().isSorted()) {
            return " order by r.reportedAt desc";
        }
        List<String> allow = List.of("reportedAt", "status", "reason", "targetId");
        List<String> parts = new ArrayList<>();
        for (Sort.Order o : pageable.getSort()) {
            String p = o.getProperty();
            if (!allow.contains(p)) continue;
            parts.add("r." + p + (o.isAscending() ? " asc" : " desc"));
        }
        if (parts.isEmpty()) return " order by r.reportedAt desc";
        return " order by " + String.join(", ", parts);
    }

    private Instant startOfDay(LocalDate d){ return d.atStartOfDay(ZoneId.systemDefault()).toInstant(); }
    private Instant startOfNextDay(LocalDate d){ return d.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant(); }
    private boolean hasText(String s){ return s!=null && !s.isBlank(); }
}