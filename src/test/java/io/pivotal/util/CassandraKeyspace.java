/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.pivotal.util;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.AssumptionViolatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Version;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.NettyOptions;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.Session;

import io.netty.channel.EventLoopGroup;

/**
 * {@link CassandraResource} to require (create or reuse) an Apache Cassandra keyspace and optionally require a specific
 * Apache Cassandra version. This {@link org.junit.rules.TestRule} can be chained to depend on another
 * {@link CassandraResource} rule to require a running instance/start an embedded Apache Cassandra instance.
 *
 * @author Mark Paluch
 */
public class CassandraKeyspace extends CassandraResource {

	private static Logger log = LoggerFactory.getLogger(CassandraKeyspace.class);
	
	private final String keyspaceName;
	private final Version requiredVersion;
	private final CassandraResource dependency;
	private List<String> cqlStatements;

	private CassandraKeyspace(String host, int port, String keyspaceName, CassandraResource dependency,
			Version requiredVersion) {

		super(host, port);

		this.keyspaceName = keyspaceName;
		this.dependency = dependency;
		this.requiredVersion = requiredVersion;
	}

	/**
	 * Create a {@link CassandraKeyspace} test rule to provide a running Cassandra instance on {@code localhost:9042} with
	 * a keyspace {@code example}. Reuses a running Cassandra instance if available or starts an embedded instance.
	 *
	 * @return the {@link CassandraKeyspace} rule.
	 */
	public static CassandraKeyspace onLocalhost() {
		return new CassandraKeyspace("localhost", ProtocolOptions.DEFAULT_PORT, "default",
				Cassandra.embeddedIfNotRunning("localhost", ProtocolOptions.DEFAULT_PORT), new Version(0, 0, 0));
	}

	
	public static CassandraKeyspace onLocalhost(String keyspaceName) {
		return new CassandraKeyspace("localhost", ProtocolOptions.DEFAULT_PORT, keyspaceName,
				Cassandra.embeddedIfNotRunning("localhost", ProtocolOptions.DEFAULT_PORT), new Version(0, 0, 0));
	}
	
	public static CassandraKeyspace onLocalhost(String keyspaceName, String cqlResourcePath) {
		CassandraKeyspace keyspace = CassandraKeyspace.onLocalhost(keyspaceName);
		keyspace.preload(cqlResourcePath);
		return keyspace;
	}
	
	/**
	 * Setup a dependency to an upstream {@link CassandraResource}. The dependency is activated by {@code this} test rule.
	 *
	 * @param cassandraResource must not be {@literal null}.
	 * @return the {@link CassandraKeyspace} rule.
	 */
	public CassandraKeyspace dependsOn(CassandraResource cassandraResource) {

		Assert.notNull(cassandraResource, "CassandraResource must not be null!");

		return new CassandraKeyspace(getHost(), getPort(), keyspaceName, cassandraResource, requiredVersion);
	}
	
	protected void preload(String cqlResourcePath) {
		try {
			this.cqlStatements = IOUtils.readLines(getClass().getClassLoader().getResourceAsStream(cqlResourcePath), "UTF-8");
		} catch (IOException e) {
			log.info("Could not load {}", cqlResourcePath);
		}
	}

	/**
	 * Setup a version requirement.
	 *
	 * @param requiredVersion must not be {@literal null}.
	 * @return the {@link CassandraKeyspace} rule
	 */
	public CassandraKeyspace atLeast(Version requiredVersion) {

		Assert.notNull(requiredVersion, "Required version must not be null!");

		return new CassandraKeyspace(getHost(), getPort(), keyspaceName, dependency, requiredVersion);
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.rules.ExternalResource#before()
	 */
	@Override
	protected void before() throws Throwable {

		dependency.before();

		Cluster cluster = Cluster.builder().addContactPoint(getHost()).withPort(getPort())
				.withNettyOptions(new NettyOptions() {
					@Override
					public void onClusterClose(EventLoopGroup eventLoopGroup) {
						eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).syncUninterruptibly();
					}
				}).build();

		Session session = cluster.newSession();

		try {

			if (requiredVersion != null) {

				Version cassandraReleaseVersion = CassandraVersion.getReleaseVersion(session);

				if (cassandraReleaseVersion.isLessThan(requiredVersion)) {
					throw new AssumptionViolatedException(
							String.format("Cassandra at %s:%s runs in Version %s but we require at least %s", getHost(), getPort(),
									cassandraReleaseVersion, requiredVersion));
				}
			}

			session.execute(String.format("CREATE KEYSPACE IF NOT EXISTS %s \n"
					+ "WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };", keyspaceName));
			
			if (!CollectionUtils.isEmpty(cqlStatements)) {
				cqlStatements.forEach(s -> session.execute(s));
			}
		} finally {
			session.close();
			cluster.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.rules.ExternalResource#after()
	 */
	@Override
	protected void after() {

		super.after();
		dependency.after();
	}
}