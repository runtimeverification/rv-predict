# TI ARM Interrupt Controller data-race demonstration

To build the example program, `simple`, run `mkcmake`.

`simple`
:   a program with a single thread, high- and low-priority
    interrupts that all modify non-atomic shared variables.  Races are
    expected on the variable called `racy`, because the application
    thread and both of the interrupt handlers modify it without
    protecting against interruption.  No races are expected on
    `racefree`, however, because it is always modified with interrupts
    disabled.
 
    The interrupt handlers in this program are established statically on
    system interrupt sources TIAIC_UART0_INT and TIAIC_SPI0_INT with the
    `tiaic_predict.h` header file.
  
    The program establishes the priorities for interrupts dynamically
    by calling tiaic_write() to modify the interrupt *channel*
    numbers assigned to sources TIAIC_UART0_INT and TIAIC_SPI0_INT.
    Handlers for interrupts with lower channel number are run at higher
    priority than handlers for higher channel numbers.
    
    For best results, run `simple` with special parameters set in the
    environment.  Example:
 
    ```
    mkcmake
    RVP_ANALYSIS_ARGS="--interrupts-target 20" RVP_WINDOW_SIZE=175 ./simple
    ```
