-- Un usuari registrat només pot tenir UNA valoració per pel·lícula (després
-- la pot editar). Índex únic PARCIAL: només s'aplica a les files amb
-- user_id; les valoracions anònimes (user_id NULL) no tenen aquest límit
-- perquè no es poden identificar.
CREATE UNIQUE INDEX ratings_movie_user_uniq ON ratings (movie_id, user_id) WHERE user_id IS NOT NULL;
