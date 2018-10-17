import os

fc = 0
fs = 0.0

with open('latency.txt') as f:
	for l in f:
		fc = fc + 1
		fs = fs + float(l)

print('count:', fc, 'avg:', fs / fc, 'sec')
