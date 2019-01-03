package io.pivotal.customer;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.CassandraContainer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(
    initializers = { ReactiveCassandraTemplateIntegrationTest.TestContainerInitializer.class })
public class ReactiveCassandraTemplateIntegrationTest {

	private static CassandraContainer container = 
			(CassandraContainer) new CassandraContainer()
				.withInitScript("cql/simple.cql")
				.withStartupTimeout(Duration.ofMinutes(2));
	
	@Autowired private ReactiveCassandraTemplate template;

	@BeforeAll
    public static void startup() {
        container.start();
    }

    @AfterAll
    public static void shutdown() {
        container.stop();
    }
    
	/**
	 * Truncate table and insert some rows.
	 */
	@BeforeEach
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
	
	static class TestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues
                .of(
                    "spring.data.cassandra.username=" + container.getUsername(),
                    "spring.data.cassandra.password=" + container.getPassword(),
                    "spring.data.cassandra.contact-points=" + container.getContainerIpAddress(),
                    "spring.data.cassandra.port=" + container.getMappedPort(9042)

                )
                .applyTo(applicationContext.getEnvironment());
        }
    }
}
