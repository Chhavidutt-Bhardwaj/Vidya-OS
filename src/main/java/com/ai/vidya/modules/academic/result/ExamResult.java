@Entity
@Table(
    name = "exam_results",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"exam_id", "student_id"}
    )
)
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private BigDecimal marksObtained;
    private BigDecimal percentage;

    @Column(length = 5)
    private String grade;              // A+, B, C, F

    @Enumerated(EnumType.STRING)
    private ResultStatus resultStatus; // PASS, FAIL, ABSENT, EXEMPTED

    @Column(columnDefinition = "TEXT")
    private String remarks;

    private boolean isPublished = false;
    private Instant publishedAt;

    @CreationTimestamp
    private Instant createdAt;
}