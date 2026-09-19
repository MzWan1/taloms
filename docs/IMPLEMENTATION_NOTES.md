# Implementation notes
- Database migrations and JPA entities can diverge after schema evolution; this working tree adds the Chief-Authority join table (V51) that diagrams reflect.
- Reported routes and checks come from the working tree; the deployed database may lack recent columns until migrations run.
- Validate service-level uniqueness against the source; the ERD lists notable UNIQUE constraints, not every index.
- Page-route guards may differ from REST checks; the Word document cites the applicable route for each use case.
- Audit and API usage logging are best-effort asynchronous records; no delivery or retention guarantee is implied.
