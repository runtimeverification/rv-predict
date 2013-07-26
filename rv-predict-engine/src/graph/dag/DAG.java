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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DAG = Directed Acyclic Graph
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id: DAG.java 8010 2009-01-07 12:59:50Z vsiveton $
 * @todo this class should be reanmed from DAG to Dag
 */
public class DAG implements Cloneable, Serializable
{
    //------------------------------------------------------------
    //Fields
    //------------------------------------------------------------
    /**
     * Nodes will be kept in two data strucures at the same time
     * for faster processing
     */
    /**
     * Maps vertex's label to vertex
     */
    private Map vertexMap = new HashMap();

    /**
     * Conatin list of all verticies
     */
    private List vertexList = new ArrayList();

    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------

    /**
     *
     */
    public DAG()
    {
        super();
    }

    // ------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------

    /**
     * @return
     */
    public List getVerticies()
    {
        return vertexList;
    }


    public Set getLabels()
    {
        final Set retValue = vertexMap.keySet();

        return retValue;
    }

    // ------------------------------------------------------------
    // Implementation
    // ------------------------------------------------------------

    /**
     * Adds vertex to DAG. If vertex of given label alredy exist in DAG
     * no vertex is added
     *
     * @param label The lable of the Vertex
     * @return New vertex if vertext of given label was not presenst in the DAG
     *         or exising vertex if vertex of given labale was alredy added to DAG
     */
    public Vertex addVertex( final String label )
    {
        Vertex retValue = null;

        // check if vertex is alredy in DAG
        if ( vertexMap.containsKey( label ) )
        {
            retValue = ( Vertex ) vertexMap.get( label );
        }
        else
        {
            retValue = new Vertex( label );

            vertexMap.put( label, retValue );

            vertexList.add( retValue );
        }

        return retValue;
    }
    
    public void addVertex(final Vertex v)
    {
    	if(!vertexList.contains(v))
    	{
    		vertexMap.put( v.getLabel(), v );

            vertexList.add( v );
    	}
    }

    public void addEdge( final String from, final String to ) throws CycleDetectedException
    {
        final Vertex v1 = addVertex( from );

        final Vertex v2 = addVertex( to );

        addEdge( v1, v2 );
    }

    public void addEdge( final Vertex from, final Vertex to ) throws CycleDetectedException
    {
    	addVertex(from);
    	addVertex(to);


        from.addEdgeTo( to );

        to.addEdgeFrom( from );

        final List cycle = CycleDetector.introducesCycle( to );

        if ( cycle != null )
        {
            // remove edge which introduced cycle

            removeEdge( from, to );

            final String msg = "Edge between '" + from + "' and '" + to + "' introduces to cycle in the graph";

            throw new CycleDetectedException( msg, cycle );
        }
    }


    public void removeEdge( final String from, final String to )
    {
        final Vertex v1 = addVertex( from );

        final Vertex v2 = addVertex( to );

        removeEdge( v1, v2 );
    }

    public void removeEdge( final Vertex from, final Vertex to )
    {
        from.removeEdgeTo( to );

        to.removeEdgeFrom( from );
    }


    public Vertex getVertex( final String label )
    {
        final Vertex retValue = ( Vertex ) vertexMap.get( label );

        return retValue;
    }

    public boolean hasEdge( final String label1, final String label2 )
    {
        final Vertex v1 = getVertex( label1 );

        final Vertex v2 = getVertex( label2 );

        final boolean retValue = v1.getChildren().contains( v2 );

        return retValue;

    }

    /**
     * @param label
     * @return
     */
    public List getChildLabels( final String label )
    {
        final Vertex vertex = getVertex( label );

        return vertex.getChildLabels();
    }

    /**
     * @param label
     * @return
     */
    public List getParentLabels( final String label )
    {
        final Vertex vertex = getVertex( label );

        return vertex.getParentLabels();
    }


    /**
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException
    {
        // this is what's failing..
        final Object retValue = super.clone();

        return retValue;
    }


    /**
     * Indicates if there is at least one edge leading to or from vertex of given label
     *
     * @return <code>true</true> if this vertex is connected with other vertex,<code>false</code> otherwise
     */
    public boolean isConnected( final String label )
    {
        final Vertex vertex = getVertex( label );

        final boolean retValue = vertex.isConnected();

        return retValue;

    }


    /**
     * Return the list of labels of successor in order decided by topological sort
     *
     * @param label The label of the vertex whose predessors are serched
     *
     * @return The list of labels. Returned list contains also
     *         the label passed as parameter to this method. This label should
     *         always be the last item in the list.
     */
    public List getSuccessorLabels( final String label )
    {
        final Vertex vertex = getVertex( label );

        final List retValue;

        //optimization.
        if ( vertex.isLeaf() )
        {
            retValue = new ArrayList( 1 );

            retValue.add( label );
        }
        else
        {
            retValue = TopologicalSorter.sort( vertex );
        }

        return retValue;
    }
    public List<Vertex> getSuccessorVertexes(final Vertex v)
    {
    	final List<Vertex> retValue;

        //optimization.
        if ( v.isLeaf() )
        {
            retValue = new ArrayList<Vertex>( 1 );

            retValue.add( v );
        }
        else
        {
            retValue = TopologicalSorter.sort( v );
        }

        return retValue;
    }


}
