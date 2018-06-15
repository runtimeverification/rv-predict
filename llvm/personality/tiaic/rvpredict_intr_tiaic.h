#include <stdint.h>

#include <rvpredict_intr.h>

#include <sys/param.h>

#include "nbcompat.h"

/* TI ARM Interrupt Controller (TIAIC) interrupt state */
struct _tiaic_state {
	uint64_t total_mask;
	uint32_t saved_hinlr1, saved_hinlr2;
};

typedef struct _tiaic_state tiaic_state_t;

/* TIAIC = TI ARM Interrupt Controller */

/* Register offset to index */
#define	__oti(__ofs)	((__ofs) / sizeof(uint32_t))

/* Register offsets */
#define	TIAIC_REVID	__oti(0x0000)	/* Revision Identification */
#define	TIAIC_CR	__oti(0x0004)	/* Control */
#define	TIAIC_GER	__oti(0x0010)	/* Global Enable */
#define	TIAIC_GNLR	__oti(0x001C)	/* Global Nesting Level */
#define	TIAIC_SISR	__oti(0x0020)	/* System Int. Status Indexed Set */
#define	TIAIC_SICR	__oti(0x0024)	/* System Int. Status Indexed Clear */
#define	TIAIC_EISR	__oti(0x0028)	/* System Int. Enable Indexed Set */
#define	TIAIC_EICR	__oti(0x002C)	/* System Int. Enable Indexed Clear */
#define	TIAIC_HIEISR	__oti(0x0034)	/* Host Interrupt Enable Indexed Set */
#define	TIAIC_HIEICR	__oti(0x0038)	/* Host Int. Enable Indexed Clear */
#define	TIAIC_VBR	__oti(0x0050)	/* Vector Base */
#define	TIAIC_VSR	__oti(0x0054)	/* Vector Size */
#define	TIAIC_VNR	__oti(0x0058)	/* Vector Null */
#define	TIAIC_GPIR	__oti(0x0080)	/* Global Prioritized Index */
#define	TIAIC_GPVR	__oti(0x0084)	/* Global Prioritized Vector */
#define	TIAIC_SRSR1	__oti(0x0200)	/* System Interrupt Status Raw/Set 1 */
#define	TIAIC_SRSR2	__oti(0x0204)	/* System Interrupt Status Raw/Set 2 */
#define	TIAIC_SRSR3	__oti(0x0208)	/* System Interrupt Status Raw/Set 3 */
#define	TIAIC_SRSR4	__oti(0x020C)	/* System Interrupt Status Raw/Set 4 */
#define	TIAIC_SECR1	__oti(0x0280)	/* System Int. Status Enabled/Clear 1 */
#define	TIAIC_SECR2	__oti(0x0284)	/* System Int. Status Enabled/Clear 2 */
#define	TIAIC_SECR3	__oti(0x0288)	/* System Int. Status Enabled/Clear 3 */
#define	TIAIC_SECR4	__oti(0x028C)	/* System Int. Status Enabled/Clear 4 */
#define	TIAIC_ESR1	__oti(0x0300)	/* System Interrupt Enable Set 1 */
#define	TIAIC_ESR2	__oti(0x0304)	/* System Interrupt Enable Set 2 */
#define	TIAIC_ESR3	__oti(0x0308)	/* System Interrupt Enable Set 3 */
#define	TIAIC_ESR4	__oti(0x030C)	/* System Interrupt Enable Set 4 */
#define	TIAIC_ECR1	__oti(0x0380)	/* System Interrupt Enable Clear 1 */
#define	TIAIC_ECR2	__oti(0x0384)	/* System Interrupt Enable Clear 2 */
#define	TIAIC_ECR3	__oti(0x0388)	/* System Interrupt Enable Clear 3 */
#define	TIAIC_ECR4	__oti(0x038C)	/* System Interrupt Enable Clear 4 */
/* Channel Map 0 - 25 */
#define	TIAIC_CMR(__idx)	__oti(0x0400 + (__idx) * 4)
#define	TIAIC_HIPIR1	__oti(0x0900)	/* Host Interrupt Prioritized Index 1 */
#define	TIAIC_HIPIR2	__oti(0x0904)	/* Host Interrupt Prioritized Index 2 */
#define	TIAIC_HINLR1	__oti(0x1100)	/* Host Interrupt Nesting Level 1 */
#define	TIAIC_HINLR2	__oti(0x1104)	/* Host Interrupt Nesting Level 2 */
#define	TIAIC_HIER	__oti(0x1500)	/* Host Interrupt Enable */
#define	TIAIC_HIPVR1	__oti(0x1600)	/* Host Int. Prioritized Vector 1 */
#define	TIAIC_HIPVR2	__oti(0x1604)	/* Host Int. Prioritized Vector 2 */

/* Register bitfields */

#define	TIAIC_REVID_REV		__BITS(31, 0)
#define	TIAIC_REVID_REV_DFLT	__SHIFTIN(0x4e82a900, TIAIC_REVID_REV)

#define TIAIC_CR_RSVD0		__BITS(31, 5)
#define TIAIC_CR_PRHOLDMODE	__BIT(4)
#define TIAIC_CR_NESTMODE_MASK	__BITS(3, 2)
#define	TIAIC_CR_NESTMODE_NONE		__SHIFTIN(0, TIAIC_CR_NESTMODE_MASK)
#define	TIAIC_CR_NESTMODE_INDIVIDUAL	__SHIFTIN(1, TIAIC_CR_NESTMODE_MASK)
#define	TIAIC_CR_NESTMODE_GLOBAL	__SHIFTIN(2, TIAIC_CR_NESTMODE_MASK)
#define	TIAIC_CR_NESTMODE_MANUAL	__SHIFTIN(3, TIAIC_CR_NESTMODE_MASK)
#define TIAIC_CR_RSVD1		__BITS(1, 0)

#define TIAIC_GER_RSVD0		__BITS(31, 1)
#define TIAIC_GER_ENABLE	__BIT(0)	/* read/write */

#define TIAIC_GNLR_OVERRIDE	__BIT(31)	/* read/write */
#define TIAIC_GNLR_RSVD0	__BITS(30, 9)
#define TIAIC_GNLR_NESTLVL	__BITS(8, 0)	/* read/write */
#define TIAIC_GNLR_NESTLVL_DFLT	__SHIFTIN(0x100, TIAIC_GNLR_NESTLVL)

#define	TIAIC_SISR_RSVD0	__BITS(31, 7)
#define	TIAIC_SISR_INDEX	__BITS(6, 0)	/* write-only */

#define	TIAIC_SICR_RSVD0	__BITS(31, 7)
#define	TIAIC_SICR_INDEX	__BITS(6, 0)	/* write-only */

#define	TIAIC_EISR_RSVD0	__BITS(31, 7)
#define	TIAIC_EISR_INDEX	__BITS(6, 0)	/* write-only */

#define	TIAIC_EICR_RSVD0	__BITS(31, 7)
#define	TIAIC_EICR_INDEX	__BITS(6, 0)	/* write-only */

#define	TIAIC_HIEISR_RSVD0	__BITS(31, 1)
#define	TIAIC_HIEISR_INDEX	__BIT(0)	/* write-only */

#define	TIAIC_HIEICR_RSVD0	__BITS(31, 1)
#define	TIAIC_HIEICR_INDEX	__BIT(0)	/* write-only */

#define	TIAIC_VBR_BASE		__BITS(31, 0)	/* read/write */

#define	TIAIC_VSR_RSVD0		__BITS(31, 8)
#define	TIAIC_VSR_SIZE		__BITS(7, 0)	/* read/write */

#define	TIAIC_VNR_NULL		__BITS(31, 0)	/* read/write */

#define	TIAIC_GPIR_NONE		__BIT(31)	/* read-only */
#define	TIAIC_GPIR_NONE_DFLT	__SHIFTIN(1, TIAIC_GPIR_NONE)
#define	TIAIC_GPIR_RSVD0	__BITS(30, 10)
#define	TIAIC_GPIR_PRI_INDX	__BITS(9, 0)	/* read-only */

#define	TIAIC_GPVR_ADDR		__BITS(31, 0)	/* read-only */

#define	TIAIC_SRSR123_RAW_STATUS	__BITS(31, 0)	/* read/write */

#define	TIAIC_SRSR4_RSVD0	__BITS(31, 5)
#define	TIAIC_SRSR4_RAW_STATUS	__BITS(4, 0)	/* read/write */

#define	TIAIC_SECR123_ENBL_STATUS	__BITS(31, 0)	/* read/write */

#define	TIAIC_SECR4_RSVD0	__BITS(31, 5)
#define	TIAIC_SECR4_ENBL_STATUS	__BITS(4, 0)	/* read/write */

#define	TIAIC_ESR123_ENABLE	__BITS(31, 0)	/* read/write */

#define	TIAIC_ESR4_RSVD0	__BITS(31, 5)
#define	TIAIC_ESR4_ENABLE	__BITS(4, 0)	/* read/write */

#define	TIAIC_ECR123_ENABLE	__BITS(31, 0)	/* read/write */

#define	TIAIC_ECR4_RSVD0	__BITS(31, 5)
#define	TIAIC_ECR4_ENABLE	__BITS(4, 0)	/* read/write */

#define	TIAIC_CMR_CHNL_MASK(__idx)	\
    __BITS(4 + 8 * (__idx), 8 * (__idx))	/* read/write */


#define	TIAIC_HIPIR_NONE	__BIT(31)	/* read-only */
#define	TIAIC_HIPIR_NONE_DFLT	__SHIFTIN(1, TIAIC_HIPIR_NONE)
#define	TIAIC_HIPIR_RSVD0	__BITS(30, 10)
#define	TIAIC_HIPIR_PRI_INDX	__BITS(9, 0)	/* read-only */

#define	TIAIC_HINLR_OVERRIDE	__BIT(31)	/* write-only */
#define	TIAIC_HINLR_RSVD0	__BITS(30, 9)
#define	TIAIC_HINLR_NESTLVL	__BITS(8, 0)	/* read/write */
#define	TIAIC_HINLR_NESTLVL_DFLT	__SHIFTIN(0x100, TIAIC_HINLR_NESTLVL)

#define	TIAIC_HIER_RSVD0	__BITS(31, 2)
#define	TIAIC_HIER_IRQ		__BIT(1)	/* read/write */
#define	TIAIC_HIER_FIQ		__BIT(0)	/* read/write */

#define	TIAIC_HIPVR_ADDR	__BITS(31, 0)	/* read-only */

/* System interrupt assignments */

#define TIAIC_COMMTX			 0
#define TIAIC_COMMRX			 1
#define TIAIC_NINT			 2
#define TIAIC_EVTOUT0			 3	/* PRUSS */
#define TIAIC_EVTOUT1			 4	/* PRUSS */
#define TIAIC_EVTOUT2			 5	/* PRUSS */
#define TIAIC_EVTOUT3			 6	/* PRUSS */
#define TIAIC_EVTOUT4			 7	/* PRUSS */
#define TIAIC_EVTOUT5			 8	/* PRUSS */
#define TIAIC_EVTOUT6			 9	/* PRUSS */
#define TIAIC_EVTOUT7			10	/* PRUSS */

/* EDMA3_0 Channel Controller 0 Shadow Region 0 Transfer Completion */
#define TIAIC_EDMA3_0_CC0_INT0		11

/* EDMA3_0 Channel Controller 0 Error */
#define TIAIC_EDMA3_0_CC0_ERRINT	12

/* EDMA3_0 Transfer Controller 0 Error */
#define TIAIC_EDMA3_0_TC0_ERRINT	13

#define TIAIC_EMIFA_INT			14	/* EMIFA */
#define TIAIC_IIC0_INT			15	/* I2C0 */
#define TIAIC_MMCSD0_INT0		16	/* MMCSD0 MMC/SD */
#define TIAIC_MMCSD0_INT1		17	/* MMCSD0 SDIO */
#define TIAIC_PSC0_ALLINT		18	/* PSC0 */
#define TIAIC_RTC_IRQS1_0		19	/* RTC */
#define TIAIC_SPI0_INT			20	/* SPI0 */
#define TIAIC_T64P0_TINT12		21	/* Timer64P0 (TINT12) */
#define TIAIC_T64P0_TINT34		22	/* Timer64P0 (TINT34) */
#define TIAIC_T64P1_TINT12		23	/* Timer64P1 (TINT12) */
#define TIAIC_T64P1_TINT34		24	/* Timer64P1 (TINT12) */
#define TIAIC_UART0_INT			25	/* UART0 */
#define TIAIC_RSVD0_INT			26	/* Reserved */
#define TIAIC_PROTERR			27	/* SYSCFG Protection Shared */
#define TIAIC_SYSCFG_CHIPINT0		28	/* SYSCFG CHIPSIG */
#define TIAIC_SYSCFG_CHIPINT1		29	/* SYSCFG CHIPSIG */
#define TIAIC_SYSCFG_CHIPINT2		30	/* SYSCFG CHIPSIG */
#define TIAIC_SYSCFG_CHIPINT3		31	/* SYSCFG CHIPSIG */

/* TBD 32 - 100 */

void tiaic_write(int, uint32_t);
uint32_t tiaic_read(int);
extern tiaic_state_t tiaic_state;
void __rvpredict_tiaic_handler(int);
int irq_to_channel(int);
