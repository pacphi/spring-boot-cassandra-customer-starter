package io.pivotal.customer;

import java.util.UUID;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.hateoas.Identifiable;

import com.datastax.driver.core.utils.UUIDs;

import lombok.Data;
import lombok.experimental.Wither;

@Data
@Wither
@Table
public class Customer implements Identifiable<UUID>{

	@PrimaryKey
	private UUID id;

	@Column("firstname")
	private String firstName;

	@Column("lastname")
	private String lastName;

	public Customer() {
		id = UUIDs.timeBased();
	}

	public Customer(UUID id, String firstName, String lastName) {
		this.id = id == null ? UUIDs.timeBased() : id;
		this.firstName = firstName;
		this.lastName = lastName;
	}
	
	public static Customer from(Customer customer) {
		return new Customer()
					.withId(customer.getId())
					.withFirstName(customer.getFirstName())
					.withLastName(customer.getLastName());
	}

}