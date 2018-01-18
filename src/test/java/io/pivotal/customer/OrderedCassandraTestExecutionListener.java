package io.pivotal.customer;

import org.cassandraunit.spring.CassandraUnitDependencyInjectionTestExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.Ordered;

public class OrderedCassandraTestExecutionListener
		extends CassandraUnitDependencyInjectionTestExecutionListener {

	private static final Logger logger = LoggerFactory
			.getLogger(OrderedCassandraTestExecutionListener.class);

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	protected void cleanServer() {
		try {
			super.cleanServer();
		}
		catch (Exception ex) {
			logger.warn("Failure during server cleanup", ex);
		}
	}

}