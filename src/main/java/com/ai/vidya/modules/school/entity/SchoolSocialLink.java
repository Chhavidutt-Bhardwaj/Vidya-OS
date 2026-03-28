package com.ai.vidya.modules.school.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.SocialPlatform;
import jakarta.persistence.*;
import lombok.*;

/**
 * Social media / online presence links for a school.
 * e.g. Facebook page, Instagram, YouTube channel, LinkedIn.
 *
 * Many-to-one with School.
 */
@Entity
@Table(
    name = "school_social_links",
    indexes = {
        @Index(name = "idx_social_school_id", columnList = "school_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolSocialLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 30)
    private SocialPlatform platform;

    @Column(name = "url", nullable = false, length = 512)
    private String url;
}