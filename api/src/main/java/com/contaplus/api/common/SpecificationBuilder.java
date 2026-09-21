package com.contaplus.api.common;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpecificationBuilder<T> {

    private final List<Specification<T>> specs = new ArrayList<>();

    public SpecificationBuilder<T> equal(String field, Object value) {
        if (value != null) {
            specs.add((root, query, cb) -> cb.equal(root.get(field), value));
        }
        return this;
    }

    public SpecificationBuilder<T> equalNested(String field, String nestedField, Object value) {
        if (value != null) {
            specs.add((root, query, cb) -> cb.equal(root.get(field).get(nestedField), value));
        }
        return this;
    }

    public SpecificationBuilder<T> like(String field, String value) {
        if (value != null && !value.isBlank()) {
            specs.add((root, query, cb) ->
                cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
        }
        return this;
    }

    public SpecificationBuilder<T> greaterThanOrEqual(String field, Comparable<?> value) {
        if (value != null) {
            specs.add((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get(field), (Comparable) value));
        }
        return this;
    }

    public SpecificationBuilder<T> lessThanOrEqual(String field, Comparable<?> value) {
        if (value != null) {
            specs.add((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get(field), (Comparable) value));
        }
        return this;
    }

    public SpecificationBuilder<T> between(String field, OffsetDateTime start, OffsetDateTime end) {
        if (start != null && end != null) {
            specs.add((root, query, cb) -> cb.between(root.get(field), start, end));
        } else if (start != null) {
            greaterThanOrEqual(field, start);
        } else if (end != null) {
            lessThanOrEqual(field, end);
        }
        return this;
    }

    public SpecificationBuilder<T> betweenInt(String field, Integer min, Integer max) {
        if (min != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(field), min));
        }
        if (max != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get(field), max));
        }
        return this;
    }

    public SpecificationBuilder<T> isNull(String field, Boolean shouldBeNull) {
        if (shouldBeNull != null && shouldBeNull) {
            specs.add((root, query, cb) -> cb.isNull(root.get(field)));
        }
        return this;
    }

    public SpecificationBuilder<T> isNotNull(String field, Boolean shouldNotBeNull) {
        if (shouldNotBeNull != null && shouldNotBeNull) {
            specs.add((root, query, cb) -> cb.isNotNull(root.get(field)));
        }
        return this;
    }

    public Specification<T> build() {
        if (specs.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Specification<T> spec : specs) {
                predicates.add(spec.toPredicate(root, query, cb));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
