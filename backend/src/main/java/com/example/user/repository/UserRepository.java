package com.example.user.repository;

import com.example.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    Page<UserEntity> findByActiveTrue(Pageable pageable);
    List<UserEntity> findByActiveTrue();

    @Query("""
      select u from UserEntity u
      where (:q is null or :q = '' or
             lower(u.firstName) like lower(concat('%', :q, '%')) or
             lower(u.lastName)  like lower(concat('%', :q, '%')) or
             lower(u.email)     like lower(concat('%', :q, '%')))
        and (:activeOnly = false or u.active = true)
    """)
    Page<UserEntity> search(@Param("q") String q,
                            @Param("activeOnly") boolean activeOnly,
                            Pageable pageable);

    @Query("select count(u) from UserEntity u where u.createdAt >= :since")
    long countUsersCreatedSince(@Param("since") Instant since);
}
