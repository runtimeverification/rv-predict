# 78k0 data-race demonstration

To build all examples, run `mkcmake`.  There are four example programs:

`simple`
:   a program with a single thread, high- and low-priority
    interrupts that all modify non-atomic shared variables.  Races are
    expected on the variable called `racy`, because the application
    thread and both of the interrupt handlers modify it without
    protecting against interruption.  No races are expected on
    `racefree`, however, because it is always modified with interrupts
    disabled.

`complex`
:   like `simple` but it manipulates the interrupt-enable control
    flag (IE) more frequently.  The trace is more complicated and more
    costly to analyze than the trace for `simple`.
 
    For best results, run `complex` with `RVP_WINDOW_SIZE=150` in the
    environment.  Example:
 
    ```
    mkcmake
    RVP_WINDOW_SIZE=150 ./complex
    ```

`hilo`
:   a _slight_ simplification of `78k0` that does not modify any
    variable in `78k0`.  Interrupts race only against interrupts in this
    example.

    For best results, run `hilo` with `RVP_WINDOW_SIZE=150` in the
    environment.  Example:

    ```
    mkcmake
    RVP_WINDOW_SIZE=150 ./hilo
    ```

`lolo`
:   a simplification of example `78k0` that involves just one,
    low-priority interrupt handler.
