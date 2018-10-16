package io.pivotal.customer;

import org.springframework.hateoas.SimpleIdentifiableResourceAssembler;
import org.springframework.stereotype.Component;

@Component
class CustomerResourceAssembler extends SimpleIdentifiableResourceAssembler<Customer> {

	/**
	 * Link the {@link Customer} domain type to the {@link CustomerEndpoints} using this
	 * {@link SimpleIdentifiableResourceAssembler} in order to generate both {@link org.springframework.hateoas.Resource}
	 * and {@link org.springframework.hateoas.Resources}.
	 */
	CustomerResourceAssembler() {
		super(CustomerEndpoints.class);
	}
}