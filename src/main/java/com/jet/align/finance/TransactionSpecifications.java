package com.jet.align.finance;

import com.jet.align.finance.dto.TransactionFilter;
import com.jet.align.user.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TransactionSpecifications {
    public static Specification<Transaction> withFilter(User user, TransactionFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user"), user));
            if (filter.type() != null) predicates.add(cb.equal(root.get("type"), filter.type()));
            if (filter.category() != null) predicates.add(cb.equal(root.get("category"), filter.category()));
            if (filter.from() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), filter.from()));
            if (filter.to() != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), filter.to()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TransactionSpecifications() {}

}
