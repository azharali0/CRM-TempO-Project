package com.crm.repository.specification;

import com.crm.model.entity.Customer;
import com.crm.model.enums.CustomerStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class CustomerSpecification {

    private CustomerSpecification() {
    }

    public static Specification<Customer> hasName(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + escapeLike(name.toLowerCase()) + "%");
    }

    public static Specification<Customer> hasCompany(String company) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("company")), "%" + escapeLike(company.toLowerCase()) + "%");
    }

    public static Specification<Customer> hasStatus(CustomerStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    public static Specification<Customer> hasCity(String city) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("city")), "%" + escapeLike(city.toLowerCase()) + "%");
    }

    public static Specification<Customer> assignedTo(UUID userId) {
        return (root, query, cb) -> {
            var join = root.join("assignedTo", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.equal(join.get("id"), userId);
        };
    }

    public static Specification<Customer> assignedToIn(List<UUID> userIds) {
        return (root, query, cb) -> {
            var join = root.join("assignedTo", jakarta.persistence.criteria.JoinType.LEFT);
            return join.get("id").in(userIds);
        };
    }

    public static Specification<Customer> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) ->
                cb.between(root.get("createdAt"), from, to);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
    }
}
