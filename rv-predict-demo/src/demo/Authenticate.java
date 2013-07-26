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
package demo;

/**
 * This program contains a data race on x, which could make the 
 * authentication of a resource z fail.
 * @author jeffhuang
 *
 */
public class Authenticate {

	static int x=0,y=0;
	static int z=0;//assume z is a shared resource
	static Object lock = new Object();
	
	public static void main(String[] args)
	{
		
		MyThread t = new MyThread();
		t.start();
		
		synchronized(lock)
		{

			y = 1;
			
			x =1;
		}
		
		try{
			
			t.join();
			//use z
			//may throw divide by zero exception 
			System.out.println("Safe: "+1/z);

		}catch(Exception e)
		{
			e.printStackTrace();
		}

	}
	
	static class MyThread extends Thread
	{
		public void run()
		{
			int r1,r2;
			
			synchronized(lock)
			{
				r1 = y;
			}
	
			r2 =x;//race here, x is not protected
			
			if(r1+r2>0)
			{
				z=1;//authenticate z here if x or y is positive
			}

		}
	}
}
