# Sa2_seed_key
Kotlin implementation of the SA2SeedKey algorithm - ported from Python script here:
https://github.com/bri3d/sa2_seed_key

Compile using kotlin and use it like so:
```
java -jar sa2seedkey.jar DEADBEEF
Challenge: deadbeef
Response: 21957ba7
```

# encryptSimos.kt
Kotlin implementation of (today) the checksumming functions used on the calibration blocks.

Compile using kotlin and use it like so:
```
java -jar encryptSimos.jar ./test.bin
Input File: ./test.bin
Current checksum:      00 00 00 00 02 b7 71 1b
Calculated checksum:   00 00 00 00 02 b7 71 1b
Checksum matches!
Current ECM3:      a3 46 23 01 73 4b b9 58
Calculated ECM3:   a3 46 23 01 73 4b b9 58
ECM3 checksum matches!
```
