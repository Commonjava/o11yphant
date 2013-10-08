package org.commonjava.maven.galley.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.PathGenerator;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.maven.galley.util.AtomicFileOutputStreamWrapper;
import org.commonjava.util.logging.Logger;

@Named( "file-galley-cache" )
public class FileCacheProvider
    implements CacheProvider
{

    private final Logger logger = new Logger( getClass() );

    private final Map<ConcreteResource, Transfer> transferCache = new ConcurrentHashMap<>( 10000 );

    @Inject
    private FileCacheProviderConfig config;

    @Inject
    private PathGenerator pathGenerator;

    @Inject
    private FileEventManager fileEventManager;

    @Inject
    private TransferDecorator transferDecorator;

    protected FileCacheProvider()
    {
    }

    public FileCacheProvider( final File cacheBasedir, final PathGenerator pathGenerator, final FileEventManager fileEventManager,
                              final TransferDecorator transferDecorator, final boolean aliasLinking )
    {
        this.pathGenerator = pathGenerator;
        this.fileEventManager = fileEventManager;
        this.transferDecorator = transferDecorator;
        this.config = new FileCacheProviderConfig( cacheBasedir ).withAliasLinking( aliasLinking );
    }

    public FileCacheProvider( final FileCacheProviderConfig config, final PathGenerator pathGenerator, final FileEventManager fileEventManager,
                              final TransferDecorator transferDecorator )
    {
        this.config = config;
        this.pathGenerator = pathGenerator;
        this.fileEventManager = fileEventManager;
        this.transferDecorator = transferDecorator;
    }

    public FileCacheProvider( final File cacheBasedir, final PathGenerator pathGenerator, final FileEventManager fileEventManager,
                              final TransferDecorator transferDecorator )
    {
        this( cacheBasedir, pathGenerator, fileEventManager, transferDecorator, true );
    }

    @Override
    public File getDetachedFile( final ConcreteResource resource )
    {
        // TODO: this might be a bit heavy-handed, but we need to be sure. 
        // Maybe I can improve it later.
        final Transfer txfr = getTransfer( resource );
        synchronized ( txfr )
        {
            final File f = new File( getFilePath( resource ) );

            if ( !resource.isRoot() && f.exists() && !f.isDirectory() )
            {
                final long current = System.currentTimeMillis();
                final long lastModified = f.lastModified();
                final int tos =
                    resource.getTimeoutSeconds() < Location.MIN_CACHE_TIMEOUT_SECONDS ? Location.MIN_CACHE_TIMEOUT_SECONDS
                                    : resource.getTimeoutSeconds();

                final long timeout = TimeUnit.MILLISECONDS.convert( tos, TimeUnit.SECONDS );

                if ( current - lastModified > timeout )
                {
                    final File mved = new File( f.getPath() + SUFFIX_TO_DELETE );
                    f.renameTo( mved );

                    try
                    {
                        logger.info( "Deleting cached file: %s (moved to: %s)\n  due to timeout after: %s\n  elapsed: %s\n  original timeout in seconds: %s",
                                     f, mved, timeout, ( current - lastModified ), tos );

                        if ( mved.exists() )
                        {
                            FileUtils.forceDelete( mved );
                        }
                    }
                    catch ( final IOException e )
                    {
                        logger.error( "Failed to delete: %s.", e, f );
                    }
                }
            }

            return f;
        }
    }

    @Override
    public boolean isDirectory( final ConcreteResource resource )
    {
        return getDetachedFile( resource ).isDirectory();
    }

    @Override
    public InputStream openInputStream( final ConcreteResource resource )
        throws IOException
    {
        return new FileInputStream( getDetachedFile( resource ) );
    }

    @Override
    public OutputStream openOutputStream( final ConcreteResource resource )
        throws IOException
    {
        final File file = getDetachedFile( resource );

        final File dir = file.getParentFile();
        if ( !dir.isDirectory() && !dir.mkdirs() )
        {
            throw new IOException( "Cannot create directory: " + dir );
        }

        return new AtomicFileOutputStreamWrapper( file );
    }

    @Override
    public boolean exists( final ConcreteResource resource )
    {
        return getDetachedFile( resource ).exists();
    }

    @Override
    public void copy( final ConcreteResource from, final ConcreteResource to )
        throws IOException
    {
        FileUtils.copyFile( getDetachedFile( from ), getDetachedFile( to ) );
    }

    @Override
    public boolean delete( final ConcreteResource resource )
        throws IOException
    {
        return getDetachedFile( resource ).delete();
    }

    @Override
    public String[] list( final ConcreteResource resource )
    {
        final String[] listing = getDetachedFile( resource ).list();
        if ( listing == null )
        {
            return null;
        }

        final List<String> list = new ArrayList<>( Arrays.asList( listing ) );
        for ( final Iterator<String> it = list.iterator(); it.hasNext(); )
        {
            final String fname = it.next();
            if ( fname.charAt( 0 ) == '.' )
            {
                it.remove();
                continue;
            }

            for ( final String suffix : HIDDEN_SUFFIXES )
            {
                if ( fname.endsWith( suffix ) )
                {
                    it.remove();
                }
            }
        }

        return list.toArray( new String[list.size()] );
    }

    @Override
    public void mkdirs( final ConcreteResource resource )
        throws IOException
    {
        getDetachedFile( resource ).mkdirs();
    }

    @Override
    public void createFile( final ConcreteResource resource )
        throws IOException
    {
        getDetachedFile( resource ).createNewFile();
    }

    @Override
    public void createAlias( final ConcreteResource from, final ConcreteResource to )
        throws IOException
    {
        // if the download landed in a different repository, copy it to the current one for
        // completeness...
        final Location fromKey = from.getLocation();
        final Location toKey = to.getLocation();
        final String fromPath = from.getPath();
        final String toPath = to.getPath();

        if ( fromKey != null && toKey != null && !fromKey.equals( toKey ) && fromPath != null && toPath != null && !fromPath.equals( toPath ) )
        {
            if ( config.isAliasLinking() )
            {
                final File fromFile = getDetachedFile( from );
                final File toFile = getDetachedFile( to );

                Files.createLink( Paths.get( fromFile.toURI() ), Paths.get( toFile.toURI() ) );
            }
            else
            {
                copy( from, to );
            }
        }
    }

    @Override
    public String getFilePath( final ConcreteResource resource )
    {
        return Paths.get( config.getCacheBasedir()
                                .getPath(), pathGenerator.getFilePath( resource ) )
                    .toString();
    }

    @Override
    public synchronized Transfer getTransfer( final ConcreteResource resource )
    {
        Transfer t = transferCache.get( resource );
        if ( t == null )
        {
            t = new Transfer( resource, this, fileEventManager, transferDecorator );
            transferCache.put( resource, t );
        }

        return t;
    }

    @Override
    public void clearTransferCache()
    {
        transferCache.clear();
    }

}
