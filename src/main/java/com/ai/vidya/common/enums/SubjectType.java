package com.ai.vidya.common.enums;

/**
 * Classifies how a subject is offered and assessed.
 *
 * CORE       → Compulsory for all students in the class (Math, English, Science)
 * ELECTIVE   → Optional — students choose from a pool (Economics, Psychology)
 * LANGUAGE   → Language papers — 1st, 2nd, 3rd language
 * VOCATIONAL → Skill-based subjects (Computer Applications, Fashion Design)
 * CO_CURRICULAR → Activity-based, typically no written exam (PE, Art, Music)
 */
public enum SubjectType {
    CORE,
    ELECTIVE,
    LANGUAGE,
    VOCATIONAL,
    CO_CURRICULAR
}
