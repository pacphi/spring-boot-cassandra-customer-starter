USE customers;
DROP TABLE IF EXISTS customer;
DROP INDEX IF EXISTS customerfirstnameindex;
DROP INDEX IF EXISTS customerlastnameindex;
CREATE TABLE customer (id TimeUUID PRIMARY KEY, firstname text, lastname text);
CREATE INDEX customerfirstnameindex ON customer (firstname);
CREATE INDEX customerlastnameindex ON customer (lastname);