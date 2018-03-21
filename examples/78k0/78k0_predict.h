void __rv_interrupt_handler(irq0, renesas_78k0_priority_low)
    lopri_handler(void);

void __rv_interrupt_handler(irq1, renesas_78k0_priority_high)
    hipri_handler(void);
