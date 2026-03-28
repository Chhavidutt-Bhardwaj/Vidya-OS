@Entity
@Table(name = "student_progress")
public class StudentProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private StudentEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    private int totalWorkingDays;
    private int daysPresent;
    private BigDecimal attendancePercentage;

    // Overall term performance
    private BigDecimal overallMarksObtained;
    private BigDecimal overallPercentage;
    private String termGrade;
    private Integer classRank;

    @Column(columnDefinition = "TEXT")
    private String teacherRemarks;

    @Enumerated(EnumType.STRING)
    private PromotionStatus promotionStatus; // PROMOTED, DETAINED, PENDING
}