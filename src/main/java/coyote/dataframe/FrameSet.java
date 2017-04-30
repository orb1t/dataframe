/*
 * Copyright (c) 2014 Stephan D. Cote' - All rights reserved.
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the MIT License which accompanies this distribution, and is 
 * available at http://creativecommons.org/licenses/MIT/
 *
 * Contributors:
 *   Stephan D. Cote 
 *      - Initial concept and implementation
 */
package coyote.dataframe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * This class models a set of rows and allows for their uniform treatment.
 * 
 * <p>This class was created in response to using DataFrames as Value Objects 
 * in a project which queried a variety of objects from data base tables. Using 
 * a Frame Set made it easy to collect the objects in one large set and parse
 * through them for easy output to files and displays.
 */
public class FrameSet {

  private List<DataFrame> rows = new ArrayList<DataFrame>();
  private Set<String> columns = new HashSet<String>();




  /**
   * Default Constructor
   */
  public FrameSet() {}




  /**
   * Constructor for a list of frames. Each frame in the list will be added to 
   * this set.
   * 
   * @param frames The list of frames to be added to this set.
   */
  public FrameSet( List<? extends DataFrame> frames ) {
    addAll( frames );
  }




  /**
   * Adds all the frames in the given collection of frames to this frame set.
   * 
   * @param frames the collection of frames to add.
   */
  public void addAll( Collection<? extends DataFrame> frames ) {
    for ( Iterator<? extends DataFrame> it = frames.iterator(); it.hasNext(); this.add( it.next() ) );
  }




  /**
   * Add the given frame to this set.
   * 
   * @param frame The frame to add
   */
  public void add( DataFrame frame ) {
    // add the frame to the collection
    rows.add( frame );

    // Add all the named fields to the 
    for ( String name : frame.getNames() ) {
      columns.add( name );
    }
  }




  /**
   * Access a list of all the named fields in all the frames in this set.
   * 
   * <p>Note that not all fields may be represented in all rows. It is 
   * possible that a frame in the set may have no named fields and its data 
   * will be inaccessible by name. This method attempts to provide uniform 
   * columnar access to a set of frames for those frames with names.
   * 
   * <p>No assertion can be made as to the order of the names in the returned 
   * list.
   * 
   * @return A list of unique names for all the named fields of all the frames 
   * in this set.
   */
  public List<String> getColumns() {
    List<String> retval = new ArrayList<String>();
    retval.addAll( columns );
    return retval;
  }




  /**
   * @return the number of frames in this set
   */
  public int size() {
    return rows.size();
  }




  /**
   * Returns the DataFrame at the specified position in this set.
   * 
   * @param index - index of the element to return
   * 
   * @return the element at the specified position in this list 
   * 
   * @throws IndexOutOfBoundsException - if the index is out of range (index &lt; 0 || index &gt;= size())
   */
  public DataFrame get( int index ) {
    return rows.get( index );
  }




  /**
   * Return the values of the named column as strings.
   * 
   * <p>The values will be in the same order as the rows in the frame set.
   *  
   * @param name The name of the column to return.
   * 
   * @return list of the string values of the object in that column.
   */
  public List<String> getColumn( String name ) {
    List<String> retval = new ArrayList<String>();
    for ( DataFrame frame : rows ) {
      if ( frame.contains( name ) ) {
        retval.add( frame.getAsString( name ) );
      }
    }
    return retval;
  }




  /**
   * Return the actual object values of the named column.
   * 
   * <p>The values will be in the same order as the rows in the frame set.
   *  
   * @param name The name of the column to return.
   * 
   * @return list of the object values in that column.
   */
  public List<Object> getColumnValue( String name ) {
    List<Object> retval = new ArrayList<Object>();
    for ( DataFrame frame : rows ) {
      if ( frame.contains( name ) ) {
        retval.add( frame.getObject( name ) );
      }
    }
    return retval;
  }




  /**
   * @return all the rows of this set as a list
   */
  public List<DataFrame> getRows() {
    return rows;
  }




  /**
   * Remove all the rows from this frame set
   */
  public void clearRows() {
    rows.clear();
  }

}
