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
python3 segment.py rhino.mp4 100 rhino/output
cd ..
```

## Compilation
- use intellij or
```
cd src/main/java
javac *.java
```

## License
MIT License

Copyright (c) [2018] [Chi-Chun, Chen]