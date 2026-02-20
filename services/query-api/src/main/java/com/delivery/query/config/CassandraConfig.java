package com.delivery.query.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.util.List;

/**
 * Configures the Cassandra session, auto-creates the keyspace and tables.
 * Extends AbstractCassandraConfiguration to get full control over
 * keyspace creation (Boot auto-config cannot create keyspaces).
 */
@Configuration
@EnableCassandraRepositories(basePackages = "com.delivery.query.repository")
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Value("${cassandra.keyspace:delivery_query}")
    private String keyspace;

    @Value("${cassandra.contact-points:localhost}")
    private String contactPoints;

    @Value("${cassandra.port:9042}")
    private int port;

    @Value("${cassandra.datacenter:dc1}")
    private String datacenter;

    @Override
    protected String getKeyspaceName() {
        return keyspace;
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    public String getLocalDataCenter() {
        return datacenter;
    }

    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
        return List.of(
                CreateKeyspaceSpecification.createKeyspace(keyspace)
                        .ifNotExists()
                        .withSimpleReplication());
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }

    @Override
    public String[] getEntityBasePackages() {
        return new String[] { "com.delivery.query.entity" };
    }
}
