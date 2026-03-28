@Entity
@Table(name = "terms")
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(nullable = false, length = 50)
    private String name;               // "Term 1", "First Semester"

    @Enumerated(EnumType.STRING)
    private TermType type;             // TERM, SEMESTER, QUARTER

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private int termNumber;

    @OneToMany(mappedBy = "term", cascade = CascadeType.ALL)
    private List<Exam> exams = new ArrayList<>();
}