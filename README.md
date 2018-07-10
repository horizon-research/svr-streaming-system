# SVR Streaming System
Make use of object semantics in virtual reality video to optimize the power-consumption
and bandwidth usage on client-side.

## TODO
- [X] basic streaming system support using a manifest file to request for video segment
- [X] fov logic
    - [X] VRPlayer read a user fov file and then request video segment with the coordination
    - [X] VRServer response to VRPlayer with fov or a full frame segment
- [ ] Write script for creating video segments
    - [ ] Full size video segment
    - [ ] FOV video segment
- [ ] efficient gui 
    - [ ] read [this](https://pavelfatin.com/low-latency-painting-in-awt-and-swing/)
- [ ] native/optimized decoder
    - [ ] port to tx2
    - [ ] benchmark

## Prepare Video Segments
```
mkdir tmp/
mkdir storage/rhino
cd storage
# this may take around 10 to 20 mintues, so be patient...
python3 segment.py rhino.mp4 100 rhino/output
cd ..
```

## Usage
Run VRServer first and then launch VRPlayer. If there is any error, it might due to
the path of storage and the video segment name in the main function.

## Compilation
- use intellij for now since it deal with maven for deps for me

## Implementation
- VRServer
    - Create manifest file using:
        - Object detection trace
        - File size of each video segment (both FULL and FOV)
    - Interact with VRPlayer using SVR-FOV protocol below
- VRPlayer
    - GUI (main thread)
        - Pop image from a concurrent queue every `x` millisecond and render it, do nothing if the queue is empty
    - Network + decode (worker thread)
        - Interact with VRServer using SVR-FOV protocol below
        - Decode is now waiting for the network stuff (intuitive because we cannot decode until the segment has been downloaded)

### SVR-FOV Protocol

| Tables        | VRPlayer                                         | VRServer                   |
| ------------- |:------------------------------------------------:| --------------------------:|
| Step 0-1      | -                                                | Send manifest to VRPlayer  |
| Step 0-2      | Receive manifest from VRServer                   | -                          |
| Step 1        | Send sensor data (metadata) to VRServer          | -                          |
| Step 2        | -                                                | Receive sensor data (metadata) from VRPlayer |
| Step 3        | -                                                | Send size message of video segment to VRPlayer |
| Step 4        | Receive video segment size message from VRServer | -                          |
| Step 5        | -                                                | Send video segment to VRPlayer |
| Step 6        | Receive video segment from VRServer              | - |
| Step 7 (if size == FOV && no miss)  | Send GOOD to VRServer      | - |
| Step 7 (if size == FOV && has miss) | Send BAD to VRServer       | - |
| Step 7 (if size == FULL)            | END                        | END |
| Step 8        | -                                                | Receive GOOD/BAD from VRPlayer |
| Step 9 (if server receive BAD)      | - | Send full size video segment to VRPlayer |
| Step 9 (if server receive GOOD)     | END | END |
| Step 10       | Download full size video segment from VRServer | END |

## License
MIT License

Copyright (c) [2018] [Chi-Chun, Chen]