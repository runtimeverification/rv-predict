package bufwriter;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:
 * @author
 * @version 1.0
 */

public class Checker extends Thread{
  private Buffer _buf;
  private int _writtenCount;
  private static boolean flag = true;
  public void stopChecker(){
	  flag = false;
  }
  public Checker(Buffer buf) {
    _buf = buf;
    _writtenCount = 0;
  }

  public void run()
  {
    while (flag)
    {
      synchronized (_buf)
      {
         _writtenCount += _buf._pos;
         _buf._pos = 0;
      }
    }
    checkResult();
    
  }
  public void checkResult()
  {
	//Outputting results ...
	    try
	    {
	      int res = _buf._count - (getWrittenCount()+_buf._pos);
	      System.out.print("<BufWriter,"+res+",");
	      
	      //outStream.writeChars("<BufWriter,");
	      //outStream.writeChars(res+",");
	      if (res != 0)
	      {
	    	  System.out.print("[Wrong/No-Lock]>");
	    	  throw new RuntimeException();
	        //outStream.writeChars("[Wrong/No-Lock]>");
	      }
	      else
	    	  System.out.print("[None]>");
	        //outStream.writeChars("[None]>");
	      //outStream.close();
	    }catch(RuntimeException e)
	  {
	    	e.printStackTrace();
	  	"leap_Crashed_with".equals(e);
	  	
	  	System.exit(-1);
	  }
  }
  public int getWrittenCount()
  {
    return _writtenCount;
  }
}