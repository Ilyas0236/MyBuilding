package com.mybuilding.incidents.init;

import com.mybuilding.incidents.model.PrioriteTicket;
import com.mybuilding.incidents.model.Ticket;
import com.mybuilding.incidents.model.WorkOrder;
import com.mybuilding.incidents.repository.TicketRepository;
import com.mybuilding.incidents.repository.WorkOrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final TicketRepository ticketRepository;
    private final WorkOrderRepository workOrderRepository;

    public DataInitializer(TicketRepository ticketRepository, WorkOrderRepository workOrderRepository) {
        this.ticketRepository = ticketRepository;
        this.workOrderRepository = workOrderRepository;
    }

    @Override
    public void run(String... args) {
        // Initialisation directe via les repositories pour éviter les appels Feign au démarrage
        if (ticketRepository.count() == 0) {
            Ticket t1 = ticketRepository.save(new Ticket(1L, "Fuite d'eau au robinet de la cuisine", PrioriteTicket.HAUTE));
            ticketRepository.save(new Ticket(2L, "Panne interphone entrée principale", PrioriteTicket.MOYENNE));

            if (workOrderRepository.count() == 0) {
                WorkOrder wo = new WorkOrder();
                wo.setTicketId(t1.getId());
                wo.setArtisan("Plomberie Express");
                wo.setDateIntervention(LocalDate.now().plusDays(1));
                wo.setCout(new BigDecimal("450.00"));
                workOrderRepository.save(wo);
            }
        }
    }
}
