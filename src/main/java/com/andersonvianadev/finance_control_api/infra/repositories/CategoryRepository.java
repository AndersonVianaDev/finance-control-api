package com.andersonvianadev.finance_control_api.infra.repositories;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query("""
            SELECT COUNT(c) > 0 FROM Category c WHERE 1=1
            AND LOWER(c.name) = LOWER(:name) 
            AND (c.owner = :owner OR c.owner IS NULL)
    """)
    boolean existsByNameAndOwnerOrGlobal(@Param("name") String name, @Param("owner") User owner);

    Integer countByOwner(User owner);

    @Query("""
            SELECT c FROM Category c WHERE 1=1
            AND c.id = :id 
            AND (c.owner = :owner OR c.owner IS NULL)
    """)
    Optional<Category> findByIdAndOwnerOrOwnerIsNull(@Param("id") UUID id, @Param("owner") User owner);

    Optional<Category> findByIdAndOwner(UUID id, User owner);

    Page<Category> findByOwnerOrOwnerIsNull(User owner, Pageable pageable);
}
