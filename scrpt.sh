# !/bin/bash
# test multi threaded server

while true
do
	curl -X GET "localhost:7556/r.html?f=test05&sc=100&sr=50&wc=100&wr=50&coff=40&roff=40" &
	sleep 3
	curl -X GET "localhost:7556/r.html?f=test05&sc=150&sr=75&wc=150&wr=75&coff=30&roff=30" &
	sleep 3
	curl -X GET "localhost:7556/r.html?f=test05&sc=180&sr=90&wc=180&wr=90&coff=50&roff=50" &
	sleep 5
	curl -X GET "localhost:7556/r.html?f=test05&sc=200&sr=100&wc=200&wr=100&coff=60&roff=60" &
	sleep 2
	curl -X GET "localhost:7556/r.html?f=test05&sc=250&sr=125&wc=250&wr=125&coff=70&roff=70" &
	sleep 5
	curl -X GET "localhost:7556/r.html?f=test05&sc=500&sr=250&wc=500&wr=250&coff=60&roff=60" &
	sleep 3
	curl -X GET "localhost:7556/r.html?f=test05&sc=400&sr=200&wc=400&wr=200&coff=30&roff=30" &
	sleep 5
	curl -X GET "localhost:7556/r.html?f=test05&sc=1000&sr=500&wc=1000&wr=500&coff=20&roff=20" &
	sleep 10
done