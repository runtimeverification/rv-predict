class A {
public:
	A();
	virtual ~A();
};

A::~A() { }
A::A() { }

int
main(void)
{
	A *a = new A();
	delete a;
	return 0;
}
