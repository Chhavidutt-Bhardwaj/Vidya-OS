@Entity
@Table(name = "academic_snapshots")
public class AcademicSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false, unique = true)
    private AcademicYear academicYear;

    // Serialised summary stored as JSON (PostgreSQL jsonb / MySQL JSON column)
    @Column(columnDefinition = "jsonb")
    private String summaryJson;

    private Integer totalStudents;
    private Integer totalExams;
    private BigDecimal schoolPassPercentage;

    @Column(nullable = false)
    private Instant closedAt;

    @Column(nullable = false, length = 100)
    private String closedByUsername;
}