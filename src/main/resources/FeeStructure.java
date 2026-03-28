import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "fee_structures")
public class FeeStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(nullable = false, length = 50)
    private String classGrade;

    @Column(nullable = false, length = 100)
    private String feeHead;            // "Tuition", "Library", "Sports"

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private FeeFrequency frequency;    // ANNUAL, TERM, MONTHLY, ONE_TIME

    private boolean isMandatory = true;
}

@Entity
@Table(name = "fee_payments")
public class FeePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private StudentEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_structure_id", nullable = false)
    private FeeStructure feeStructure;

    @Column(nullable = false)
    private BigDecimal amountPaid;

    private LocalDate paymentDate;
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;   // CASH, ONLINE, CHEQUE, DD

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;      // PAID, PARTIAL, PENDING, WAIVED
}