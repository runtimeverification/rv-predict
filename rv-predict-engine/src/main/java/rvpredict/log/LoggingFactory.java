package rvpredict.log;

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * LoggingFactory interface for abstracting I/O operations
 * 
 * @author TraianSF
 */
public interface LoggingFactory {
    EventPipe createEventPipe();

    ObjectOutputStream createMetadataOS() throws IOException;

    EventOutputStream createEventOutputStream() throws IOException;
    
    void finishLogging();
    
    EventInputStream getInputStream() throws InterruptedException, IOException;

    String getStmtSig(int locId);

    boolean isVolatile(int fieldId);

    String getVarSig(int fieldId);
}
