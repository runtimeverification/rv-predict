open Error_t
open Arg

let symbolize_flag = ref true
and args = ref []
and pgm = ref ""

(* TODO: files can contain colons and semicolons! this string parsing is not sound! *)

let rec print_chunks l1 l2 = match l1, l2 with
| hd1 :: tl1, hd2 :: tl2 -> hd1 ^ hd2 ^ print_chunks tl1 tl2
| [], l | l, [] -> String.concat "" l

let format_description fmt strs =
let fmt_chunks = Str.split_delim (Str.regexp "\\%s") fmt in
print_chunks fmt_chunks strs

let rvsyms_raw str = 
  let escaped_program = "'" ^ (Str.global_replace (Str.regexp "'") "'\\''" !pgm) ^ "'" in
  let _out, _in = Unix.open_process ("rvsyms " ^ escaped_program) in
  output_string _in str;
  output_char _in '\n';
  close_out _in;
  let raw = try input_line _out with End_of_file -> "" in
  let status = Unix.close_process (_out, _in) in
  match status with Unix.WEXITED 0 -> raw | _ -> failwith "rvsyms returned error"

let rvsyms_field str =
  if not !symbolize_flag then str else
  let raw = rvsyms_raw str in
  if raw = "" then str else
  let parts = Str.split_delim (Str.regexp ";") raw in
  match parts with
  | loc :: _ :: _  :: symbol :: [] -> "`" ^ symbol ^ "` at " ^ loc
  | loc :: _  :: symbol :: [] -> "`" ^ symbol ^ "` at " ^ loc
  | _ -> failwith "could not parse field in rvsyms output"

let parse_loc loc =
  let parts = Str.split_delim (Str.regexp ":") loc in
  match parts with
  | file :: [] -> {rel_file=file; abs_file=file; line=None; column=None; system_header=false}
  | file :: line :: [] -> {rel_file=file; abs_file=file; line=Some(int_of_string line); column=None; system_header=false}
  | file :: line :: column :: [] -> {rel_file=file; abs_file=file; line=Some(int_of_string line); column=Some(int_of_string column); system_header=false}
  | _ -> failwith "could not parse location in rvsyms output"

let rvsyms_frame str =
  if not !symbolize_flag then (str, None) else
  let raw = rvsyms_raw str in
  let parts = Str.split_delim (Str.regexp ";") raw in
  match parts with
  | loc  :: symbol :: [] -> (symbol, Some (parse_loc loc))
  | _ -> (str, None)

let symbolize_field (c: raw_field) : string = match c with {address=a} -> rvsyms_field a

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
| 20 -> "SIGTSTP"
| 21 -> "SIGTTIN"
| 22 -> "SIGTTOU"
| 23 -> "SIGURG"
| 24 -> "SIGXCPU"
| 25 -> "SIGXFSZ"
| 26 -> "SIGVTALRM"
| 27 -> "SIGPROF"
| 28 -> "SIGWINCH"
| 29 -> "SIGIO"
| 30 -> "SIGPWR"
| 31 -> "SIGSYS"
| n -> "SIG" ^ (string_of_int n)

let symbolize_component_field = function
| `Signal n -> rvpsigname n
| `Lock f -> symbolize_field f

let symbolize_format_str fmt fields =
let strs = List.map symbolize_component_field fields in
format_description fmt strs

let symbolize_lock raw : lock =
let (symbol, loc) = rvsyms_frame raw.locked_at in
{id=symbolize_field raw.id; locked_at={symbol=symbol; loc=loc; locks=[]; elided=false; local_vars=[]}}

let symbolize_frame raw : frame =
let (symbol, loc) = rvsyms_frame raw.address in
{symbol=symbol; loc=loc; locks=List.map symbolize_lock raw.locks; elided=false; local_vars=[]}

let symbolize_trace_component (raw : raw_stack_trace_component) : stack_trace_component =
{description=Some (symbolize_format_str raw.description_format raw.description_fields); frames=List.map symbolize_frame raw.frames}

let symbolize_trace (raw : raw_stack_trace) : stack_trace =
{components=List.map symbolize_trace_component raw.components; thread_id=raw.thread_id; thread_created_by=raw.thread_created_by; thread_created_at=(match raw.thread_created_at with None -> None | Some f -> Some (symbolize_frame f))}
let symbolize_format_str fmt fields =
let strs = List.map symbolize_field fields in
format_description fmt strs
let symbolize raw =
{description=symbolize_format_str raw.description_format raw.description_fields; stack_traces=List.map symbolize_trace raw.stack_traces; category=raw.category; error_id=raw.error_id; citations=[]; friendly_cat=None; long_desc=None}

let magic = "[RV-Predict]"
let magic_len = String.length magic

let specs =
[
( "-S", Clear symbolize_flag, "do not symbolize")
]

let prog = Sys.argv.(0)

let errx fmt = Printf.eprintf (format_of_string("%s: ") ^^ fmt) prog

let () = try
  parse specs (fun arg -> args := List.append !args [arg]) (Printf.sprintf "usage: %s [-S] <program>" prog);
  (match !args with
    arg :: [] -> pgm := arg
  | [] -> (errx "Too few arguments\n"; exit 1)
  | _ -> (errx "Too many arguments\n"; exit 1));
  while true do
    let line = input_line stdin in
    if String.length line >= magic_len && String.sub line 0 magic_len = magic then prerr_endline line else
    let err = Error_j.raw_stack_error_of_string line in
    let symbolized = symbolize err in
    let s = Error_j.string_of_stack_error symbolized in
    print_endline s
  done
with End_of_file -> ()
