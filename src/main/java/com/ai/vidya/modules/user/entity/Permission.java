package com.ai.vidya.modules.user.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(
    name = "permissions",
    indexes = {
        @Index(name = "idx_perm_code",   columnList = "code",   unique = true),
        @Index(name = "idx_perm_module", columnList = "module")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission extends BaseEntity {

    /**
     * Format: MODULE:ACTION
     * Examples:
     *   SCHOOL:READ       SCHOOL:UPDATE       SCHOOL:ONBOARD
     *   CHAIN:READ        CHAIN:MANAGE_BRANCHES
     *   STUDENT:CREATE    STUDENT:READ        STUDENT:UPDATE
     *   ATTENDANCE:MARK   ATTENDANCE:REPORT
     *   FEE:COLLECT       FEE:DEFAULTER_REPORT
     *   TEACHER:ONBOARD   TEACHER:READ
     *   SALARY:PROCESS    SALARY:VIEW
     *   REPORT:GENERATE   REPORT:EXPORT
     *   AI:INSIGHTS       AI:REMARK_GENERATE
     */
    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(length = 255)
    private String description;

    /** Module grouping: school | chain | student | teacher | attendance | fee | academic | hr | ai */
    @Column(nullable = false, length = 50)
    private String module;
}
