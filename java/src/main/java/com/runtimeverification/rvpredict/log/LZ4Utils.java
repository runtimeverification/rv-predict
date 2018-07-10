package com.runtimeverification.rvpredict.log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import com.runtimeverification.rvpredict.config.Configuration.OS;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

/**
 * Contains utility methods related to LZ4 compression library.
 *
 * @author YilongL
 */
public class LZ4Utils {

    private static final int COMPRESS_BLOCK_SIZE = 8 * 1024 * 1024; // 8MB

    private static final LZ4Compressor FAST_COMPRESSOR = LZ4Factory.fastestInstance().fastCompressor();

    private static final LZ4FastDecompressor FAST_DECOMPRESSOR = LZ4Factory.fastestInstance().fastDecompressor();

    public static LZ4BlockOutputStream createCompressionStream(Path path) throws IOException {
        return new LZ4BlockOutputStream(
                new BufferedOutputStream(new FileOutputStream(path.toString())),
                /*
                TODO: MappedByteBufferOutputStream was better optimized on Linux, but David's last tests show that
                this is not the case anymore. Also, it seems to cause crashes.

                On the other hand, BufferedChannelOutputStream seems to skip creating the file sometimes.

                A raw BufferedOutputStream should be significantly worse, but it seems to be reliable.

                We should investigate these and do something reasonable.

                OS.current() == OS.WINDOWS ?
                    new BufferedChannelOutputStream(path) :
                    new MappedByteBufferOutputStream(path),
                    */
                COMPRESS_BLOCK_SIZE,
                FAST_COMPRESSOR);
    }

    public static LZ4BlockInputStream createDecompressionStream(Path path) throws FileNotFoundException {
        return new LZ4BlockInputStream(
                new BufferedInputStream(new FileInputStream(path.toFile()), COMPRESS_BLOCK_SIZE),
                FAST_DECOMPRESSOR);
    }

}
    