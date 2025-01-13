USE MOVIEDB;

DELIMITER $$



CREATE PROCEDURE get_table_metadata()
BEGIN
    SELECT table_name AS tableName,
           column_name AS columnName,
           data_type AS dataType
    FROM information_schema.columns
    WHERE table_schema = DATABASE();
END $$


CREATE PROCEDURE get_highest_star_id(OUT starId VARCHAR(10))
BEGIN
    DECLARE nextStarId VARCHAR(10);
    SELECT IFNULL(MAX(CAST(SUBSTRING(id, 3) AS SIGNED)), 0) INTO nextStarId FROM stars;
    SET nextStarId = LPAD(CAST(nextStarId + 1 AS CHAR), 8, '0');    -- increment and convert to string
    SET starId = CONCAT('nm', nextStarId);  -- add the prefix
END $$

CREATE PROCEDURE get_highest_movie_id(OUT movieId VARCHAR(10))
BEGIN
    DECLARE nextMovieId VARCHAR(10);
    SELECT IFNULL(MAX(CAST(SUBSTRING(id, 3) AS SIGNED)), 0) INTO nextMovieId FROM movies;
    SET nextMovieId = LPAD(CAST(nextMovieId + 1 AS CHAR), 8, '0');
    SET movieId = CONCAT('tt', nextMovieId);
END $$

-- checks if genre exists already or and if not, creates new genre
CREATE PROCEDURE add_genre(IN genreName VARCHAR(32),
                            OUT genreId INTEGER,
                            OUT alreadyExists BOOLEAN)
BEGIN
    DECLARE returnedGenreId INTEGER;
    -- check if the genre exists
    SELECT id INTO returnedGenreId FROM genres WHERE name = genreName LIMIT 1;
    IF returnedGenreId IS NOT NULL THEN
        SET genreId = returnedGenreId;
        SET alreadyExists = TRUE;
    ELSE
        INSERT INTO genres (name) VALUES (genreName);
        SET genreId = LAST_INSERT_ID();
        SET alreadyExists = FALSE;
    END IF;
END $$



-- gets called only after checking star doesn't exist already
CREATE PROCEDURE add_star_with_id(IN starId VARCHAR(10),
                                    IN starName VARCHAR(100),
                                    IN starBirthYear INTEGER,
                                    OUT errorMessage VARCHAR(30))
BEGIN
    DECLARE existingStarId VARCHAR(10);
    SELECT id INTO existingStarId FROM stars WHERE id = starId; -- fast check to see whether it exists already or not so it doesn't break
    IF existingStarId IS NOT NULL THEN
        SET errorMessage = 'Error: Star already exists.';
    ELSE
        INSERT INTO stars (id, name, birthYear) VALUES (starId, starName, starBirthYear);
    END IF;
END $$



-- gets called only after checking star doesn't exist already
CREATE PROCEDURE add_star(IN starName VARCHAR(100), IN starBirthYear INTEGER, OUT starId VARCHAR(10))
BEGIN
    DECLARE nextStarId VARCHAR(10);
    SELECT IFNULL(MAX(CAST(SUBSTRING(id, 3) AS SIGNED)), 0) INTO nextStarId FROM stars;
    -- increment the star id
    SET nextStarId = LPAD(CAST(nextStarId + 1 AS CHAR), 8, '0');
    -- insert the new star into the stars table with the appropriate prefix 'nm'
    INSERT INTO stars (id, name, birthYear) VALUES (CONCAT('nm', nextStarId), starName, starBirthYear);
    SET starId = CONCAT('nm', nextStarId);
END $$



CREATE PROCEDURE add_movie(IN movieTitle VARCHAR(100),
                            IN movieYear INTEGER,
                            IN movieDirector VARCHAR(100),
                            IN starName VARCHAR(100),
                            IN starBirthYear INTEGER,
                            IN genreName VARCHAR(32),
                            OUT movieId VARCHAR(10),
                            OUT starId VARCHAR(10),
                            OUT genreId INTEGER,
                            OUT errorMessage VARCHAR(35))
BEGIN
    DECLARE existingMovieId VARCHAR(10);
    DECLARE nextMovieId VARCHAR(10);
    DECLARE returnedStarId VARCHAR(10);
    DECLARE returnedGenreId INTEGER;

    -- check if the movie already exists first
    SELECT id INTO existingMovieId FROM movies WHERE title = movieTitle and year = movieYear and director = movieDirector;
    IF existingMovieId IS NOT NULL THEN
        SET errorMessage = 'Error: Movie already exists.';
    ELSE
        SELECT IFNULL(MAX(CAST(SUBSTRING(id, 3) AS SIGNED)), 0) INTO nextMovieId FROM movies;
        -- increment the movie id
        SET nextMovieId = LPAD(CAST(nextMovieId + 1 AS CHAR), 8, '0');
        -- insert the new movie into the movies table with the appropriate prefix
        INSERT INTO movies (id, title, year, director) VALUES (CONCAT('tt', nextMovieId),
                                                               movieTitle,
                                                               movieYear,
                                                               movieDirector);
        SET movieId = CONCAT('tt', nextMovieId);

        -- now check if the star exists, if not create it, then add to stars_in_movies table
        -- get the id of the first star that matches with the name (if one exists)
        IF starBirthYear IS NULL THEN
            SELECT id INTO returnedStarId FROM stars WHERE name = starName LIMIT 1;
        ELSE
            SELECT id INTO returnedStarId FROM stars WHERE name = starName AND birthYear = starBirthYear LIMIT 1;
        END IF;

        IF returnedStarId IS NOT NULL THEN
            SET starId = returnedStarId;
        ELSE
            CALL add_star(starName, starBirthYear, @newStarId);
            SET returnedStarId = @newStarId;
            SET starId = @newStarId;
        END IF;
        INSERT INTO stars_in_movies (starId, movieId) VALUES (returnedStarId, CONCAT('tt', nextMovieId));


        -- add the genre (if existing won't add it but if new, it will create it)
        CALL add_genre(genreName, @genreId, @alreadyExists);
        SET returnedGenreId = @genreId;
        SET genreId = @genreId;
        INSERT INTO genres_in_movies (genreId, movieId) VALUES (returnedGenreId, CONCAT('tt', nextMovieId));
    END IF;
END $$



CREATE PROCEDURE add_movie_alone_with_id(IN movieId VARCHAR(10),
                                 IN movieTitle VARCHAR(100),
                                 IN movieYear INTEGER,
                                 IN movieDirector VARCHAR(100),
                                 OUT errorMessage VARCHAR(35))
BEGIN
    DECLARE existingMovieId VARCHAR(10);
    -- check if the movie already exists first
    SELECT id INTO existingMovieId FROM movies WHERE id = movieId;
    IF existingMovieId IS NOT NULL THEN
        SET errorMessage = 'Error: Movie already exists.';
    ELSE
        INSERT INTO movies (id, title, year, director) VALUES (movieId,
                                                               movieTitle,
                                                               movieYear,
                                                               movieDirector);
    END IF;
END $$



CREATE PROCEDURE add_movie_alone(IN movieTitle VARCHAR(100),
                           IN movieYear INTEGER,
                           IN movieDirector VARCHAR(100),
                           OUT movieId VARCHAR(10),
                           OUT errorMessage VARCHAR(35))
BEGIN
    DECLARE existingMovieId VARCHAR(10);
    DECLARE nextMovieId VARCHAR(10);

    -- check if the movie already exists first
    SELECT id INTO existingMovieId FROM movies WHERE title = movieTitle and year = movieYear and director = movieDirector;
    IF existingMovieId IS NOT NULL THEN
        SET errorMessage = 'Error: Movie already exists.';
    ELSE
        SELECT IFNULL(MAX(CAST(SUBSTRING(id, 3) AS SIGNED)), 0) INTO nextMovieId FROM movies;
        -- increment the movie id
        SET nextMovieId = LPAD(CAST(nextMovieId + 1 AS CHAR), 8, '0');
        -- insert the new movie into the movies table with the appropriate prefix
        INSERT INTO movies (id, title, year, director) VALUES (CONCAT('tt', nextMovieId),
                                                               movieTitle,
                                                               movieYear,
                                                               movieDirector);
        SET movieId = CONCAT('tt', nextMovieId);
    END IF;
END $$



-- star id and movie id (in the cast.xml file, get the highest id from 1 get_highest_star_id call
-- and then store this and use when you encounter new actors you didn't see in actors.xml)
-- make sure to create the stored procedure of adding the star before we do this
CREATE PROCEDURE link_star_id_and_movie_id(IN givenStarId VARCHAR(10),
                                     IN givenMovieId VARCHAR(10),
                                     OUT errorMessage VARCHAR(35))
BEGIN
    DECLARE existingMovieId VARCHAR(10);
    DECLARE existingStarId VARCHAR(10);
    DECLARE foundPairAlready VARCHAR(10); -- if this gets set, the pair is already in the table (here so it doesn't crash in case we missed duplicate in parser)

    SELECT id INTO existingMovieId FROM movies WHERE id = givenMovieId LIMIT 1;
    IF existingMovieId IS NOT NULL THEN
        -- now check if the star exists, if not create it, then add to stars_in_movies table
        -- get the id of the first star that matches with the name (if one exists)
        SELECT id INTO existingStarId FROM stars WHERE id = givenStarId LIMIT 1;
        IF existingStarId IS NOT NULL THEN
            INSERT INTO stars_in_movies (starId, movieId) VALUES (existingStarId, existingMovieId);
        ELSE
            SET errorMessage = 'Error: Star does not exist.';
        END IF;
    ELSE
        SET errorMessage = 'Error: Movie does not exist.';
    END IF;

END $$



-- movie must exist already, and star can be new/existing
CREATE PROCEDURE link_star_and_movie(IN starName VARCHAR(100),
                                    IN movieTitle VARCHAR(100),
                                    OUT starId VARCHAR(10),
                                    OUT movieId VARCHAR(10),
                                    OUT errorMessage VARCHAR(35))
BEGIN
    DECLARE existingMovieId VARCHAR(10);
    DECLARE returnedStarId VARCHAR(10);
    DECLARE foundPairAlready VARCHAR(10); -- if this gets set, the pair is already in the table

    SELECT id INTO existingMovieId FROM movies WHERE title = movieTitle LIMIT 1;
    IF existingMovieId IS NOT NULL THEN
        SET movieId = existingMovieId;
        -- now check if the star exists, if not create it, then add to stars_in_movies table
        -- get the id of the first star that matches with the name (if one exists)
        SELECT id INTO returnedStarId FROM stars WHERE name = starName LIMIT 1;
        IF returnedStarId IS NOT NULL THEN
            SET starId = returnedStarId;
        ELSE
            CALL add_star(starName, NULL, @newStarId);
            SET returnedStarId = @newStarId;
            SET starId = @newStarId;
        END IF;
        -- check if the movieId and starId are already in this table
        SELECT starId INTO foundPairAlready FROM stars_in_movies WHERE starId = returnedStarId AND movieId = existingMovieId LIMIT 1;
        IF foundPairAlready IS NOT NULL THEN
            SET errorMessage = 'Error: Pair exists already.';
        ELSE
            INSERT INTO stars_in_movies (starId, movieId) VALUES (returnedStarId, existingMovieId);
        END IF;
    ELSE
        SET errorMessage = 'Error: Movie does not exist.';
    END IF;

END $$



-- genre name with movie id
CREATE PROCEDURE link_genre_and_movie_id(IN givenGenreName VARCHAR(32),
                                      IN givenMovieId VARCHAR(10),
                                      OUT outGenreId INTEGER,
                                      OUT errorMessage VARCHAR(35))
BEGIN
    DECLARE existingMovieId VARCHAR(10);
    DECLARE returnedGenreId VARCHAR(10);
    DECLARE foundPairAlready VARCHAR(10); -- if this gets set, the pair is already in the table

    SELECT id INTO existingMovieId FROM movies WHERE id = givenMovieId LIMIT 1;
    IF existingMovieId IS NOT NULL THEN
        -- add_genre will return the id of the existing or create a new one if it doesn't
        CALL add_genre(givenGenreName, @genreId, @alreadyExists);
        SET returnedGenreId = @genreId;
        SET outGenreId = @genreId;

        -- check if the genreId and movieId are already in this table
        SELECT movieId INTO foundPairAlready FROM genres_in_movies WHERE genreId = returnedGenreId AND movieId = givenMovieId LIMIT 1;
        IF foundPairAlready IS NOT NULL THEN
            SET errorMessage = 'Error: Pair exists already.';
        ELSE
            INSERT INTO genres_in_movies (genreId, movieId) VALUES (returnedGenreId, givenMovieId);
        END IF;
    ELSE
        SET errorMessage = 'Error: Movie does not exist.';
    END IF;
END $$



-- movie must exist already, and genre can be new/existing
CREATE PROCEDURE link_genre_and_movie(IN genreName VARCHAR(100),
                                     IN movieTitle VARCHAR(100),
                                     OUT genreId INTEGER,
                                     OUT movieId VARCHAR(10),
                                     OUT errorMessage VARCHAR(35))
BEGIN
    DECLARE existingMovieId VARCHAR(10);
    DECLARE returnedGenreId VARCHAR(10);
    DECLARE foundPairAlready VARCHAR(10); -- if this gets set, the pair is already in the table

    SELECT id INTO existingMovieId FROM movies WHERE title = movieTitle LIMIT 1;
    IF existingMovieId IS NOT NULL THEN
        SET movieId = existingMovieId;
        -- add_genre will return the id of the existing or create a new one if it doesn't
        CALL add_genre(genreName, @genreId, @alreadyExists);
        SET returnedGenreId = @genreId;
        SET genreId = @genreId;

        -- check if the genreId and movieId are already in this table
        SELECT movieId INTO foundPairAlready FROM genres_in_movies WHERE genreId = returnedGenreId AND movieId = existingMovieId LIMIT 1;
        IF foundPairAlready IS NOT NULL THEN
            SET errorMessage = 'Error: Pair exists already.';
        ELSE
            INSERT INTO genres_in_movies (genreId, movieId) VALUES (returnedGenreId, existingMovieId);
        END IF;
    ELSE
        SET errorMessage = 'Error: Movie does not exist.';
    END IF;
END $$

DELIMITER ;
