# SVR Streaming System
Make use of object semantics in virtual reality video to optimize the power-consumption
and bandwidth usage on client-side.

## TODO
- [X] basic streaming system support using a manifest file to request for video segment
- [ ] fov logic
    - [ ] VRPlayer read a user fov file and then request video segment with the coordination
    - [ ] VRServer response to VRPlayer with fov or a full frame segment
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
- use intellij or
```
cd src/main/java
javac *.java
```

## License
MIT License

Copyright (c) [2018] [Chi-Chun, Chen]