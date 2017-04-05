# !/bin/bash
# test multi threaded server

a=0

modelFilename="file"
scene_columns=123
scene_rows=123
window_columns=123
window_rows=123
column_offset=50
row_offset=50

for a in 0 1 2 3 4 5
do
	( a="$((a +1))" && curl -X GET "http://localhost:8000/test?f="$modelFilename"&sc="$scene_columns"&sr="$scene_rows"&wc="$window_columns"&wr="$window_rows"&coff="$column_offset"&roff="$row_offset"" && echo "Request $a" ) &
done 