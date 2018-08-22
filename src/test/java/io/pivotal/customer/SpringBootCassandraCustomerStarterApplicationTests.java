package io.pivotal.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.junit4.SpringRunner;

@Ignore
// Note: cassandra-unit starts Cassandra on port 9142 instead of 9042!
@RunWith(SpringRunner.class)
@TestExecutionListeners(mergeMode = MergeMode.MERGE_WITH_DEFAULTS, listeners = {
		OrderedCassandraTestExecutionListener.class })
@SpringBootTest(properties = { "spring.data.cassandra.port=9142" })
@CassandraDataSet(keyspace = "customer-space", value = "setup.cql")
@EmbeddedCassandra(timeout = 60000)
public class SpringBootCassandraCustomerStarterApplicationTests {

	@ClassRule
	public static OutputCapture outputCapture = new OutputCapture();

	@ClassRule
	public static SkipOnWindows skipOnWindows = new SkipOnWindows();

	@Test
	public void testDefaultSettings() {
		String output = outputCapture.toString();
		assertThat(output).contains("firstName='Alice', lastName='Smith'");
	}

	static class SkipOnWindows implements TestRule {

		@Override
		public Statement apply(Statement base, Description description) {
			return new Statement() {

				@Override
				public void evaluate() throws Throwable {
					if (!runningOnWindows()) {
						base.evaluate();
					}
				}

				private boolean runningOnWindows() {
					return File.separatorChar == '\\';
				}

			};
		}

	}

}