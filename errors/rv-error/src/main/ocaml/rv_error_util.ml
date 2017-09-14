let format_exception (e : exn) : string =
  Printf.sprintf "Exception: %s\n" (Printexc.to_string e)


let print_stack_trace (e : exn) : unit =
    Printf.fprintf stderr "%s" (format_exception e) ;
    Printexc.print_backtrace stderr


type rv_buffer = {
  data : Buffer.t;
  first : bool ref;
}


let create_buffer (size : int) : rv_buffer =
  {
    data = Buffer.create size ;
    first = ref true ;
  }


let add_line (buffer : rv_buffer) (line : string) : unit =
  (if !(buffer.first)
  then  buffer.first := false
  else Buffer.add_char buffer.data '\n') ;
  Buffer.add_string buffer.data line


let add_char (buffer : rv_buffer) (chr : char) : unit =
  Buffer.add_char buffer.data chr


let add_string (buffer : rv_buffer) (str : string) : unit =
  Buffer.add_string buffer.data str


let concat (buffer : rv_buffer) (sep : char) (l : string list) : unit =
  match l with
  | [] -> ()
  | h :: t ->
    buffer.first := false ;
    Buffer.add_string buffer.data h ;
    List.iter
      (fun s ->
        Buffer.add_char buffer.data sep ;
        Buffer.add_string buffer.data s)
      t
