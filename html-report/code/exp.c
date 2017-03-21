#include<stdlib.h>

#include "exp.h"

int compute(struct exp e) {
  switch(e.operator) {
    case ADD:
      return e.operand1 + e.operand2;
    case SUBTRACT:
      return e.operand1 - e.operand2;
    case MULTIPLY:
      return e.operand1 * e.operand2;
    case DIVIDE:
      return e.operand1 / e.operand2;
    default:
      abort();
  }
}

