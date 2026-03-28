package com.ai.vidya.modules.fee.service;

import com.ai.vidya.exception.BadRequestException;
import com.ai.vidya.exception.ResourceNotFoundException;
import com.ai.vidya.modules.academic.repository.FeeStructureRepository;
import com.ai.vidya.modules.fee.dto.request.ApplyDiscountRequest;
import com.ai.vidya.modules.fee.dto.request.CollectFeeRequest;
import com.ai.vidya.modules.fee.dto.response.FeeDefaulterResponse;
import com.ai.vidya.modules.fee.dto.response.FeeStatementResponse;
import com.ai.vidya.modules.fee.entity.*;
import com.ai.vidya.modules.fee.repository.*;
import com.ai.vidya.modules.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeService {

    private final FeeInstalmentRepository  instalmentRepository;
    private final FeePaymentRepository     paymentRepository;
    private final FeeDiscountRepository    discountRepository;
    private final FeeStructureRepository   feeStructureRepository;
    private final ReceiptNumberGenerator   receiptGen;
    private final SchoolRepository         schoolRepository;

    // ══════════════════════════════════════════════════════════════════════
    // GENERATE INSTALMENTS ON ENROLLMENT
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public void generateInstalmentsForStudent(UUID schoolId, UUID studentId,
                                               UUID academicYearId, UUID gradeRangeId) {
        var structures = feeStructureRepository.findActiveByAcademicYearId(academicYearId);
        var instalments = new ArrayList<FeeInstalment>();

        for (var structure : structures) {
            if (structure.getGradeRange() != null &&
                !structure.getGradeRange().getId().equals(gradeRangeId)) continue;

            for (var head : structure.getHeads()) {
                if (!head.isActive()) continue;

                BigDecimal discount = computeDiscount(studentId, academicYearId, head.getAmount());
                List<LocalDate> dueDates = computeDueDates(
                    head.getFrequency(), academicYearId, head.getDueDayOfMonth());

                for (int i = 0; i < dueDates.size(); i++) {
                    BigDecimal net = head.getAmount()
                        .subtract(discount)
                        .max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP);

                    instalments.add(FeeInstalment.builder()
                        .schoolId(schoolId)
                        .studentId(studentId)
                        .academicYearId(academicYearId)
                        .feeStructureHead(head)
                        .instalmentNumber(i + 1)
                        .baseAmount(head.getAmount())
                        .discountAmount(discount)
                        .netAmount(net)
                        .dueDate(dueDates.get(i))
                        .status(FeeInstalment.InstalmentStatus.PENDING)
                        .build());
                }
            }
        }

        instalmentRepository.saveAll(instalments);
        log.info("Generated {} fee instalments for student {} year {}",
                 instalments.size(), studentId, academicYearId);
    }

    // ══════════════════════════════════════════════════════════════════════
    // COLLECT PAYMENT
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public FeePayment collectFee(UUID schoolId, CollectFeeRequest req) {
        applyLateFees(req.getStudentId(), req.getInstalmentIds());

        List<FeeInstalment> instalments = instalmentRepository.findAllById(req.getInstalmentIds());
        if (instalments.isEmpty()) throw new BadRequestException("No valid instalments found.");

        BigDecimal totalDue = instalments.stream()
            .map(FeeInstalment::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (req.getAmount().compareTo(totalDue) > 0) {
            throw new BadRequestException(
                "Payment ₹" + req.getAmount() + " exceeds balance ₹" + totalDue);
        }

        String receiptNo = receiptGen.generate(schoolId);
        FeePayment payment = FeePayment.builder()
            .schoolId(schoolId)
            .studentId(req.getStudentId())
            .academicYearId(req.getAcademicYearId())
            .receiptNo(receiptNo)
            .paymentDate(req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now())
            .totalAmount(req.getAmount())
            .paymentMode(req.getPaymentMode())
            .transactionRef(req.getTransactionRef())
            .bankName(req.getBankName())
            .chequeNo(req.getChequeNo())
            .collectedBy(req.getCollectedBy())
            .remarks(req.getRemarks())
            .build();

        BigDecimal remaining = req.getAmount();
        List<FeePaymentItem> items = new ArrayList<>();

        for (FeeInstalment inst : instalments) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal paying = remaining.min(inst.getBalance());
            inst.setAmountPaid(inst.getAmountPaid().add(paying));
            inst.setPaidDate(payment.getPaymentDate());
            inst.setReceiptNo(receiptNo);
            inst.setStatus(inst.getBalance().compareTo(BigDecimal.ZERO) == 0
                ? FeeInstalment.InstalmentStatus.PAID
                : FeeInstalment.InstalmentStatus.PARTIAL);
            items.add(FeePaymentItem.builder().payment(payment).instalment(inst).amount(paying).build());
            remaining = remaining.subtract(paying);
        }

        payment.setItems(items);
        instalmentRepository.saveAll(instalments);
        FeePayment saved = paymentRepository.save(payment);
        log.info("Fee collected: receipt={} student={} amount={}", receiptNo, req.getStudentId(), req.getAmount());
        return saved;
    }

    // ══════════════════════════════════════════════════════════════════════
    // FEE STATEMENT
    // ══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public FeeStatementResponse getStatement(UUID schoolId, UUID studentId, UUID yearId) {
        List<FeeInstalment> instalments = instalmentRepository.findByStudentAndYear(studentId, yearId);

        BigDecimal totalFee      = instalments.stream().map(FeeInstalment::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid     = instalments.stream().map(FeeInstalment::getAmountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDiscount = instalments.stream().map(FeeInstalment::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLateFee  = instalments.stream().map(FeeInstalment::getLateFeeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return FeeStatementResponse.builder()
            .studentId(studentId).academicYearId(yearId)
            .totalFee(totalFee).totalPaid(totalPaid)
            .totalPending(totalFee.subtract(totalPaid))
            .totalDiscount(totalDiscount).totalLateFee(totalLateFee)
            .instalments(instalments.stream()
                .map(i -> FeeStatementResponse.InstalmentEntry.builder()
                    .id(i.getId())
                    .headName(i.getFeeStructureHead().getDisplayName())
                    .instalmentNumber(i.getInstalmentNumber())
                    .baseAmount(i.getBaseAmount())
                    .discountAmount(i.getDiscountAmount())
                    .lateFeeAmount(i.getLateFeeAmount())
                    .netAmount(i.getNetAmount())
                    .amountPaid(i.getAmountPaid())
                    .balance(i.getBalance())
                    .dueDate(i.getDueDate())
                    .paidDate(i.getPaidDate())
                    .status(i.getStatus())
                    .receiptNo(i.getReceiptNo())
                    .build())
                .toList())
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // DISCOUNT
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public FeeDiscount applyDiscount(UUID schoolId, UUID studentId, UUID yearId,
                                      UUID approvedBy, ApplyDiscountRequest req) {
        FeeDiscount discount = FeeDiscount.builder()
            .schoolId(schoolId).studentId(studentId).academicYearId(yearId)
            .discountType(req.getDiscountType())
            .discountName(req.getDiscountName())
            .discountValue(req.getDiscountValue())
            .discountMode(req.getDiscountMode())
            .maxCap(req.getMaxCap())
            .validFrom(req.getValidFrom())
            .validTo(req.getValidTo())
            .approvedBy(approvedBy)
            .remarks(req.getRemarks())
            .active(true)
            .build();
        return discountRepository.save(discount);
    }

    // ══════════════════════════════════════════════════════════════════════
    // DEFAULTERS
    // ══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<FeeDefaulterResponse> getDefaulters(UUID schoolId, UUID yearId, int overdueDays) {
        LocalDate cutoff = LocalDate.now().minusDays(overdueDays);
        return instalmentRepository.findDefaulters(schoolId, yearId, cutoff);
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────

    private void applyLateFees(UUID studentId, List<UUID> ids) {
        instalmentRepository.findAllById(ids).stream()
            .filter(FeeInstalment::isOverdue)
            .forEach(inst -> {
                var head = inst.getFeeStructureHead();
                if (head.getLateFeeValue() == null || head.getLateFeeType() == null) return;
                BigDecimal late = switch (head.getLateFeeType()) {
                    case FLAT -> head.getLateFeeValue();
                    case PERCENTAGE_PER_MONTH -> {
                        long months = java.time.temporal.ChronoUnit.MONTHS.between(inst.getDueDate(), LocalDate.now());
                        yield inst.getBaseAmount()
                            .multiply(head.getLateFeeValue().divide(BigDecimal.valueOf(100)))
                            .multiply(BigDecimal.valueOf(Math.max(1, months)))
                            .setScale(2, RoundingMode.HALF_UP);
                    }
                    case NONE -> BigDecimal.ZERO;
                };
                if (late.compareTo(BigDecimal.ZERO) > 0) {
                    inst.setLateFeeAmount(late);
                    inst.setNetAmount(inst.getBaseAmount().subtract(inst.getDiscountAmount()).add(late).max(BigDecimal.ZERO));
                    inst.setStatus(FeeInstalment.InstalmentStatus.OVERDUE);
                }
            });
    }

    private BigDecimal computeDiscount(UUID studentId, UUID yearId, BigDecimal base) {
        return discountRepository.findActiveByStudentAndYear(studentId, yearId).stream()
            .filter(d -> d.getValidTo() == null || !LocalDate.now().isAfter(d.getValidTo()))
            .map(d -> switch (d.getDiscountMode()) {
                case PERCENTAGE -> base.multiply(d.getDiscountValue().divide(BigDecimal.valueOf(100)))
                    .min(d.getMaxCap() != null ? d.getMaxCap() : base).setScale(2, RoundingMode.HALF_UP);
                case FLAT -> d.getDiscountValue().min(base);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add).min(base);
    }

    private List<LocalDate> computeDueDates(
            com.ai.vidya.common.enums.FeeFrequency frequency, UUID yearId, Integer dayOfMonth) {
        // Simplified: start from April 1 of current year
        int year = LocalDate.now().getMonthValue() >= 4
            ? LocalDate.now().getYear() : LocalDate.now().getYear() - 1;
        LocalDate start = LocalDate.of(year, 4, 1);
        int day = dayOfMonth != null ? Math.min(dayOfMonth, 28) : 10;

        return switch (frequency) {
            case ANNUAL      -> List.of(start.withDayOfMonth(day));
            case SEMI_ANNUAL -> List.of(start.withDayOfMonth(day), start.plusMonths(6).withDayOfMonth(day));
            case QUARTERLY   -> List.of(
                start.withDayOfMonth(day), start.plusMonths(3).withDayOfMonth(day),
                start.plusMonths(6).withDayOfMonth(day), start.plusMonths(9).withDayOfMonth(day));
            case MONTHLY     -> java.util.stream.IntStream.range(0, 12)
                .mapToObj(i -> start.plusMonths(i).withDayOfMonth(day)).toList();
            case ONE_TIME    -> List.of(start.withDayOfMonth(day));
        };
    }
}
