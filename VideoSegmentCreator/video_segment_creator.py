from argparse import ArgumentParser
import subprocess
import time
import numpy as np
import queue
import os

parser = ArgumentParser('Create segments for the input video')
parser.add_argument('video', help='video filename')
parser.add_argument('total', type=int, help='integer for total duration of the video')
parser.add_argument('output', help='output path')
parser.add_argument('name', help='filename')

args = parser.parse_args()

# compute start and end with fps=30
step = 0.5
step_str = '.5'
bias = False
index = 1
q = queue.Queue()

for i in np.arange(0.0, args.total, step):
    start = time.strftime('%H:%M:%S', time.gmtime(i))
    end = time.strftime('%H:%M:%S', time.gmtime(i+step))
    print('start', start, 'end', end)
    ext = args.video.split('.')[-1]
    if bias:
        start = str(start) + step_str
        bias = False
    else:
        end = str(end) + step_str
        bias = True
    if not os.path.exists(args.output):
        os.makedirs(args.output)
    output_path = os.path.join(args.output, args.name) + '_' + str(index) + '.' + ext
    index = index+1
    q.put(subprocess.Popen(['ffmpeg', '-i', args.video, '-ss', start, '-to', end, '-async', '1', output_path]))
    # wait
    if q.qsize() > 4:
        q.get().wait()
