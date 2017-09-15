open Error_t

let () =
  let metadata : Error_t.metadata =
    {
      suppressions = [{ condition = `Category `LintError; suppress = false }] ;
      message_length = 80 ;
      format = `Console ;
      previous_errors = [] ;
      fatal_errors = false ;
      rv_error = "" ;
      output = None ;
    }
  in
  let renderer = Rv_error.create metadata in
  let data = Rv_error.get_metadata renderer in
  Ag_util.Json.to_file Error_j.write_metadata "metadata.json" data

