package io.pivotal.customer;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import io.pivotal.util.CassandraKeyspace;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


@RunWith(SpringRunner.class)
@SpringBootTest
public class ReactiveCassandraTemplateIntegrationTest {

	@ClassRule public final static CassandraKeyspace CASSANDRA_KEYSPACE = CassandraKeyspace.onLocalhost("customers", "cql/simple.cql");
	
	@Autowired private ReactiveCassandraTemplate template;

	/**
	 * Truncate table and insert some rows.
	 */
	@Before
	public void setUp() {

		Flux<Customer> truncateAndInsert = template.truncate(Customer.class)
				.thenMany(Flux.just(new Customer().withFirstName("Nick").withLastName("Fury"),
						new Customer().withFirstName("Tony").withLastName("Stark"),
						new Customer().withFirstName("Bruce").withLastName("Banner"),
						new Customer().withFirstName("Peter").withLastName("Parker")))
				.flatMap(template::insert);

		StepVerifier.create(truncateAndInsert).expectNextCount(4).verifyComplete();
	}

	/**
	 * This sample performs a count, inserts data and performs a count again using reactive operator chaining. It prints
	 * the two counts ({@code 4} and {@code 6}) to the console.
	 */
	@Test
	public void shouldInsertAndCountData() {

		Mono<Long> saveAndCount = template.count(Customer.class)
				.doOnNext(System.out::println)
				.thenMany(Flux.just(new Customer().withFirstName("Stephen").withLastName("Strange"),
				new Customer().withFirstName("Carol").withLastName("Danvers")))
				.flatMap(template::insert)
				.last()
				.flatMap(v -> template.count(Customer.class))
				.doOnNext(System.out::println);

		StepVerifier.create(saveAndCount).expectNext(6L).verifyComplete();
	}

}