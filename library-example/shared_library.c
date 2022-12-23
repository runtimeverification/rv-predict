__attribute__((visibility("default")))
void shared_call( void (*f) () ) {
    f();
}
