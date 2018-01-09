open Base_renderer
open Error_t
open Rv_error_util


let render_id_and_category (buffer : rv_buffer) (category : error_category) (error_id : string) : unit =
  let cat = string_of_error_category category in
  add_line buffer   "" ;
  add_line buffer   "    " ;
  add_string buffer cat ;
  add_string buffer " (" ;
  add_string buffer error_id ;
  add_string buffer "):"


let render_citation (buffer : rv_buffer) (citation : citation) : unit =
  let url = render_citation_url citation in
  add_line buffer     "        see " ;
  add_string buffer   citation.document ;
  add_string buffer   " section " ;
  add_string buffer   citation.section ;
  (match citation.paragraph with
  | None -> ()
  | Some p ->
    add_char buffer   ':' ;
    add_string buffer p
  ) ;
  add_char buffer     ' ' ;
  add_string buffer   url


let render_stack_error_impl (this : renderer) (error : stack_error) (str : out_channel) : unit =
  let buffer = Rv_error_util.create_buffer 256 in
  add_line buffer   "" ;
  add_string buffer (Str.global_replace (Str.regexp "\\.$") "" error.description);
  add_char buffer ':' ;
  List.iter (render_trace buffer) error.stack_traces ;
  render_id_and_category buffer error.category error.error_id ;
  List.iter (render_citation buffer) error.citations ;
  add_line buffer   "";
  add_line buffer   "";
  Buffer.output_buffer str buffer.data ;
  flush str


let render_error_super_category (category : error_category) : string =
  match category with
  | `ImplementationDefined _ -> "warning"
  | `LintError -> "warning"
  | _ -> "error"


let render_location_error_impl (this : renderer) (error : location_error) (str : out_channel) : unit =
  let buffer = Rv_error_util.create_buffer 256 in
  add_line buffer   "" ;
  render_loc buffer error.loc.loc ;
  add_string buffer ": " ;
  add_string buffer (render_error_super_category error.category) ;
  add_string buffer ": " ;
  add_string buffer error.description ;
  render_id_and_category buffer error.category error.error_id ;
  List.iter (render_citation buffer) error.citations ;
  add_line buffer   "";
  add_line buffer   "";
  Buffer.output_buffer str buffer.data ;
  flush str


let render_impl (this : renderer) (error : rv_error) (str : out_channel) : unit =
  match error with
  | StackError error -> render_stack_error_impl this error str
  | LocationError error -> render_location_error_impl this error str


let instance (renderer : renderer) : renderer =
  Base_renderer.create_instance renderer render_impl
