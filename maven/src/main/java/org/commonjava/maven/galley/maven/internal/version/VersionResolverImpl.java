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
package org.commonjava.maven.galley.maven.internal.version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.VersionSpec;
import org.commonjava.maven.atlas.ident.version.VersionUtils;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenMetadataView;
import org.commonjava.maven.galley.maven.parse.MavenMetadataReader;
import org.commonjava.maven.galley.maven.spi.version.VersionResolver;
import org.commonjava.maven.galley.model.Location;

@ApplicationScoped
public class VersionResolverImpl
    implements VersionResolver
{

    //    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MavenMetadataReader metadataReader;

    protected VersionResolverImpl()
    {
    }

    public VersionResolverImpl( final MavenMetadataReader metadataReader )
    {
        this.metadataReader = metadataReader;
    }

    @Override
    public ProjectVersionRef resolveVariableVersions( final List<? extends Location> locations, final ProjectVersionRef ref )
        throws TransferException
    {
        if ( !ref.getVersionSpec()
                 .isSingle() )
        {
            return resolveMulti( locations, ref );
        }
        else if ( ref.isSnapshot() )
        {
            return resolveSnapshot( locations, ref );
        }
        else
        {
            return ref;
        }
    }

    private ProjectVersionRef resolveSnapshot( final List<? extends Location> locations, final ProjectVersionRef ref )
        throws TransferException
    {
        try
        {
            final MavenMetadataView metadata = metadataReader.getMetadata( ref, locations );

            if ( metadata != null )
            {
                final String latest = metadata.resolveSingleValue( "/metadata/versioning/latest" );
                if ( latest != null )
                {
                    try
                    {
                        return ref.selectVersion( VersionUtils.createSingleVersion( latest ) );
                    }
                    catch ( final InvalidVersionSpecificationException e )
                    {
                        throw new TransferException( "Unparsable version spec found in metadata: '{}'. Reason: {}", e, latest, e.getMessage() );
                    }
                }
            }
        }
        catch ( final GalleyMavenException e )
        {
            throw new TransferException( "Failed to resolve/parse metadata for snapshot version of: {}. Reason: {}", e, ref, e.getMessage() );
        }

        return null;
    }

    // TODO: pluggable selection strategy!
    private ProjectVersionRef resolveMulti( final List<? extends Location> locations, final ProjectVersionRef ref )
        throws TransferException
    {
        final List<String> allVersions = new ArrayList<String>();
        try
        {
            final MavenMetadataView metadata = metadataReader.getMetadata( ref.asProjectRef(), locations );

            if ( metadata != null )
            {
                final List<String> versions = metadata.resolveValues( "/metadata/versioning/versions/version" );

                if ( versions != null )
                {
                    for ( final String version : versions )
                    {
                        if ( !allVersions.contains( version ) )
                        {
                            allVersions.add( version );
                        }
                    }
                }
            }
        }
        catch ( final GalleyMavenException e )
        {
            throw new TransferException( "Failed to resolve/parse metadata for variable version of: {}. Reason: {}", e, ref, e.getMessage() );
        }

        //        logger.info( "{}: RAW versions found: {}", ref, new JoinString( ", ", allVersions ) );
        final LinkedList<SingleVersion> specs = new LinkedList<SingleVersion>();
        if ( allVersions != null && !allVersions.isEmpty() )
        {
            for ( String ver : allVersions )
            {
                if ( ver == null )
                {
                    continue;
                }

                ver = ver.trim();
                if ( ver.length() < 1 )
                {
                    continue;
                }

                try
                {
                    specs.add( VersionUtils.createSingleVersion( ver ) );
                }
                catch ( final InvalidVersionSpecificationException e )
                {
                    throw new TransferException( "Unparsable version spec found in metadata: '{}'. Reason: {}", e, ver, e.getMessage() );
                }
            }
        }

        //        logger.info( "{}: Available versions are: {}", ref, new JoinString( ", ", specs ) );
        if ( !specs.isEmpty() )
        {
            final VersionSpec spec = ref.getVersionSpec();

            Collections.sort( specs );
            SingleVersion ver = null;
            do
            {
                ver = specs.removeLast();
                //                logger.info( "Checking whether {} is concrete...", ver );
            }
            while ( !ver.isConcrete() || !spec.contains( ver ) );

            if ( ver != null )
            {
                //                logger.info( "Selecting {} for {}", ver, ref );
                return ref.selectVersion( ver );
            }
        }

        return null;
    }
}