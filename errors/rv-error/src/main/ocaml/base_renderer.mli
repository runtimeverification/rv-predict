type rv_error =
  | StackError of Error_t.stack_error
  | LocationError of Error_t.location_error


type renderer = {
  data : Error_t.metadata;
  previous_errors : (rv_error, unit) Hashtbl.t;
  render_impl : renderer -> rv_error -> out_channel -> unit;
  streams :  (string, out_channel) Hashtbl.t
}

val create_instance : renderer -> (renderer -> rv_error -> out_channel -> unit) -> renderer

val render_citation_url : Error_t.citation -> string
val string_of_error_category : Error_t.error_category -> string
val render_trace : Rv_error_util.rv_buffer -> Error_t.stack_trace -> unit
val render_loc : Rv_error_util.rv_buffer -> Error_t.location option -> unit

val rv_error_of_string : string -> rv_error
val is_previous_error : renderer -> rv_error -> bool
val add_previous_error : renderer -> rv_error -> unit
