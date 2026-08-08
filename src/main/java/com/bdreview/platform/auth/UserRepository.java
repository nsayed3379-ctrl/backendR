package com.bdreview.platform.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Enforces "one phone number, one role": look up before allowing signup under the other role. */
    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    // -----------------------------------------------------------------
    // Admin panel (com.bdreview.platform.admin) — read-side search/listing
    // only; account mutation still goes through UserRepository#save as
    // everywhere else in the codebase.
    // -----------------------------------------------------------------
    Page<User> findByRole(UserRole role, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE (:query IS NULL OR :query = ''
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR u.phoneNumber LIKE CONCAT('%', :query, '%'))
              AND (:role IS NULL OR u.role = :role)
            """)
    Page<User> search(@Param("query") String query, @Param("role") UserRole role, Pageable pageable);
}
