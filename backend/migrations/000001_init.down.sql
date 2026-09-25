-- Desfà la migració inicial. L'ordre és l'invers de la creació perquè
-- ratings depèn de movies i users (claus foranes).
DROP TABLE IF EXISTS ratings;
DROP TABLE IF EXISTS movies;
DROP TABLE IF EXISTS users;
