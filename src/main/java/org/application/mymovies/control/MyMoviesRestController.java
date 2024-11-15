package org.application.mymovies.control;

import com.google.gson.Gson;
import com.mongodb.client.*;
import org.application.mymovies.entities.Movie;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;

@RestController
@RequestMapping(value = "/api")
public class MyMoviesRestController {

    private final String connectionString = "mongodb://localhost:27017";
    private final String databaseName = "movies_db";
    private final String collectionName = "movies";
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;

    public MyMoviesRestController() {
        mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase(databaseName);
        collection = database.getCollection(collectionName);
    }

    @GetMapping(value = "test")
    public ResponseEntity<Object> testarConexao() {
        return ResponseEntity.ok("conectado");
    }

    @GetMapping(value = "movies")
    public ResponseEntity<Object> findMovies(@RequestParam String filter) {
        Bson filtro = or(Arrays.asList(
                eq("title", Pattern.compile(filter + "(?i)")),
                eq("cast", Pattern.compile(filter + "(?i)")),
                eq("extract", Pattern.compile(filter + "(?i)"))
        ));
        FindIterable<Document> iterDoc = collection.find(filtro);
        List<Movie> movieList = new ArrayList<>();
        for (Document doc : iterDoc) {
            movieList.add(new Gson().fromJson(doc.toJson(), Movie.class));
        }
        return ResponseEntity.ok(movieList);
    }

    @GetMapping(value = "load")
    public ResponseEntity<Object> loadMovies() {
        try {
            String jsonFilePath = "src/main/resources/movies.json";
            if (!Files.exists(Paths.get(jsonFilePath))) {
                return ResponseEntity.badRequest().body("Arquivo JSON não encontrado.");
            }

            String jsonContent = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            Movie[] movies = new Gson().fromJson(jsonContent, Movie[].class);

            for (Movie movie : movies) {
                Document doc = Document.parse(new Gson().toJson(movie));
                collection.insertOne(doc);
            }

            return ResponseEntity.ok("Filmes carregados com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao carregar filmes: " + e.getMessage());
        }
    }
}
