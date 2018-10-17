# SVR Streaming System
- This repository is for implementing the system mentioned in APSys '18 workshop paper: [Semantic-Aware Virtual Reality Video Streaming](https://dl.acm.org/citation.cfm?id=3265738).
- General idea
    - Make use of object semantics in virtual reality video to optimize the power-consumption and bandwidth usage on client-side.

## TODO
- [X] basic streaming system support using a manifest file to request for video segment
- [X] fov logic
    - [X] VRPlayer read a user fov file and then request video segment with the coordination
    - [X] VRServer response to VRPlayer with fov or a full frame segment
- [ ] gapless playback on TX2
    - [ ] Nvidia does not provide seamless playback for gst-play on TX2 so it's not able to implement this part right now...
- [ ] integrate with texture mapping for VR reprojection
- [X] native/optimized decoder
    - [X] port to tx2

## Usage
Run VRServer first and then launch VRPlayer. If there is any error, it might due to
the path of storage and the video segment name in the main function.

## Compilation
- This repository now has only been tested on ubuntu 16.04
- Install openjdk version >= 1.8
- Install maven
    - sudo apt-get install mvn
    - if mvn doesn't work, replace it with maven
    - `mvn package`
- Get S3 Key and put it in `~/.aws/credentials`
- VRServer
    - `java -jar vrserver.jar 1988 elephant SVR`
- VRPlayer
    - `java -jar vrplayer.jar localhost 1988 tmp rhino SVR`
    
## Implementation
- VRServer
    - Create manifest file using:
        - Object detection trace
        - File size of each video segment (both FULL and FOV)
    - Interact with VRPlayer using SVR-FOV protocol below
- VRPlayer
    - Use Gstreamer for hardware decoder on TX2
    - SVR-FOV protocol
- S3
    - VRPlayer use the manifest file that downloaded from VRServer to request the file on Amazon-S3

### SVR-FOV Protocol

| Tables        | VRPlayer                                         | VRServer (EC2)             | Video Storage (AWS S3) |
| ------------- |:------------------------------------------------:|:--------------------------:|:----------------------:|
| Step 0-1      | -                                                | Send manifest to VRPlayer  | -                      |
| Step 0-2      | Receive manifest from VRServer                   | -                          | -                      |
| Step 1        | Send sensor data (metadata) to VRServer          | -                          | -                      |
| Step 2        | -                                                | Receive sensor data (metadata) from VRPlayer | -                        |
| Step 3        | -                                                | Send size message of video segment to VRPlayer | -                      |
| Step 4        | Receive video segment size message from VRServer | -                          | -                      |
| Step 5        | -                                                | -                          | Send video segment to VRPlayer             |
| Step 6        | Receive video segment from S3              | - | -                      |
| Step 7 (no miss)  | END      | END | END                      |
| Step 7 (has miss) | -    | END | Send full size video segment to VRPlayer |
| Step 7 (if size == FULL) | END | END | END                      |
| Step 8 (has miss)       | Download full size video segment from S3        | END | END |

## License
MIT License

Copyright (c) [2018] [Chi-Chun, Chen]