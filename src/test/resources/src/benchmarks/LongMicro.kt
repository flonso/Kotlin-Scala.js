/*                     __                                               *\
**     ________ ___   / /  ___      __ ____  Scala.js Benchmarks        **
**    / __/ __// _ | / /  / _ | __ / // __/  (c) 2016, LAMP/EPFL        **
**  __\ \/ /__/ __ |/ /__/ __ |/_// /_\ \                               **
** /____/\___/_/ |_/____/_/ | |__/ /____/                               **
**                          |/____/                                     **
\*                                                                      */

package benchmarks.longmicro

import benchmarks.Benchmark
import benchmarks.collections.LinkedList
import benchmarks.longmicro.LongMicroDataSets.random32s
import benchmarks.longmicro.LongMicroDataSets.random53s
import benchmarks.longmicro.LongMicroDataSets.random64s
import benchmarks.longmicro.LongMicroDataSets.random8s
import benchmarks.longmicro.LongMicroDataSets.randomPow2s

object LongMicroAll : Benchmark() {
    private val allBenches = arrayOf(
            LongNop/*,
            LongXor,
            LongAdd,
            LongMul,
            LongDiv32_32,
            LongDiv32_8,
            LongDiv53_53,
            LongDiv53_8,
            LongDiv64_Pow2,
            LongDiv64_64,
            LongDiv64_8,
            LongToString32,
            LongToString53,
            LongToString64*/
    )

    override fun main(args: Array<String>) {
        for (bench in allBenches)
            bench.main(args)
    }

    override fun report(): String {
        val reports = LinkedList(allBenches).map { bench -> bench.prefix + ": " + bench.report() + " -- "}
        return reports.joinToString("")
    }

    override fun run(): Unit = throw Exception("Not implemented")
}

object LongMicroDataSets {

    val random64s = arrayOf<Long>(
            0x416a5e6e5a17717dL,
            -0x755c952fc04391f0L,
            -0x3ea6e0dd3fb902beL,
            -0x442a5178e96e6710L,
            0x79d411e4b174ea28L,
            -0x03ea77b7a9781e62L,
            -0x7602f95ae2f24bdaL,
            0x6f9d1ec5ab9f4f0bL,
            0x692db997f1814ebL,
            0x668e4565cf1d9148L,
            -0x0eac575e12838d7aL,
            0x6a7ffcca4afe30a0L,
            0x7b5fdf1850d7803L,
            -0x30ee375b88714d59L,
            -0x15b7c98d00b151f4L,
            -0x17fa3fba27ab212cL,
            0x201515f8fd4f897fL,
            -0x1be81c00a8265b3bL,
            -0x715aff75e6fcbcb2L,
            -0x57f8614d6f1bc742L,
            0x345b4bdcb51d55c4L,
            0x210d3b7f8570b40dL,
            0xdaa42071cae18b0L,
            0x7a31ad433480cd29L,
            -0x0118136ef50f7607L,
            -0x5f064c4a6e3f5bdeL,
            0x2124bc1525b2b702L,
            0x686fddaa477235cL,
            0x2cf77ca4916556d6L,
            0x75d1778fcbaf4c33L,
            0x1e5152d812e81d4cL,
            -0x26cc8859e3af1308L,
            0x3e841419b344ce90L,
            0x160c6dc899012e85L,
            0xdee0e6cd62e8448L,
            0x2e50b98ab3732091L,
            -0x2714538b88e603e8L,
            0x72bc8b31cc9be6c1L,
            -0x3b7412535ddca644L,
            0x33b22239aa38be0dL,
            -0x66c5a25d0fe5a466L,
            0x1470c48f9e5edc6L,
            0x53f225ee646edda1L,
            -0x71d673b7a9a4cdc7L,
            -0x27a6e6129a376ae6L,
            0x778b5251c1ff7beaL,
            -0x557fcd07e28b9355L,
            -0x2d148c5a3eadf4beL,
            -0x1074a2d88372f616L,
            -0x445137d1a0a78376L,
            -0x73caf04afab6fd6aL,
            0x29e393f9c3c6bd23L,
            0x7988abfbc974eefcL,
            0x527d751641d57b25L,
            0x648215c807502d7dL,
            0x5aa12874703988dfL,
            0x20df646de08e7f6fL,
            0x43be14acd3d1f8a9L,
            0x5c9d29732cb16f3fL,
            -0x4a409a29fb20540aL,
            0x74548f4528bd1899L,
            0x73a0f7e9b8d81b26L,
            0x65c6d5a4728df6f8L,
            0x71f538811ba24775L,
            -0x0b79b03cdea4897bL,
            -0x00466b629289c5b5L,
            -0x19686b96f0e6dfffL,
            -0x1829ae61fb611b21L,
            0x74a7fc3240456569L,
            -0x4a21cb74e0c0a9e3L,
            -0x3d1a5bed0e317b43L,
            -0x7566eb9a34048c6L,
            -0x32fd9a7e2a9ba8aL,
            0x587a38bd5486222fL,
            -0x327b3f292588edc5L,
            -0x2b8930f985f00e4dL,
            0x26c7f142e3021859L,
            -0x549d5c9f873bdc8fL,
            0x2aaf6e342d051f1bL,
            0xdaf7d968ee64056L,
            -0x169dce4eb2ece627L,
            0x59f63dc3fd3f54ebL,
            -0x140b2ada1d90f7efL,
            -0x73f64a8a87aeecb3L,
            -0x04aa0f38bce20996L,
            -0x2d7a24953a921fa1L,
            -0x46a9584aeed9f07eL,
            0x16d0a7d238beccebL,
            0x102a261302fcdd1fL,
            -0x0b1d8fc137510400L,
            -0x2f0c39af332ef1aL,
            0x3a777bee663188b8L,
            0x2861a914279e768dL,
            0x1c4338bbb103e6cdL,
            -0x3349fc41408476a9L,
            -0x04f45063ac1dc889L,
            -0x7305331604a23d80L,
            0x5657283cad1084c1L,
            -0x2887bc9b9ca42523L,
            0x59092e3d49691801L
    )

    val random32s = arrayOf<Long>(
            0x3d7bca72L,
            0x6e125b10L,
            -0x7ffffffff3299127L,
            -0x7fffffffdefa0492L,
            -0x7fffffffeea0a2cfL,
            -0x7fffffffb8038179L,
            -0x20e5f0cL,
            0x4d830f21L,
            -0x7fffffffef18aa84L,
            0x4f042fc1L,
            0x12dbd8a5L,
            -0x7fffffffe3558dc1L,
            0x71ecbe93L,
            -0x7fffffffa956d5c6L,
            0x640c3bd5L,
            0x44d77fb1L,
            0x99c9f40L,
            -0x7fffffffe79d2c19L,
            -0x7fffffffee95932bL,
            0x6552cb0bL,
            0x5905b9f7L,
            0x56af0217L,
            -0x7fffffffd5f56c67L,
            -0x7fffffff839c8af1L,
            0x26d22d6bL,
            0x7cc75936L,
            -0x7fffffff9c62f33dL,
            -0x7fffffffa4268d06L,
            0x4c916c02L,
            0x751761a0L,
            0x5fc17458L,
            -0x7fffffffa5a6b220L,
            0x20ce206fL,
            -0x7fffffffc5b42c05L,
            0x45098ca1L,
            0x807263bL,
            0x2a8b38a0L,
            -0x7fffffffe5cd24adL,
            0x5e7f88c0L,
            0x510da3b0L,
            0x4ecc3adbL,
            -0x7fffffffd266abccL,
            -0x7fffffffe971b8e2L,
            -0x7fffffff865735a5L,
            -0x7fffffffcc6b4923L,
            0x67b55f19L,
            -0x7fffffff8aa1bcbcL,
            0xc292dc7L,
            0x71c176e0L,
            -0x7fffffffa1ebe8a1L,
            0x51280bd0L,
            -0x7fffffffe36f4136L,
            -0x7fffffff98cc54beL,
            0x631666d7L,
            0x5ed13defL,
            0x7a4a00c1L,
            -0x7fffffffdbc5d6d5L,
            0x31bbc1f0L,
            -0x7fffffff86e8d7feL,
            0x39102053L,
            -0x7fffffffbbd1f909L,
            0x76ae826dL,
            0x695da0fcL,
            -0x7fffffff8cd841acL,
            -0x7fffffffdfb7c5b3L,
            -0x7fffffffc4fbfcf8L,
            0x3467c343L,
            0x3fc83653L,
            0x47d1a280L,
            0x7681e774L,
            0x7a993e0dL,
            -0x7fffffff81c1eccaL,
            0x20bb0799L,
            0x7f9fab4fL,
            0x62c56b3aL,
            -0x7fffffff98d57d03L,
            0x6e000504L,
            0x7dfa0919L,
            -0x7fffffffdab49d4fL,
            -0x7ffffffffb630813L,
            -0x7fffffffeb753a49L,
            0x369ce15aL,
            -0x7fffffffa16519c1L,
            -0x7fffffffbd4e5f25L,
            0x75252458L,
            -0x7fffffff9030369fL,
            -0x7fffffffb1eaf374L,
            0x1951a5b0L,
            0x5d29e5a9L,
            -0x7fffffff83b2bcd6L,
            0x5914897dL,
            0x469b62ecL,
            0x1298eca5L,
            0x27d89dbL,
            -0x7fffffff9611ae2fL,
            0x3cb7b681L,
            -0x7fffffffbf181c44L,
            -0x7fffffffc079bb52L,
            0x1db7ba55L,
            -0x7ffffffffb8ba481L
    )

    val random53s = arrayOf<Long>(
            -0x2014b5eb2d544L,
            -0x7ff81ba21761c495L,
            0x4d77578d1370cL,
            0x49d5abe6d91c4L,
            0x6da66b7976ab8L,
            -0x7ff94d7c0aadc01dL,
            0x3ba7bb07a26c5L,
            -0x7ff4898488de1b39L,
            0x1fb4aa97c4e72L,
            -0x7ffc7d8417a681a2L,
            -0x7ff9ea38de4a30dcL,
            -0x7ff7cc020050b1d1L,
            0x4a8cf8ba7de6eL,
            0xecd5c1740b2e1L,
            -0x7fff1205454d9105L,
            -0x7ffa282763ece31aL,
            -0x7ff3307811507e49L,
            -0x7ff74422d262a9dbL,
            -0x7ff50edccd83516aL,
            0xdd1d46ac78c4aL,
            0x2abb05aee2182L,
            -0x259f192ca5caeL,
            -0x7ff50d7f12f2acb7L,
            0x93ad50a5325L,
            -0x7ff8a5df04621518L,
            0x7bf7bb28d7be4L,
            0x8a68176bafc25L,
            0x452e6fad6f308L,
            0xb6d883731cbd9L,
            0xd04102c60487dL,
            0x204932dbbe8dbL,
            0xd24a5d3f0743L,
            -0x7ff2a0c2b8ce989dL,
            0x1c43d2b5522f3L,
            0x3693351fb3fcbL,
            -0x7ff8d508357ce416L,
            0x14d3efb1e69a3L,
            -0x7ff513559b31387bL,
            0x4ac18f1c26f68L,
            0xe254405a2f781L,
            -0x7ffdc4202c90c060L,
            -0x7da18b8021bb9L,
            0x4998d1faf5e5bL,
            -0x7ff6413227f51335L,
            -0x7ff709e7487f48f7L,
            0x4ecbb9e547e36L,
            -0x7ff45bf70302f287L,
            0xe1e2a5f71e8d9L,
            -0x7ff492b0260a9c91L,
            0x5a294db85a7caL,
            0xc64d9e7934795L,
            0xda57c677ae9ebL,
            -0x2c3008971e1eeL,
            -0x7ff8c1a9fd27b144L,
            -0x7ffc91854d903c4bL,
            -0x7ffba14683e5a47eL,
            -0x7ffd7569332f2758L,
            0x1985f2113ef89L,
            0xc8fb6f9fea5f0L,
            0xb578e10f39e0aL,
            0x98d38f04a793eL,
            0xe2788866f6470L,
            0x5ce6f05d120f0L,
            -0x7ffe99da13b495a3L,
            -0x76681aa1ae0bbL,
            0x5471e1bd27292L,
            0x58f35ef151e17L,
            0x1978fc992febcL,
            0x9bbaf76ee9d4eL,
            0x8b7d111577960L,
            -0x7ffbf414a7c7738eL,
            -0x7ff94951387b6dcaL,
            -0x7fff60f1a8f314eeL,
            -0x7ff31bc366eb66a2L,
            0x42e7ea6ed60e2L,
            0x9d39dbeb59809L,
            -0x7ff1fdb5df711dd7L,
            0x6d8a17d17b2c3L,
            -0x23b6df7859d59L,
            -0x7c6f4a3206f89L,
            -0x7ff0b475160b6d34L,
            0x8ed5da55ea504L,
            -0x7ffbe0f14988afceL,
            0x5abb9a512cce7L,
            0xb7112b4631dfbL,
            0x1cdcdb10924f9L,
            0xce96c01207757L,
            -0x7ff6d01b8cc6ffd5L,
            -0x25e0d9f196b33L,
            -0x7ff65e6a8230c678L,
            0x1e0c8471c73f9L,
            -0x7ff0d63cba1187c0L,
            0x4c8c34775e0dfL,
            -0x7ffc32719be1253fL,
            0xdfdb18b3069eL,
            -0x7fff08ddb6521dceL,
            -0x7ffb8504baa56ed5L,
            -0x7ff636a8b0e10d9fL,
            -0x7ff53a3a57d7879dL,
            -0x7ffd04c9c2d2217cL
    )

    val random8s = arrayOf<Long>(
            -0x7fffffffffffff97L,
            0x31L,
            -0x7fffffffffffffa8L,
            0x26L,
            -0x7fffffffffffffd5L,
            0xcL,
            0x4bL,
            0x16L,
            0x35L,
            0x4L,
            0x62L,
            -0x7fffffffffffffd6L,
            -0x7fffffffffffffdfL,
            0x1aL,
            0x63L,
            -0x7fffffffffffffa2L,
            -0x7fffffffffffffc7L,
            -0x7fffffffffffffa2L,
            -0x7fffffffffffffdcL,
            -0x7fffffffffffff8aL,
            0x56L,
            0x16L,
            -0x7fffffffffffffb6L,
            0x66L,
            0x4aL,
            0x26L,
            -0x7fffffffffffffbdL,
            0x6L,
            -0x7fffffffffffffdaL,
            0x12L,
            0x5fL,
            -0x7fffffffffffffaeL,
            0x1cL,
            0x3cL,
            -0x7fffffffffffffb3L,
            -0x7fffffffffffffc1L,
            -0x7fffffffffffffabL,
            -0x7ffffffffffffff8L,
            -0x7fffffffffffffbdL,
            -0x7fffffffffffffbbL,
            -0x7fffffffffffff84L,
            -0x7fffffffffffffa6L,
            -0x7ffffffffffffff0L,
            0x64L,
            0x30L,
            0x3L,
            -0x7ffffffffffffff3L,
            0x42L,
            0x28L,
            -0x7fffffffffffffbbL,
            -0x7fffffffffffffa7L,
            0x4eL,
            0x74L,
            0x42L,
            0x69L,
            -0x7fffffffffffffebL,
            -0x7fffffffffffff94L,
            -0x7fffffffffffffe3L,
            -0x7fffffffffffff94L,
            0x13L,
            -0x7fffffffffffffa4L,
            0x18L,
            0x49L,
            -0x7fffffffffffffd3L,
            -0x7fffffffffffffbaL,
            0x1cL,
            0x72L,
            -0x7ffffffffffffff8L,
            -0x7fffffffffffff92L,
            -0x7fffffffffffffbfL,
            0x3L,
            -0x2L,
            0x6dL,
            0x78L,
            -0x7fffffffffffff82L,
            0x79L,
            0x35L,
            -0x7fffffffffffffc0L,
            -0x7fffffffffffffc9L,
            0x16L,
            -0x7fffffffffffffe5L,
            -0x7fffffffffffff9dL,
            -0x2L,
            -0x7fffffffffffffc4L,
            0x27L,
            0x7cL,
            0x14L,
            0x6eL,
            0x3eL,
            0x43L,
            -0x7fffffffffffffd4L,
            0x3bL,
            0x25L,
            -0x7fffffffffffffa6L,
            0x1dL,
            -0x2L,
            -0x7ffffffffffffff8L,
            -0x7fffffffffffffbaL,
            -0x7fffffffffffffabL,
            -0x7ffffffffffffff6L
    )

    val randomPow2s = arrayOf<Long>(
            0x1000000000L,
            0x400000000000000L,
            0x2000000000000000L,
            0x10000000000L,
            0x200L,
            0x4000L,
            0x800000000000000L,
            0x2000000L,
            0x2000L,
            0x20000L,
            0x400000000000L,
            0x10000L,
            0x8000L,
            0x10000000000L,
            0x40000000000L,
            0x8000L,
            0x4000000000000000L,
            0x20000000000000L,
            0x200000000000000L,
            0x200000000000000L,
            0x2000000L,
            0x1000L,
            0x1L,
            0x400000L,
            0x10000000L,
            0x400L,
            0x20000000L,
            0x200000000L,
            0x8L,
            0x200L,
            0x80000000000L,
            0x20000000L,
            0x2000000L,
            0x100000000000L,
            0x4000000000L,
            0x10000000L,
            0x100L,
            0x200L,
            0x20000000000000L,
            0x40000000000L,
            0x80000000L,
            0x80000000000L,
            0x200000000L,
            0x20000000000000L,
            0x200000000000000L,
            -0x0000000000000000L,
            0x400000L,
            0x1000L,
            -0x0000000000000000L,
            0x4000000L,
            0x4000000000000000L,
            0x1L,
            0x8000000000L,
            0x40000000000000L,
            0x10000000000000L,
            0x4L,
            0x80000000000L,
            0x8000000L,
            0x200000000000000L,
            0x4L,
            0x40L,
            0x200L,
            0x800000000000L,
            0x800000L,
            0x4000000000000L,
            0x1000L,
            0x2000000000000000L,
            0x800000000000L,
            0x100000000000000L,
            0x8000000000000L,
            0x2000000000000L,
            0x80000000000000L,
            -0x0000000000000000L,
            0x10000000000000L,
            0x100000000L,
            0x1000000L,
            0x1L,
            0x20L,
            0x80000000000000L,
            0x4000L,
            0x100000000L,
            0x100L,
            0x8L,
            0x80000000000000L,
            0x20L,
            0x4000000L,
            0x8000000000L,
            0x10L,
            0x400L,
            0x4000000000L,
            0x2000000L,
            0x800000000L,
            0x1L,
            0x2000000L,
            0x1000000000000000L,
            0x80000000L,
            0x8000L,
            0x2000000000L,
            0x100000000000L,
            0x40L
    )

}

/**
 * Long micro-benchmarks.
 */
abstract class LongMicro: Benchmark() {

    fun doRun(randomAs: Array<Long>, randomBs: Array<Long>): Long {
        val alen = randomAs.size
        val blen = randomBs.size
        var result = 0L
        var i = 0
        while (i != alen) {
            var j = 0
            while (j != blen) {
                val a = randomAs[i]
                val b = randomBs[j]
                result = result xor binaryOp(a, b)
                j += 1
            }
            i += 1
        }
        return result
    }

    abstract fun binaryOp(a: Long, b: Long): Long
}

object LongNop : LongMicro() {
    override val prefix = "LongNop"

    override fun run(): Unit {
        if (doRun(random64s, random64s) != 0L)
            throw Exception("wrong result")
    }

    override fun binaryOp(a: Long, b: Long): Long = a
}

object LongXor : LongMicro() {
    override val prefix = "LongXor"

    override fun run(): Unit {
        if (doRun(random64s, random64s) != 0L)
            throw Exception("wrong result")
    }

    override fun binaryOp(a: Long, b: Long): Long = a xor b
}

object LongAdd : LongMicro() {
    override val prefix = "LongAdd"

    override fun run(): Unit {
        if (doRun(random64s, random64s) != -3199834553443988620L)
            throw Exception("wrong result")
    }

    override fun binaryOp(a: Long, b: Long): Long = a + b
}

object LongMul : LongMicro() {
    override val prefix = "LongMul"

    override fun run(): Unit {
        if (doRun(random64s, random64s) != -4111379928290889828L)
            throw Exception("wrong result")
    }

    override fun binaryOp(a: Long, b: Long): Long = a * b
}

object LongDiv32_32 : LongMicro() {
    override val prefix = "LongDiv32_32"

    override fun run(): Unit {
        if (doRun(random32s, random32s) != 54L)
            throw Exception("wrong result")
    }

    override fun binaryOp(a: Long, b: Long): Long = a / b
}

object LongDiv32_8 : LongMicro() {
    override val prefix = "LongDiv32_8"

    override fun run(): Unit {
        if (doRun(random32s, random8s) != 396463647L)
            throw Exception("wrong result")
    }

    override fun binaryOp(a: Long, b: Long): Long = a / b
}

object LongDiv53_53 : LongMicro() {
    override val prefix = "LongDiv53_53"

    override fun run(): Unit {
        if (doRun(random53s, random53s) != 41L)
            throw Exception("wrong result")
    }

    override fun binaryOp(a: Long, b: Long): Long = a / b
}

object LongDiv53_8 : LongMicro() {
    override val prefix = "LongDiv53_8"

    override fun run(): Unit {
        if (doRun(random53s, random8s) != 39173628864963L)
            throw Exception("wrong result")
    }

    override fun binaryOp(a: Long, b: Long): Long = a / b
}

object LongDiv64_Pow2 : LongMicro() {
    override val prefix = "LongDiv64_Pow2"

    override fun run(): Unit {
        if (doRun(random64s, randomPow2s) != 475661794007097238L)
            throw Exception("wrong result")
    }

    override fun binaryOp(a: Long, b: Long): Long = a / b
}

object LongDiv64_64 : LongMicro() {
    override val prefix = "LongDiv64_64"

    override fun run(): Unit {
        if (doRun(random64s, random64s) != 64L)
            throw Exception("wrong result")
    }

    override fun binaryOp(a: Long, b: Long): Long = a / b
}

object LongDiv64_8 : LongMicro() {
    override val prefix = "LongDiv64_8"

    override fun run(): Unit {
        if (doRun(random64s, random8s) != 1415998624949685666L)
            throw Exception("wrong result")
    }

    override fun binaryOp(a: Long, b: Long): Long = a / b
}

object LongToString32 : LongMicro() {
    override val prefix = "LongToString32"

    override fun run(): Unit {
        if (doRun(random32s, random32s) != 0L)
            throw Exception("wrong result")
    }

    override fun binaryOp(a: Long, b: Long): Long = a.toString().length.toLong()
}


object LongToString53 : LongMicro() {
    override val prefix = "LongToString53"

    override fun run(): Unit {
        if (doRun(random53s, random53s) != 0L)
            throw Exception("wrong result")
    }

    override fun binaryOp(a: Long, b: Long): Long = a.toString().length.toLong()
}

object LongToString64 : LongMicro() {
    override val prefix = "LongToString64"

    override fun run(): Unit {
        if (doRun(random64s, random64s) != 0L)
            throw Exception("wrong result")
    }

    override fun binaryOp(a: Long, b: Long): Long = a.toString().length.toLong()
}
