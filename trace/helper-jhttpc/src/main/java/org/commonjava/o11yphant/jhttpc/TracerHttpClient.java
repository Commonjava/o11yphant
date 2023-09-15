/**
 * Copyright (C) 2020-2023 Red Hat, Inc. (https://github.com/commonjava/o11yphant)
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
package org.commonjava.o11yphant.jhttpc;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.commonjava.o11yphant.trace.TraceManager;
import org.commonjava.o11yphant.trace.spi.adapter.SpanAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import static org.commonjava.o11yphant.trace.TraceManager.addFieldToActiveSpan;
import static org.commonjava.o11yphant.trace.httpclient.HttpClientTools.contextInjector;

public class TracerHttpClient
        extends CloseableHttpClient
{
    private final CloseableHttpClient delegate;

    private final Optional<TraceManager> traceManager;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public TracerHttpClient( CloseableHttpClient delegate, Optional<TraceManager> traceManager )
    {
        this.traceManager = traceManager;
        this.delegate = delegate;
    }

    @Override
    protected CloseableHttpResponse doExecute( HttpHost target, HttpRequest request, HttpContext context )
            throws IOException
    {
        try
        {
            URL url = new URL( request.getRequestLine().getUri() );
            Optional<SpanAdapter> span;
            if ( traceManager.isPresent() )
            {
                span = traceManager.get()
                                   .startClientRequestSpan(
                                           request.getRequestLine().getMethod() + "_" + url.getHost() + "_"
                                                   + url.getPort(), contextInjector( request ) );
            }
            else
            {
                span = Optional.empty();
            }

            addFieldToActiveSpan( "target-http-url", request.getRequestLine().getUri() );
            printHttpRequestHeaders( request );
            CloseableHttpResponse response = delegate.execute( target, request, context );
            printHttpResponseHeaders( response );
            if ( response != null )
            {
                addFieldToActiveSpan( "target-http-status", response.getStatusLine().getStatusCode() );
            }

            return new SpanClosingResponse( response, span );
        }
        catch ( IOException e )
        {
            throw e;
        }
        catch ( Throwable t )
        {
            logger.error( "Failed to execute http request: " + t.getLocalizedMessage(), t );
            if ( t instanceof RuntimeException )
            {
                throw (RuntimeException) t;
            }
            throw new RuntimeException( "Failed to execute: " + t.getMessage(), t );
        }
    }

    private void printHttpRequestHeaders( HttpRequest request )
    {
        if ( logger.isTraceEnabled() )
        {
            Header[] headers = request.getAllHeaders();
            logger.trace( "========= Start print request headers for request {}: ====================",
                          request.getRequestLine() );
            for ( Header header : headers )
            {
                logger.trace( "{} -> {}", header.getName(), header.getValue() );
            }
            logger.trace( "========= Stop print request headers for request {}: ====================",
                          request.getRequestLine() );
        }
    }

    private void printHttpResponseHeaders( HttpResponse response )
    {
        if ( logger.isTraceEnabled() )
        {
            Header[] headers = response.getAllHeaders();
            logger.trace( "========= Start print response headers for response {}: ====================",
                          response.getStatusLine() );
            for ( Header header : headers )
            {
                logger.trace( "{} -> {}", header.getName(), header.getValue() );
            }
            logger.trace( "========= Stop print response headers for response {}: ====================",
                          response.getStatusLine() );
        }
    }

    @Override
    public void close()
            throws IOException
    {
        delegate.close();
    }

    @Override
    @Deprecated
    public HttpParams getParams()
    {
        return delegate.getParams();
    }

    @Override
    @Deprecated
    public ClientConnectionManager getConnectionManager()
    {
        return delegate.getConnectionManager();
    }
}
