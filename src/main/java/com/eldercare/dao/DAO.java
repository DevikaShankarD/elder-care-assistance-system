package com.eldercare.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object (DAO) interface demonstrating the OOP principle of Abstraction.
 * Standardizes Create, Read, Update, and Delete (CRUD) operations across all entities.
 *
 * @param <T> the domain model type
 */
public interface DAO<T> {

    /**
     * Persists a new entity into the database.
     *
     * @param entity the entity to persist
     * @return the auto-generated primary key ID
     * @throws SQLException if a database access error occurs
     */
    int add(T entity) throws SQLException;

    /**
     * Retrieves an entity by its unique ID.
     *
     * @param id primary key identifier
     * @return an Optional containing the entity if found, or empty
     * @throws SQLException if a database access error occurs
     */
    Optional<T> getById(int id) throws SQLException;

    /**
     * Retrieves all entities of this type from the database.
     *
     * @return list of entities
     * @throws SQLException if a database access error occurs
     */
    List<T> getAll() throws SQLException;

    /**
     * Updates an existing entity in the database.
     *
     * @param entity the entity with updated values
     * @return true if an existing record was updated, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean update(T entity) throws SQLException;

    /**
     * Deletes an entity from the database by ID.
     *
     * @param id primary key identifier
     * @return true if a record was deleted, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean delete(int id) throws SQLException;
}
