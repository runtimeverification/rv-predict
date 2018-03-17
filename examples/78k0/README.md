# 78k0 data-race demonstration

Build all examples with `mkcmake`.  For best results, run with
`RVP_WINDOW_SIZE=150` in the environment.  Example:

```
mkcmake
RVP_WINDOW_SIZE=150 ./78k0
```

`78k0`: a program with a single thread, high- and low-priority
    interrupts that all modify non-atomic shared variables.  Races are
    expected on the variable called `racy`, because the application
    thread and both of the interrupt handlers modify it without
    protecting against interruption.  No races are expected on
    `racefree`, however, because it is always modified with interrupts
    disabled.

`hilo`: a _slight_ simplification of `78k0` that does not modify any
    variable in `78k0`.  Interrupts race only against interrupts in this
    example.

`lolo`: a simplification of example `78k0` that involves just one,
    low-priority interrupt handler.
