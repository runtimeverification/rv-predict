type t


type rv_error = Base_renderer.rv_error =
  | StackError of Error_t.stack_error
  | LocationError of Error_t.location_error


val create : Error_t.metadata -> t
val render_error : t -> rv_error -> bool
val get_metadata : t -> Error_t.metadata
val rv_error_of_string : string -> rv_error
