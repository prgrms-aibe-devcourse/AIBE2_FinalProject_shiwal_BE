package com.example.hyu.repository.adminUserPage;

import com.example.hyu.dto.adminUserPage.UserSearchCond;
import com.example.hyu.entity.Users;
import jakarta.persistence.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
public class AdminUserQueryRepository {

    @PersistenceContext
    private EntityManager em;

    // 허용된 정렬 필드만 사용
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "createdAt", "email", "name", "nickname", "role", "state", "riskLevel");

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
        if (hasText(cond.role()))    jpql.append(" and u.role = :role");
        if (hasText(cond.state()))   jpql.append(" and u.state = :state");
        if (hasText(cond.riskLevel())) jpql.append(" and u.riskLevel = :riskLevel");
        if (cond.joinedFrom() != null) jpql.append(" and u.createdAt >= :fromTs");
        if (cond.joinedTo()   != null) jpql.append(" and u.createdAt < :toTs");

        // 정렬 (화이트 리스트 적용)
        Sort safeSort = sanitizeSort(pageable.getSort());
        jpql.append(" order by ");
        jpql.append(safeSort.stream()
                .map(o -> "u." + o.getProperty() + (o.isAscending() ? " asc" : " desc"))
                .reduce((a,b) ->  a + ", " + b)
                .orElse("u.createdAt desc"));

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
            Users.UserState state = Users.UserState.valueOf(cond.state().trim().toUpperCase());
            q.setParameter("state", state);
        }
        if (hasText(cond.riskLevel())) {
            Users.RiskLevel level = Users.RiskLevel.valueOf(cond.riskLevel().trim().toUpperCase());
            q.setParameter("riskLevel", level);
        }
        if (cond.joinedFrom() != null) {
            q.setParameter("fromTs", startOfDay(cond.joinedFrom()));
        }
        if (cond.joinedTo() != null) {
            q.setParameter("toTs", startOfNextDay(cond.joinedTo()));
        }
    }

    private Sort sanitizeSort(Sort input){
        List<Sort.Order> allowed = new ArrayList<>();
        for (Sort.Order o : input) {
            String p = o.getProperty();
            if("joindAt".equalsIgnoreCase(p)) {
                allowed.add(new Sort.Order(o.getDirection(), p));
            }
        }
        return allowed.isEmpty()
                ? Sort.by(Sort.Order.desc("createdAt"))
                : Sort.by(allowed);
    }

    private Instant startOfDay(LocalDate d){ return d.atStartOfDay(ZoneId.systemDefault()).toInstant(); }
    private Instant startOfNextDay(LocalDate d){ return d.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant(); }
    private boolean hasText(String s){ return s != null && !s.isBlank(); }
}