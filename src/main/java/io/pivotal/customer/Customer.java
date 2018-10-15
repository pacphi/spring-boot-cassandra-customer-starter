package io.pivotal.customer;

import java.util.UUID;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.hateoas.Identifiable;
import org.springframework.util.Assert;

import com.datastax.driver.core.utils.UUIDs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

@Data
@Wither
@AllArgsConstructor
@NoArgsConstructor
@Table("customer")
public class Customer implements Identifiable<UUID>{

	@PrimaryKey
	private UUID id = UUIDs.timeBased();

	@Column("firstname")
	private String firstName;

	@Column("lastname")
	private String lastName;
	
	public static Customer from(Customer customer) {
		Assert.isTrue(customer.getId() != null, "Customer id must not be null");
		return new Customer(customer.getId(), customer.getFirstName(), customer.getLastName());
	}

}