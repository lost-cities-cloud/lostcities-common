package io.dereknelson.lostcities.common.auditing

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.io.Serializable
import java.time.Instant

/**
 * Base abstract class for entities which will hold definitions for created, last modified, created by,
 * last modified by attributes.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class AbstractAuditingEntity : Serializable {
    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 50, updatable = false)
    @JsonIgnore
    open var createdBy: String? = null

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    @JsonIgnore
    open var createdDate: Instant? = null

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 50)
    @JsonIgnore
    open var lastModifiedBy: String? = null

    @LastModifiedDate
    @Column(name = "last_modified_date")
    @JsonIgnore
    open var lastModifiedDate: Instant? = Instant.now()

    companion object {
        private const val serialVersionUID = 1L
    }
}
