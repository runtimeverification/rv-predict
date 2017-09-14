#ifndef EXP_H
#define EXP_H

struct exp {
  int operand1;
  int operand2;
  enum {
    ADD = 0,SUBTRACT,MULTIPLY,DIVIDE
  } operator;
};

int compute(struct exp e);

#endif

