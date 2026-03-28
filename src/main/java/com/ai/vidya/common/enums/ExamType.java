package com.ai.vidya.common.enums;

/**
 * Classifies the type of examination within a school term.
 *
 * Drives exam schedule templates, mark-sheet generation,
 * and result processing rules.
 */
public enum ExamType {

    /** Periodic / weekly class test — low stakes, teacher-assessed */
    CLASS_TEST,

    /**
     * Unit test — covers one unit of the syllabus.
     * Typically 20–25 marks; contributes to internal assessment.
     */
    UNIT_TEST,

    /**
     * Mid-term examination — covers first half of the term.
     * Usually 50–80 marks; school-level supervision.
     */
    MID_TERM,

    /**
     * End-of-term / final examination — covers full term syllabus.
     * Highest weightage; often formally invigilated.
     */
    FINAL,

    /** Practical / lab examination */
    PRACTICAL,

    /**
     * Pre-board / mock board examination — simulates board exam conditions.
     * Applicable to Grade 10 and Grade 12 only.
     */
    PRE_BOARD,

    /**
     * Board examination — conducted by external board (CBSE, ICSE, State).
     * Stored for scheduling reference; marks imported from board results.
     */
    BOARD_EXAM,

    /**
     * Oral / viva examination — language or project viva.
     */
    VIVA,

    /** Project / assignment submission — graded over a window */
    PROJECT_SUBMISSION
}