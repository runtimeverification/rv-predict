open Error_t

(* TODO: files can contain colons and semicolons! this string parsing is not sound! *)

let rec print_chunks l1 l2 = match l1, l2 with
| hd1 :: tl1, hd2 :: tl2 -> hd1 ^ hd2 ^ print_chunks tl1 tl2
| [], l | l, [] -> String.concat "" l

let format_description fmt strs =
let fmt_chunks = Str.split_delim (Str.regexp "\\%s") fmt in
print_chunks fmt_chunks strs

let escaped_program = "'" ^ (Str.global_replace (Str.regexp "'") "'\\''" Sys.argv.(1)) ^ "'"

let rvsyms_raw str =
  let _out, _in = Unix.open_process ("rvsyms " ^ escaped_program) in
  output_string _in str;
  output_char _in '\n';
  close_out _in;
  let raw = try input_line _out with End_of_file -> "" in
  let status = Unix.close_process (_out, _in) in
  match status with Unix.WEXITED 0 -> raw | _ -> failwith "rvsyms returned error"

let rvsyms_field str =
  let raw = rvsyms_raw str in
  if raw = "" then str else
  let parts = Str.split_delim (Str.regexp ";;") raw in
  match parts with
  | loc :: symbol :: [] -> symbol ^ " at " ^ loc
  | _ -> failwith "could not parse field in rvsyms output"

let parse_loc loc =
  let parts = Str.split_delim (Str.regexp ":") loc in
  match parts with
  | file :: line :: [] -> {rel_file=file; abs_file=file; line=(int_of_string line); column=None; system_header=false}
  | file :: line :: column :: [] -> {rel_file=file; abs_file=file; line=(int_of_string line); column=Some(int_of_string column); system_header=false}
  | _ -> failwith "could not parse location in rvsyms output"

let rvsyms_frame str =
  let raw = rvsyms_raw str in
  let parts = Str.split_delim (Str.regexp ";") raw in
  match parts with
  | loc  :: symbol :: [] -> (symbol, Some (parse_loc loc))
  | _ -> (str, None)

let symbolize_field = function
| {address=a; frame1=None; frame2=None} -> rvsyms_field ("[0x" ^ a ^ "]")
| {address=a; frame1=Some {pc=pc; cfa=cfa}; frame2=None} -> rvsyms_field ("[0x" ^ a ^ " : 0x" ^ pc ^ "/0x" ^ cfa ^ "]")
| {address=a; frame1=Some {pc=pc1; cfa=cfa1}; frame2=Some {pc=pc2; cfa=cfa2}} -> rvsyms_field ("[0x" ^ a ^ " : 0x" ^ pc1 ^ "/0x" ^ cfa1 ^ " 0x" ^ pc2 ^ "/0x" ^ cfa2 ^ "]")
| {frame1=None; frame2=Some _} -> invalid_arg "malformed field json with only frame2"

let rvpsigname = function
|  1 -> "SIGHUP"
|  2 -> "SIGINT"
|  3 -> "SIGQUIT"
|  4 -> "SIGILL"
|  6 -> "SIGABRT"
|  8 -> "SIGFPE"
|  9 -> "SIGKILL"
| 10 -> "SIGUSR1"
| 11 -> "SIGSEGV"
| 12 -> "SIGUSR2"
| 13 -> "SIGPIPE"
| 14 -> "SIGALRM"
| 15 -> "SIGTERM"
| 17 -> "SIGCHLD"
| 18 -> "SIGCONT"
| 19 -> "SIGSTOP"
| _ -> failwith "could not determine name of signal"

let symbolize_component_field = function
| `Signal n -> rvpsigname n
| `Lock f -> symbolize_field f

let symbolize_format_str fmt fields =
let strs = List.map symbolize_component_field fields in
format_description fmt strs

let symbolize_lock raw : lock =
let (symbol, loc) = rvsyms_frame ("{0x" ^ raw.locked_at ^ "}") in
{id=symbolize_field raw.id; locked_at={symbol=symbol; loc=loc; locks=[]}}

let symbolize_frame raw : frame =
let (symbol, loc) = rvsyms_frame ("{0x" ^ raw.address ^ "}") in
{symbol=symbol; loc=loc; locks=List.map symbolize_lock raw.locks}

let symbolize_trace_component (raw : raw_trace_component) : trace_component =
{description=Some (symbolize_format_str raw.description_format raw.description_fields); frames=List.map symbolize_frame raw.frames}

let symbolize_trace (raw : raw_trace) : trace =
{components=List.map symbolize_trace_component raw.components; thread_id=raw.thread_id; thread_created_by=raw.thread_created_by; thread_created_at=(match raw.thread_created_at with None -> None | Some f -> Some (symbolize_frame f))}
let symbolize_format_str fmt fields =
let strs = List.map symbolize_field fields in
format_description fmt strs
let symbolize raw = 
{description=symbolize_format_str raw.description_format raw.description_fields; traces=List.map symbolize_trace raw.traces; category=raw.category; error_id=raw.error_id; citations=[]; friendly_cat=None; long_desc=None}

let lexer = Yojson.init_lexer ()
let buf = Lexing.from_channel stdin
let err = Error_j.read_raw_stack_error lexer buf
let symbolized = symbolize err
let () = print_string (Error_j.string_of_stack_error symbolized)
