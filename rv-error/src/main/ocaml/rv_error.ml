open Base_renderer
open Error_t


type rv_error = Base_renderer.rv_error =
  | StackError of Error_t.stack_error
  | LocationError of Error_t.location_error


type t = Base_renderer.renderer


let user_home_location =
  try
    Sys.getenv "HOME"
  with Not_found ->
    prerr_endline "HOME variable not defined in the environment. Aborting" ;
    exit 2

let citations_location = "citations.csv"
let description_location = "descriptions"
let rv_suppress_location = Filename.concat user_home_location ".rvsuppress"
let kcc_report_csv_location = (* Filename.concat user_home_location ".kcc-report.csv" *)
  try
    Some (Sys.getenv "RV_MATCH_CSV_REPORT")
  with Not_found -> None
let kcc_report_json_location = (* Filename.concat user_home_location ".kcc-report.json" *)
  try
    Some (Sys.getenv "RV_MATCH_JSON_REPORT")
  with Not_found -> None


let load_resource (path : string) : string option =
  let p = OCamlRes.Path.of_string path in
  try
    Some (OCamlRes.Res.find p Rv_error_res.root)
  with Not_found -> None


let string_of_language (lang : language) : string =
  match lang with
  | `C -> ""
  | `CPP -> "++"


let abbr_of_error_category (category : error_category) : string =
  match category with
  | `ConditionallySupported -> "CND"
  | `ConstraintViolation -> "CV"
  | `IllFormed -> "ILF"
  | `ImplementationDefined lang -> "IMPL" ^ string_of_language lang
  | `ImplementationUndefined lang -> "IMPLUB" ^ string_of_language lang
  | `LintError -> "L"
  | `SyntaxError lang -> "SE" ^ string_of_language lang
  | `Undefined lang -> "UB" ^ string_of_language lang
  | `Underspecified lang -> "DR" ^ string_of_language lang
  | `Unspecified lang -> "USP" ^ string_of_language lang
  | `Unknown -> "UNK"


let get_real_error_id (error_id : string) (category : error_category) : string =
  abbr_of_error_category category ^ "-" ^ error_id


let get_top_frame (traces : stack_trace list) : frame option =
  match traces with
  | [] -> None
  | trace::_ -> match trace.components with
    | [] -> None
    | component::_ -> match component.frames with
      | [] -> None
      | frame::_ -> Some frame


let citations : Csv.t =
  match load_resource citations_location with
  | None -> failwith "Missing file 'citations.csv'"
  | Some text ->
    let i_str = Csv.of_string ~excel_tricks:true text in
    Csv.input_all i_str


let get_citations (error_id : string) : citation list =
  try
    let format_citation = function
      | [_; document; section; paragraph] -> {
          document = document;
          section = section;
          paragraph = if paragraph = "" then None else Some paragraph
        }
      | _ -> failwith "invalid citation"
    in
    List.map format_citation
      (List.filter (fun e -> List.hd e = error_id) citations)
  with e ->
    Rv_error_util.print_stack_trace e ;
    []


let get_description (error_id : string) : string =
  let resource = description_location ^ "/" ^ error_id ^ ".txt" in
  match load_resource resource with
  | None -> ""
  | Some description -> description


type suppress_state = {
  suppress_error_id : bool;
  suppress_system_header : bool;
  suppress_loc : bool;
  suppress_duplicate : bool;
}


let empty_suppress_state = {
  suppress_error_id = false;
  suppress_system_header = false;
  suppress_loc = false;
  suppress_duplicate = false;
}


let builtin_suppress : suppression list =
  let suppress_system_header = {
    suppress = true;
    condition = `SystemHeader true;
  } in
  let suppress_lint = {
    suppress = true;
    condition = `Category `LintError;
  } in
  let suppress_duplicate = {
    suppress = true;
    condition = `Duplicate true;
  } in
  [suppress_system_header; suppress_lint; suppress_duplicate]


let matches_glob (rel_file : string) (abs_file : string) (glob : string) : bool =
  try
    let working_dir = String.sub abs_file 0 (String.length abs_file - String.length rel_file) in
    let ret_code = Sys.command (String.concat " " (List.map Filename.quote ["rv-fileglob"; abs_file; working_dir; glob])) in
    ret_code = 0
  with e ->
    Rv_error_util.print_stack_trace e ;
    false


let matches_file (loc: location option) (f :string) : bool =
  match loc with
  | None -> false
  | Some loc -> matches_glob loc.rel_file loc.abs_file f


let matches_line (loc: location option) (l: line_spec) : bool =
  match loc with
  | None -> false
  | Some loc
  -> loc.line >= l.start_line
  && loc.line <= l.end_line
  && Some loc.rel_file = l.file


let matches_symbol (symbol : string option) (s : string) : bool =
  match symbol with
  | None -> false
  | Some symbol -> Str.string_match (Str.regexp s) symbol 0


let matches_system_header (loc : location option) (b : bool) =
  match loc with
  | None -> false
  | Some loc -> loc.system_header = b


let process_suppression (category : error_category) (error_id : string) (loc : location option) (symbol : string option) (is_duplicate : bool) (state : suppress_state) (suppression : suppression) =
  match suppression.condition with
  | `Category c when  c = category
  -> {state with suppress_error_id = suppression.suppress}
  | `Duplicate b when b = is_duplicate
  -> {state with suppress_duplicate = suppression.suppress}
  | `ErrorId e when e = error_id
  -> {state with suppress_error_id = suppression.suppress}
  | `File f when matches_file loc f
  -> {state with suppress_loc = suppression.suppress}
  | `Line l when matches_line loc l
  -> {state with suppress_loc = suppression.suppress}
  | `Symbol s when matches_symbol symbol s
  -> {state with suppress_loc = suppression.suppress}
  | `SystemHeader b when matches_system_header loc b
  -> {state with suppress_system_header = suppression.suppress}
  (* TODO(dwightguth): implement ifdef checking *)
  | _ -> state


let cache_suppress (loc : location option) : suppression list =
  match loc with
  | None -> []
  | Some loc ->
    try
      let path = Filename.concat (Filename.concat rv_suppress_location loc.abs_file) "ifdef.json" in
      if Sys.file_exists path
      then
        let data = Ag_util.Json.from_file Error_j.read_metadata path in
      data.suppressions
      else []
    with e ->
      Rv_error_util.print_stack_trace e ;
      []


let suppress (this : renderer) (category : error_category) (error_id : string) (top_frame : frame option) (error : rv_error) : bool =
  let (loc,symbol) =
    match top_frame with
    | None -> (None, None)
    | Some frame -> (frame.loc, Some frame.symbol)
  in
  let is_duplicate = is_previous_error this error in
  let process = List.fold_left (process_suppression category error_id loc symbol is_duplicate) in
  let state = List.fold_left process  empty_suppress_state
    [builtin_suppress; cache_suppress loc; this.data.suppressions]
  in state.suppress_error_id || state.suppress_system_header || state.suppress_loc || state.suppress_duplicate


let get_out_stream (this : renderer) (path : string option) : out_channel =
  match path with
  | None -> stderr
  | Some path ->
    try
      Hashtbl.find this.streams path
    with Not_found ->
      try
        let stream = open_out_gen [Open_creat; Open_text; Open_append] 0o640 path in
        Hashtbl.add this.streams path stream ;
        stream
      with e ->
        Rv_error_util.print_stack_trace e ;
        exit 2


let render_error_to_channel (r : renderer) (error : rv_error) (path : string option) : unit =
  try
    let stream = get_out_stream r path in
    r.render_impl r error stream
  with _ -> ()

let render_error_to_channels (this : renderer) (error : rv_error) : unit =
  let map f = function
    | Some _ as x -> f x
    | None -> ()
  in
  render_error_to_channel this error this.data.output ;
  map (render_error_to_channel (Csv_renderer.instance this) error) kcc_report_csv_location ;
  map (render_error_to_channel (Json_renderer.instance this) error) kcc_report_json_location


let render_stack_error (this : renderer) (error : stack_error * (rv_error -> rv_error)) : bool =
  let (error, update_error) = error in
  let top_frame = get_top_frame error.stack_traces in
  let error_id = get_real_error_id error.error_id error.category in
  let citations = get_citations error_id in
  let friendly_cat = string_of_error_category error.category in
  let long_desc = get_description error_id in
  if suppress this error.category error_id top_frame (StackError error)
  then false
  else
    let error =
      if this.render_local_vars
      then
        match update_error (StackError error) with
        | StackError error -> error
        | _ -> failwith "update_error: expected StackError output"
      else error
    in
    let rendered_error = { error with
                           error_id = error_id;
                           citations = citations;
                           friendly_cat = Some friendly_cat;
                           long_desc = Some long_desc;
                         }
    in
      render_error_to_channels this (StackError rendered_error) ;
      this.data.fatal_errors


let render_location_error (this : renderer) (error : location_error * (rv_error -> rv_error)) : bool =
  let (error, update_error) = error in
  let error_id = get_real_error_id error.error_id error.category in
  let citations = get_citations error_id in
  let friendly_cat = string_of_error_category error.category in
  let long_desc = get_description error_id in
  if suppress this error.category error_id (Some error.loc) (LocationError error)
  then false
  else
    let error =
      if this.render_local_vars
      then
        match update_error (LocationError error) with
        | LocationError error -> error
        | _ -> failwith "update_error: expected LocationError output"
      else error
    in
    let rendered_error = { error with
                           error_id = error_id;
                           citations = citations;
                           friendly_cat = Some friendly_cat;
                           long_desc = Some long_desc;
                         }
    in
      render_error_to_channels this (LocationError rendered_error) ;
      this.data.fatal_errors


let render_error (this : renderer) (error : rv_error * (rv_error -> rv_error)) : bool =
  let (error, update_error) = error in
  let fatal =
    match error with
    | StackError error -> render_stack_error this (error, update_error)
    | LocationError error -> render_location_error this (error, update_error)
  in
  add_previous_error this error ;
  fatal


let rv_issue_report_file : string option =
  try (Some (Sys.getenv "RV_ISSUE_REPORT"))
  with Not_found -> None


let file_extension (name : string) : string =
  try
    let i = String.rindex name '.' in
    String.sub name (i+1) (String.length name - i - 1)
  with Not_found -> ""


let format_of_extension (name : string) (def : format) : format =
  let s = String.lowercase_ascii (file_extension name) in
  if s = "json"
  then `JSON
  else
    if s = "csv"
    then `CSV
    else def


let create (metadata : metadata) : renderer =
  let data =
    match rv_issue_report_file with
    | None -> metadata
    | Some file ->
      let format = format_of_extension file metadata.format in
      let output =
        if not (Filename.is_relative file)
        then file
        else Filename.concat (Sys.getcwd ()) file
      in {metadata with format = format; output = Some output}
  in
  let renderer =
    {
      data = data;
      previous_errors = Hashtbl.create 256;
      render_impl = (fun _ -> failwith "not implemented");
      streams = Hashtbl.create 256;
      render_local_vars = false;
    }
  in
  List.iter (fun e -> add_previous_error renderer (rv_error_of_string e)) data.previous_errors ;
  match data.format with
  | `Console -> Console_renderer.instance renderer
  | `CSV -> Csv_renderer. instance renderer
  | `JSON -> Json_renderer.instance renderer
  | _ -> failwith "Unimplemented renderer"


let get_metadata (this : renderer) : metadata =
  let p_errors = Hashtbl.fold
    (fun err _ errs ->
      let err_str =
        match err with
        | StackError err -> Error_j.string_of_stack_error err
        | LocationError err -> Error_j.string_of_location_error err
      in err_str :: errs
    )
    this.previous_errors
    []
  in { this.data with previous_errors = p_errors }

let rv_error_of_string = Base_renderer.rv_error_of_string
