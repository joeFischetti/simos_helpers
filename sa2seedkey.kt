import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun main(args: Array<String>) {
		var challenge: ByteArray

		if(args.size == 1){
			challenge = hexStringToByteArray(args[0])
		}
		else{
            challenge = byteArrayOf(0x1a.toByte(), 0x1b.toByte(), 0x1c.toByte(), 0x1d.toByte())
		}


        val VW_SEEDKEY_TAPE = byteArrayOf(
            0x68.toByte(),
            0x02.toByte(), 
            0x81.toByte(), 
            0x49.toByte(), 
            0x93.toByte(), 
            0xa5.toByte(), 
            0x5a.toByte(), 
            0x55.toByte(), 
            0xaa.toByte(), 
            0x4a.toByte(), 
            0x05.toByte(), 
            0x87.toByte(), 
            0x81.toByte(), 
            0x05.toByte(), 
            0x95.toByte(), 
            0x26.toByte(), 
            0x68.toByte(), 
            0x05.toByte(), 
            0x82.toByte(), 
            0x49.toByte(), 
            0x84.toByte(), 
            0x5a.toByte(), 
            0xa5.toByte(), 
            0xaa.toByte(), 
            0x55.toByte(), 
            0x87.toByte(), 
            0x03.toByte(), 
            0xf7.toByte(), 
            0x80.toByte(), 
            0x6a.toByte(), 
            0x4c.toByte()
        )

        var vs = Sa2SeedKey(VW_SEEDKEY_TAPE, challenge)
        println("Challenge: " + challenge.toHex())

        var response = vs.execute()
        println("Response: " + response.toHex())

}

public class Sa2SeedKey(inputTape: ByteArray, seed: ByteArray) {
    var instructionPointer = 0
    var instructionTape = inputTape
    var register = byteArrayToInt(seed)
    var carry_flag: Int = 0
    var for_pointers: ArrayDeque<Int> = ArrayDeque()
    var for_iterations: ArrayDeque<Int> = ArrayDeque()

    fun rsl(){
        carry_flag = register and 0x80000000.toInt()
        register = register shl 1
        if(carry_flag != 0)
            register = register or 0x1.toInt()

        register = register and 0xFFFFFFFF.toInt()
        instructionPointer += 1
    }

    fun rsr(){
        carry_flag = register and 0x1.toInt()
        register = register ushr 1

        if(carry_flag != 0)
            register = register or 0x80000000.toInt()
    
        instructionPointer += 1

    }

    fun add(){
        carry_flag = 0
        var operands = instructionTape.copyOfRange(instructionPointer + 1, instructionPointer + 5)

        var output_register = register + byteArrayToInt(operands)

        if (output_register > 0xffffffff.toInt()){
            carry_flag = 1
            output_register = output_register and 0xffffffff.toInt()
        }

        register = output_register

        instructionPointer += 5

    }

    fun sub(){
        carry_flag = 0
        var operands = instructionTape.copyOfRange(instructionPointer + 1, instructionPointer + 5)
        var output_register = register - byteArrayToInt(operands)

        if (output_register < 0){
            carry_flag = 1
            output_register = output_register and 0xffffffff.toInt()
        }

        register = output_register
        instructionPointer += 5
    }

    fun eor(){
        var operands = instructionTape.copyOfRange(instructionPointer + 1,instructionPointer + 5)
        register = register xor byteArrayToInt(operands)
        instructionPointer += 5
    }

    fun for_loop(){
        var operands = instructionTape.copyOfRange(instructionPointer + 1,instructionPointer + 2)
        for_iterations.addFirst(operands[0] - 1)
        instructionPointer += 2
        for_pointers.addFirst(instructionPointer)
    }

    fun next_loop(){
        if(for_iterations[0] > 0){
            for_iterations[0] -= 1
            instructionPointer = for_pointers[0]
        }
        else{
            for_iterations.first()
            for_pointers.first()
            instructionPointer += 1
        }
       
    }

    fun bcc(){
        var operands = instructionTape.copyOfRange(instructionPointer + 1,instructionPointer + 2)
        var skip_count = operands[0].toUByte().toInt() + 2
        if(carry_flag == 0){
            instructionPointer += skip_count
        }
        else{
            instructionPointer += 2
        }
       
    }

    fun bra(){
        var operands = instructionTape.copyOfRange(instructionPointer + 1,instructionPointer + 2)
        var skip_count = operands[0].toUByte().toInt() + 2
        instructionPointer += skip_count

    }

    fun finish(){
        instructionPointer += 1
    }

    fun execute(): ByteArray{
        val instructionSet = mapOf(
            0x81.toByte() to ::rsl,
            0x82.toByte() to ::rsr,
            0x93.toByte() to ::add,
            0x84.toByte() to ::sub,
            0x87.toByte() to ::eor,
            0x68.toByte() to ::for_loop,
            0x49.toByte() to ::next_loop,
            0x4A.toByte() to ::bcc,
            0x6B.toByte() to ::bra,
            0x4C.toByte() to ::finish,
        )

        while(instructionPointer < instructionTape.size){
            instructionSet[instructionTape[instructionPointer]]?.invoke()
        }

        return intToByteArray(register)
        
    }
}


private val HEX_CHARS = "0123456789ABCDEF"

fun hexStringToByteArray(input: String) : ByteArray {

    val result = ByteArray(input.length / 2)

    for (i in 0 until input.length step 2) {
        val firstIndex = HEX_CHARS.indexOf(input[i]);
        val secondIndex = HEX_CHARS.indexOf(input[i + 1]);

        val octet = firstIndex.shl(4).or(secondIndex)
        result.set(i.shr(1), octet.toByte())
    }

    return result
}


fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }


fun littleEndianConversion(bytes: ByteArray): Int {
    var result = 0
    for (i in bytes.indices) {
        result = result or (bytes[i].toInt() shl 8 * i)
    }
    return result
}

fun byteArrayToInt(data: ByteArray): Int {
    return (data[3].toUByte().toInt()) or
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
