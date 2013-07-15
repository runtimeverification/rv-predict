package rvpredict.logging;

public final class RecordRT {

  public static  void logBranch(int ID) {
  }
  public static void logBasicBlock(int ID)
  {
  }

  public static  void logWait(int ID,final Object o) {
  }

  public static  void logNotify(int ID,final Object o) {
  }


  public static  void logStaticSyncLock(int ID, int SID) {
  }

  public static  void logStaticSyncUnlock(int ID, int SID) {
  }

  public static  void logLock(int ID, final Object lock) {
  }

  public static  void logUnlock(int ID,final Object lock) {
 }

  public static  void logFieldAcc(int ID, final Object o, int SID, final Object v, final boolean write) {
  }
  public static  void logArrayAcc(int ID, final Object o, int index, final Object v, final boolean write) {
  }
  public static void logInitialWrite(int ID, final Object o, int index, final Object v){
  }

  public static  void logStart(int ID, final Object o) {
  }
  public static  void logJoin(int ID, final Object o) {
  }

}