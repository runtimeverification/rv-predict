package com.runtimeverification.rvpredict.log;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
                OS.current() == OS.WINDOWS ?
                    new BufferedChannelOutputStream(path) :
                    new MappedByteBufferOutputStream(path),
                COMPRESS_BLOCK_SIZE,
                FAST_COMPRESSOR);
    }

    public static LZ4BlockInputStream createDecompressionStream(Path path) throws FileNotFoundException {
        return new LZ4BlockInputStream(
                new BufferedInputStream(new FileInputStream(path.toFile()), COMPRESS_BLOCK_SIZE),
                FAST_DECOMPRESSOR);
    }

}
