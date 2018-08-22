package io.pivotal.customer;

import java.util.List;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="customers", path="customers")
public interface CustomerRepository extends CassandraRepository<Customer, String> {

	@Query("select * from customer where firstname=?0")
	public Customer findByFirstName(String firstName);

	@Query("select * from customer where lastname=?0")
	public List<Customer> findByLastName(String lastName);

}