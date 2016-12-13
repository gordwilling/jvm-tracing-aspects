package com.highbar.aspects;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.SourceLocation;

/**
 * <dl>
 * <dt><b>Description:</b>
 * <dd>
 * Logging aspect. This class enables logging at the entry and exit points of every method in the
 * com.highbar.sample package (and subpackages).  The logging message produced shows the entry parameter values
 * and return values for traced methods.  It is an indispensible tool for debugging and eliminates the need for a
 * formal debugger in most circumstances. (Particularly useful when the application is running on a remote system)
 * </p>
 * <p>
 * Typically log4j can include source location information in classes, but that mechanism doesn't work when using
 * aspects because the the aspect itself appears as the source of the log message instead of the advised method.
 * Consequently, the log message produced by these tracing aspects is designed
 * to include the correct source class, method name, source file, and line number in the message they produce (the latter two are
 * only available when compiled in the recommended debug mode). This means that you will want to minimize the message content
 * set in your log4j configuration because it's done here already.  Here is an example of my log4j appender configuration that
 * leaves the formatting up to this aspect:
 * </p>
 * <tt>
 * log4j.appender.stdout.layout.ConversionPattern=%5p [%t] %m%n
 * </tt>
 * <p>
 * Basically, it includes the log level, the thread id, and the log message (produced here).  Obviously, for
 * non-trace logging levels that do not involve these aspects, you'll want to use your standard log4j message format.
 * </p>
 * <p>
 * <b>
 * An important note: This class requires that the aspectj compiler compile the entire project (instead of javac).
 * </b>
 * The alternative is to use run-time weaving, which I obviously haven't done here.
 * <p>
 * Also note that at time of writing, the version of aspectj in use was 1.6.11
 * </p>
 * </dd>
 * </dt>
 * </dl>
 *
 * @author Gordon Wallace - gw@highbar.com
 * @version 1.0
 */
@Aspect
public class TraceAspect
{
    // Fairly cheap way to prevent logging the same exception multiple
    // times as it bubbles up the call hierarchy
    Throwable previousThrowable;

    @Pointcut("(execution(* com.highbar.sample..*(..)) || execution(com.highbar.sample..new(..))) && " +
                      "!within(com.highbar.util.AbstractDomainObject) && !within(TraceAspect)")
    public void traceable()
    {
    }

    /**
     * Gets the logger instance associated with target class of the given join point
     * @param joinPoint the current join point
     * @return the Logger instance belonging to the target class of the joinpoint
     */
    private Logger getLogger(JoinPoint joinPoint )
    {
        try
        {
            return Logger.getLogger( joinPoint.getTarget().getClass() );
        }
        catch ( NullPointerException e )
        {
            // this is usually seen when the class is static, thus the target Object doesn't exist.
            // looking into the stack trace seems to get us a consistently accurate origin class name, at least in
            // the basic circumstances I was able to observe...(running in jetty locally)
            //
            // todo: verify this works reliably in the general case
            return Logger.getLogger( Thread.currentThread().getStackTrace()[3].getClassName() );
        }
    }

    @AfterThrowing(value="traceable()", throwing="throwable")
    public void logException( JoinPoint joinPoint, Throwable throwable )
    {
        Logger log = getLogger( joinPoint );

        if ( log.isTraceEnabled() && throwable != previousThrowable )
        {
            // only log an exception instance at its origin
            previousThrowable = throwable;

            Signature signature = joinPoint.getSignature();
            SourceLocation sourceLocation = joinPoint.getSourceLocation();

            StringBuilder message = new StringBuilder( "Exception at " )
                    .append( log.getName() )
                    .append( '.' )
                    .append( signature.getName() )
                    .append( '(' )
                    .append( sourceLocation.getFileName() )
                    .append( ':' )
                    .append( sourceLocation.getLine() )
                    .append( ") " );

            log.trace( message, throwable );
        }
    }

    @Before("traceable()")
    public void entry( JoinPoint joinPoint )
    {
        Logger log = getLogger( joinPoint );

        if ( log.isTraceEnabled() )
        {
            Signature signature = joinPoint.getSignature();
            SourceLocation sourceLocation = joinPoint.getSourceLocation();

            StringBuilder message = new StringBuilder( "Entry at ")
                    .append( log.getName() )
                    .append( '.' )
                    .append( signature.getName() )
                    .append( '(' )
                    .append( sourceLocation.getFileName() )
                    .append( ':' )
                    .append( sourceLocation.getLine() )
                    .append( ") " );

            Object[] args = joinPoint.getArgs();

            for ( int i = 0; i < args.length; i++ )
            {
                Pair<String,String> pair = getPair( args[i] );

                message.append( "arg[" )
                       .append( i )
                       .append( "] {type=" )
                       .append( pair.getPea() )
                       .append( "; value=" )
                       .append( pair.getCarrot() )
                       .append( "}, " );
            }

            // remove the superfluous comma
            if ( message.charAt( message.length() - 1 ) == ',' )
            {
                message.setLength( message.length() - 1 );
            }

            log.trace( message );

            NDC.push( " " );
        }
    }

    @AfterReturning(value="traceable()",returning="returnValue")
    public void _exit( JoinPoint joinPoint, Object returnValue )
    {
        Logger log = getLogger( joinPoint );

        if ( log.isTraceEnabled() )
        {
            NDC.pop();

            Signature signature = joinPoint.getSignature();
            SourceLocation sourceLocation = joinPoint.getSourceLocation();

            StringBuilder message = new StringBuilder( " Exit at ")
                    .append( log.getName() )
                    .append( '.' )
                    .append( signature.getName() )
                    .append( '(' )
                    .append( sourceLocation.getFileName() )
                    .append( ':' )
                    .append( sourceLocation.getLine() )
                    .append( ") " );


            if ( !signature.toString().startsWith( "void " ) )
            {
                Pair<String,String> pair = getPair( returnValue );

                message.append( "returned {type=" )
                       .append( pair.getPea() )
                       .append( "; value=" )
                       .append( pair.getCarrot() )
                       .append( "}" );
            }

            log.trace( message );
        }
    }

    /**
     * Gets the type and value of the specified object, both in string form.
     * @param o the object for which the type and value strings are to be produced
     * @return a type-value pair. the object type can be accessed via the {@link Pair#getPea()} method, and the
     *         object value can be accessed via the {@link Pair#getCarrot()} method
     */
    private Pair<String,String> getPair( Object o )
    {
        Pair<String,String> pair = new Pair<String,String>();

        if ( o == null )
        {
            pair.setPea( "?" );
            pair.setCarrot( "null" );
        }
        else if ( o.getClass().isArray() )
        {
            if ( o instanceof Object[] )
            {
                pair.setPea( "Object[]" );
                pair.setCarrot( Arrays.toString( (Object[])o ) );
            }
            else if ( o instanceof int[] )
            {
                pair.setPea( "int[]" );
                pair.setCarrot( Arrays.toString( (int[])o ) );
            }
            else if ( o instanceof long[] )
            {
                pair.setPea( "long[]" );
                pair.setCarrot( Arrays.toString( (long[])o ) );
            }
            else if ( o instanceof short[] )
            {
                pair.setPea( "short[]" );
                pair.setCarrot( Arrays.toString( (short[])o ) );
            }
            else if ( o instanceof char[] )
            {
                pair.setPea( "char[]" );
                pair.setCarrot( Arrays.toString( (char[])o ) );
            }
            else if ( o instanceof byte[] )
            {
                pair.setPea( "byte[]" );
                pair.setCarrot( Arrays.toString( (byte[])o ) );
            }
            else if ( o instanceof boolean[] )
            {
                pair.setPea( "boolean[]" );
                pair.setCarrot( Arrays.toString( (boolean[])o ) );
            }
            else if ( o instanceof float[] )
            {
                pair.setPea( "float[]" );
                pair.setCarrot( Arrays.toString( (float[])o ) );
            }
            else if ( o instanceof double[] )
            {
                pair.setPea( "double[]" );
                pair.setCarrot( Arrays.toString( (double[])o ) );
            }
        }
        else // some other object
        {
            pair.setPea( o.getClass().getSimpleName() );
            pair.setCarrot( String.valueOf( o ) );
        }

        return pair;
    }

    /**
     * A pair of values
     * @param <P> the type of one value
     * @param <C> the type of the other value
     */
    private class Pair<P,C>
    {
        P p;
        C c;

        public P getPea()
        {
            return p;
        }

        public void setPea( P pea )
        {
            this.p = pea;
        }

        public C getCarrot()
        {
            return c;
        }

        public void setCarrot( C value )
        {
            this.c = value;
        }
    }
}
