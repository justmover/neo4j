/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.security.enterprise.auth;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import org.neo4j.bolt.BoltKernelExtension;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.mockfs.EphemeralFileSystemAbstraction;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.security.AuthenticationResult;
import org.neo4j.kernel.enterprise.api.security.EnterpriseAuthSubject;
import org.neo4j.kernel.impl.coreapi.InternalTransaction;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.test.TestEnterpriseGraphDatabaseFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.neo4j.graphdb.factory.GraphDatabaseSettings.BoltConnector.EncryptionLevel.OPTIONAL;
import static org.neo4j.graphdb.factory.GraphDatabaseSettings.boltConnector;
import static org.neo4j.server.security.auth.SecurityTestUtils.authToken;

public class EmbeddedInteraction implements NeoInteractionLevel<EnterpriseAuthSubject>
{
    private GraphDatabaseFacade db;
    private MultiRealmAuthManager manager;
    private EnterpriseUserManager userManager;
    private FileSystemAbstraction fileSystem;

    EmbeddedInteraction( Map<String, String> config ) throws Throwable
    {
        TestEnterpriseGraphDatabaseFactory factory = new TestEnterpriseGraphDatabaseFactory();
        factory.setFileSystem( new EphemeralFileSystemAbstraction() );
        GraphDatabaseBuilder builder = factory.newImpermanentDatabaseBuilder().setConfig( config );
        this.fileSystem = factory.getFileSystem();
        init( builder );
    }

    public EmbeddedInteraction( Map<Setting<?>, String> config, GraphDatabaseBuilder builder ) throws Throwable
    {
        for ( Map.Entry<Setting<?>,String> entry : config.entrySet() )
        {
            builder.setConfig( entry.getKey(), entry.getValue() );
        }
        init( builder );
    }

    private void init( GraphDatabaseBuilder builder ) throws Throwable
    {
        builder.setConfig( boltConnector( "0" ).enabled, "true" );
        builder.setConfig( boltConnector( "0" ).encryption_level, OPTIONAL.name() );
        builder.setConfig( BoltKernelExtension.Settings.tls_key_file, NeoInteractionLevel.tempPath( "key", ".key" ) );
        builder.setConfig( BoltKernelExtension.Settings.tls_certificate_file,
                NeoInteractionLevel.tempPath( "cert", ".cert" ) );
        builder.setConfig( GraphDatabaseSettings.auth_enabled, "true" );
        builder.setConfig( GraphDatabaseSettings.auth_manager, "enterprise-auth-manager" );
        db = (GraphDatabaseFacade) builder.newGraphDatabase();
        manager = db.getDependencyResolver().resolveDependency( MultiRealmAuthManager.class );
        manager.init();
        manager.start();
        userManager = manager.getUserManager();
    }

    @Override
    public EnterpriseUserManager getLocalUserManager()
    {
        return userManager;
    }

    @Override
    public GraphDatabaseFacade getLocalGraph() { return db; }

    @Override
    public FileSystemAbstraction fileSystem()
    {
        return fileSystem;
    }

    @Override
    public InternalTransaction beginLocalTransactionAsUser( EnterpriseAuthSubject subject,
            KernelTransaction.Type txType ) throws Throwable
    {
        return db.beginTransaction( txType, subject );
    }

    @Override
    public String executeQuery( EnterpriseAuthSubject subject, String call, Map<String,Object> params,
            Consumer<ResourceIterator<Map<String, Object>>> resultConsumer )
    {
        try ( InternalTransaction tx = db.beginTransaction( KernelTransaction.Type.implicit, subject ) )
        {
            Map<String,Object> p = (params == null) ? Collections.emptyMap() : params;
            resultConsumer.accept( db.execute( call, p ) );
            tx.success();
            return "";
        }
        catch ( Exception e )
        {
            return e.getMessage();
        }
    }

    @Override
    public EnterpriseAuthSubject login( String username, String password ) throws Exception
    {
        return manager.login( authToken( username, password ) );
    }

    @Override
    public void logout( EnterpriseAuthSubject subject )
    {
        subject.logout();
    }

    @Override
    public void updateAuthToken( EnterpriseAuthSubject subject, String username, String password )
    {
    }

    @Override
    public String nameOf( EnterpriseAuthSubject subject )
    {
        return subject.name();
    }

    @Override
    public void tearDown() throws Throwable
    {
        manager.stop();
        manager.shutdown();
        db.shutdown();
    }

    @Override
    public void assertAuthenticated( EnterpriseAuthSubject subject )
    {
        assertThat( subject.getAuthenticationResult(), equalTo( AuthenticationResult.SUCCESS ) );
    }

    @Override
    public void assertPasswordChangeRequired( EnterpriseAuthSubject subject )
    {
        assertThat( subject.getAuthenticationResult(), equalTo( AuthenticationResult.PASSWORD_CHANGE_REQUIRED ) );
    }

    @Override
    public void assertInitFailed( EnterpriseAuthSubject subject )
    {
        assertThat( subject.getAuthenticationResult(), equalTo( AuthenticationResult.FAILURE ) );
    }

    @Override
    public String getConnectionDetails()
    {
        return "embedded-session";
    }
}
