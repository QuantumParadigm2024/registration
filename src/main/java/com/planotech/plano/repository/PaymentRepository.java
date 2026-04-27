package com.planotech.plano.repository;

import com.planotech.plano.model.Payment;
import com.planotech.plano.model.RegistrationEntry;
import com.planotech.plano.response.PaymentUserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Payment findByRegistrationEntry(RegistrationEntry entry);

    // ✅ Paid Users
    @Query("""
        SELECT COUNT(e)
        FROM RegistrationEntry e
        WHERE e.event.eventId = :eventId
        AND e.payment IS NOT NULL
        AND e.payment.status = com.planotech.plano.enums.PaymentStatus.SUCCESS
    """)
    Long countPaidUsers(Long eventId);

    // ✅ Pending
    @Query("""
        SELECT COUNT(e)
        FROM RegistrationEntry e
        WHERE e.event.eventId = :eventId
        AND e.payment IS NOT NULL
        AND e.payment.status = com.planotech.plano.enums.PaymentStatus.PENDING
    """)
    Long countPendingPayments(Long eventId);

    // ✅ Failed
    @Query("""
        SELECT COUNT(e)
        FROM RegistrationEntry e
        WHERE e.event.eventId = :eventId
        AND e.payment IS NOT NULL
        AND e.payment.status = com.planotech.plano.enums.PaymentStatus.FAILED
    """)
    Long countFailedPayments(Long eventId);

    // ✅ Revenue
    @Query("""
        SELECT COALESCE(SUM(e.payment.amount), 0)
        FROM RegistrationEntry e
        WHERE e.event.eventId = :eventId
        AND e.payment IS NOT NULL
        AND e.payment.status = com.planotech.plano.enums.PaymentStatus.SUCCESS
    """)
    BigDecimal getTotalRevenueByEvent(Long eventId);

    // ✅ Users list with payment
    @Query("""
        SELECT new com.planotech.plano.response.PaymentUserDTO(
            e.name,
            e.email,
            e.phone,
            e.type,
            e.payment.status,
            e.payment.amount,
            e.payment.paidAt,
            e.payment.razorpayPaymentId
        )
        FROM RegistrationEntry e
        WHERE e.event.eventId = :eventId
    """)
    Page<PaymentUserDTO> getUsersWithPaymentStatus(Long eventId, Pageable pageable);
}