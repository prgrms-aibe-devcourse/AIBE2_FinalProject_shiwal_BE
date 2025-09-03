package com.example.hyu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * JPA lifecycle callback invoked before the entity is persisted.
     *
     * If the creation or update timestamps are unset, assigns both to the current instant.
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    /**
     * JPA lifecycle callback run before the entity is updated.
     *
     * Sets the entity's `updatedAt` timestamp to the current instant.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
 * Returns the entity's creation timestamp.
 *
 * <p>The value is mapped to the `created_at` column and is populated when the entity is persisted.
 *
 * @return the creation Instant (non-null after the entity has been persisted)
 */
public Instant getCreatedAt() { return createdAt; }
    /**
 * Returns the timestamp of the last modification.
 *
 * @return the last-modified timestamp as an {@link Instant}; after the entity has been persisted, this value will be set and non-null
 */
public Instant getUpdatedAt() { return updatedAt; }
}
