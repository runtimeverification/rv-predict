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
package violation;

import java.util.HashSet;

public class TestHashCode {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String s1 = "<causal.Tricky4: void main(java.lang.String[])>|<causal.Tricky4: int y> = 1|16";
		String s2 = "<causal.Tricky4$MyThread: void run()>|<causal.Tricky4: int y> = 2|36";
		String s4 = "<causal.Tricky4: void main(java.lang.String[])>|<causal.Tricky4: int y> = 1|16";
		String s3 = "<causal.Tricky4$MyThread: void run()>|<causal.Tricky4: int y> = 2|36";

		Race race1 = new Race(s1,s2,1,2);
		Race race2 = new Race(s3,s4,2,1);
		
		System.out.println(race1.hashCode());
		System.out.println(race2.hashCode());

		HashSet<Race> set = new HashSet<Race>();
		set.add(race1);
		if(!set.contains(race2))
		{
			System.out.println("BUG");
		}

	}

}
