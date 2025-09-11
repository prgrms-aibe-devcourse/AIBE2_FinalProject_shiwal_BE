package com.example.hyu.repository;

import com.example.hyu.dto.admin.user.UserSearchCond;
import com.example.hyu.entity.Users;
import jakarta.persistence.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.time.*;
import java.util.List;

@Repository
public class AdminUserQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * 관리자용 사용자 목록 검색
     * 지원 필터:
     *  - q       : 이메일/닉네임/이름 부분검색
     *  - role    : "ADMIN" | "USER"
     *  - state   : "ACTIVE" | "SUSPENDED" | "WITHDRAWN"
     *  - joinedFrom ~ joinedTo : 가입일 범위 (BaseTimeEntity.createdAt 기준)
     */
    public Page<Users> search(UserSearchCond cond, Pageable pageable) {
        StringBuilder jpql = new StringBuilder("select u from Users u where 1=1");

        if (hasText(cond.q()))       jpql.append(" and (lower(u.email) like :kw or lower(u.nickname) like :kw or lower(u.name) like :kw)");
        if (hasText(cond.role()))    jpql.append(" and u.role = :role");         // role은 String 필드(기존 설계)
        if (hasText(cond.state()))   jpql.append(" and u.state = :state");       // state 필드가 있으면 사용
        if (cond.joinedFrom() != null) jpql.append(" and u.createdAt >= :fromTs");
        if (cond.joinedTo()   != null) jpql.append(" and u.createdAt < :toTs");

        // 정렬 (기본 createdAt desc 권장)
        if (pageable.getSort().isSorted()) {
            jpql.append(" order by ");
            jpql.append(pageable.getSort().stream()
                    .map(o -> "u." + o.getProperty() + (o.isAscending() ? " asc" : " desc"))
                    .reduce((a,b)->a+", "+b).orElse("u.createdAt desc"));
        } else {
            jpql.append(" order by u.createdAt desc");
        }

        TypedQuery<Users> query = em.createQuery(jpql.toString(), Users.class);
        bindParams(query, cond);

        // 페이징
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<Users> content = query.getResultList();

        // 카운트 (order by 제거)
        String countJpql = jpql.toString()
                .replaceFirst("select u from Users u", "select count(u) from Users u")
                .replaceFirst(" order by .*", "");
        TypedQuery<Long> countQuery = em.createQuery(countJpql, Long.class);
        bindParams(countQuery, cond);
        long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    /* ================= 내부 유틸 ================= */

    private void bindParams(TypedQuery<?> q, UserSearchCond cond) {
        if (hasText(cond.q())) {
            q.setParameter("kw", "%" + cond.q().trim().toLowerCase() + "%");
        }
        if (hasText(cond.role())) {
            q.setParameter("role", cond.role().trim()); // Users.role은 String
        }
        if (hasText(cond.state())) {
            // Users.state 가 Enum이면 Enum으로, String이면 문자열로 바인딩
            Object stateParam = cond.state().trim();
            try {
                // Users.UserState 가 존재하고 Enum이라면 이 라인이 성공함
                stateParam = Enum.valueOf((Class<Enum>) Class.forName("com.example.hyu.entity.Users$UserState"), cond.state().trim());
            } catch (Exception ignore) { /* 필드가 String 이거나 enum 클래스 없으면 문자열 그대로 사용 */ }
            q.setParameter("state", stateParam);
        }
        if (cond.joinedFrom() != null) {
            q.setParameter("fromTs", startOfDay(cond.joinedFrom()));
        }
        if (cond.joinedTo() != null) {
            q.setParameter("toTs", startOfNextDay(cond.joinedTo()));
        }
    }

    private Instant startOfDay(LocalDate d){ return d.atStartOfDay(ZoneId.systemDefault()).toInstant(); }
    private Instant startOfNextDay(LocalDate d){ return d.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant(); }
    private boolean hasText(String s){ return s != null && !s.isBlank(); }
}