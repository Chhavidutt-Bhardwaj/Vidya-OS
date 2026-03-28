package com.ai.vidya.modules.staff.entity;

/**
 * Discriminator / HR role for staff.
 * Maps to the JPA {@code InheritanceType.JOINED} discriminator column.
 */
public enum StaffRoleType {
    TEACHER,
    PRINCIPAL,
    ACCOUNTANT,
    EXAM_COORDINATOR,
    HR
}
