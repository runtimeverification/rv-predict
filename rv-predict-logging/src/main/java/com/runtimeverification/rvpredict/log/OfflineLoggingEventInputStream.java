package com.runtimeverification.rvpredict.log;

import static com.runtimeverification.rvpredict.log.OfflineLoggingEventOutputStream.COMPRESS_BLOCK_SIZE;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

/**
 * An event output stream used in offline logging.
 *
 * @author YilongL
 */
public class OfflineLoggingEventInputStream extends EventInputStream {

    private static final LZ4FastDecompressor FAST_DECOMPRESSOR =
            LZ4Factory.fastestInstance().fastDecompressor();

    public OfflineLoggingEventInputStream(Path path) throws IOException {
        super(new LZ4BlockInputStream(new BufferedInputStream(new FileInputStream(path.toFile()),
                COMPRESS_BLOCK_SIZE), FAST_DECOMPRESSOR));
    }

}
