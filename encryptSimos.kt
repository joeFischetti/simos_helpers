import java.io.File

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
}

fun checksumSimos18(bin: ByteArray): ByteArray{
	var dwChecksum = 0;
  var currentChecksum = bin.copyOfRange(0x300, 0x308)
	var offset = (0xA0800000).toUInt()
	var startAddress1 = byteArrayToInt(bin.copyOfRange(0x30c, 0x30c + 4).reversedArray()).toUInt() - offset
	var endAddress1 = byteArrayToInt(bin.copyOfRange(0x310, 0x310 + 4).reversedArray()).toUInt() - offset
	var startAddress2 = byteArrayToInt(bin.copyOfRange(0x314, 0x314 + 4).reversedArray()).toUInt() - offset
	var endAddress2 = byteArrayToInt(bin.copyOfRange(0x318, 0x318 + 4).reversedArray()).toUInt() - offset


	var block1Size = endAddress1 - startAddress1 + 1.toUInt();
	var block2Size = endAddress2 - startAddress2 + 1.toUInt();

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
    println("Current checksum:      " + currentChecksum.toHex())
	println("Calculated checksum:   " + checksumCalculated.toHex())


    if(currentChecksum contentEquals checksumCalculated){
        println("Checksum matches!")
    }
    else{
        println("Checksum doesn't match!")
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
    println("Current ECM3:      " + checksumCurrent.toHex())
    println("Calculated ECM3:   " + checksumCalculated.toHex())

    if(checksumCurrent contentEquals checksumCalculated){
        println("ECM3 checksum matches!")
    }
    else{
        println("ECM3 checksum doesn't match!")
    }

    return(bin)
}
fun ByteArray.toHex(): String = joinToString(separator = " ") { eachByte -> "%02x".format(eachByte) }

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
