package com.mybuilding.finances.init;

import com.mybuilding.finances.model.Charge;
import com.mybuilding.finances.model.MoyenPaiement;
import com.mybuilding.finances.model.Payment;
import com.mybuilding.finances.model.StatutCharge;
import com.mybuilding.finances.repository.ChargeRepository;
import com.mybuilding.finances.repository.PaymentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ChargeRepository chargeRepository;
    private final PaymentRepository paymentRepository;

    public DataInitializer(ChargeRepository chargeRepository, PaymentRepository paymentRepository) {
        this.chargeRepository = chargeRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void run(String... args) {
        // Initialisation directe via les repositories pour éviter les appels Feign et garantir l'ordre Charge -> Payment
        if (chargeRepository.count() == 0) {
            Charge c1 = new Charge();
            c1.setApartmentId(1L);
            c1.setPeriode("2026-11");
            c1.setMontant(new BigDecimal("1500.00"));
            c1.setMontantPaye(new BigDecimal("500.00"));
            c1.setStatut(StatutCharge.PARTIELLE);
            Charge savedC1 = chargeRepository.save(c1);

            Charge c2 = new Charge();
            c2.setApartmentId(2L);
            c2.setPeriode("2026-11");
            c2.setMontant(new BigDecimal("1200.00"));
            c2.setMontantPaye(BigDecimal.ZERO);
            c2.setStatut(StatutCharge.EMISE);
            chargeRepository.save(c2);

            if (paymentRepository.count() == 0) {
                paymentRepository.save(new Payment(
                        savedC1.getId(),
                        LocalDate.now(),
                        new BigDecimal("500.00"),
                        MoyenPaiement.VIREMENT
                ));
            }
        }
    }
}
