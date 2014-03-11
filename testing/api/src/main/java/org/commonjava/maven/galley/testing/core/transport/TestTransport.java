/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.galley.testing.core.transport;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Default;
import javax.inject.Named;
import javax.inject.Singleton;

import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.transport.DownloadJob;
import org.commonjava.maven.galley.spi.transport.ExistenceJob;
import org.commonjava.maven.galley.spi.transport.ListingJob;
import org.commonjava.maven.galley.spi.transport.PublishJob;
import org.commonjava.maven.galley.spi.transport.Transport;
import org.commonjava.maven.galley.testing.core.cdi.TestData;
import org.commonjava.maven.galley.testing.core.transport.job.TestDownload;
import org.commonjava.maven.galley.testing.core.transport.job.TestExistence;
import org.commonjava.maven.galley.testing.core.transport.job.TestListing;
import org.commonjava.maven.galley.testing.core.transport.job.TestPublish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stubbed out {@link Transport} implementation that allows pre-registering
 * {@link DownloadJob} and {@link PublishJob} instances before attempting to 
 * access them from a higher component (such as {@link TransferManagerImpl}).
 * 
 * @author jdcasey
 */
@TestData
@Default
@Named( "test-galley-transport" )
@Singleton
public class TestTransport
    implements Transport
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<ConcreteResource, TestDownload> downloads = new HashMap<ConcreteResource, TestDownload>();

    private final Map<ConcreteResource, TestPublish> publishes = new HashMap<ConcreteResource, TestPublish>();

    private final Map<ConcreteResource, TestListing> listings = new HashMap<ConcreteResource, TestListing>();

    private final Map<ConcreteResource, TestExistence> exists = new HashMap<ConcreteResource, TestExistence>();

    /**
     * Use this to pre-register data for a {@link DownloadJob} you plan on accessing during
     * your unit test.
     */
    public void registerDownload( final ConcreteResource resource, final TestDownload job )
    {
        logger.info( "Got transport: {}", this );
        logger.info( "Registering download: {} with job: {}", resource, job );
        downloads.put( resource, job );
    }

    /**
     * Use this to pre-register the result for a {@link PublishJob} you plan on accessing during
     * your unit test.
     */
    public void registerPublish( final ConcreteResource resource, final TestPublish job )
    {
        logger.info( "Registering publish: {} with job: {}", resource, job );
        publishes.put( resource, job );
    }

    public void registerListing( final ConcreteResource resource, final TestListing listing )
    {
        listings.put( resource, listing );
    }

    public void registerExistence( final ConcreteResource resource, final TestExistence exists )
    {
        this.exists.put( resource, exists );
    }

    // Transport implementation...

    @Override
    public DownloadJob createDownloadJob( final ConcreteResource resource, final Transfer target, final int timeoutSeconds )
        throws TransferException
    {
        final TestDownload job = downloads.get( resource );
        logger.info( "Download for: {} is: {}", resource, job );
        if ( job == null )
        {
            throw new TransferException( "No download registered for the endpoint: {}", resource );
        }

        job.setTransfer( target );
        return job;
    }

    @Override
    public PublishJob createPublishJob( final ConcreteResource resource, final InputStream stream, final long length, final int timeoutSeconds )
        throws TransferException
    {
        return createPublishJob( resource, stream, length, null, timeoutSeconds );
    }

    @Override
    public PublishJob createPublishJob( final ConcreteResource resource, final InputStream stream, final long length, final String contentType,
                                        final int timeoutSeconds )
        throws TransferException
    {
        final TestPublish job = publishes.get( resource );
        if ( job == null )
        {
            throw new TransferException( "No publish job registered for: {}", resource );
        }

        job.setContent( stream, length, contentType );
        return job;
    }

    @Override
    public boolean handles( final Location location )
    {
        return true;
    }

    @Override
    public ListingJob createListingJob( final ConcreteResource resource, final int timeoutSeconds )
        throws TransferException
    {
        final TestListing job = listings.get( resource );
        if ( job == null )
        {
            throw new TransferException( "No listing job registered for: {}", resource );
        }

        return job;
    }

    @Override
    public ExistenceJob createExistenceJob( final ConcreteResource resource, final int timeoutSeconds )
        throws TransferException
    {
        final TestExistence job = exists.get( resource );
        if ( job == null )
        {
            throw new TransferException( "No existence job registered for: {}", resource );
        }

        return job;
    }

}