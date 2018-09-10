open Error_t

let abbreviation = ""

let rec first_differing_index (s1:string) (s2:string) =
  let m = min (String.length s1) (String.length s2) in
  let rec f i =
    if i >= m
      then m
      else if s1.[i] != s2.[i]
        then i
        else f (i+1)
  in f 0

let common_prefix (s1:string) (s2:string) =
  let i = first_differing_index s1 s2 in
  String.sub s1 0 i

let rec update_prefix_frame (prefix:string option) (frame: frame) =
  match frame.loc with
  | None -> List.fold_left update_prefix_lock prefix frame.locks
  | Some loc ->
      List.fold_left update_prefix_lock (match prefix with
        | None -> Some loc.abs_file
        | Some p -> Some (common_prefix p loc.abs_file)) frame.locks
and update_prefix_lock (prefix:string option) (lock: lock) =
  update_prefix_frame prefix lock.locked_at
;;

let rec remove_prefix_frame (prefix:int) (frame: frame) =
    match frame.loc with
    | None -> frame
    | Some loc ->
        let abs_file = loc.abs_file in
        let no_prefix = String.sub abs_file prefix (String.length abs_file - prefix) in
        {frame with loc=Some {loc with rel_file= abbreviation ^ no_prefix}; locks=List.map (remove_prefix_lock prefix) frame.locks}
and remove_prefix_lock (prefix: int) (lock: lock) =
  {lock with locked_at=remove_prefix_frame prefix lock.locked_at}
;;

let update_prefix_component (prefix:string option) (c: stack_trace_component) : string option =
  List.fold_left update_prefix_frame prefix c.frames

let remove_prefix_component (prefix:int) (c: stack_trace_component) : stack_trace_component =
  {c with frames=List.map (remove_prefix_frame prefix) c.frames}

let update_prefix_trace (prefix:string option) (tr: stack_trace) =
  let prefix = List.fold_left update_prefix_component prefix tr.components in
  match tr.thread_created_at with
  | None -> prefix
  | Some frame -> update_prefix_frame prefix frame

let remove_prefix_trace (prefix:int) (tr: stack_trace) =
  let created =
    match tr.thread_created_at with
    | None -> None
    | Some frame -> Some (remove_prefix_frame prefix frame)
  in
  {tr with components=List.map (remove_prefix_component prefix) tr.components; thread_created_at=created}

let update_prefix_error (prefix:string option) (err: stack_error) =
  List.fold_left update_prefix_trace prefix err.stack_traces

let remove_prefix_error (prefix:int) (err: stack_error) =
  {err with stack_traces=List.map (remove_prefix_trace prefix) err.stack_traces}


let () = let rlist = ref [] in
  try
  while true do
    let line = input_line stdin in
    let err = Error_j.stack_error_of_string line in
    rlist := err :: !rlist
  done
with End_of_file ->
  let prefix = List.fold_left update_prefix_error None !rlist in
  let update_fn =
    match prefix with
    | None -> (fun x -> x)
    | Some p ->
      try
        let i = String.rindex p '/' in
        remove_prefix_error (i + 1)
      with Not_found -> fun x -> x
  in
  List.iter print_endline (List.map Error_j.string_of_stack_error (List.map update_fn !rlist))
