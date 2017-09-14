type rv_buffer = {
  data : Buffer.t;
  first : bool ref;
}

val format_exception : exn -> string
val print_stack_trace : exn -> unit

val create_buffer : int -> rv_buffer
val add_line : rv_buffer -> string -> unit
val add_string : rv_buffer -> string -> unit
val add_char : rv_buffer -> char -> unit
val concat : rv_buffer -> char -> string list -> unit
