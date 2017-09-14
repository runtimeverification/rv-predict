open Base_renderer
open Error_t
open Rv_error_util


let default (d : 'a) (x : 'a option) : 'a =
  match x with
  | None -> d
  | Some x -> x


let render_citation (buffer : rv_buffer) (citation : citation) : unit =
  add_line buffer "" ;
  concat buffer ',' [
    citation.document;
    citation.section;
    default "" citation.paragraph;
    render_citation_url citation;
  ]


let get_common_fields (error_id : string) (description : string) (category : error_category) (citations : citation list) : string list =
  let cat = string_of_error_category category in
  let cit_buffer = create_buffer 256 in
  List.iter (render_citation cit_buffer) citations ;
  [error_id; description; cat; (Buffer.contents cit_buffer.data)]


let print_record (printer : out_channel) (strings : string list) : unit =
  try
    Csv.output_record (Csv.to_channel ~excel_tricks:true printer) strings ;
    flush printer
  with e ->
    Rv_error_util.print_stack_trace e ;
    exit 2


let render_stack_error_impl (this : renderer) (error : stack_error) (str : out_channel) =
  let common_fields = get_common_fields error.error_id error.description error.category error.citations in
  let uncommon_fields =
    match error.stack_traces with
    | [] -> [""; ""]
    | frame::traces ->
      let buf_frame = create_buffer 256 in
      render_trace buf_frame frame ;
      let buf_traces = create_buffer 256 in
      List.iter (render_trace buf_traces) traces ;
      [Buffer.contents buf_frame.data; Buffer.contents buf_traces.data]
  in
  let strings = List.append common_fields uncommon_fields in
  print_record str strings


let render_location_error_impl (this : renderer) (error : location_error) (str : out_channel) =
  let common_fields = get_common_fields error.error_id error.description error.category error.citations in
  let buf_loc = create_buffer 256 in
  render_loc buf_loc error.loc.loc ;
  let strings = List.append common_fields [""; Buffer.contents buf_loc.data] in
  print_record str strings

let render_impl (this : renderer) (error : rv_error) (str : out_channel) : unit =
  match error with
  | StackError error -> render_stack_error_impl this error str
  | LocationError error -> render_location_error_impl this error str


let instance (renderer : renderer) : renderer =
  Base_renderer.create_instance renderer render_impl
