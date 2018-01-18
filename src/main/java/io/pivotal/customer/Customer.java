package io.pivotal.customer;

import java.util.UUID;

import org.springframework.data.cassandra.mapping.PrimaryKey;
import org.springframework.data.cassandra.mapping.Table;

import com.datastax.driver.core.utils.UUIDs;

import lombok.Data;
import lombok.experimental.Wither;

@Data
@Wither
@Table
public class Customer {

	@PrimaryKey
	private UUID id;

	private String firstName;

	private String lastName;

	public Customer() {
	    id = UUIDs.timeBased();
	}

	public Customer(UUID id, String firstName, String lastName) {
		this.id = id == null ? UUIDs.timeBased() : id;
		this.firstName = firstName;
		this.lastName = lastName;
	}

}