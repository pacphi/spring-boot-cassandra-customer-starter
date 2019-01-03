package io.pivotal.customer;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.CassandraContainer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(
    initializers = { ReactiveCustomerRepositoryIntegrationTest.TestContainerInitializer.class })
public class ReactiveCustomerRepositoryIntegrationTest {

	private static CassandraContainer container = 
			(CassandraContainer) new CassandraContainer()
				.withInitScript("cql/simple.cql")
				.withExposedPorts(9042)
				.withStartupTimeout(Duration.ofMinutes(2));
	
	@Autowired CustomerRepository repository;

	@BeforeAll
    public static void startup() {
        container.start();
    }

    @AfterAll
    public static void shutdown() {
        container.stop();
    }
    
	/**
	 * Clear table and insert some rows.
	 */
	@BeforeEach
	public void setUp() {
		
		Flux<Customer> deleteAndInsert = repository.deleteAll() 
				.thenMany(Flux.just(Customer.builder().withFirstName("Nick").withLastName("Fury").build(),
                Customer.builder().withFirstName("Tony").withLastName("Stark").build(),
                Customer.builder().withFirstName("Bruce").withLastName("Banner").build(),
                Customer.builder().withFirstName("Peter").withLastName("Parker").build()))
				.flatMap(c -> repository.save(c));

		StepVerifier.create(deleteAndInsert).expectNextCount(4).verifyComplete();
	}

	/**
	 * This sample performs a count, inserts data and performs a count again using reactive operator chaining.
	 */
	@Test
	public void shouldInsertAndCountData() {

		Mono<Long> saveAndCount = repository.count()
				.doOnNext(System.out::println)
				.thenMany(repository.saveAll(Flux.just(Customer.builder().withFirstName("Stephen").withLastName("Strange").build(),
				Customer.builder().withFirstName("Carol").withLastName("Danvers").build())))
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
	@Test
	@Disabled
	public void shouldQueryDataWithQueryDerivation() {
		StepVerifier.create(repository.findByLastName("Banner")).expectNextCount(1).verifyComplete();
	}

	/**
	 * Fetch data using a string query.
	 */
	@Test
	public void shouldQueryDataWithStringQuery() {
		StepVerifier.create(repository.findByFirstNameInAndLastName("Tony", "Stark")).expectNextCount(1).verifyComplete();
	}

	/**
	 * Fetch data using query derivation.
	 */
	@Test
	@Disabled
	public void shouldQueryDataWithDeferredQueryDerivation() {
		StepVerifier.create(repository.findByLastName(Mono.just("Fury"))).expectNextCount(1).verifyComplete();
	}

	/**
	 * Fetch data using query derivation and deferred parameter resolution.
	 */
	@Test
	public void shouldQueryDataWithMixedDeferredQueryDerivation() {

		StepVerifier.create(repository.findByFirstNameAndLastName(Mono.just("Bruce"), "Banner"))
				.expectNextCount(1)
				.verifyComplete();
	}
	
	static class TestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues
                .of(
                    "spring.datasource.username=" + container.getUsername(),
                    "spring.datasource.password=" + container.getPassword(),
                    "spring.data.cassandra.contact-points=" + container.getContainerIpAddress(),
                    "spring.data.cassandra.port=" + container.getMappedPort(9042)
                )
                .applyTo(applicationContext.getEnvironment());
        }
    }

}