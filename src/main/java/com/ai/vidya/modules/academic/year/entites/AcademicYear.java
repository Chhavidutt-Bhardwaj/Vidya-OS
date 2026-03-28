@Entity
@Table(name = "academic_years")
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false, length = 20)
    private String yearLabel;          // e.g. "2024-25"

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AcademicYearStatus status; // PLANNED, ACTIVE, CLOSED, ARCHIVED

    @Column(nullable = false)
    private boolean current = false;

    @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Term> terms = new ArrayList<>();

    @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudentEnrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL)
    private List<FeeStructure> feeStructures = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // getters/setters or use Lombok @Data
}