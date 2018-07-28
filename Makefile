CC=gcc
CFLAG=-g `pkg-config --cflags --libs gstreamer-1.0`
BINARY=render

all:
	$(CC) render.c $(CFLAG) -o $(BINARY) 

clean:
	rm $(BINARY)
