@Entity
@Table(
    name = "student_enrollments",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"student_id", "academic_year_id"}
    )
)
public class StudentEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(nullable = false, length = 50)
    private String classGrade;

    @Column(length = 20)
    private String section;

    @Column(length = 20)
    private String rollNumber;

    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status;   // ACTIVE, TRANSFERRED, DROPPED, PROMOTED

    @Column(nullable = false)
    private LocalDate enrolledOn;
}