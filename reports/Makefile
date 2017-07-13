rvpsymbolize-json: $(RV_ERROR_DIR)/error_t.mli $(RV_ERROR_DIR)/error_t.ml $(RV_ERROR_DIR)/error_j.mli $(RV_ERROR_DIR)/error_j.ml rvpsymbolize_json.ml
	ocamlfind opt -I $(RV_ERROR_DIR) $^ -o rvpsymbolize-json -package atdgen -linkpkg -package unix
