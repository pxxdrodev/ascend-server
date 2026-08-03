package com.ascend.core.api.backend.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

public final class Connect implements AutoCloseable {

    private final Logger logger;
    private final String connectionUri;
    private final String databaseName;

    private MongoClient client;
    private MongoDatabase database;

    public Connect(Logger logger, String connectionUri, String databaseName) {
        this.logger = logger;
        this.connectionUri = connectionUri;
        this.databaseName = databaseName;
    }

    public void connect() {
        logger.info("Estabelecendo conexão com o banco de dados...");

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionUri))
                .applyToSocketSettings(builder ->
                        builder.connectTimeout(5, TimeUnit.SECONDS))
                .build();

        client = MongoClients.create(settings);
        database = client.getDatabase(databaseName);

        database.runCommand(new Document("ping", 1));

        logger.info("Conexão estabelecida com sucesso → banco: {}", databaseName);
    }

    public MongoDatabase getDatabase() {
        if (database == null) {
            throw new IllegalStateException("MongoDB ainda não foi conectado.");
        }

        return database;
    }

    public boolean isConnected() {
        return client != null && database != null;
    }

    @Override
    public void close() {
        if (client == null) {
            return;
        }

        logger.info("Encerrando conexão...");

        client.close();

        client = null;
        database = null;

        logger.info("Conexão encerrada.");
    }
}
