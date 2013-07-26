/*
  Copyright (c) 2011,2012, 
   Saswat Anand (saswat@gatech.edu)
   Mayur Naik  (naik@cc.gatech.edu)
  All rights reserved.
  
  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met: 
  
  1. Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer. 
  2. Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution. 
  
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  
  The views and conclusions contained in the software and documentation are those
  of the authors and should not be interpreted as representing official policies, 
  either expressed or implied, of the FreeBSD Project.
*/


package z3;

import java.util.Vector;
import java.text.DecimalFormat;
import org.w3c.tools.sexpr.Symbol;
import org.w3c.tools.sexpr.SimpleSExprStream;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStreamReader;

public class YicesModelReaderSMTLIB1
{ 
	public static Z3Model read(File file)
	{
		try{
			FileInputStream fis = new FileInputStream(file);	
			DataInputStream in = new DataInputStream(fis);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String result =  br.readLine();
			if(result.startsWith("(error "))
				throw new Error("smt file has errors");
			//System.out.println("Feasible: " + "sat".equals(result));

			if("sat".equals(result)) {
				
				Z3Model model = new Z3Model();
				
				//Read File Line By Line
				while ((result = br.readLine()) != null)   {

					if(result.startsWith("("))
					{
						String[] strs = result.split(" ");
		
						String varName = strs[1];
						String value_str = strs[2];
						value_str = value_str.replace(")","");
	
						Object value = Integer.valueOf(value_str);
	
						model.put(varName, value);
					}
				}
				
				//Close the input stream
				in.close();
				
				return model;
			}

			return null;
		}catch(Exception e){
			//throw new Error(e);
			//e.printStackTrace();
			return null;
		}
		catch(Error e){
			//throw new Error(e);
			//e.printStackTrace();//don't throw it if it is a NPE
			return null;
		}
	}

}