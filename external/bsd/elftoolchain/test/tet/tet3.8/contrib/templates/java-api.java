//
// (C) Copyright 1997 The Open Group
//
// All rights reserved.  No part of this source code may be reproduced,
// stored in a retrieval system, or transmitted, in any form or by any
// means, electronic, mechanical, photocopying, recording or otherwise,
// except as stated in the end-user licence agreement, without the prior
// permission of the copyright owners.
// A copy of the end-user licence agreement is contained in the file
// Licence which accompanies this distribution.
// 
//

import java.lang.*;

// replace MyTestCase with the name of the java class
// file on the next line and in the main() line below

public class MyTestCase extends TET.SimpleTestCase
{

  public static void main(String args[]) {
    main("MyTestCase", args, new MyTestCase());
  }

  // The startup routine is executed before the first requested
  // invocable component. Typically this is used to check major
  // dependencies or to perform some global setup. 
  // The sample below checks for a dependency being set in
  // the tetexec.cfg file and calls tet_delete to mark certain
  // test purposes as cancelled if the dependency is not met.

  public void startup (TET.TestSession ts) {
	String depend = ts.tet_getvar ("TET_DEPENDENCY");
        if ( depend != null && depend.equalsIgnoreCase("TRUE" )) {
		ts.tet_infoline("This message was generated in the startup method");
	}
	else {
	// dependency not met, cancel the test purposes 1 and 3
	ts.tet_delete( 1, "TET_DEPENDENCY not set in tetexec.cfg");
	ts.tet_delete( 2, "TET_DEPENDENCY not set in tetexec.cfg");
	}
	
	
  }

  // The cleanup routine is executed after the last requested
  // invocable component. Typically this is used to  perform
  // some cleanup operation such as deleting any temporary files.

  public void cleanup (TET.TestSession ts) {
	ts.tet_infoline("This message was generated in the cleanup method");
  }

// test functions follow
// note that the naming is important

  public void t1 (TET.TestSession ts) {
    ts.tet_infoline("This is test purpose: " + ts.tet_thistest());
    ts.tet_infoline("Expected Result : Pass");
    ts.tet_result(ts.TET_PASS);
  }

  public void t2 (TET.TestSession ts) {
    ts.tet_infoline("This is test purpose:" + ts.tet_thistest());
    ts.tet_infoline("This has some Unicode in it: \u0234");
    ts.tet_infoline("Expected Result : Fail");
    ts.tet_result(ts.TET_FAIL);
  }



}
