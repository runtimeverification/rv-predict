
% Signals model
% Traian Florin Șerbănuță
%

## Events

All events must have the following attributes:

* \instanceId – identifying the current thread/signal instance
* \instanceCounter – capturing the execution order within an instance
* \type of event:
  * START – a thread/signal instance start
  * END – a thread/signal instance end
  * WRITE – the write of a memory location
  * READ – the read of a memory location
  * LOCK – acquiring a resource
  * UNLOCK – releasing a resource
  * WRITE-MASK – establishing a new mask for the current instance
  * READ-MASK – reading the mask for the current instance
  * DISABLE-SIGNAL – disabling a single signal
  * ENABLE-SIGNAL – enabling a single signal
  * ESTABLISH-SIGNAL – establishing a new handler and default mask for a
    signal
  * READ-SIGNAL – reading the current handler and default mask for a
    signal
  * CURRENT-SIGNAL – identifying the currently executing signal
  * SPAWN – spawning a new thread
  * JOIN – waiting for a thread to finish

Additionally, each type of event might require extra attributes, as follows:

* START
  * \handlerId – identifier for the handler treating this signal
* WRITE/READ
  * \location – location being accessed
  * \val – value written/read
  * \isAtomic – whether the operation was performed atomically
* LOCK/UNLOCK
  * \resourceId – identifier for the resource being locked/unlocked
* WRITE-MASK/READ-MASK
  * \mask – the mask being read / written
* ESTABLISH-SIGNAL/READ-SIGNAL
  * \signalNumber  – the signal to be handled
  * \handlerId – identifier for the handler
  * \defaultMask – the default mask for this signal. This will be or-ed
    with the existing mask of the thread/signal being interrupted.
* CURRENT-SIGNAL
  * \signalNumber  – the signal currently being executed
* ENABLE-SIGNAL/DISABLE-SIGNAL
  * \signalNumber – the signal to be enabled/disabled
* SPAWN/JOIN
  * \threadInstanceId – the instance identifying the thread being
    created/joined

A *mask* tells for each signal whether it is enabled or not. We abstract it as
a map from signal numbers to $\{T, F\}$.  Let \allEnabled be the mask mapping
all signal numbers to *T*.
Given a mask *m*, the mask obtained from *m* by updating a signal number *n* to
flag $b\in \{T, F\}$ will be denoted as $m[n\mapsto b]$

## Traces

A trace (prefix) is a collection of events satisfying the following properties:

1. Different instances of threads/signals have distinct \instanceId.
   Therefore, we identify an instance of a thread/signal in a trace as all
   events sharing the same \instanceId.
   Let *Instances* be the set of values of all \instanceId attributes.

1. Each thread/signal instance has an unique START and at most one END event.
   Let \iStart and \iEnd be functions defined on *Instances* mapping an
   instance to its corresponding START or END event.  Note that $\iStart$ is a
   total function while $\iEnd$ can be partial.

1. The set of \instanceCounter attributes within the same thread/signal
   constitutes a sequence of consecutive natural numbers
   starting with 0 for its START event and reaching maximum at its END event
   (if the END event exists).

1. The main thread within a trace has \instanceId 0.  There exists no SPAWN
   event whose \threadInstanceId is 0

1. Each thread (except for the main) is started by a unique SPAWN instance.
   More formally, the \threadInstanceId of a SPAWN event is different from
   its \instanceId, and there are no two SPAWN events with the same
   \threadInstanceId.

   Let *Threads* be the set of values of \threadInstanceId attributes,
   to which 0 is added.
   It must be that \threadInstanceId attributes of JOIN events are in *Threads*
   Let \spawnedBy be the function mapping each of the *Threads* (except for k0)
   to their corresponding SPAWN events.

Let *Signals* be the set of \instanceId attributes *not* corresponding to
threads.

Note that each event is uniquely identified by its \instanceId and
\instanceCounter attributes.  Let \event be the partial function which given
\instanceId *id* and \instanceCounter *counter* defines *\event(id, counter)*
as the event uniquely identified by them (if such an event exists).

A subtrace (prefix) of a given trace (prefix) *Trace* is a subset of *Trace*
satisfying all trace properties enumerated above.

## Causal model

For this section assume a fixed trace (prefix) *Trace* and regard all
definitions as being parametric in *Trace*.  Whenever needed for disambiguation
we will superscript the relations and mappings defined below with the trace
they are defined on.

A trace ordering is a partial order $\ord$ on the events of a trace prefix.
Given an unary predicate on events *P* and an event *e*, let
$\eMax{\ord e}[P]$ be the set of the maximal elements preceding *e*
(w.r.t. $\ord$) which satisfy *P*.

### Thread/Signal instance ordering

Let $\pOrd{n}$ be the total order relation on elements sharing the
\instanceId *n* defined by $\e1 \pOrd{n} \e2$ iff
$\instanceId(\e1) = \instanceId(\e2) = n$
and $\instanceCounter(\e1) < \instanceCounter(\e2)$.

A trace ordering $\ord$ is consistent with the thread/signal instance
ordering if it includes $\pOrd{n}$ for all $n \in *Instances*$

Let \nextT be the partial mapping from events to events yielding the next event
within the same thread/signal instance, defined by
$$\nextT(e) = \event(\instanceId(e), \instanceCounter(e)+1)$$
Note that *\nextT* is defined for all events of a trace except those final
for their \instanceId.

Conversely, let *\precT* be the (partial) inverse of *\nextT* yielding the
preceding event within the same thread/signal instance, defined by
$$\precT(e) = \event(\instanceId(e), \instanceCounter(e)-1)$$
Note that *\precT* is defined for all events of a trace except those of type
START.

### SPAWN/JOIN consistency

A trace ordering $\prec$ is SPAWN/JOIN consistent if SPAWN executes before the
corresponding START and JOIN executes after the corresponding END.  Formally,

$$\type(e) = *SPAWN* \implies e \prec \iStart(\threadInstanceId(e))$$

and
$$\type(e) = *JOIN* \implies \iEnd(\threadInstanceId(e)) \prec e$$

### Interruption mapping and ordering

An interruption mapping is a function \interrupts mapping *Signals* to
the events they interrupt.

An interruption mapping \interrupts is well-defined if:

* a signal cannot interrupt (after) the END event.  Formally, for all
    $s\in*Signals*$, $\type(\interrupts(s)) \neq *END*$
* If the interrupted instance continues then the interrupting signal must end.
    Formally, if $\nextT(\interrupts(s))$ is defined, then $\iEnd(s)$ is also
    defined.

A well-defined interruption mapping \interrupts defines an interruption
ordering $\pOrd{\intr}$ such that for all $s\in *Signals*$

* $\interrupts(s) \pOrd{\intr} \iStart(s)$
* If $\nextT(\interrupts(s))$ is defined, then
    $\iEnd(s) \pOrd{\intr} \nextT(\interrupts(s))$

A trace ordering is compatible with a well-defined interruption mapping if
it includes the corresponding interruption ordering.

### Memory consistency

A trace ordering $\prec$ is memory consistent if for any READ/READ-SIGNAL
operation, its immediately preceding WRITE/ESTABLISH-SIGNAL operation(s)
on the same \location/\signalNumber exist(s) and has(have) the same
value/handler-mask-attributes.
Formally, for any event $e$ such that $\type(e) = *READ*$,

$$ \val(\eMax{\prec e}[\location(x) = \location(e)\wedge \type(x) = *WRITE*])
 = \{\val(e)\}$$

and, for any event $e$ such that $\type(e) = *READ-SIGNAL*$, and for all
events $e'\in \eMax{\prec e}[\signalNumber(x) = \signalNumber(e)
                             \wedge \type(x) = *ESTABLISH-SIGNAL*]$,
we have that $\handlerId(e') = \handlerId(e)$ and $\mask(e') = \mask(e)$.

### Resource consistency

A trace ordering $\prec$ is resource-consistent if each LOCK operation acquires
a previously free resource, and each UNLOCK operation releases a resource
previously acquired by the same thread.  Formally, given an event *e* such that
$\type(e) = *LOCK*$, if $P = \eMax{\prec e}[\resourceId(x) = \resourceId(e)]$,
then

$$ P\neq \emptyset \implies P = \{e'\} \wedge \type(e') = *UNLOCK* $$

Similarly, if $\type(e) = *UNLOCK*$, then

$$ \eMax{\prec e}[\resourceId(x) = \resourceId(e)] = \{e'\}
    \wedge \resourceId(e') = \resourceId(e) \wedge \type(e') = *LOCK* $$

### Signal handler mapping and consistency

A signal handler mapping \establishedBy is a mapping from *Signals* to events
(of type ESTABLISH-SIGNAL) whose purpose is to map each signal to its
corresponding establishing operation.

A signal handler mapping \establishedBy is well-defined if it maps all signals
to ESTABLISH-SIGNAL events whose \handlerId is the same as the \handlerId for
the START event of the signal, and if CURRENT-SIGNAL events correspond to the
signal being established.  Formally, for all $s\in *Signals*$,

$$\handlerId(\establishedBy(s)) = \handlerId(\iStart(s))$$
and, for all events $e$ such that $\instanceId(e) = s$ and
$\type(e) = *CURRENT-SIGNAL*$,

$$\signalNumber(e) = \signalNumber(\establishedBy(s))$$

A trace ordering $\prec$ is compatible with a well-defined signal-handler
mapping \establishedBy if for any signal *s*, $\establishedBy(s)$ is the
immediately preceding ESTABLISH-SIGNAL event for signal *s* for $\iStart(s)$.
Formally, for all $s\in *Signals*$, if $e = \establishedBy(s)$, then

$$\eMax{\prec \iStart(s)}[\signalNumber(x) = \signalNumber(e)
   \wedge \type(x) = *ESTABLISH-SIGNAL*] = e$$

### Current mask and mask consistency

Assume a trace ordering $\prec$ compatible with a well-defined interruption
mapping \interrupts and a well-defined signal-handler mapping \establishedBy,
and consistent with the thread/signal instance ordering.

Given such an ordering we can define a function \currentMask assigning to
each event the mask of signals currently enabled for its thread, as follows:

* $\currentMask(e) = \mask(e)$
    if $\type(e) = *WRITE-MASK*$;
* $\currentMask(e) = \currentMask(\precT(e))[\signalNumber(e)\mapsto T]$
    if $\type(e) = *ENABLE-SIGNAL*$;
* $\currentMask(e) = \currentMask(\precT(e))[\signalNumber(e)\mapsto F]$
    if $\type(e) = *DISABLE-SIGNAL*$;
* $\currentMask(e) = \currentMask(\interrupts(\instanceId(e)))
                     \vee \mask(\establishedBy(\instanceId(e)))$
    if $\type(e) = *START*$ and $\instanceId(e)\in *Signals*$;
* $\currentMask(e) = \allEnabled$
    if $\type(e) = *START*$ and $\instanceId(e) = 0$;
* $\currentMask(e) = \currentMask(\spawnedBy(\instanceId(e)))$
    if $\type(e) = *START*$ and $\instanceId(e) \in *Thread* \setminus \{0\}$;
* $\currentMask(e) = \currentMask(\precT(e))$, otherwise.

Except for the fourth case, all cases are self-explanatory.  The fourth case
describes formally that when a signal interrupts a thread, its starting mask
is obtained as a disjunction between the existing mask for the interrupted
thread/signal instance and the default mask for the signal assigned by the
corresponding ESTABLISH-SIGNAL event.

The trace ordering $\prec$ is mask-consistent if:

* READ-MASK reads the current mask. Formally for all events $e$,

    $$\type(e) = *READ-MASK* \implies \mask(e) = \currentMask(e)$$
* Any signal is enabled at any moment one of its instances starts.
    Formally, for any $s \in *Signals*$,

    $$\currentMask(\interrupts(s))[\signalNumber(\establishedBy(s))] = T$$

### Feasible trace prefix

A feasible trace is a tuple
$(*Trace*, \interrupts, \establishedBy, \prec),$ such that:

* *Trace* is a trace
* \interrupts is a well-defined interruption mapping for *Trace*
* \establishedBy is a well-defined signal-handler mapping for *Trace*
* $\prec$ is a trace ordering on *Trace* compatible with \interrupts and
    \establishedBy, consistent with the thread/signal instance ordering,
    SPAWN/JOIN consistent, mask consistent, memory-consistent, and
    resource consistent.

Given a trace *Trace*, a feasible subtrace of *Trace* is a feasible
trace whose first component is a subtrace of *Trace*

## Causal race

A causal race is a *race candidate* for which there exists a feasible *race
witness*.

### Race candidate

A race candidate for a trace is a pair of events $(\e1, \e2)$ such that:

* They access the same memory location:
    $\location(\e1) = \location(\e2)$
* At least one of them is writing:
    $*WRITE* \in \{\type(\e1), \type(\e2)\}$
* At least one of them is non-atomic:
    $\isAtomic(\e1)\wedge \isAtomic(\e2) = *false*$
* They belong to different thread/signal instances:
    $\instanceId(\e1) \neq \instanceId(\e2)$

### Associated threads

For two events to be in a race, they must be run concurrently.
A necessary condition for that is they need to run on different threads.

Note that in our race candidate definition above we required a weaker
condition: that they belong to different instances;  however it is possible
that in a feasible trace they are both scheduled on the same thread.

Given a trace ordering $\prec$ compatible with the thread/signal instance
ordering and with an interruption mapping \interrupts, we can assign to each
event the actual thread it runs onto. We recursively define a function
\thread mapping events to *Threads* as follows:

* $\thread(e) = \instanceId(e)$ if $\instanceId(e) \in *Threads*$
* $\thread(e) = \thread(\interrupts(\instanceId(e)))$
  if $\instanceId(e) \in *Signals*$

Note that the definition is well founded, since

$$\interrupts(\instanceId(e)) \pOrd{\intr} \iStart(\instanceId(e))
   \pOrd{\instanceId(e)} e.$$

### Race witness

A race witness for a race candidate $(\e1, \e2)$ on trace *Trace* is a
feasible subtrace *SubTrace* of *Trace* such that:

* $\e1$ and $\e2$ are enabled but haven't occurred yet:
   $\precT^{*Trace*}(\e i) \in *SubTrace*$ but
   $\nextT^{*SubTrace*}(\precT^{*Trace*}(\e i))$ is undefined, for $i\in\{1,2\}$
* $\e1$ and $\e2$ belong to different threads:
   $\thread(\e1) \neq \thread(\e2)$

### Signal-race witness

We say that there is a signal race when a thread/signal instance is
interrupted during a non-atomic access to a location, and during that
interruption the location is accessed, which could result in either a corrupt
read or corrupted memory.

A signal-race witness on trace *Trace* for a race candidate $(\e1, \e2)$,
where $\isAtomic(\e1) = \{*false*\}$ is a feasible subtrace *Subtrace* of
*Trace* such that:

* $\e2$ belongs to a signal (transitively) interrupting $\e1$ and is enabled.
    Let $\e2' = \precT^{*Trace*}(\e2)$. Then
    $\e1, \e2'\in *SubTrace*$ and $\thread(\e1) = \thread(\e2')$, but
    $\nextT^{*SubTrace*}(\e2')$ and $\nextT^{*SubTrace*}(\e1)$ are undefined
* $\e1$ immediately precedes $\e2'$ as access on its location
    $\eMax{\prec \e2'}[\location(x) = \location(\e1)] = \{\e1\}$
