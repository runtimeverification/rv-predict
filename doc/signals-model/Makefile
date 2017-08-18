all:
	latexmk -pdf --shell-escape signals-model.tex
	latexmk -c
	rm -rf _markdown_signals-model/
	rm -f signals-model.bbl
	rm -f signals-model.markdown.lua
	rm -f signals-model.markdown.out

clean:
	rm -f signals-model.pdf