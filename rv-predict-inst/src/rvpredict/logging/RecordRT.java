/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package rvpredict.logging;

/**
 * The stub class for record instrumentation. 
 * The methods are concreted differently
 * for record and replay.
 * @author jeffhuang
 *
 */
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
