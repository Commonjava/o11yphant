/**
 * Copyright (C) 2020 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.o11yphant.honeycomb;

import io.honeycomb.beeline.tracing.Span;
import io.honeycomb.libhoney.shaded.org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.commonjava.o11yphant.honeycomb.util.HeaderMetricConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isBlank;

@ApplicationScoped
public class HoneycombFilter
                implements Filter
{
    @Inject
    private DefaultHoneycombManager honeycombManager;

    @Inject
    private HoneycombConfiguration config;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void init( final FilterConfig filterConfig )
    {
    }

    @Override
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain chain )
                    throws IOException, ServletException
    {
        logger.trace( "START: {}", getClass().getSimpleName() );

        HttpServletRequest hsr = (HttpServletRequest) request;
        logger.debug( "START: {}", hsr.getPathInfo() );

        Span rootSpan = null;
        try
        {
            rootSpan = honeycombManager.startRootTracer( getEndpointName( hsr.getMethod(), hsr.getPathInfo() ) );
            if ( rootSpan != null )
            {
                rootSpan.addField( "path_info", hsr.getPathInfo() );
                addHeaderFields( hsr );
            }

            chain.doFilter( request, response );
        }
        finally
        {
            logger.debug( "END: {}", hsr.getPathInfo() );
            if ( rootSpan != null )
            {
                HttpServletResponse resp = (HttpServletResponse) response;
                honeycombManager.addSpanField( "status_code", getStatusLine( resp.getStatus() ) );

                honeycombManager.addFields( rootSpan );
                rootSpan.close();
                honeycombManager.endTrace();
            }

            logger.trace( "END: {}", getClass().getSimpleName() );
        }
    }

    private String getStatusLine( int status )
    {
        return status + " " + EnglishReasonPhraseCatalog.INSTANCE.getReason( status, null );
    }

    private String getEndpointName( String method, String pathInfo )
    {
        StringBuilder sb = new StringBuilder( method + "_" );
        String[] toks = pathInfo.split( "/" );
        for ( String s : toks )
        {
            if ( isBlank( s ) || "api".equals( s ) )
            {
                continue;
            }
            sb.append( s );
            if ( "admin".equals( s ) )
            {
                sb.append( "_" );
            }
            else
            {
                break;
            }
        }
        return sb.toString();
    }

    private void addHeaderFields( HttpServletRequest request )
    {
        for ( String header : HeaderMetricConstants.HEADERS )
        {
            String value = request.getHeader( header );
            honeycombManager.addSpanField( header, value == null ? "NOOP" : value );
        }
    }

    @Override
    public void destroy()
    {
    }

}
