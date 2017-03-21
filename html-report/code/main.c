#include<stdio.h>
#include<stdlib.h>
#include<string.h>

#include "exp.h"

char ops[4] = {'+', '-', '*', '/'};

int get_operator(char c) {
  switch (c) {
  case '+':
    return 0;
  case '-':
    return 1;
  case '*':
    return 2;
  case '/':
    return 3;
  default:
    return -1;
  }
}

int main(int argc, char *argv[])
{
  struct exp buf;

  if (argc != 4) {
    fprintf(stderr, "Usage: %s NUM OP NUM (argc = %d)\n", argv[0], argc);
    exit(EXIT_FAILURE);
  }
  if (strlen (argv[2]) != 1 ||
     ((buf.operator = get_operator(argv[2][0])) < 0)) {
    fprintf(stderr, "Supported operators are + - * /\n");
    exit(EXIT_FAILURE);
  }

  buf.operand1 = atoi(argv[1]);
  buf.operand2 = atoi(argv[3]);

  int res = compute(buf);
  printf("%d %c %d = %d\n", buf.operand1, ops[buf.operator], buf.operand2, res);
  return 0;
}
