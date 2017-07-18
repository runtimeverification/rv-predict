open Error_t

let rec trim_main frames = match frames with
| [] -> []
| frame :: tail -> 
    if frame.symbol = "main" then 
      [frame] 
    else
      frame :: (trim_main tail)

let magic = "__rvpredict"
let magic_len = String.length magic

let rec trim_rvp frames = match frames with
| [] -> []
| frame :: tail ->
    if String.length frame.symbol >= magic_len 
        && String.sub frame.symbol 0 magic_len = magic then
      trim_rvp frames
    else
      frame :: (trim_rvp tail)

let library_frames_regex = Str.regexp "\\(/usr/include\\)\\|\\(/usr/local/include\\)"

let escaped_shell s = "'" ^ (Str.global_replace (Str.regexp "'") "'\\''" s) ^ "'"

let realpath path =
  let escaped = escaped_shell path in
  let _in = Unix.open_process_in ("realpath -ms " ^ escaped) in
  let result = input_line _in in
  let status = Unix.close_process_in _in in
  match status with Unix.WEXITED 0 -> result | _ -> path

let is_library_frame (frame: frame) = match frame.loc with 
| None -> false
| Some loc -> 
    let canonical_path = realpath loc.abs_file in
    Str.string_match library_frames_regex canonical_path 0

let rec trim_library frames = match frames with
| [] -> []
| frame :: [] ->
    if is_library_frame frame then 
      {frame with elided=true} :: []
    else
      frame :: []
| frame :: next :: tail -> 
    if is_library_frame frame && is_library_frame next then 
      {frame with elided=true} :: trim_library (next :: tail)
    else
      frame :: (trim_library (next :: tail))

let trim_frames frames =
  trim_main (trim_rvp (trim_library frames)) 

let trim_component (c: trace_component) : trace_component = 
{c with frames=trim_frames c.frames}

let trim_trace (tr: trace) =
{tr with components=List.map trim_component tr.components}

let trim_error (err: stack_error) =
{err with traces=List.map trim_trace err.traces}

let is_real_trace (tr: trace) = match tr with
| {components={frames=_ :: _} :: _} -> true
| _ -> false

let is_real_race (err: stack_error) = match err with
{traces=traces} -> List.fold_left (||) false (List.map is_real_trace traces)

let () = try
  while true do
    let line = input_line stdin in
    let err = Error_j.stack_error_of_string line in
    let trimmed = trim_error err in
    if is_real_race trimmed then begin
      print_string (Error_j.string_of_stack_error trimmed);
      print_newline ()
    end
  done
with End_of_file -> ()
