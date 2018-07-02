# VROS Client Server
SVR stremaing system.

# Prepare Video Segments
```
mkdir tmp/
mkdir storage/rhino
cd storage
python3 segment.py rhino.mp4 100 rhino/output
cd ..
```

# Compilation
- use intellij or
```
cd src/main/java
javac *.java
```
