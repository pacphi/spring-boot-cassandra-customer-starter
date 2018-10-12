package io.pivotal.customer;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import io.pivotal.util.CassandraKeyspace;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


@RunWith(SpringRunner.class)
@SpringBootTest
public class ReactiveCustomerRepositoryIntegrationTest {

	@ClassRule public final static CassandraKeyspace CASSANDRA_KEYSPACE = CassandraKeyspace.onLocalhost("customers", "cql/simple.cql");
	
	@Autowired CustomerRepository repository;

	/**
	 * Clear table and insert some rows.
	 */
	@Before
	public void setUp() {
		
		Flux<Customer> deleteAndInsert = repository.deleteAll() 
				.thenMany(Flux.just(new Customer().withFirstName("Nick").withLastName("Fury"),
                new Customer().withFirstName("Tony").withLastName("Stark"),
                new Customer().withFirstName("Bruce").withLastName("Banner"),
                new Customer().withFirstName("Peter").withLastName("Parker")))
				.flatMap(c -> repository.save(c));

		StepVerifier.create(deleteAndInsert).expectNextCount(4).verifyComplete();
	}

	/**
	 * This sample performs a count, inserts data and performs a count again using reactive operator chaining.
	 */
	//@Test
	public void shouldInsertAndCountData() {

		Mono<Long> saveAndCount = repository.count()
				.doOnNext(System.out::println)
				.thenMany(repository.saveAll(Flux.just(new Customer().withFirstName("Stephen").withLastName("Strange"),
				new Customer().withFirstName("Carol").withLastName("Danvers"))))
				.last()
				.flatMap(v -> repository.count())
				.doOnNext(System.out::println);

		StepVerifier.create(saveAndCount).expectNext(6L).verifyComplete();
	}

	/**
	 * Result set {@link com.datastax.driver.core.Row}s are converted to entities as they are emitted. Reactive pull and
	 * prefetch define the amount of fetched records.
	 */
	@Test
	public void shouldPerformConversionBeforeResultProcessing() {

		StepVerifier.create(repository.findAll().doOnNext(System.out::println))
				.expectNextCount(4)
				.verifyComplete();
	}

	/**
	 * Fetch data using query derivation.
	 */
	//@Test
	public void shouldQueryDataWithQueryDerivation() {
		StepVerifier.create(repository.findByLastName("B")).expectNextCount(1).verifyComplete();
	}

	/**
	 * Fetch data using a string query.
	 */
	//@Test
	public void shouldQueryDataWithStringQuery() {
		StepVerifier.create(repository.findByFirstNameInAndLastName("Tony", "Stark")).expectNextCount(1).verifyComplete();
	}

	/**
	 * Fetch data using query derivation.
	 */
	//@Test
	public void shouldQueryDataWithDeferredQueryDerivation() {
		StepVerifier.create(repository.findByLastName(Mono.just("Fury"))).expectNextCount(1).verifyComplete();
	}

	/**
	 * Fetch data using query derivation and deferred parameter resolution.
	 */
	//@Test
	public void shouldQueryDataWithMixedDeferredQueryDerivation() {

		StepVerifier.create(repository.findByFirstNameAndLastName(Mono.just("Bruce"), "Banner"))
				.expectNextCount(1)
				.verifyComplete();
	}

}