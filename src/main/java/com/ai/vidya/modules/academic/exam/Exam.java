@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, length = 100)
    private String title;              // "Mid-Term Math", "Final Science"

    @Enumerated(EnumType.STRING)
    private ExamType examType;         // UNIT_TEST, MID_TERM, FINAL, PRACTICAL, PROJECT

    @Column(nullable = false)
    private LocalDate examDate;

    private LocalTime startTime;
    private LocalTime endTime;
    private Integer totalMarks;
    private Integer passingMarks;

    @Column(length = 200)
    private String venue;

    @Enumerated(EnumType.STRING)
    private ExamStatus status;         // SCHEDULED, ONGOING, COMPLETED, CANCELLED

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL)
    private List<ExamResult> results = new ArrayList<>();
}