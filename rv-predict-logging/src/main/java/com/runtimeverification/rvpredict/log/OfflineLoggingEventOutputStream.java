package com.runtimeverification.rvpredict.log;

import java.io.IOException;
import java.nio.file.Path;

import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

/**
 * An event output stream used in offline logging.
 *
 * @author YilongL
 */
public class OfflineLoggingEventOutputStream extends EventOutputStream {

    public static final int COMPRESS_BLOCK_SIZE = 32 * 1024 * 1024; // 32MB

    private static final LZ4Compressor FAST_COMPRESSOR = LZ4Factory.fastestInstance().fastCompressor();

    public OfflineLoggingEventOutputStream(Path path) throws IOException {
        super(new LZ4BlockOutputStream(
                new MappedByteBufferOutputStream(path),
                COMPRESS_BLOCK_SIZE,
                FAST_COMPRESSOR));
    }

}
