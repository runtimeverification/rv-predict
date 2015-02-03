package rvpredict.logging;

import rvpredict.db.EventInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Set;

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

    Set<Integer> getVolatileFieldIds() throws IOException, ClassNotFoundException;

    Map<Integer,String> getVarIdToVarSig() throws IOException, ClassNotFoundException;

    Map<Integer,String> getLocIdToStmtSig() throws IOException, ClassNotFoundException;

}
