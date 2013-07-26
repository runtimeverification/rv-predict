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
package graph.dag;

/*
 * Copyright The Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id: Vertex.java 8010 2009-01-07 12:59:50Z vsiveton $
 */
public class Vertex implements Cloneable, Serializable, IVertex
{
    //------------------------------------------------------------
    //Fields
    //------------------------------------------------------------
    private String label = null;

    List children = new ArrayList();

    List parents = new ArrayList();


    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------

    /**
     *
     */
    public Vertex( final String label )
    {
        this.label = label;
    }

    // ------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------

    //and-join node for vertex has multiple parents
    ANDJOINVertex ajvertex; 
    ANDSPLITVertex asvertex;
    int ANDJOINGlobalId,ANDSPLITGlobalId;
    //marked or not
    private boolean marked;
    public synchronized boolean mark()
    {
    	if(marked)
    		return false;
    	else
    	{
    		marked = true;
    		
    		if(this.parents.size()>1)
    			ajvertex = new ANDJOINVertex(this);//++ANDJOINGlobalId,
    		
    		return true;
    	}
    }
    public ANDJOINVertex getANDJOINVertex()
    {
    	return ajvertex;
    }
    public ANDSPLITVertex createANDSPLITVertex()
    {
    	asvertex = new ANDSPLITVertex(this);//++ANDSPLITGlobalId,
    	return asvertex;
    }
    /**
     * @return
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * @param vertex
     */
    public void addEdgeTo( final Vertex vertex )
    {
        children.add( vertex );
    }


    /**
     * @param vertex
     */
    public void removeEdgeTo( final Vertex vertex )
    {
        children.remove( vertex );
    }


    /**
     * @param vertex
     */
    public void addEdgeFrom( final Vertex vertex )
    {
        parents.add( vertex );
    }

    public void removeEdgeFrom( final Vertex vertex )
    {

        parents.remove( vertex );

    }


    public List getChildren()
    {
        return children;
    }


    /**
     * Get the labels used by the most direct children.
     *
     * @return the labels used by the most direct children.
     */
    public List getChildLabels()
    {
        final List retValue = new ArrayList( children.size() );

        for ( final Iterator iter = children.iterator(); iter.hasNext(); )
        {
            final Vertex vertex = ( Vertex ) iter.next();

            retValue.add( vertex.getLabel() );
        }
        return retValue;
    }


    /**
     * Get the list the most direct ancestors (parents).
     *
     * @return list of parents
     */
    public List getParents()
    {
        return parents;
    }


    /**
     * Get the labels used by the most direct ancestors (parents).
     *
     * @return the labels used parents
     */
    public List getParentLabels()
    {
        final List retValue = new ArrayList( parents.size() );

        for ( final Iterator iter = parents.iterator(); iter.hasNext(); )
        {
            final Vertex vertex = ( Vertex ) iter.next();

            retValue.add( vertex.getLabel() );
        }
        return retValue;
    }


    /**
     * Indicates if given vertex has no child
     *
     * @return <code>true</true> if this vertex has no child, <code>false</code> otherwise
     */
    public boolean isLeaf()
    {
        return children.size() == 0;
    }


    /**
     * Indicates if given vertex has no parent
     *
     * @return <code>true</true> if this vertex has no parent, <code>false</code> otherwise
     */
    public boolean isRoot()
    {
        return parents.size() == 0;
    }


    /**
     * Indicates if there is at least one edee leading to or from given vertex
     *
     * @return <code>true</true> if this vertex is connected with other vertex,<code>false</code> otherwise
     */
    public boolean isConnected()
    {
        return isRoot() || isLeaf();
    }


    public Object clone() throws CloneNotSupportedException
    {
        // this is what's failing..
        final Object retValue = super.clone();

        return retValue;
    }

    public String toString()
    {
        return "Vertex{" +
               "label='" + label + "'" +
               "}";
    }


}

