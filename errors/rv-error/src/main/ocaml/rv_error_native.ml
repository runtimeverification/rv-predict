open Error_t

let rec read_stream ic =
  try let line = input_line ic in
    line ^ read_stream ic
  with End_of_file ->
    ""


let run (data_file : string) =
  let metadata = Ag_util.Json.from_file Error_j.read_metadata data_file in
  try while true do begin
    let json = input_line stdin in 
    if (String.trim json) = "" then exit 0 else
    let renderer = Rv_error.create metadata in
    let error = Rv_error.rv_error_of_string json in
    let is_fatal = Rv_error.render_error renderer (error, fun x -> x) in
  (*  let data = Rv_error.get_metadata renderer in
    Ag_util.Json.to_file Error_j.write_metadata data_file { data with previous_errors = json :: data.previous_errors} ; *)
    if is_fatal then exit 1
  end done with End_of_file -> ()

let () =
  if Array.length Sys.argv < 2
  then (prerr_endline "Mandatory metadata file missing" ; exit 1)
  else
  let arg = Sys.argv.(1) in
  run arg
