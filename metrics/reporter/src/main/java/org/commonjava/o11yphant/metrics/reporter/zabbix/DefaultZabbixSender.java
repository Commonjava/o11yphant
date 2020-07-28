/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.o11yphant.metrics.reporter.zabbix;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.commonjava.o11yphant.metrics.reporter.zabbix.api.IndyZabbixApi;
import org.commonjava.o11yphant.metrics.reporter.zabbix.api.ZabbixApi;
import org.commonjava.o11yphant.metrics.reporter.zabbix.cache.ZabbixCacheStorage;
import org.commonjava.o11yphant.metrics.reporter.zabbix.sender.DataObject;
import org.commonjava.o11yphant.metrics.reporter.zabbix.sender.SenderResult;
import org.commonjava.o11yphant.metrics.reporter.zabbix.sender.ZabbixSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultZabbixSender
{
    private static final Logger logger = LoggerFactory.getLogger( DefaultZabbixSender.class );

    private ZabbixSender sender;

    private boolean bCreateNotExistHostGroup = true;

    private boolean bCreateNotExistHost = true;

    private boolean bCreateNotExistItem = true;

    private boolean bCreateNotExistZabbixApi = true;

    private ZabbixApi zabbixApi;

    private String zabbixHostUrl;

    private String hostGroup = "NOS";//// default host group

    private String group = "NOS";

    private long clock = 0l;

    private String hostName;

    private String ip;

    private String zabbixUserName;

    private String zabbixUserPwd;

    private ZabbixCacheStorage zabbixCacheStorage;

    private static final String regEx = "^-?[0-9]+$";

    private static final Pattern pat = Pattern.compile( regEx );

    public static Builder create()
    {
        return new Builder();
    }

    public static class Builder
    {
        protected boolean bCreateNotExistHostGroup = true;

        protected boolean bCreateNotExistHost = true;

        protected boolean bCreateNotExistItem = true;

        protected boolean bCreateNotExistZabbixSender = true;

        protected ZabbixApi zabbixApi;

        protected String zabbixHostUrl;

        protected String hostGroup = "NOS";//// default host group

        protected String group = "NOS";

        protected long clock = 0l;

        protected String hostName;

        protected String ip;

        protected String zabbixUserName;

        protected String zabbixUserPwd;

        protected String zabbixHost;

        protected int zabbixPort;

        protected ZabbixCacheStorage zabbixCacheStorage;

        public Builder metricsZabbixCache( ZabbixCacheStorage zabbixCacheStorage )
        {
            this.zabbixCacheStorage = zabbixCacheStorage;
            return this;
        }

        public Builder zabbixHost( String zabbixHost )
        {
            this.zabbixHost = zabbixHost;
            return this;
        }

        public Builder hostName( String hostName )
        {
            this.hostName = hostName;
            return this;
        }

        public Builder zabbixPort( int zabbixPort )
        {
            this.zabbixPort = zabbixPort;
            return this;
        }

        public Builder bCreateNotExistHostGroup( boolean bCreateNotExistHostGroup )
        {
            this.bCreateNotExistHostGroup = bCreateNotExistHostGroup;
            return this;
        }

        public Builder bCreateNotExistHost( boolean bCreateNotExistHost )
        {
            this.bCreateNotExistHost = bCreateNotExistHost;
            return this;
        }

        public Builder bCreateNotExistItem( boolean bCreateNotExistItem )
        {
            this.bCreateNotExistItem = bCreateNotExistItem;
            return this;
        }

        public Builder bCreateNotExistZabbixSender( boolean bCreateNotExistZabbixSender )
        {
            this.bCreateNotExistZabbixSender = bCreateNotExistZabbixSender;
            return this;
        }

        public Builder zabbixApi( ZabbixApi zabbixApi )
        {
            this.zabbixApi = zabbixApi;
            return this;
        }

        public Builder zabbixHostUrl( String zabbixHostUrl )
        {
            this.zabbixHostUrl = zabbixHostUrl;
            return this;
        }

        public Builder hostGroup( String hostGroup )
        {
            this.hostGroup = hostGroup;
            return this;
        }

        public Builder group( String group )
        {
            this.group = group;
            return this;
        }

        public Builder ip( String ip )
        {
            this.ip = ip;
            return this;
        }

        public Builder zabbixUserName( String zabbixUserName )
        {
            this.zabbixUserName = zabbixUserName;
            return this;
        }

        public Builder clock( long clock )
        {
            this.clock = clock;
            return this;
        }

        public Builder zabbixUserPwd( String zabbixUserPwd )
        {
            this.zabbixUserPwd = zabbixUserPwd;
            return this;
        }

        public DefaultZabbixSender build()
        {
            return new DefaultZabbixSender( this.bCreateNotExistHostGroup, this.bCreateNotExistHost,
                                            this.bCreateNotExistItem, this.bCreateNotExistZabbixSender, this.zabbixApi,
                                            this.zabbixHostUrl, this.hostGroup, this.group, this.clock, this.hostName,
                                            this.ip, this.zabbixUserName, this.zabbixUserPwd, this.zabbixHost,
                                            this.zabbixPort, this.zabbixCacheStorage );
        }

    }

    public DefaultZabbixSender( boolean bCreateNotExistHostGroup, boolean bCreateNotExistHost,
                                boolean bCreateNotExistItem, boolean bCreateNotExistZabbixApi, ZabbixApi zabbixApi,
                                String zabbixHostUrl, String hostGroup, String group, long clock, String hostName,
                                String ip, String zabbixUserName, String zabbixUserPwd, String zabbixHost,
                                int zabbixPort, ZabbixCacheStorage zabbixCacheStorage )
    {

        this.bCreateNotExistHostGroup = bCreateNotExistHostGroup;
        this.bCreateNotExistHost = bCreateNotExistHost;
        this.bCreateNotExistItem = bCreateNotExistItem;
        this.bCreateNotExistZabbixApi = bCreateNotExistZabbixApi;
        this.zabbixApi = zabbixApi;
        this.zabbixHostUrl = zabbixHostUrl;
        this.hostGroup = hostGroup;
        this.group = group;
        this.clock = clock;
        this.hostName = hostName;
        this.ip = ip;
        this.zabbixUserName = zabbixUserName;
        this.zabbixUserPwd = zabbixUserPwd;
        this.zabbixCacheStorage = zabbixCacheStorage;
        this.sender = new ZabbixSender( zabbixHost, zabbixPort );
    }

    String checkHostGroup( String hostGroup ) throws Exception
    {
        if ( zabbixCacheStorage.getHostGroup( hostGroup ) == null )
        {
            try
            {
                this.zabbixApiInit();
                String groupid = zabbixApi.getHostgroup( hostGroup );
                if ( groupid == null )
                {
                    groupid = zabbixApi.hostgroupCreate( hostGroup );
                    zabbixCacheStorage.putHostGroup( hostGroup, groupid );
                }
                zabbixCacheStorage.putHostGroup( hostGroup, groupid );
                return groupid;
            }
            finally
            {
                this.destroy();
            }
        }
        return null;
    }

    String checkHost( String host, String ip ) throws Exception
    {
        try
        {
            if ( zabbixCacheStorage.getHost( host ) == null )
            {
                this.zabbixApiInit();
                String hostid = zabbixApi.getHost( host );
                if ( hostid != null )
                {
                    zabbixCacheStorage.putHost( host, hostid );

                }
                else
                {// host not exists, create it.

                    hostid = zabbixApi.hostCreate( host, zabbixCacheStorage.getHostGroup( hostGroup ), ip );
                    zabbixCacheStorage.putHost( host, hostid );
                }
                return hostid;
            }
        }
        finally
        {
            this.destroy();
        }
        return null;
    }

    private String itemCacheKey( String host, String item )
    {
        return host + ":" + item;
    }

    String checkItem( String host, String item, int valueType ) throws Exception
    {

        try
        {
            if ( zabbixCacheStorage.getItem( itemCacheKey( host, item ) ) == null )
            {
                this.zabbixApiInit();

                String itemid = zabbixApi.getItem( host, item, zabbixCacheStorage.getHost( host ) );
                if ( itemid == null )
                {
                    itemid = zabbixApi.createItem( host, item, zabbixCacheStorage.getHost( host ), valueType );
                    zabbixCacheStorage.putItem( itemCacheKey( host, item ), itemid );
                }
                else
                {
                    // put into metricsZabbixCache
                    zabbixCacheStorage.putItem( itemCacheKey( host, item ), itemid );
                }
                return itemid;
            }
        }
        finally
        {
            this.destroy();
        }

        return null;
    }

    public SenderResult send( DataObject dataObject ) throws Exception
    {
        return this.send( dataObject, System.currentTimeMillis() / 1000L );
    }

    public SenderResult send( DataObject dataObject, long clock ) throws Exception
    {
        return this.send( Collections.singletonList( dataObject ), clock );
    }

    public SenderResult send( List<DataObject> dataObjectList ) throws Exception
    {
        return this.send( dataObjectList, System.currentTimeMillis() / 1000L );
    }

    /**
     *
     * @param dataObjectList
     * @param clock
     *            TimeUnit is SECONDS.
     * @return
     * @throws IOException
     */
    public SenderResult send( List<DataObject> dataObjectList, long clock ) throws Exception
    {
        if ( bCreateNotExistHostGroup )
        {
            try
            {
                checkHostGroup( hostGroup );
            }
            catch ( Exception e )
            {
                logger.error( "Check HostGroup of Zabbix is error:" + e.getMessage() );
                throw e;
            }
        }
        if ( bCreateNotExistHost )
        {
            try
            {
                checkHost( hostName, ip );
            }
            catch ( Exception e )
            {
                logger.error( "Check Host of Zabbix is error:" + e.getMessage() );
                throw e;
            }
        }

        if ( bCreateNotExistItem )
        {
            for ( DataObject object : dataObjectList )
            {
                String key = object.getKey();
                int vauleType = 0;
                Matcher mat = pat.matcher( object.getValue() );
                if ( !mat.find() )
                {
                    vauleType = 4;
                }
                try
                {
                    checkItem( hostName, key, vauleType );
                }
                catch ( Exception e )
                {
                    logger.error( "Check Item of Zabbix is error:" + e.getMessage() );
                    throw e;
                }
            }
        }

        try
        {
            SenderResult senderResult = sender.send( dataObjectList, clock );
            if ( !senderResult.success() )
            {
                logger.error( "send data to zabbix server error! senderResult:" + senderResult );
            }
            return senderResult;
        }
        catch ( IOException e )
        {
            logger.error( "send data to zabbix server error!", e );
            throw e;
        }
    }

    public void destroy()
    {
        if ( bCreateNotExistZabbixApi )
        {
            return;
        }
        if ( zabbixApi != null )
            zabbixApi.destroy();
    }

    private void zabbixApiInit() throws Exception
    {
        if ( !bCreateNotExistZabbixApi )
        {
            return;
        }
        if ( this.zabbixHostUrl == null || "".equals( this.zabbixHostUrl ) )
        {
            throw new Exception( "can not find Zabbix's Host" );
        }

        zabbixApi = new IndyZabbixApi( this.zabbixHostUrl, createClient( new URL( zabbixHostUrl ).getHost() ) );

        zabbixApi.init();

        if ( this.zabbixUserName == null || "".equals( this.zabbixUserName ) || this.zabbixUserPwd == null || "".equals(
                        this.zabbixUserPwd ) )
        {
            throw new Exception( "can not find Zabbix's username or password" );
        }
        boolean login = zabbixApi.login( this.zabbixUserName, this.zabbixUserPwd );

        logger.info( "User:" + this.zabbixUserName + " login is " + login );
    }

    public CloseableHttpClient createClient( final String siteId ) throws Exception
    {
        return HttpClients.createDefault();
    }

    public ZabbixApi getZabbixApi()
    {
        return zabbixApi;
    }

    public void setZabbixApi( ZabbixApi zabbixApi )
    {
        this.zabbixApi = zabbixApi;
    }

    public boolean isbCreateNotExistHost()
    {
        return bCreateNotExistHost;
    }

    public void setbCreateNotExistHost( boolean bCreateNotExistHost )
    {
        this.bCreateNotExistHost = bCreateNotExistHost;
    }

    public String getHostGroup()
    {
        return hostGroup;
    }

    public void setHostGroup( String hostGroup )
    {
        this.hostGroup = hostGroup;
    }

    public boolean isbCreateNotExistItem()
    {
        return bCreateNotExistItem;
    }

    public void setbCreateNotExistItem( boolean bCreateNotExistItem )
    {
        this.bCreateNotExistItem = bCreateNotExistItem;
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup( String group )
    {
        this.group = group;
    }

    public long getClock()
    {
        return clock;
    }

    public void setClock( long clock )
    {
        this.clock = clock;
    }

    public String getHostName()
    {
        return hostName;
    }

    public void setHostName( String hostName )
    {
        this.hostName = hostName;
    }

    public String getIp()
    {
        return ip;
    }

    public void setIp( String ip )
    {
        this.ip = ip;
    }

    public boolean isbCreateNotExistHostGroup()
    {
        return bCreateNotExistHostGroup;
    }

    public void setbCreateNotExistHostGroup( boolean bCreateNotExistHostGroup )
    {
        this.bCreateNotExistHostGroup = bCreateNotExistHostGroup;
    }

    public String getZabbixHostUrl()
    {
        return zabbixHostUrl;
    }

    public void setZabbixHostUrl( String zabbixHostUrl )
    {
        this.zabbixHostUrl = zabbixHostUrl;
    }

    public String getZabbixUserName()
    {
        return zabbixUserName;
    }

    public void setZabbixUserName( String zabbixUserName )
    {
        this.zabbixUserName = zabbixUserName;
    }

    public String getZabbixUserPwd()
    {
        return zabbixUserPwd;
    }

    public void setZabbixUserPwd( String zabbixUserPwd )
    {
        this.zabbixUserPwd = zabbixUserPwd;
    }

    public boolean isbCreateNotExistZabbixApi()
    {
        return bCreateNotExistZabbixApi;
    }

    public void setbCreateNotExistZabbixApi( boolean bCreateNotExistZabbixApi )
    {
        this.bCreateNotExistZabbixApi = bCreateNotExistZabbixApi;
    }

}
