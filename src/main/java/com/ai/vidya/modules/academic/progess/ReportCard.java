@Entity
@Table(
    name = "report_cards",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"enrollment_id", "term_id"}
    )
)
public class ReportCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private StudentEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private Term term;                // null = full-year card

    private boolean isAnnual = false;
    private boolean isPublished = false;
    private Instant publishedAt;

    @Column(columnDefinition = "TEXT")
    private String principalRemarks;

    @CreationTimestamp
    private Instant generatedAt;
}