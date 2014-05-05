/*
 * Copyright (c) 2006 Stephan D. Cote' - All rights reserved.
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the MIT License which accompanies this distribution, and is 
 * available at http://creativecommons.org/licenses/MIT/
 *
 * Contributors:
 *   Stephan D. Cote 
 *      - Initial API and implementation
 */
package coyote.dataframe;

import coyote.commons.ByteUtil;

/** Type code representing a 64-bit floating point value in the range of +/-4.9406e-324 to +/-1.7977e+308. */
public class DoubleType implements FieldType
{
  private static final int _size = 8;

  private final static String _name = "DBL";




  public boolean checkType( Object obj )
  {
    return obj instanceof Double;
  }




  public Object decode( byte[] value )
  {
    return new Double( ByteUtil.retrieveDouble( value, 0 ) );
  }




  public byte[] encode( Object obj )
  {
    return ByteUtil.renderDouble( ( (Double)obj ).doubleValue() );
  }




  public String getTypeName()
  {
    return _name;
  }




  public boolean isNumeric()
  {
    return true;
  }




  public int getSize()
  {
    return _size;
  }

}
