#ifndef _RV_INTR_EXPORTS_H_
#define _RV_INTR_EXPORTS_H_

#ifndef __section
#define __section(x)    __attribute__((__section__(x)))
#endif /* __section */

void __rvpredict_intr_enable(void);
void __rvpredict_intr_disable(void);
void __rvpredict_isr_fire(void (*)(void));

#define	__annotate(__ann)	__attribute__((annotate(__ann)))

#define	__rv_interrupt_handler_arg_resolved(__resolved_source,	\
    __resolved_priority)					\
	__annotate("rvp-isr-" #__resolved_source "@" #__resolved_priority)

#define	__rv_interrupt_handler(__source, __priority)	\
	__rv_interrupt_handler_arg_resolved(__source, __priority)

#define	__rv_register	__annotate("rvp-register") __section(".data.registers")

#endif /* _RV_INTR_EXPORTS_H_ */
