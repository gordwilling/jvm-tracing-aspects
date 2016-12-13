package com.highbar.util.domain;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * <dl>
 * <dt><b>Description:</b>
 * <dd>
 *   Base class for generated classes which provides equals(), hashcode() and
 *   toString() implementations.  While JAXB supposedly gives you pretty fine
 *   control over what the generated classes look like, I've encountered
 *   difficulties in practise and have thus used one base class for everything - this
 *   class.  Consequently this attempts to be as general as possible in its implementation,
 *   and actually seems to have turned out extremely well as far as I can currently tell.
 * </dd>
 * </dt>
 * </dl>
 *
 * @author Gordon Wallace - gw@highbar.com
 * @version 1.0
 */
public abstract class AbstractDomainObject implements DomainObject
{
    /**
     * Generalized equals() method which uses reflection to compare the values
     * of all locally declared fields (fields declared in ancestor classes are
     * not included in the test).
     * @param that the object whose equality is to be tested
     * @return true if the object is equal to this one
     */
    @Override
    public boolean equals( Object that )
    {
        if ( this == that )
        {
            return true;
        }
        if ( !(that instanceof AbstractDomainObject) )
        {
            return false;
        }
        if ( !getClass().getName().equals( that.getClass().getName() ) )
        {
            return false;
        }

        List<Field> fields = getDeclaredFields();
        for ( Field field : fields )
        {
            Object thisValue = getFieldValue( field, this );
            Object thatValue = getFieldValue( field, that );

            if ( thisValue != null ? !thisValue.equals( thatValue ) : thatValue != null )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Generalized hashCode() method which uses reflection to generate a hash value based on
     * locally declared field values (fields declared in ancestor classes are not included in
     * the calculation).  This method will produce the same hash value for objects that are
     * deemed {@link #equals(Object) equal}.
     * @return the hash value for the instance
     */
    @Override
    public int hashCode()
    {
        int result = 0;
        List<Field> fields = getDeclaredFields();
        for ( Field field : fields )
        {
            field.setAccessible( true );
            Object value = getFieldValue( field, this );
            result = 31 * result + (value != null ? value.hashCode() : 0 );
        }
        return result;
    }

    /**
     * Generalized toString() method which uses reflection to build the string representation of an object's state.
     * This method includes the values of fields that are declared in the class corresponding to the current
     * object - no ancestor fields are included.  This method uses
     * {@link #getFieldValueAsString(java.lang.reflect.Field)} to get the string form of a field value.
     * If you are not satisfied with the string representations, override that method.
     * @return the string representation of the current object.
     */
    @Override
    public String toString()
    {
        List<Field> fields = getDeclaredFields();

        StringBuilder s = new StringBuilder( getClass().getName() );
        s.append( '{' );
        for ( int i = 0; i < fields.size(); i++ )
        {
            Field field = fields.get( i );
            if ( i != 0 )
            {
                s.append( ", " );
            }
            s.append( field.getName() );
            s.append( "='" );
            s.append( getFieldValueAsString( field ) );
            s.append( '\'' );
        }
        s.append( '}' );

        return s.toString();
    }

    /**
     * Used by {@link #toString()} to convert field values to string form. For objects other than
     * lists, this method returns the 'natural' string value by simply calling toString() on the
     * field value.  Lists, however, may contain hundreds or maybe thousands of elements, so this method
     * omits list content and just outputs the list type and size. (The real purpose of the toString()
     * method is to enable the creation of useful logs and while the list content sounds helpful,
     * in practise it isn't as important as it might seem and often winds up making the logs
     * harder to read.  Override this method in subclasses if circumstances warrant exposing the
     * list content.
     * @param field the list field
     * @return the list value in String form
     */
    protected String getFieldValueAsString( Field field )
    {
        Object value = getFieldValue( field, this );
        if ( value instanceof List )
        {
            value = field.getGenericType() + " {content omitted; size=" + ((List)value).size() + '}';
        }
        return String.valueOf( value );
    }

    /**
     * Gets the value of the specified field in the given object.
     * @param field the field value to retrieve
     * @param owner the object to retrieve the field value from
     * @return the field value
     */
    private Object getFieldValue( Field field, Object owner )
    {
        try
        {
            field.setAccessible( true );
            return field.get( owner );
        }
        catch ( IllegalAccessException e )
        {
            // we set the field to accessible=true so this shouldn't happen, but just in case...
            throw new RuntimeException( e );
        }
    }

    /**
     * @return the list of declared fields in the current object.  this method filters out
     * fields that havea $ character in their name, as fields with that character in the name
     * seem to be inserted by the compiler in some circumstances.
     */
    private List<Field> getDeclaredFields()
    {
        List<Field> fields = new ArrayList<Field>();
        for ( Field field : getClass().getDeclaredFields() )
        {
            // aspectj introduces some fields whose names start with 'ajc$' (for pointcuts), and
            // I've witnessed the compiler add a '$this' field on an inner class while I was
            // running tests, so omit all fields with a $ in their name.
            if ( !field.getName().contains( "$" ) )
            {
                fields.add( field );
            }
        }
        return fields;
    }
}