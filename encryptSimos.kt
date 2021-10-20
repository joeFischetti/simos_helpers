import java.io.*;
import java.nio.charset.StandardCharsets;

fun main(args: Array<String>) {
    var filename: String

	if(args.size == 1){
		filename = args[0]
	}
	else{
        filename = "./test.bin"
	}

    println("Input File: " + filename)

    var bin = File(filename).readBytes()

    println("Running checksumSimos18")
    checksumSimos18(bin)

    println("Running checksumECM3")
    checksumECM3(bin)

    println("Compressing file")
    var compressedBin = encodeLZSS(bin)

    println("  Uncompressed size:   " + bin.size)
    println("  Compressed size:     " + compressedBin.size)
}

fun checksumSimos18(bin: ByteArray): ByteArray{
    var currentChecksum = bin.copyOfRange(0x300, 0x308)
	var offset = (0xA0800000).toUInt()
	var startAddress1 = byteArrayToInt(bin.copyOfRange(0x30c, 0x30c + 4).reversedArray()).toUInt() - offset
	var endAddress1 = byteArrayToInt(bin.copyOfRange(0x310, 0x310 + 4).reversedArray()).toUInt() - offset
	var startAddress2 = byteArrayToInt(bin.copyOfRange(0x314, 0x314 + 4).reversedArray()).toUInt() - offset
	var endAddress2 = byteArrayToInt(bin.copyOfRange(0x318, 0x318 + 4).reversedArray()).toUInt() - offset


    var checksumData: ByteArray = bin.copyOfRange(startAddress1.toInt(), endAddress1.toInt() + 1) + bin.copyOfRange(startAddress2.toInt(), endAddress2.toInt() + 1)

	var polynomial = 0x4c11db7;
	var crc = 0x00000000;
	
	for (c in checksumData)
	{
		for (j in 7 downTo 0)
		{
			var z32: Byte = (crc ushr 31).toByte()
			crc = crc shl 1;
               var test = ((c.toUInt() shr j) and 1.toUInt()) xor z32.toUInt()
			if (test.toInt() > 0) {
				crc = crc xor polynomial;
			}

			crc = crc and 0xffffffff.toInt();

		}
	}

    var checksumCalculated = byteArrayOf(0x0.toByte(), 0x0.toByte(), 0x0.toByte(), 0x0.toByte()) + intToByteArray(crc).reversedArray()
    println("  Current checksum:      " + currentChecksum.toHex())
	println("  Calculated checksum:   " + checksumCalculated.toHex())


    if(currentChecksum contentEquals checksumCalculated){
        println("  Checksum matches!")
    }
    else{
        println("  Checksum doesn't match!")
    }

    return(bin)
}

fun checksumECM3(bin: ByteArray): ByteArray{
    var startAddress = 55724
    var endAddress = 66096

    var checksumLocation = 0x400;
    var checksumCurrent = bin.copyOfRange(checksumLocation, checksumLocation + 8)

    //Starting Value
    var checksum = byteArrayToInt(bin.copyOfRange(checksumLocation + 8, checksumLocation + 12).reversedArray()).toULong() shl 32;
    checksum += byteArrayToInt(bin.copyOfRange(checksumLocation + 12, checksumLocation + 16).reversedArray()).toUInt()

    for(i in (startAddress)..(endAddress - 1) step 4){
        checksum += byteArrayToInt(bin.copyOfRange(i, i+4).reversedArray()).toUInt()
    } 

    var checksumCalculated = intToByteArray((checksum shr 32).toInt()).reversedArray() + intToByteArray((checksum.toInt() and 0xFFFFFFFF.toInt())).reversedArray()
    println("  Current ECM3:      " + checksumCurrent.toHex())
    println("  Calculated ECM3:   " + checksumCalculated.toHex())

    if(checksumCurrent contentEquals checksumCalculated){
        println("  ECM3 checksum matches!")
    }
    else{
        println("  ECM3 checksum doesn't match!")
    }

    return(bin)
}

fun byteArrayToInt(data: ByteArray): Int {
    return (data[3].toUByte().toInt() shl 0) or
           (data[2].toUByte().toInt() shl 8) or
           (data[1].toUByte().toInt() shl 16) or
           (data[0].toUByte().toInt() shl 24)
}

fun intToByteArray(data: Int): ByteArray {
    return byteArrayOf(
        (data shr 24).toByte(),
        (data shr 16).toByte(),
        (data shr 8).toByte(),
        (data shr 0).toByte()
    )
}


fun encodeLZSS(input: ByteArray, maxSlidingWindowSize: Int = 1023): ByteArray {

    var searchBuffer: ByteArray = byteArrayOf()
    var checkCharacters: ByteArray = byteArrayOf()
    var output: ByteArray = byteArrayOf()

    var i = 0
    for(char in input){
        checkCharacters = checkCharacters + byteArrayOf(char)
        var index = searchBuffer.findFirst(checkCharacters) //The index where the bytes appear in the search buffer

        if(index == -1 || i == input.size - 1){
            if(checkCharacters.size > 1){
                index = (checkCharacters.copyOfRange(0, checkCharacters.size - 1)).findFirst(searchBuffer, 0)
                var offset = i - index - checkCharacters.size + 1 //Calculate the relative offset
                var length = checkCharacters.size //Set the length of the token
                var token: String = "<$offset,$length>" //Build the token

                if(token.length > length){
                    //Length of the token is greater than the length it represents...
                    output = output + checkCharacters
                }
                else{
                    output = output + token.toByteArray()
                }
            }
            else{
                output = output + checkCharacters
            }

            checkCharacters = byteArrayOf()

        }

        searchBuffer = searchBuffer + byteArrayOf(char)

        if(searchBuffer.size > maxSlidingWindowSize){
            searchBuffer = searchBuffer.copyOfRange(1, searchBuffer.size)
        }

        i += 1
    }

    return(output)
}


fun ByteArray.findFirst(sequence: ByteArray,startFrom: Int = 0): Int {
    if(sequence.isEmpty()) throw IllegalArgumentException("non-empty byte sequence is required")
    if(startFrom < 0 ) throw IllegalArgumentException("startFrom must be non-negative")
    var matchOffset = 0
    var start = startFrom
    var offset = startFrom
    while( offset < size ) {
        if( this[offset] == sequence[matchOffset]) {
            if( matchOffset++ == 0 ) start = offset
            if( matchOffset == sequence.size ) return start
        }
        else
            matchOffset = 0
        offset++
    }
    return -1
}

fun ByteArray.toHex(): String = joinToString(separator = " ") { eachByte -> "%02x".format(eachByte) }
