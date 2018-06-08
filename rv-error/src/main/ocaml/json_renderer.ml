open Base_renderer

let render_stack_error_impl (this : renderer) (error : Error_t.stack_error) (str : out_channel) : unit =
  Atdgen_runtime.Util.Json.to_channel Error_j.write_stack_error str error ;
  Printf.fprintf str "\n" ;
  flush str


let render_location_error_impl (this : renderer) (error : Error_t.location_error) (str : out_channel) : unit =
  Atdgen_runtime.Util.Json.to_channel Error_j.write_location_error str error ;
  Printf.fprintf str "\n" ;
  flush str

let render_impl (this : renderer) (error : rv_error) (str : out_channel) : unit =
  match error with
  | StackError error -> render_stack_error_impl this error str
  | LocationError error -> render_location_error_impl this error str


let instance (renderer : renderer) : renderer =
  Base_renderer.create_instance renderer ~local_vars:true render_impl
