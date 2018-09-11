open Error_t
open Rv_error_util


module StringSet = Set.Make(String)
module StringMap = Map.Make(String)


type rv_error =
  | StackError of stack_error
  | LocationError of location_error


type renderer = {
  data : Error_t.metadata;
  previous_errors : (rv_error, unit) Hashtbl.t;
  render_impl : renderer -> rv_error -> out_channel -> unit;
  streams :  (string, out_channel) Hashtbl.t;
  render_local_vars : bool;
}


let rv_error_of_string (json : string) : rv_error =
  try
     let error = Error_j.stack_error_of_string json in
     StackError error
  with ex ->
    let e = Printf.sprintf "%s\n%s" (format_exception ex) (Printexc.get_backtrace ()) in
    try
     let error = Error_j.location_error_of_string json in
     LocationError error
    with e2 ->
      Printf.fprintf stderr "%s\n%s\n" json e ;
      print_stack_trace e2 ;
      exit 2


let is_previous_error (this : renderer) (error : rv_error) : bool =
  Hashtbl.mem this.previous_errors error


let add_previous_error (r : renderer) (e : rv_error) : unit =
  Hashtbl.add r.previous_errors e ()


let create_instance (renderer : renderer) ?local_vars:(lv=false) (impl : renderer -> rv_error -> out_channel -> unit) : renderer =
  { renderer with render_impl = impl; render_local_vars = lv }


let string_of_error_category (category : error_category) : string =
  match category with
  | `ConditionallySupported -> "Conditionally-supported behavior"
  | `ConstraintViolation -> "Constraint violation"
  | `IllFormed -> "Ill-formed program"
  | `ImplementationDefined lang -> "Implementation defined behavior"
  | `ImplementationUndefined lang -> "Implementation-dependent undefined behavior"
  | `LintError -> "Possible unintended behavior"
  | `SyntaxError lang -> "Syntax error"
  | `Undefined lang -> "Undefined behavior"
  | `Underspecified lang -> "Behavior underspecified by standard"
  | `Unspecified lang -> "Unspecified value or behavior"
  | `Unknown -> "Unknown error"


let render_citation_url (citation : citation) : string =
  Uri.to_string (
    Uri.make
    ~scheme:"http"
    ~host:"rvdoc.org"
    ~path:("/" ^ citation.document ^ "/" ^ citation.section)
    ()
  )


type frame_output = {
  numElided : int;
  start : string;
}


let render_elided (buffer : rv_buffer) (numElided : int) : unit =
  if numElided = 0
  then ()
  else (
    add_line buffer      "        ... " ;
    add_string buffer    (string_of_int numElided) ;
    add_string buffer    " library frame" ;
    if numElided > 1
    then add_char buffer 's'
    else ()
  )


let render_loc (buffer : rv_buffer) (loc : location option) : unit =
  match loc with
  | None -> add_string buffer "<unknown>"
  | Some loc ->
    add_string buffer         loc.rel_file ;
    match loc.line with
    | None -> ()
    | Some line -> 
      add_char buffer           ':' ;
      add_string buffer         (string_of_int line) ;
      match loc.column with
      | None -> ()
      | Some column ->
        add_char buffer         ':' ;
        add_string buffer       (string_of_int column)


let render_lock (buffer : rv_buffer) (lock : lock) : unit =
  add_line buffer   "      - locked " ;
  add_string buffer lock.id ;
  add_string buffer   " in " ;
  add_string buffer lock.locked_at.symbol ;
  add_string buffer   " at " ;
  render_loc buffer lock.locked_at.loc


let render_frame (buffer : rv_buffer) (state : frame_output) (frame : frame) : frame_output =
  if frame.elided
  then { numElided=state.numElided + 1; start="    in" }
  else (
    render_elided buffer state.numElided ;
    add_line buffer   "    " ;
    add_string buffer state.start ;
    add_char buffer   ' ' ;
    add_string buffer frame.symbol ;
    add_string buffer   " at " ;
    render_loc buffer frame.loc ;
    List.iter (render_lock buffer) frame.locks ;
    {
      numElided = 0;
      start = "    in";
    }
  )


let render_component (buffer : rv_buffer) (component : stack_trace_component) : unit =
  (match component.description with
  | None -> ()
  | Some d ->
    add_line buffer   "    " ;
    add_string buffer d
  ) ;
  let start_state = { numElided = 0; start = "  > in" } in
  let state = List.fold_left (render_frame buffer) start_state component.frames in
  render_elided buffer state.numElided


let render_trace (buffer : rv_buffer) (trace : stack_trace) : unit =
  List.iter (render_component buffer) trace.components ;
  match trace.thread_id with
  | None -> ()
  | Some thread_id ->
    match trace.thread_created_by with
    | None ->
      add_line buffer   "    Thread " ;
      add_string buffer thread_id ;
      add_string buffer " is the main thread" ;
      add_line buffer ""
    | Some by ->
      add_line buffer   "    Thread " ;
      add_string buffer thread_id ;
      add_string buffer " created by thread ";
      add_string buffer by ;
      match trace.thread_created_at with
      | None -> add_line buffer ""
      | Some frame ->
        let start_state = { numElided = 0; start = "  > in" } in
        ignore (render_frame buffer start_state frame) ;
  add_line buffer ""
