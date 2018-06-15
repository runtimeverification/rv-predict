void __rv_interrupt_handler(uart0, TIAIC_UART0_INT)
    uart_handler(void);

void __rv_interrupt_handler(spi0, TIAIC_SPI0_INT)
    spi_handler(void);
